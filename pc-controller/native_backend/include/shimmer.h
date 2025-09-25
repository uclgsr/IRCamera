#pragma once

#include <vector>
#include <string>
#include <chrono>
#include <functional>
#include <memory>

namespace ircamera {

struct GSRData {
    uint64_t timestamp_ns;
    uint16_t raw_gsr_value;
    double gsr_microsiemens;
    uint16_t raw_ppg_value;
    double ppg_normalized;
    uint32_t packet_sequence;
    
    GSRData() : timestamp_ns(0), raw_gsr_value(0), gsr_microsiemens(0.0),
                raw_ppg_value(0), ppg_normalized(0.0), packet_sequence(0) {}
};

struct ThermalData {
    uint64_t timestamp_ns;
    std::vector<uint16_t> raw_temperatures;
    std::vector<double> celsius_temperatures;
    double min_temp_c;
    double max_temp_c;
    double avg_temp_c;
    double center_temp_c;
    uint32_t frame_sequence;
    uint16_t width;
    uint16_t height;
    
    ThermalData() : timestamp_ns(0), min_temp_c(0.0), max_temp_c(0.0),
                    avg_temp_c(0.0), center_temp_c(0.0), frame_sequence(0),
                    width(0), height(0) {}
};

class EnhancedShimmer {
public:
    using GSRDataCallback = std::function<void(const GSRData&)>;
    using ErrorCallback = std::function<void(const std::string&)>;
    
    explicit EnhancedShimmer(const std::string& port_name = "");
    ~EnhancedShimmer();
    
    // Connection management
    bool connect(const std::string& port_name = "");
    void disconnect();
    bool is_connected() const;
    
    // Configuration
    bool set_sampling_rate(int rate_hz);
    bool set_gsr_range(int range);
    bool enable_ppg(bool enable);
    
    // Data streaming
    bool start_streaming();
    bool stop_streaming();
    bool is_streaming() const;
    
    // Callbacks
    void set_gsr_callback(GSRDataCallback callback);
    void set_error_callback(ErrorCallback callback);
    
    // Statistics
    size_t get_packets_processed() const;
    size_t get_packets_dropped() const;
    double get_data_rate_hz() const;
    
    // Manual data processing
    std::vector<GSRData> process_raw_packet(const std::vector<uint8_t>& packet);
    
    // Configuration queries
    std::string get_firmware_version() const;
    std::string get_device_id() const;
    int get_sampling_rate() const;
    
private:
    class Impl;
    std::unique_ptr<Impl> pimpl_;
};

// Utility functions for data processing
namespace processing {
    // High-performance GSR data filtering
    std::vector<double> apply_lowpass_filter(const std::vector<double>& data, double cutoff_hz, double sample_rate);
    std::vector<double> apply_highpass_filter(const std::vector<double>& data, double cutoff_hz, double sample_rate);
    std::vector<double> apply_notch_filter(const std::vector<double>& data, double notch_hz, double sample_rate);
    
    // Statistical analysis
    double calculate_mean(const std::vector<double>& data);
    double calculate_std(const std::vector<double>& data);
    double calculate_rms(const std::vector<double>& data);
    
    // Artifact detection
    std::vector<bool> detect_motion_artifacts(const std::vector<double>& gsr_data, double threshold = 2.0);
    std::vector<bool> detect_electrical_artifacts(const std::vector<double>& gsr_data);
    
    // Data validation
    bool validate_gsr_packet(const std::vector<uint8_t>& packet);
    double calculate_signal_quality(const std::vector<double>& data);
}

} // namespace ircamera
