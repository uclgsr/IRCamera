#pragma once

#include <memory>
#include <string>
#include <vector>
#include <thread>
#include <queue>
#include <mutex>
#include <atomic>
#include <functional>

namespace ircamera {

    struct GSRData {
        uint64_t timestamp_ns;      // Nanosecond timestamp
        uint16_t raw_gsr_value;     // Raw 12-bit ADC value (0-4095)
        double gsr_microsiemens;    // Converted GSR value in microsiemens
        uint16_t raw_ppg_value;     // Raw PPG value
        double ppg_normalized;      // Normalized PPG value
        uint8_t packet_sequence;    // Packet sequence number for sync
    };

    class NativeShimmer {
    public:
        using DataCallback = std::function<void(const GSRData &)>;

        explicit NativeShimmer(const std::string &port_name = "");

        ~NativeShimmer();

        bool connect(const std::string &port_name = "");

        void disconnect();

        bool is_connected() const;

        bool start_streaming();

        bool stop_streaming();

        bool is_streaming() const;

        bool set_sampling_rate(int rate_hz);

        int get_sampling_rate() const;

        bool set_gsr_range(int range);

        bool calibrate_gsr(double known_resistance_ohms);

        void set_data_callback(DataCallback callback);

        std::vector <GSRData> get_buffered_data();

        void clear_buffer();

        std::string get_device_info() const;

        std::string get_last_error() const;

        bool perform_self_test();

    private:
        class Impl;

        std::unique_ptr <Impl> pimpl;
    };

} // namespace ircamera
