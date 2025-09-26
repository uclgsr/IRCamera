#include "data_processor.h"
#include <iostream>
#include <sstream>
#include <chrono>
#include <algorithm>
#include <cmath>
#include <numeric>

// Simplified JSON parsing (in production, use proper JSON library)
#include <regex>

namespace ircamera {

// DataProcessor implementation
    class DataProcessor::Impl {
    public:
        Impl() : sample_count_(0), sum_(0.0), sum_squares_(0.0) {}

        std::map <std::string, std::string> parse_json_message(const std::string &json_str) {
            std::map <std::string, std::string> result;

            // Simplified JSON parsing using regex (not production-ready)
            std::regex field_regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
            std::regex number_regex("\"([^\"]+)\"\\s*:\\s*([0-9.-]+)");

            std::sregex_iterator iter(json_str.begin(), json_str.end(), field_regex);
            std::sregex_iterator end;

            for (; iter != end; ++iter) {
                std::smatch match = *iter;
                result[match[1].str()] = match[2].str();
            }

            // Handle numeric fields
            std::sregex_iterator num_iter(json_str.begin(), json_str.end(), number_regex);
            for (; num_iter != end; ++num_iter) {
                std::smatch match = *num_iter;
                result[match[1].str()] = match[2].str();
            }

            return result;
        }

        std::string create_json_response(const std::map <std::string, std::string> &fields) {
            std::ostringstream json;
            json << "{";

            bool first = true;
            for (const auto &field: fields) {
                if (!first) json << ",";
                first = false;

                json << "\"" << field.first << "\":";

                // Try to determine if value is numeric
                if (is_numeric(field.second)) {
                    json << field.second;
                } else {
                    json << "\"" << field.second << "\"";
                }
            }

            json << "}";
            return json.str();
        }

        std::vector <uint8_t> compress_data(const std::vector <uint8_t> &data) {
            // Simplified compression (just copy for now)
            // In production, use proper compression library like zlib
            return data;
        }

        std::vector <uint8_t> decompress_data(const std::vector <uint8_t> &compressed_data) {
            // Simplified decompression
            return compressed_data;
        }

        std::vector <uint8_t>
        encode_jpeg(const std::vector <uint8_t> &raw_image, int width, int height, int quality) {
            // Simplified JPEG encoding (placeholder)
            // In production, use proper JPEG library like libjpeg
            std::vector <uint8_t> result;
            result.reserve(raw_image.size() / 2);  // Estimate compression

            // Add JPEG header (simplified)
            result.insert(result.end(), {0xFF, 0xD8});  // SOI marker

            // Add image data (simplified)
            for (size_t i = 0; i < raw_image.size(); i += 2) {
                if (i + 1 < raw_image.size()) {
                    result.push_back((raw_image[i] + raw_image[i + 1]) / 2);
                }
            }

            // Add end marker
            result.insert(result.end(), {0xFF, 0xD9});  // EOI marker

            return result;
        }

        std::vector <uint8_t> decode_jpeg(const std::vector <uint8_t> &jpeg_data) {
            // Simplified JPEG decoding (placeholder)
            if (jpeg_data.size() < 4) return {};

            std::vector <uint8_t> result;
            result.reserve(jpeg_data.size() * 2);

            // Skip header and trailer
            for (size_t i = 2; i < jpeg_data.size() - 2; ++i) {
                result.push_back(jpeg_data[i]);
                result.push_back(jpeg_data[i]);  // Duplicate for expansion
            }

            return result;
        }

        void update_statistics(double value) {
            sample_count_++;
            sum_ += value;
            sum_squares_ += value * value;
        }

        double get_mean() const {
            return sample_count_ > 0 ? sum_ / sample_count_ : 0.0;
        }

        double get_variance() const {
            if (sample_count_ < 2) return 0.0;
            double mean = get_mean();
            return (sum_squares_ - sample_count_ * mean * mean) / (sample_count_ - 1);
        }

        double get_std_deviation() const {
            return std::sqrt(get_variance());
        }

        size_t get_sample_count() const {
            return sample_count_;
        }

        void reset_statistics() {
            sample_count_ = 0;
            sum_ = 0.0;
            sum_squares_ = 0.0;
        }

        void add_sample(double timestamp, double value) {
            buffer_.push_back({timestamp, value});

            // Keep buffer size reasonable
            if (buffer_.size() > 10000) {
                buffer_.erase(buffer_.begin(), buffer_.begin() + 1000);
            }
        }

        std::vector <std::pair<double, double>>
        get_recent_samples(double time_window_seconds) const {
            if (buffer_.empty()) return {};

            double current_time = buffer_.back().first;
            double cutoff_time = current_time - time_window_seconds;

            std::vector <std::pair<double, double>> result;
            for (const auto &sample: buffer_) {
                if (sample.first >= cutoff_time) {
                    result.push_back(sample);
                }
            }

            return result;
        }

        void clear_buffer() {
            buffer_.clear();
        }

        size_t get_buffer_size() const {
            return buffer_.size();
        }

    private:
        bool is_numeric(const std::string &str) const {
            if (str.empty()) return false;

            char *end;
            std::strtod(str.c_str(), &end);
            return end == str.c_str() + str.length();
        }

        // Statistics
        size_t sample_count_;
        double sum_;
        double sum_squares_;

        // Sample buffer
        std::vector <std::pair<double, double>> buffer_;
    };

// ThermalProcessor implementation
    class ThermalProcessor::Impl {
    public:
        Impl() {}

        std::vector<double>
        raw_to_celsius(const std::vector <uint16_t> &raw_data, double emissivity) {
            std::vector<double> celsius_data;
            celsius_data.reserve(raw_data.size());

            for (uint16_t raw_value: raw_data) {
                // Simplified temperature conversion (placeholder formula)
                // In production, use proper calibration data from thermal sensor
                double temp_k = 273.15 + (raw_value - 1000) * 0.02;  // Example conversion
                double temp_c = temp_k - 273.15;

                // Apply emissivity correction
                temp_c = temp_c * emissivity + (1.0 - emissivity) * 25.0;  // Assume 25°C ambient

                celsius_data.push_back(temp_c);
            }

            return celsius_data;
        }

        std::vector <uint8_t>
        apply_colormap(const std::vector<double> &temp_data, const std::string &colormap) {
            if (temp_data.empty()) return {};

            // Find temperature range
            auto minmax = std::minmax_element(temp_data.begin(), temp_data.end());
            double min_temp = *minmax.first;
            double max_temp = *minmax.second;
            double temp_range = max_temp - min_temp;

            std::vector <uint8_t> rgb_data;
            rgb_data.reserve(temp_data.size() * 3);

            for (double temp: temp_data) {
                // Normalize temperature to 0-1 range
                double normalized = temp_range > 0 ? (temp - min_temp) / temp_range : 0.0;

                uint8_t r, g, b;
                if (colormap == "jet") {
                    apply_jet_colormap(normalized, r, g, b);
                } else if (colormap == "hot") {
                    apply_hot_colormap(normalized, r, g, b);
                } else {
                    apply_grayscale_colormap(normalized, r, g, b);
                }

                rgb_data.push_back(r);
                rgb_data.push_back(g);
                rgb_data.push_back(b);
            }

            return rgb_data;
        }

        std::vector<double>
        apply_gaussian_blur(const std::vector<double> &image, int width, int height, double sigma) {
            if (image.size() != static_cast<size_t>(width * height)) {
                return image;  // Size mismatch
            }

            // Simplified Gaussian blur (3x3 kernel)
            std::vector<double> blurred = image;

            double kernel[3][3] = {
                    {0.0625, 0.125, 0.0625},
                    {0.125,  0.25,  0.125},
                    {0.0625, 0.125, 0.0625}
            };

            for (int y = 1; y < height - 1; ++y) {
                for (int x = 1; x < width - 1; ++x) {
                    double sum = 0.0;
                    for (int ky = -1; ky <= 1; ++ky) {
                        for (int kx = -1; kx <= 1; ++kx) {
                            int idx = (y + ky) * width + (x + kx);
                            sum += image[idx] * kernel[ky + 1][kx + 1];
                        }
                    }
                    blurred[y * width + x] = sum;
                }
            }

            return blurred;
        }

        std::vector<double> apply_histogram_equalization(const std::vector<double> &image) {
            if (image.empty()) return image;

            // Find min/max
            auto minmax = std::minmax_element(image.begin(), image.end());
            double min_val = *minmax.first;
            double max_val = *minmax.second;
            double range = max_val - min_val;

            if (range == 0.0) return image;

            // Create histogram
            const int num_bins = 256;
            std::vector<int> histogram(num_bins, 0);

            for (double val: image) {
                int bin = static_cast<int>((val - min_val) / range * (num_bins - 1));
                bin = std::max(0, std::min(num_bins - 1, bin));
                histogram[bin]++;
            }

            // Create cumulative distribution
            std::vector<double> cdf(num_bins);
            cdf[0] = histogram[0];
            for (int i = 1; i < num_bins; ++i) {
                cdf[i] = cdf[i - 1] + histogram[i];
            }

            // Normalize CDF
            double total_pixels = static_cast<double>(image.size());
            for (double &val: cdf) {
                val /= total_pixels;
            }

            // Apply equalization
            std::vector<double> equalized;
            equalized.reserve(image.size());

            for (double val: image) {
                int bin = static_cast<int>((val - min_val) / range * (num_bins - 1));
                bin = std::max(0, std::min(num_bins - 1, bin));

                double new_val = min_val + cdf[bin] * range;
                equalized.push_back(new_val);
            }

            return equalized;
        }

        double find_min_temperature(const std::vector<double> &temp_data) {
            if (temp_data.empty()) return 0.0;
            return *std::min_element(temp_data.begin(), temp_data.end());
        }

        double find_max_temperature(const std::vector<double> &temp_data) {
            if (temp_data.empty()) return 0.0;
            return *std::max_element(temp_data.begin(), temp_data.end());
        }

        double calculate_average_temperature(const std::vector<double> &temp_data) {
            if (temp_data.empty()) return 0.0;
            return std::accumulate(temp_data.begin(), temp_data.end(), 0.0) / temp_data.size();
        }

        std::pair<int, int>
        find_hotspot_location(const std::vector<double> &temp_data, int width, int height) {
            if (temp_data.size() != static_cast<size_t>(width * height)) {
                return {-1, -1};
            }

            auto max_iter = std::max_element(temp_data.begin(), temp_data.end());
            if (max_iter == temp_data.end()) {
                return {-1, -1};
            }

            size_t max_index = std::distance(temp_data.begin(), max_iter);
            int x = static_cast<int>(max_index % width);
            int y = static_cast<int>(max_index / width);

            return {x, y};
        }

    private:
        void apply_jet_colormap(double value, uint8_t &r, uint8_t &g, uint8_t &b) {
            // Jet colormap: blue -> cyan -> yellow -> red
            value = std::max(0.0, std::min(1.0, value));

            if (value < 0.25) {
                r = 0;
                g = static_cast<uint8_t>(255 * value * 4);
                b = 255;
            } else if (value < 0.5) {
                r = 0;
                g = 255;
                b = static_cast<uint8_t>(255 * (0.5 - value) * 4);
            } else if (value < 0.75) {
                r = static_cast<uint8_t>(255 * (value - 0.5) * 4);
                g = 255;
                b = 0;
            } else {
                r = 255;
                g = static_cast<uint8_t>(255 * (1.0 - value) * 4);
                b = 0;
            }
        }

        void apply_hot_colormap(double value, uint8_t &r, uint8_t &g, uint8_t &b) {
            // Hot colormap: black -> red -> yellow -> white
            value = std::max(0.0, std::min(1.0, value));

            if (value < 0.33) {
                r = static_cast<uint8_t>(255 * value * 3);
                g = 0;
                b = 0;
            } else if (value < 0.66) {
                r = 255;
                g = static_cast<uint8_t>(255 * (value - 0.33) * 3);
                b = 0;
            } else {
                r = 255;
                g = 255;
                b = static_cast<uint8_t>(255 * (value - 0.66) * 3);
            }
        }

        void apply_grayscale_colormap(double value, uint8_t &r, uint8_t &g, uint8_t &b) {
            uint8_t gray = static_cast<uint8_t>(255 * std::max(0.0, std::min(1.0, value)));
            r = g = b = gray;
        }
    };

// MessageProcessor implementation
    class MessageProcessor::Impl {
    public:
        Impl() {}

        std::string create_hello_message(const std::string &device_id,
                                         const std::vector <std::string> &capabilities) {
            std::ostringstream json;
            json << "{";
            json << "\"type\":\"HELLO\",";
            json << "\"device_id\":\"" << device_id << "\",";
            json << "\"capabilities\":[";

            for (size_t i = 0; i < capabilities.size(); ++i) {
                if (i > 0) json << ",";
                json << "\"" << capabilities[i] << "\"";
            }

            json << "],";
            json << "\"timestamp\":" << get_current_timestamp_ms();
            json << "}";

            return json.str();
        }

        std::string create_sync_request(uint64_t timestamp_ms) {
            std::ostringstream json;
            json << "{";
            json << "\"type\":\"SYNC_REQUEST\",";
            json << "\"t_pc\":" << timestamp_ms;
            json << "}";

            return json.str();
        }

        std::string create_sync_response(uint64_t pc_timestamp, uint64_t device_timestamp) {
            std::ostringstream json;
            json << "{";
            json << "\"type\":\"SYNC_RESPONSE\",";
            json << "\"t_pc\":" << pc_timestamp << ",";
            json << "\"t_device\":" << device_timestamp;
            json << "}";

            return json.str();
        }

        std::string create_ack_message(const std::string &command) {
            std::ostringstream json;
            json << "{";
            json << "\"type\":\"ACK\",";
            json << "\"command\":\"" << command << "\",";
            json << "\"timestamp\":" << get_current_timestamp_ms();
            json << "}";

            return json.str();
        }

        std::string create_error_message(const std::string &command, const std::string &error_code,
                                         const std::string &message) {
            std::ostringstream json;
            json << "{";
            json << "\"type\":\"ERROR\",";
            json << "\"command\":\"" << command << "\",";
            json << "\"error_code\":\"" << error_code << "\",";
            json << "\"message\":\"" << message << "\",";
            json << "\"timestamp\":" << get_current_timestamp_ms();
            json << "}";

            return json.str();
        }

        bool parse_message(const std::string &message, std::string &type,
                           std::map <std::string, std::string> &params) {
            // Simplified JSON parsing
            std::regex type_regex("\"type\"\\s*:\\s*\"([^\"]+)\"");
            std::smatch match;

            if (std::regex_search(message, match, type_regex)) {
                type = match[1].str();
            } else {
                return false;
            }

            // Parse parameters
            std::regex param_regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
            std::regex num_regex("\"([^\"]+)\"\\s*:\\s*([0-9.-]+)");

            std::sregex_iterator iter(message.begin(), message.end(), param_regex);
            std::sregex_iterator end;

            for (; iter != end; ++iter) {
                std::smatch param_match = *iter;
                params[param_match[1].str()] = param_match[2].str();
            }

            // Handle numeric parameters
            std::sregex_iterator num_iter(message.begin(), message.end(), num_regex);
            for (; num_iter != end; ++num_iter) {
                std::smatch num_match = *num_iter;
                params[num_match[1].str()] = num_match[2].str();
            }

            return true;
        }

        bool validate_message_format(const std::string &message) {
            // Basic JSON validation
            if (message.empty()) {
                return false;
            }
            return message.front() == '{' && message.back() == '}' &&
                   message.find("\"type\"") != std::string::npos;
        }

        uint64_t get_current_timestamp_ms() {
            auto now = std::chrono::system_clock::now();
            auto duration = now.time_since_epoch();
            return std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
        }

        int64_t calculate_time_offset(uint64_t t1, uint64_t t2, uint64_t t3, uint64_t t4) {
            // NTP-style time offset calculation
            return ((static_cast<int64_t>(t2) - static_cast<int64_t>(t1)) +
                    (static_cast<int64_t>(t4) - static_cast<int64_t>(t3))) / 2;
        }

        double calculate_round_trip_time(uint64_t t1, uint64_t t3) {
            return static_cast<double>(t3 - t1);
        }
    };

// Public interface implementations
    DataProcessor::DataProcessor() : pimpl_(std::make_unique<Impl>()) {}

    DataProcessor::~DataProcessor() = default;

    std::map <std::string, std::string>
    DataProcessor::parse_json_message(const std::string &json_str) {
        return pimpl_->parse_json_message(json_str);
    }

    std::string
    DataProcessor::create_json_response(const std::map <std::string, std::string> &fields) {
        return pimpl_->create_json_response(fields);
    }

    std::vector <uint8_t> DataProcessor::compress_data(const std::vector <uint8_t> &data) {
        return pimpl_->compress_data(data);
    }

    std::vector <uint8_t>
    DataProcessor::decompress_data(const std::vector <uint8_t> &compressed_data) {
        return pimpl_->decompress_data(compressed_data);
    }

    std::vector <uint8_t>
    DataProcessor::encode_jpeg(const std::vector <uint8_t> &raw_image, int width, int height,
                               int quality) {
        return pimpl_->encode_jpeg(raw_image, width, height, quality);
    }

    std::vector <uint8_t> DataProcessor::decode_jpeg(const std::vector <uint8_t> &jpeg_data) {
        return pimpl_->decode_jpeg(jpeg_data);
    }

    void DataProcessor::update_statistics(double value) {
        pimpl_->update_statistics(value);
    }

    double DataProcessor::get_mean() const {
        return pimpl_->get_mean();
    }

    double DataProcessor::get_variance() const {
        return pimpl_->get_variance();
    }

    double DataProcessor::get_std_deviation() const {
        return pimpl_->get_std_deviation();
    }

    size_t DataProcessor::get_sample_count() const {
        return pimpl_->get_sample_count();
    }

    void DataProcessor::reset_statistics() {
        pimpl_->reset_statistics();
    }

    void DataProcessor::add_sample(double timestamp, double value) {
        pimpl_->add_sample(timestamp, value);
    }

    std::vector <std::pair<double, double>>
    DataProcessor::get_recent_samples(double time_window_seconds) const {
        return pimpl_->get_recent_samples(time_window_seconds);
    }

    void DataProcessor::clear_buffer() {
        pimpl_->clear_buffer();
    }

    size_t DataProcessor::get_buffer_size() const {
        return pimpl_->get_buffer_size();
    }

// ThermalProcessor implementations
    ThermalProcessor::ThermalProcessor() : pimpl_(std::make_unique<Impl>()) {}

    ThermalProcessor::~ThermalProcessor() = default;

    std::vector<double>
    ThermalProcessor::raw_to_celsius(const std::vector <uint16_t> &raw_data, double emissivity) {
        return pimpl_->raw_to_celsius(raw_data, emissivity);
    }

    std::vector <uint8_t> ThermalProcessor::apply_colormap(const std::vector<double> &temp_data,
                                                           const std::string &colormap) {
        return pimpl_->apply_colormap(temp_data, colormap);
    }

    std::vector<double>
    ThermalProcessor::apply_gaussian_blur(const std::vector<double> &image, int width, int height,
                                          double sigma) {
        return pimpl_->apply_gaussian_blur(image, width, height, sigma);
    }

    std::vector<double>
    ThermalProcessor::apply_histogram_equalization(const std::vector<double> &image) {
        return pimpl_->apply_histogram_equalization(image);
    }

    double ThermalProcessor::find_min_temperature(const std::vector<double> &temp_data) {
        return pimpl_->find_min_temperature(temp_data);
    }

    double ThermalProcessor::find_max_temperature(const std::vector<double> &temp_data) {
        return pimpl_->find_max_temperature(temp_data);
    }

    double ThermalProcessor::calculate_average_temperature(const std::vector<double> &temp_data) {
        return pimpl_->calculate_average_temperature(temp_data);
    }

    std::pair<int, int>
    ThermalProcessor::find_hotspot_location(const std::vector<double> &temp_data, int width,
                                            int height) {
        return pimpl_->find_hotspot_location(temp_data, width, height);
    }

// MessageProcessor implementations
    MessageProcessor::MessageProcessor() : pimpl_(std::make_unique<Impl>()) {}

    MessageProcessor::~MessageProcessor() = default;

    std::string MessageProcessor::create_hello_message(const std::string &device_id,
                                                       const std::vector <std::string> &capabilities) {
        return pimpl_->create_hello_message(device_id, capabilities);
    }

    std::string MessageProcessor::create_sync_request(uint64_t timestamp_ms) {
        return pimpl_->create_sync_request(timestamp_ms);
    }

    std::string
    MessageProcessor::create_sync_response(uint64_t pc_timestamp, uint64_t device_timestamp) {
        return pimpl_->create_sync_response(pc_timestamp, device_timestamp);
    }

    std::string MessageProcessor::create_ack_message(const std::string &command) {
        return pimpl_->create_ack_message(command);
    }

    std::string MessageProcessor::create_error_message(const std::string &command,
                                                       const std::string &error_code,
                                                       const std::string &message) {
        return pimpl_->create_error_message(command, error_code, message);
    }

    bool MessageProcessor::parse_message(const std::string &message, std::string &type,
                                         std::map <std::string, std::string> &params) {
        return pimpl_->parse_message(message, type, params);
    }

    bool MessageProcessor::validate_message_format(const std::string &message) {
        return pimpl_->validate_message_format(message);
    }

    uint64_t MessageProcessor::get_current_timestamp_ms() {
        return pimpl_->get_current_timestamp_ms();
    }

    int64_t
    MessageProcessor::calculate_time_offset(uint64_t t1, uint64_t t2, uint64_t t3, uint64_t t4) {
        return pimpl_->calculate_time_offset(t1, t2, t3, t4);
    }

    double MessageProcessor::calculate_round_trip_time(uint64_t t1, uint64_t t3) {
        return pimpl_->calculate_round_trip_time(t1, t3);
    }

} // namespace ircamera
