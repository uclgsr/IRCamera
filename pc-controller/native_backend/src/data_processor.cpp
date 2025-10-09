#include "data_processor.h"

#include <algorithm>
#include <cmath>
#include <limits>

namespace ircamera {

namespace {
constexpr std::uint8_t kHeaderByte0 = 0xAA;
constexpr std::uint8_t kHeaderByte1 = 0x55;
constexpr double kConductanceScale = 0.01;  // µS per LSB
constexpr std::size_t kExpectedPacketSize = 10;

template <typename T>
T read_big_endian(const std::vector<std::uint8_t>& buffer, std::size_t offset) {
    T value = 0;
    for (std::size_t i = 0; i < sizeof(T); ++i) {
        value = static_cast<T>((value << 8) | buffer[offset + i]);
    }
    return value;
}
}  // namespace

GSRPacket parse_gsr_packet(const std::vector<std::uint8_t>& packet) noexcept {
    GSRPacket result;

    if (packet.size() < kExpectedPacketSize) {
        return result;
    }

    if (packet[0] != kHeaderByte0 || packet[1] != kHeaderByte1) {
        return result;
    }

    result.sequence = read_big_endian<std::uint16_t>(packet, 2);
    result.timestamp_ms = read_big_endian<std::uint32_t>(packet, 4);
    result.raw_value = read_big_endian<std::uint16_t>(packet, 8);
    result.conductance_us = static_cast<double>(result.raw_value) * kConductanceScale;
    result.valid = true;

    return result;
}

GSRStatistics compute_gsr_statistics(const std::vector<double>& samples) {
    GSRStatistics stats;
    if (samples.empty()) {
        return stats;
    }

    const std::size_t n = samples.size();
    double sum = 0.0;
    double sum_sq = 0.0;
    double min_val = std::numeric_limits<double>::infinity();
    double max_val = -std::numeric_limits<double>::infinity();

    for (double sample : samples) {
        sum += sample;
        sum_sq += sample * sample;
        min_val = std::min(min_val, sample);
        max_val = std::max(max_val, sample);
    }

    const double mean = sum / static_cast<double>(n);
    const double variance = (sum_sq / static_cast<double>(n)) - (mean * mean);

    stats.sample_count = n;
    stats.mean_us = mean;
    stats.min_us = min_val;
    stats.max_us = max_val;
    stats.stddev_us = variance > 0.0 ? std::sqrt(variance) : 0.0;

    return stats;
}

}  // namespace ircamera

