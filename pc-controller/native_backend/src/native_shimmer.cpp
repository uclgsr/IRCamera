#include "native_shimmer.h"
#include <iostream>
#include <chrono>
#include <iomanip>
#include <sstream>

#ifdef _WIN32
#include <windows.h>
#include <setupapi.h>
#include <devguid.h>
#pragma comment(lib, "setupapi.lib")
#else
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <sys/ioctl.h>
#endif

namespace ircamera {


class NativeShimmer::Impl {
public:
    explicit Impl(const std::string& port_name)
        : port_name_(port_name)
        , is_connected_(false)
        , is_streaming_(false)
        , sampling_rate_(128)  // Default 128Hz for GSR
        , gsr_range_(4)        // Default range
        , should_stop_(false)
        , data_callback_(nullptr)
        , packet_sequence_(0)
    {
        // Initialize GSR calibration constants
        // These are typical values for Shimmer3 GSR+
        gsr_calibration_factor_ = 1.0 / 4095.0;  // 12-bit ADC
        gsr_ref_voltage_ = 3.0;  // Reference voltage
        gsr_gain_ = 5.0;         // Amplifier gain
    }
    
    ~Impl() {
        disconnect();
    }
    
    bool connect(const std::string& port_name) {
        if (is_connected_) {
            return true;
        }
        
        std::string port = port_name.empty() ? port_name_ : port_name;
        if (port.empty()) {
            port = detect_shimmer_port();
        }
        
        if (port.empty()) {
            last_error_ = "No Shimmer device detected";
            return false;
        }
        
        // Platform-specific serial port opening
#ifdef _WIN32
        port_handle_ = CreateFileA(
            port.c_str(),
            GENERIC_READ | GENERIC_WRITE,
            0,
            nullptr,
            OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL,
            nullptr
        );
        
        if (port_handle_ == INVALID_HANDLE_VALUE) {
            last_error_ = "Failed to open serial port: " + port;
            return false;
        }
        
        // Configure serial port
        DCB dcb = {};
        dcb.DCBlength = sizeof(dcb);
        if (!GetCommState(port_handle_, &dcb)) {
            last_error_ = "Failed to get comm state";
            CloseHandle(port_handle_);
            return false;
        }
        
        dcb.BaudRate = CBR_115200;
        dcb.ByteSize = 8;
        dcb.Parity = NOPARITY;
        dcb.StopBits = ONESTOPBIT;
        
        if (!SetCommState(port_handle_, &dcb)) {
            last_error_ = "Failed to set comm state";
            CloseHandle(port_handle_);
            return false;
        }
        
#else
        port_fd_ = open(port.c_str(), O_RDWR | O_NOCTTY | O_SYNC);
        if (port_fd_ < 0) {
            last_error_ = "Failed to open serial port: " + port;
            return false;
        }
        
        // Configure serial port
        struct termios tty;
        if (tcgetattr(port_fd_, &tty) != 0) {
            last_error_ = "Failed to get terminal attributes";
            close(port_fd_);
            return false;
        }
        
        cfsetospeed(&tty, B115200);
        cfsetispeed(&tty, B115200);
        
        tty.c_cflag = (tty.c_cflag & ~CSIZE) | CS8;  // 8-bit chars
        tty.c_iflag &= ~IGNBRK;         // disable break processing
        tty.c_lflag = 0;                // no signaling chars, no echo,
                                        // no canonical processing
        tty.c_oflag = 0;                // no remapping, no delays
        tty.c_cc[VMIN]  = 0;            // read doesn't block
        tty.c_cc[VTIME] = 5;            // 0.5 seconds read timeout
        
        tty.c_iflag &= ~(IXON | IXOFF | IXANY); // shut off xon/xoff ctrl
        tty.c_cflag |= (CLOCAL | CREAD);// ignore modem controls,
                                        // enable reading
        tty.c_cflag &= ~(PARENB | PARODD);      // shut off parity
        tty.c_cflag &= ~CSTOPB;
        tty.c_cflag &= ~CRTSCTS;
        
        if (tcsetattr(port_fd_, TCSANOW, &tty) != 0) {
            last_error_ = "Failed to set terminal attributes";
            close(port_fd_);
            return false;
        }
#endif
        
        is_connected_ = true;
        port_name_ = port;
        
        // Send inquiry command to verify Shimmer connection
        if (!send_inquiry_command()) {
            disconnect();
            return false;
        }
        
        return true;
    }
    
    void disconnect() {
        if (!is_connected_) {
            return;
        }
        
        stop_streaming();
        
#ifdef _WIN32
        if (port_handle_ != INVALID_HANDLE_VALUE) {
            CloseHandle(port_handle_);
            port_handle_ = INVALID_HANDLE_VALUE;
        }
#else
        if (port_fd_ >= 0) {
            close(port_fd_);
            port_fd_ = -1;
        }
#endif
        
        is_connected_ = false;
    }
    
    bool start_streaming() {
        if (!is_connected_ || is_streaming_) {
            return false;
        }
        
        // Send start streaming command (0x07)
        uint8_t start_cmd = 0x07;
        if (!write_command(&start_cmd, 1)) {
            last_error_ = "Failed to send start command";
            return false;
        }
        
        // Start data acquisition thread
        should_stop_ = false;
        data_thread_ = std::thread(&Impl::data_acquisition_loop, this);
        
        is_streaming_ = true;
        return true;
    }
    
    bool stop_streaming() {
        if (!is_streaming_) {
            return true;
        }
        
        // Send stop streaming command (0x20)
        uint8_t stop_cmd = 0x20;
        if (is_connected_) {
            write_command(&stop_cmd, 1);
        }
        
        // Stop data acquisition thread
        should_stop_ = true;
        if (data_thread_.joinable()) {
            data_thread_.join();
        }
        
        is_streaming_ = false;
        return true;
    }
    
    bool set_sampling_rate(int rate_hz) {
        // Shimmer3 GSR+ supports 1-1000Hz typically
        if (rate_hz < 1 || rate_hz > 1000) {
            last_error_ = "Invalid sampling rate. Must be 1-1000 Hz";
            return false;
        }
        
        sampling_rate_ = rate_hz;
        
        // If connected, send sampling rate command
        if (is_connected_) {
            // Shimmer sampling rate command format
            uint8_t cmd[] = {0x05, static_cast<uint8_t>(rate_hz & 0xFF), 
                            static_cast<uint8_t>((rate_hz >> 8) & 0xFF)};
            return write_command(cmd, sizeof(cmd));
        }
        
        return true;
    }
    
    std::vector<GSRData> get_buffered_data() {
        std::lock_guard<std::mutex> lock(data_mutex_);
        std::vector<GSRData> result;
        
        while (!data_queue_.empty()) {
            result.push_back(data_queue_.front());
            data_queue_.pop();
        }
        
        return result;
    }
    
    void clear_buffer() {
        std::lock_guard<std::mutex> lock(data_mutex_);
        std::queue<GSRData> empty;
        data_queue_.swap(empty);
    }
    
    std::string get_device_info() const {
        std::stringstream ss;
        ss << "Shimmer3 GSR+ Device" << std::endl;
        ss << "Port: " << port_name_ << std::endl;
        ss << "Connected: " << (is_connected_ ? "Yes" : "No") << std::endl;
        ss << "Streaming: " << (is_streaming_ ? "Yes" : "No") << std::endl;
        ss << "Sampling Rate: " << sampling_rate_ << " Hz" << std::endl;
        ss << "GSR Range: " << gsr_range_ << std::endl;
        return ss.str();
    }
    
    bool perform_self_test() {
        if (!is_connected_) {
            last_error_ = "Device not connected";
            return false;
        }
        
        // Send inquiry command and check response
        return send_inquiry_command();
    }
    
private:
    std::string detect_shimmer_port() {
        // Platform-specific code to detect Shimmer devices
#ifdef _WIN32
        // Use Windows SetupAPI to enumerate COM ports
        HDEVINFO device_info_set = SetupDiGetClassDevs(
            &GUID_DEVCLASS_PORTS, nullptr, nullptr, DIGCF_PRESENT);
        
        if (device_info_set == INVALID_HANDLE_VALUE) {
            return "";
        }
        
        SP_DEVINFO_DATA device_info_data;
        device_info_data.cbSize = sizeof(SP_DEVINFO_DATA);
        
        for (DWORD i = 0; SetupDiEnumDeviceInfo(device_info_set, i, &device_info_data); i++) {
            char device_desc[256];
            if (SetupDiGetDeviceRegistryPropertyA(
                device_info_set, &device_info_data, SPDRP_DEVICEDESC,
                nullptr, (PBYTE)device_desc, sizeof(device_desc), nullptr)) {
                
                std::string desc(device_desc);
                if (desc.find("Shimmer") != std::string::npos) {
                    // Found a Shimmer device, get the COM port name
                    char friendly_name[256];
                    if (SetupDiGetDeviceRegistryPropertyA(
                        device_info_set, &device_info_data, SPDRP_FRIENDLYNAME,
                        nullptr, (PBYTE)friendly_name, sizeof(friendly_name), nullptr)) {
                        
                        std::string name(friendly_name);
                        // Extract COM port number from friendly name
                        size_t com_pos = name.find("COM");
                        if (com_pos != std::string::npos) {
                            size_t end_pos = name.find(")", com_pos);
                            if (end_pos != std::string::npos) {
                                std::string port = name.substr(com_pos, end_pos - com_pos);
                                SetupDiDestroyDeviceInfoList(device_info_set);
                                return "\\\\.\\" + port;  // Windows COM port format
                            }
                        }
                    }
                }
            }
        }
        
        SetupDiDestroyDeviceInfoList(device_info_set);
        return "COM3";  // Default fallback
        
#else
        // Linux/macOS: Check common USB serial device paths
        std::vector<std::string> possible_ports = {
            "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2",
            "/dev/ttyACM0", "/dev/ttyACM1", "/dev/ttyACM2",
            "/dev/cu.usbserial-*", "/dev/cu.usbmodem*"
        };
        
        for (const auto& port : possible_ports) {
            int fd = open(port.c_str(), O_RDWR | O_NOCTTY | O_NONBLOCK);
            if (fd >= 0) {
                close(fd);
                return port;  // Return first available port
            }
        }
        
        return "/dev/ttyUSB0";  // Default fallback
#endif
    }
    
    bool send_inquiry_command() {
        // Send Shimmer inquiry command (0x01)
        uint8_t inquiry_cmd = 0x01;
        if (!write_command(&inquiry_cmd, 1)) {
            last_error_ = "Failed to send inquiry command";
            return false;
        }
        
        // Wait for response
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        
        uint8_t response[256];
        int bytes_read = read_data(response, sizeof(response));
        
        if (bytes_read > 0) {
            // Check for valid Shimmer response
            // Typically starts with device ID and contains "Shimmer" identifier
            std::string response_str(reinterpret_cast<char*>(response), bytes_read);
            return response_str.find("Shimmer") != std::string::npos;
        }
        
        return false;
    }
    
    bool write_command(const uint8_t* data, size_t length) {
        if (!is_connected_) {
            return false;
        }
        
#ifdef _WIN32
        DWORD bytes_written;
        return WriteFile(port_handle_, data, static_cast<DWORD>(length), 
                        &bytes_written, nullptr) && 
               bytes_written == length;
#else
        return write(port_fd_, data, length) == static_cast<ssize_t>(length);
#endif
    }
    
    int read_data(uint8_t* buffer, size_t buffer_size) {
        if (!is_connected_) {
            return -1;
        }
        
#ifdef _WIN32
        DWORD bytes_read;
        if (ReadFile(port_handle_, buffer, static_cast<DWORD>(buffer_size), 
                    &bytes_read, nullptr)) {
            return static_cast<int>(bytes_read);
        }
        return -1;
#else
        return read(port_fd_, buffer, buffer_size);
#endif
    }
    
    void data_acquisition_loop() {
        uint8_t buffer[1024];
        
        while (!should_stop_) {
            int bytes_read = read_data(buffer, sizeof(buffer));
            
            if (bytes_read > 0) {
                parse_shimmer_data(buffer, bytes_read);
            }
            
            // Small delay to prevent CPU spinning
            std::this_thread::sleep_for(std::chrono::microseconds(100));
        }
    }
    
    void parse_shimmer_data(const uint8_t* data, int length) {
        // Parse Shimmer3 GSR+ data packets
        // Typical packet format: [Header][Timestamp][GSR][PPG][Checksum]
        
        for (int i = 0; i < length; ) {
            // Look for packet header (typically 0xA0 for data packet)
            if (i + 7 < length && data[i] == 0xA0) {
                GSRData gsr_data;
                
                // Extract timestamp (nanoseconds since epoch)
                gsr_data.timestamp_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(
                    std::chrono::high_resolution_clock::now().time_since_epoch()).count();
                
                // Extract 12-bit GSR value (CRITICAL: must use 12-bit range 0-4095)
                uint16_t raw_gsr = (data[i+2] << 8) | data[i+3];
                gsr_data.raw_gsr_value = raw_gsr & 0x0FFF;  // Mask to 12 bits
                
                // Convert to microsiemens using 12-bit ADC formula
                double voltage = (gsr_data.raw_gsr_value / 4095.0) * gsr_ref_voltage_;
                double conductance = voltage / (gsr_gain_ * 100000.0);  // 100kΩ reference
                gsr_data.gsr_microsiemens = conductance * 1000000.0;  // Convert to µS
                
                // Extract PPG value
                uint16_t raw_ppg = (data[i+4] << 8) | data[i+5];
                gsr_data.raw_ppg_value = raw_ppg;
                gsr_data.ppg_normalized = raw_ppg / 4095.0;
                
                gsr_data.packet_sequence = packet_sequence_++;
                
                // Add to queue (thread-safe)
                {
                    std::lock_guard<std::mutex> lock(data_mutex_);
                    data_queue_.push(gsr_data);
                    
                    // Limit queue size to prevent memory issues
                    if (data_queue_.size() > 10000) {
                        data_queue_.pop();
                    }
                }
                
                // Call callback if set
                if (data_callback_) {
                    data_callback_(gsr_data);
                }
                
                i += 7;  // Move to next packet
            } else {
                i++;  // Search for next header
            }
        }
    }
    
    // Member variables
    std::string port_name_;
    std::atomic<bool> is_connected_;
    std::atomic<bool> is_streaming_;
    std::atomic<bool> should_stop_;
    int sampling_rate_;
    int gsr_range_;
    std::string last_error_;
    
    // GSR calibration constants
    double gsr_calibration_factor_;
    double gsr_ref_voltage_;
    double gsr_gain_;
    
    // Threading
    std::thread data_thread_;
    std::mutex data_mutex_;
    std::queue<GSRData> data_queue_;
    DataCallback data_callback_;
    
    uint8_t packet_sequence_;
    
    // Platform-specific handles
#ifdef _WIN32
    HANDLE port_handle_ = INVALID_HANDLE_VALUE;
#else
    int port_fd_ = -1;
#endif
};

// NativeShimmer public interface implementation

NativeShimmer::NativeShimmer(const std::string& port_name)
    : pimpl(std::make_unique<Impl>(port_name)) {
}

NativeShimmer::~NativeShimmer() = default;

bool NativeShimmer::connect(const std::string& port_name) {
    return pimpl->connect(port_name);
}

void NativeShimmer::disconnect() {
    pimpl->disconnect();
}

bool NativeShimmer::is_connected() const {
    return pimpl->is_connected_;
}

bool NativeShimmer::start_streaming() {
    return pimpl->start_streaming();
}

bool NativeShimmer::stop_streaming() {
    return pimpl->stop_streaming();
}

bool NativeShimmer::is_streaming() const {
    return pimpl->is_streaming_;
}

bool NativeShimmer::set_sampling_rate(int rate_hz) {
    return pimpl->set_sampling_rate(rate_hz);
}

int NativeShimmer::get_sampling_rate() const {
    return pimpl->sampling_rate_;
}

bool NativeShimmer::set_gsr_range(int range) {
    pimpl->gsr_range_ = range;
    return true;
}

bool NativeShimmer::calibrate_gsr(double known_resistance_ohms) {
    // Implement GSR calibration logic
    return true;
}

void NativeShimmer::set_data_callback(DataCallback callback) {
    pimpl->data_callback_ = callback;
}

std::vector<GSRData> NativeShimmer::get_buffered_data() {
    return pimpl->get_buffered_data();
}

void NativeShimmer::clear_buffer() {
    pimpl->clear_buffer();
}

std::string NativeShimmer::get_device_info() const {
    return pimpl->get_device_info();
}

std::string NativeShimmer::get_last_error() const {
    return pimpl->last_error_;
}

bool NativeShimmer::perform_self_test() {
    return pimpl->perform_self_test();
}

} // namespace ircamera