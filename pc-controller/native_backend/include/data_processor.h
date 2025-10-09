        std::vector<double> apply_histogram_equalization(const std::vector<double> &image);

        // Analysis
        double find_min_temperature(const std::vector<double> &temp_data);

        double find_max_temperature(const std::vector<double> &temp_data);

        double calculate_average_temperature(const std::vector<double> &temp_data);

        std::pair<int, int>
        find_hotspot_location(const std::vector<double> &temp_data, int width, int height);

    private:
        class Impl;

        std::unique_ptr <Impl> pimpl_;
    };

// Network message utilities
    class MessageProcessor {
    public:
        MessageProcessor();

        ~MessageProcessor();

        // Protocol message creation
        std::string create_hello_message(const std::string &device_id,
                                         const std::vector <std::string> &capabilities);

        std::string create_sync_request(uint64_t timestamp_ms);

        std::string create_sync_response(uint64_t pc_timestamp, uint64_t device_timestamp);

        std::string create_ack_message(const std::string &command);

        std::string create_error_message(const std::string &command, const std::string &error_code,
                                         const std::string &message);

        // Message parsing
        bool parse_message(const std::string &message, std::string &type,
                           std::map <std::string, std::string> &params);

        bool validate_message_format(const std::string &message);

        // Time synchronization utilities
        uint64_t get_current_timestamp_ms();

        int64_t calculate_time_offset(uint64_t t1, uint64_t t2, uint64_t t3, uint64_t t4);

        double calculate_round_trip_time(uint64_t t1, uint64_t t3);

    private:
        class Impl;

        std::unique_ptr <Impl> pimpl_;
    };

} // namespace ircamera
#pragma once

#include <cstdint>
#include <cstddef>
#include <vector>

namespace ircamera {

/**
 * Representation of a decoded GSR packet.
 *
 * The parser is intentionally lightweight so it can be invoked from Python
 * inside a streaming loop without incurring additional allocations.
 */
struct GSRPacket {
    bool valid{false};
    std::uint32_t timestamp_ms{0};
    std::uint16_t raw_value{0};
    double conductance_us{0.0};
    std::uint16_t sequence{0};
};

/**
 * Running statistics computed over a collection of GSR samples.
 */
struct GSRStatistics {
    double mean_us{0.0};
    double min_us{0.0};
    double max_us{0.0};
    double stddev_us{0.0};
    std::size_t sample_count{0};
};

/**
 * Parse a binary packet emitted by the Shimmer GSR device.
 *
 * The expected packet layout is:
 *   Byte 0   : 0xAA (frame header)
 *   Byte 1   : 0x55 (frame header)
 *   Bytes 2-3: 16-bit sequence number (big-endian)
 *   Bytes 4-7: 32-bit timestamp in milliseconds since device boot (big-endian)
 *   Bytes 8-9: 16-bit raw conductance sample (big-endian)
 *
 * The raw conductance value is converted to micro-siemens using a scale factor
 * of 0.01 µS per LSB, which matches the Shimmer3 GSR+ sensor configuration used
 * in the project. If the packet fails validation an invalid packet is returned
 * (valid=false) and an exception is not thrown so callers can decide how to
 * handle malformed frames.
 */
GSRPacket parse_gsr_packet(const std::vector<std::uint8_t>& packet) noexcept;

/**
 * Compute descriptive statistics (mean, min, max, standard deviation) over a
 * set of conductance samples (µS).
 */
GSRStatistics compute_gsr_statistics(const std::vector<double>& samples);

}  // namespace ircamera
