#pragma once


#include <chrono>
#include <string>
#include <vector>

namespace ircamera {

    struct PerformanceStats {
        uint64_t total_packets_received = 0;
        uint64_t total_packets_lost = 0;
        double average_latency_ms = 0.0;
        double current_fps = 0.0;
        double cpu_usage_percent = 0.0;
        double memory_usage_mb = 0.0;
        std::chrono::high_resolution_clock::time_point last_update;

        double get_packet_loss_rate() const {
            if (total_packets_received + total_packets_lost == 0) return 0.0;
            return static_cast<double>(total_packets_lost) /
                   (total_packets_received + total_packets_lost) * 100.0;
        }
    };

    struct DeviceInfo {
        std::string device_id;
        std::string device_name;
        std::string device_type;
        std::string serial_number;
        std::string firmware_version;
        std::string connection_type;
        bool is_connected = false;
        bool is_streaming = false;
        std::chrono::system_clock::time_point connection_time;
    };

    struct SyncMarker {
        std::chrono::high_resolution_clock::time_point timestamp;
        std::string marker_type;
        std::string description;
        uint64_t sequence_number;

        SyncMarker() : sequence_number(0) {
        }

        SyncMarker(const std::string &type, const std::string &desc = "")
                : marker_type(type), description(desc), sequence_number(0) {
            timestamp = std::chrono::high_resolution_clock::now();
        }
    };

    struct CameraFrame {
        std::chrono::high_resolution_clock::time_point timestamp;
        std::vector <uint8_t> data;
        uint32_t width;
        uint32_t height;
        uint32_t channels;
        std::string format;
        uint64_t frame_number;
        bool valid = true;

        CameraFrame() : width(0), height(0), channels(0), frame_number(0) {
        }

        size_t size() const {
            return width * height * channels;
        }
    };

} 
