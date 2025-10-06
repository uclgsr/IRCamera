#include "shimmer.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <algorithm>
#include <cmath>
#include <numeric>
#include <atomic>
#include <mutex>

#ifdef _WIN32
#include <windows.h>
#include <setupapi.h>
#else

#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <dirent.h>

#endif

namespace ircamera {

// Implementation class for enhanced Shimmer
    class EnhancedShimmer::Impl {
    public:
        explicit Impl(const std::string &port_name)
                : port_name_(port_name), is_connected_(false), is_streaming_(false),
                  sampling_rate_(128), gsr_range_(4), ppg_enabled_(true),
                  packets_processed_(0), packets_dropped_(0), data_callback_(nullptr),
                  error_callback_(nullptr) {

            // Initialize calibration parameters
            gsr_calibration_factor_ = 1.0 / 4095.0;
            gsr_ref_voltage_ = 3.0;
            gsr_gain_ = 5.0;

            last_packet_time_ = std::chrono::steady_clock::now();
        }

        ~Impl() {
            disconnect();
        }

        bool connect(const std::string &port_name) {
            if (is_connected_) {
                return true;
            }

            std::string port = port_name.empty() ? port_name_ : port_name;
            if (port.empty()) {
                port = detect_shimmer_port();
            }

            if (port.empty()) {
                if (error_callback_) {
                    error_callback_("No Shimmer device detected");
                }
                return false;
            }

            // Simulate connection for demonstration
            // In real implementation, this would open serial port
            std::this_thread::sleep_for(std::chrono::milliseconds(100));

            is_connected_ = true;
            port_name_ = port;

            // Start background thread for data simulation
            should_stop_ = false;
            data_thread_ = std::thread(&Impl::data_thread_function, this);

            return true;
        }

        void disconnect() {
            if (!is_connected_) {
                return;
            }

            stop_streaming();

            should_stop_ = true;
            if (data_thread_.joinable()) {
                data_thread_.join();
            }

            is_connected_ = false;
        }

        bool is_connected() const {
            return is_connected_;
        }

        bool set_sampling_rate(int rate_hz) {
            if (rate_hz < 1 || rate_hz > 512) {
                return false;
            }
            sampling_rate_ = rate_hz;
            return true;
        }

        bool set_gsr_range(int range) {
            if (range < 1 || range > 7) {
                return false;
            }
            gsr_range_ = range;
            return true;
        }

        bool enable_ppg(bool enable) {
            ppg_enabled_ = enable;
            return true;
        }

        bool start_streaming() {
            if (!is_connected_) {
                return false;
            }

            is_streaming_ = true;
            packets_processed_ = 0;
            packets_dropped_ = 0;
            last_packet_time_ = std::chrono::steady_clock::now();

            return true;
        }

        bool stop_streaming() {
            is_streaming_ = false;
            return true;
        }

        bool is_streaming() const {
            return is_streaming_;
        }

        void set_gsr_callback(GSRDataCallback callback) {
            std::lock_guard <std::mutex> lock(callback_mutex_);
            data_callback_ = callback;
        }

        void set_error_callback(ErrorCallback callback) {
            std::lock_guard <std::mutex> lock(callback_mutex_);
            error_callback_ = callback;
        }

        size_t get_packets_processed() const {
            return packets_processed_;
        }

        size_t get_packets_dropped() const {
            return packets_dropped_;
        }

        double get_data_rate_hz() const {
            return static_cast<double>(sampling_rate_);
        }

        std::vector <GSRData> process_raw_packet(const std::vector <uint8_t> &packet) {
            std::vector <GSRData> result;

            // Validate packet size (simplified)
            if (packet.size() < 8) {
                packets_dropped_++;
                return result;
            }

            GSRData data;
            data.timestamp_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(
                    std::chrono::steady_clock::now().time_since_epoch()).count();

            // Parse raw GSR value (16-bit)
            data.raw_gsr_value = (packet[1] << 8) | packet[0];

            // Convert to microsiemens
            double voltage = (data.raw_gsr_value * gsr_ref_voltage_) / 4095.0;
            data.gsr_microsiemens = (voltage * 1000000.0) / gsr_gain_;

            // Parse PPG if enabled
            if (ppg_enabled_ && packet.size() >= 12) {
                data.raw_ppg_value = (packet[3] << 8) | packet[2];
                data.ppg_normalized = data.raw_ppg_value / 4095.0;
            }

            data.packet_sequence = packets_processed_++;

            result.push_back(data);
            return result;
        }

        std::string get_firmware_version() const {
            return "Shimmer3-1.0.0";
        }

        std::string get_device_id() const {
            return "SHM-" + port_name_;
        }

        int get_sampling_rate() const {
            return sampling_rate_;
        }

    private:
        std::string detect_shimmer_port() {
            // Simulate port detection
            return "/dev/ttyUSB0";  // Default port for Linux
        }

        void data_thread_function() {
            const auto interval = std::chrono::microseconds(1000000 / sampling_rate_);
            auto next_time = std::chrono::steady_clock::now();

            while (!should_stop_) {
                if (is_streaming_) {
                    // Generate simulated GSR data
                    generate_simulated_data();
                }

                next_time += interval;
                std::this_thread::sleep_until(next_time);
            }
        }

        void generate_simulated_data() {
            static uint32_t sequence = 0;
            static double phase = 0.0;

            GSRData data;
            data.timestamp_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(
                    std::chrono::steady_clock::now().time_since_epoch()).count();

            // Generate realistic GSR simulation with baseline + noise + slow drift
            double baseline = 500.0;  // μS
            double noise = (rand() / static_cast<double>(RAND_MAX) - 0.5) * 50.0;
            double slow_wave = 100.0 * std::sin(phase * 0.001);  // Slow breathing-like component
            double fast_wave = 20.0 * std::sin(phase * 0.1);     // Faster cardiac component

            data.gsr_microsiemens = baseline + slow_wave + fast_wave + noise;

            // Convert back to raw value for consistency
            data.raw_gsr_value = static_cast<uint16_t>(
                    (data.gsr_microsiemens * gsr_gain_ / 1000000.0) * 4095.0 / gsr_ref_voltage_);

            // Generate PPG data if enabled
            if (ppg_enabled_) {
                double ppg_signal =
                        0.7 + 0.2 * std::sin(phase * 0.05) + 0.05 * std::sin(phase * 0.5);
                data.ppg_normalized = std::max(0.0, std::min(1.0, ppg_signal));
                data.raw_ppg_value = static_cast<uint16_t>(data.ppg_normalized * 4095.0);
            }

            data.packet_sequence = sequence++;

            phase += 1.0;
            packets_processed_++;

            // Call callback if set
            std::lock_guard <std::mutex> lock(callback_mutex_);
            if (data_callback_) {
                data_callback_(data);
            }
        }

        std::string port_name_;
        bool is_connected_;
        bool is_streaming_;
        int sampling_rate_;
        int gsr_range_;
        bool ppg_enabled_;

        // Calibration parameters
        double gsr_calibration_factor_;
        double gsr_ref_voltage_;
        double gsr_gain_;

        // Statistics
        std::atomic <size_t> packets_processed_;
        std::atomic <size_t> packets_dropped_;
        std::chrono::steady_clock::time_point last_packet_time_;

        // Threading
        std::thread data_thread_;
        std::atomic<bool> should_stop_;
        std::mutex callback_mutex_;

        // Callbacks
        GSRDataCallback data_callback_;
        ErrorCallback error_callback_;
    };

// EnhancedShimmer public interface implementation
    EnhancedShimmer::EnhancedShimmer(const std::string &port_name)
            : pimpl_(std::make_unique<Impl>(port_name)) {
    }

    EnhancedShimmer::~EnhancedShimmer() = default;

    bool EnhancedShimmer::connect(const std::string &port_name) {
        return pimpl_->connect(port_name);
    }

    void EnhancedShimmer::disconnect() {
        pimpl_->disconnect();
    }

    bool EnhancedShimmer::is_connected() const {
        return pimpl_->is_connected();
    }

    bool EnhancedShimmer::set_sampling_rate(int rate_hz) {
        return pimpl_->set_sampling_rate(rate_hz);
    }

    bool EnhancedShimmer::set_gsr_range(int range) {
        return pimpl_->set_gsr_range(range);
    }

    bool EnhancedShimmer::enable_ppg(bool enable) {
        return pimpl_->enable_ppg(enable);
    }

    bool EnhancedShimmer::start_streaming() {
        return pimpl_->start_streaming();
    }

    bool EnhancedShimmer::stop_streaming() {
        return pimpl_->stop_streaming();
    }

    bool EnhancedShimmer::is_streaming() const {
        return pimpl_->is_streaming();
    }

    void EnhancedShimmer::set_gsr_callback(GSRDataCallback callback) {
        pimpl_->set_gsr_callback(callback);
    }

    void EnhancedShimmer::set_error_callback(ErrorCallback callback) {
        pimpl_->set_error_callback(callback);
    }

    size_t EnhancedShimmer::get_packets_processed() const {
        return pimpl_->get_packets_processed();
    }

    size_t EnhancedShimmer::get_packets_dropped() const {
        return pimpl_->get_packets_dropped();
    }

    double EnhancedShimmer::get_data_rate_hz() const {
        return pimpl_->get_data_rate_hz();
    }

    std::vector <GSRData> EnhancedShimmer::process_raw_packet(const std::vector <uint8_t> &packet) {
        return pimpl_->process_raw_packet(packet);
    }

    std::string EnhancedShimmer::get_firmware_version() const {
        return pimpl_->get_firmware_version();
    }

    std::string EnhancedShimmer::get_device_id() const {
        return pimpl_->get_device_id();
    }

    int EnhancedShimmer::get_sampling_rate() const {
        return pimpl_->get_sampling_rate();
    }

// Processing utility functions implementation
    namespace processing {

        std::vector<double> apply_lowpass_filter(const std::vector<double> &data, double cutoff_hz,
                                                 double sample_rate) {
            if (data.empty()) return data;

            // Simple single-pole IIR lowpass filter
            double alpha = cutoff_hz / (cutoff_hz + sample_rate);
            std::vector<double> filtered(data.size());

            filtered[0] = data[0];
            for (size_t i = 1; i < data.size(); ++i) {
                filtered[i] = alpha * data[i] + (1.0 - alpha) * filtered[i - 1];
            }

            return filtered;
        }

        std::vector<double> apply_highpass_filter(const std::vector<double> &data, double cutoff_hz,
                                                  double sample_rate) {
            if (data.empty()) return data;

            // Simple single-pole IIR highpass filter
            double alpha = sample_rate / (cutoff_hz + sample_rate);
            std::vector<double> filtered(data.size());

            filtered[0] = data[0];
            for (size_t i = 1; i < data.size(); ++i) {
                filtered[i] = alpha * (filtered[i - 1] + data[i] - data[i - 1]);
            }

            return filtered;
        }

        std::vector<double>
        apply_notch_filter(const std::vector<double> &data, double notch_hz, double sample_rate) {
            // Simplified notch filter - in practice would use proper IIR design
            return apply_lowpass_filter(data, notch_hz * 0.9, sample_rate);
        }

        double calculate_mean(const std::vector<double> &data) {
            if (data.empty()) return 0.0;
            return std::accumulate(data.begin(), data.end(), 0.0) / data.size();
        }

        double calculate_std(const std::vector<double> &data) {
            if (data.size() < 2) return 0.0;

            double mean = calculate_mean(data);
            double sq_sum = std::accumulate(data.begin(), data.end(), 0.0,
                                            [mean](double a, double b) {
                                                return a + (b - mean) *
                                                           (b - mean);
                                            });

            return std::sqrt(sq_sum / (data.size() - 1));
        }

        double calculate_rms(const std::vector<double> &data) {
            if (data.empty()) return 0.0;

            double sq_sum = std::accumulate(data.begin(), data.end(), 0.0,
                                            [](double a, double b) { return a + b * b; });

            return std::sqrt(sq_sum / data.size());
        }

        std::vector<bool>
        detect_motion_artifacts(const std::vector<double> &gsr_data, double threshold) {
            std::vector<bool> artifacts(gsr_data.size(), false);

            if (gsr_data.size() < 3) return artifacts;

            // Detect sudden changes (derivative-based)
            for (size_t i = 1; i < gsr_data.size() - 1; ++i) {
                double derivative = std::abs(gsr_data[i + 1] - gsr_data[i - 1]) / 2.0;
                if (derivative > threshold) {
                    artifacts[i] = true;
                }
            }

            return artifacts;
        }

        std::vector<bool> detect_electrical_artifacts(const std::vector<double> &gsr_data) {
            std::vector<bool> artifacts(gsr_data.size(), false);

            // Detect values outside reasonable physiological range
            for (size_t i = 0; i < gsr_data.size(); ++i) {
                if (gsr_data[i] < 0.1 || gsr_data[i] > 100.0) {  // μS
                    artifacts[i] = true;
                }
            }

            return artifacts;
        }

        bool validate_gsr_packet(const std::vector <uint8_t> &packet) {
            // Basic packet validation
            return packet.size() >= 8 && packet.size() <= 32;
        }

        double calculate_signal_quality(const std::vector<double> &data) {
            if (data.size() < 10) return 0.0;

            // Simple quality metric based on signal-to-noise ratio
            double mean = calculate_mean(data);
            double std_dev = calculate_std(data);

            if (std_dev == 0.0) return 0.0;

            // Quality score between 0 and 1
            double snr = mean / std_dev;
            return std::min(1.0, std::max(0.0, snr / 10.0));
        }

    } // namespace processing

} // namespace ircamera
