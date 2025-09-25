#pragma once

#include <vector>
#include <string>
#include <memory>
#include <map>

namespace ircamera {

// High-performance data processing utilities
class DataProcessor {
public:
    DataProcessor();
    ~DataProcessor();
    
    // JSON message processing
    std::map<std::string, std::string> parse_json_message(const std::string& json_str);
    std::string create_json_response(const std::map<std::string, std::string>& fields);
    
    // Binary data processing
    std::vector<uint8_t> compress_data(const std::vector<uint8_t>& data);
    std::vector<uint8_t> decompress_data(const std::vector<uint8_t>& compressed_data);
    
    // Frame processing
    std::vector<uint8_t> encode_jpeg(const std::vector<uint8_t>& raw_image, int width, int height, int quality = 90);
    std::vector<uint8_t> decode_jpeg(const std::vector<uint8_t>& jpeg_data);
    
    // Real-time statistics
    void update_statistics(double value);
    double get_mean() const;
    double get_variance() const;
    double get_std_deviation() const;
    size_t get_sample_count() const;
    void reset_statistics();
    
    // Buffer management
    void add_sample(double timestamp, double value);
    std::vector<std::pair<double, double>> get_recent_samples(double time_window_seconds) const;
    void clear_buffer();
    size_t get_buffer_size() const;
    
private:
    class Impl;
    std::unique_ptr<Impl> pimpl_;
};

// Thermal image processing utilities
class ThermalProcessor {
public:
    ThermalProcessor();
    ~ThermalProcessor();
    
    // Temperature conversion
    std::vector<double> raw_to_celsius(const std::vector<uint16_t>& raw_data, double emissivity = 0.95);
    std::vector<uint8_t> apply_colormap(const std::vector<double>& temp_data, const std::string& colormap = "jet");
    
    // Image enhancement
    std::vector<double> apply_gaussian_blur(const std::vector<double>& image, int width, int height, double sigma);
    std::vector<double> apply_histogram_equalization(const std::vector<double>& image);
    
    // Analysis
    double find_min_temperature(const std::vector<double>& temp_data);
    double find_max_temperature(const std::vector<double>& temp_data);
    double calculate_average_temperature(const std::vector<double>& temp_data);
    std::pair<int, int> find_hotspot_location(const std::vector<double>& temp_data, int width, int height);
    
private:
    class Impl;
    std::unique_ptr<Impl> pimpl_;
};

// Network message utilities
class MessageProcessor {
public:
    MessageProcessor();
    ~MessageProcessor();
    
    // Protocol message creation
    std::string create_hello_message(const std::string& device_id, const std::vector<std::string>& capabilities);
    std::string create_sync_request(uint64_t timestamp_ms);
    std::string create_sync_response(uint64_t pc_timestamp, uint64_t device_timestamp);
    std::string create_ack_message(const std::string& command);
    std::string create_error_message(const std::string& command, const std::string& error_code, const std::string& message);
    
    // Message parsing
    bool parse_message(const std::string& message, std::string& type, std::map<std::string, std::string>& params);
    bool validate_message_format(const std::string& message);
    
    // Time synchronization utilities
    uint64_t get_current_timestamp_ms();
    int64_t calculate_time_offset(uint64_t t1, uint64_t t2, uint64_t t3, uint64_t t4);
    double calculate_round_trip_time(uint64_t t1, uint64_t t3);
    
private:
    class Impl;
    std::unique_ptr<Impl> pimpl_;
};

} // namespace ircamera
