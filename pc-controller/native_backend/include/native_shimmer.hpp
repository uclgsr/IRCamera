

#pragma once

#include <atomic>
#include <memory>
#include <string>
#include <thread>
#include <vector>
#include <functional>

#include "thread_safe_queue.hpp"

namespace ircamera {
namespace sensors {

struct ShimmerData {
    uint64_t timestamp_ns;  // Nanosecond timestamp
    uint16_t gsr_raw;       // Raw GSR value (12-bit ADC)
    double gsr_microsiemens; // Converted GSR value
    uint16_t ppg_raw;       // Raw PPG value
    int16_t accel_x, accel_y, accel_z; // Accelerometer data
    bool valid;             // Data validity flag
};

class NativeShimmer {
public:
    NativeShimmer();
    ~NativeShimmer();

    // Non-copyable
    NativeShimmer(const NativeShimmer&) = delete;
    NativeShimmer& operator=(const NativeShimmer&) = delete;


    bool connect(const std::string& port_name, int baud_rate = 115200);


    void disconnect();


    bool start_streaming(int sample_rate = 512);


    bool stop_streaming();


    bool is_connected() const { return connected_.load(); }


    bool is_streaming() const { return streaming_.load(); }


    bool get_data(ShimmerData& data);


    size_t get_queue_size() const;


    void clear_queue();


    std::string get_sensor_info() const;


    void set_gsr_range(int range);


    std::string get_last_error() const { return last_error_; }


    struct Statistics {
        uint64_t total_samples;
        uint64_t dropped_samples;
        double data_rate_hz;
        uint64_t last_timestamp_ns;
    };
    
    Statistics get_statistics() const;

private:
    // Connection state
    std::atomic<bool> connected_{false};
    std::atomic<bool> streaming_{false};
    std::atomic<bool> should_stop_{false};

    // Serial communication
    int serial_fd_{-1};
    std::string port_name_;
    int baud_rate_;

    // Data processing thread
    std::unique_ptr<std::thread> reader_thread_;
    
    // Data queue
    std::unique_ptr<utils::ThreadSafeQueue<ShimmerData>> data_queue_;

    // GSR calibration parameters
    double gsr_uncal_limit_[5] = {40.5, 287.0, 1498.0, 2895.0, 3660.0};
    double gsr_cal_limit_[5] = {0.1, 1.0, 10.0, 100.0, 1000.0};
    int gsr_range_{4}; // Default to highest range

    // Statistics
    mutable std::atomic<uint64_t> total_samples_{0};
    mutable std::atomic<uint64_t> dropped_samples_{0};
    mutable std::atomic<uint64_t> last_timestamp_ns_{0};

    // Error tracking
    mutable std::string last_error_;

    // Private methods
    void reader_thread_func();
    bool setup_serial_port();
    bool send_command(uint8_t command);
    bool parse_data_packet(const std::vector<uint8_t>& packet, ShimmerData& data);
    double convert_gsr_to_microsiemens(uint16_t raw_value);
    uint64_t get_current_timestamp_ns();
    void update_statistics();
};

} // namespace sensors
} // namespace ircamera