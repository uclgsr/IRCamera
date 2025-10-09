#include <pybind11/pybind11.h>
#include <pybind11/stl.h>

#include <string>
#include <vector>

#include "data_processor.h"

namespace py = pybind11;

namespace {

ircamera::GSRPacket parse_from_bytes(py::bytes data) {
    std::string buffer = data;
    std::vector<std::uint8_t> bytes(buffer.begin(), buffer.end());
    auto packet = ircamera::parse_gsr_packet(bytes);
    if (!packet.valid) {
        throw py::value_error("Invalid GSR packet: expected 10-byte frame starting with 0xAA55");
    }
    return packet;
}

ircamera::GSRPacket parse_from_sequence(const std::vector<std::uint8_t>& data) {
    auto packet = ircamera::parse_gsr_packet(data);
    if (!packet.valid) {
        throw py::value_error("Invalid GSR packet: expected 10-byte frame starting with 0xAA55");
    }
    return packet;
}

}  // namespace

PYBIND11_MODULE(enhanced_native_backend, m) {
    m.doc() = "IRCamera native backend bindings for high-performance sensor processing";

    py::class_<ircamera::GSRPacket>(m, "GSRPacket")
        .def(py::init<>())
        .def_readwrite("valid", &ircamera::GSRPacket::valid)
        .def_readwrite("timestamp_ms", &ircamera::GSRPacket::timestamp_ms)
        .def_readwrite("raw_value", &ircamera::GSRPacket::raw_value)
        .def_readwrite("conductance_us", &ircamera::GSRPacket::conductance_us)
        .def_readwrite("sequence", &ircamera::GSRPacket::sequence)
        .def("__repr__", [](const ircamera::GSRPacket& packet) {
            return "<GSRPacket seq=" + std::to_string(packet.sequence) +
                   " conductance_us=" + std::to_string(packet.conductance_us) + ">";
        });

    py::class_<ircamera::GSRStatistics>(m, "GSRStatistics")
        .def(py::init<>())
        .def_readwrite("mean_us", &ircamera::GSRStatistics::mean_us)
        .def_readwrite("min_us", &ircamera::GSRStatistics::min_us)
        .def_readwrite("max_us", &ircamera::GSRStatistics::max_us)
        .def_readwrite("stddev_us", &ircamera::GSRStatistics::stddev_us)
        .def_readwrite("sample_count", &ircamera::GSRStatistics::sample_count)
        .def("__repr__", [](const ircamera::GSRStatistics& stats) {
            return "<GSRStatistics samples=" + std::to_string(stats.sample_count) +
                   " mean_us=" + std::to_string(stats.mean_us) + ">";
        });

    m.def(
        "parse_gsr_packet",
        &parse_from_bytes,
        py::arg("packet"),
        "Parse a binary GSR packet provided as Python bytes and return a decoded structure.");

    m.def(
        "parse_gsr_packet_from_sequence",
        &parse_from_sequence,
        py::arg("packet"),
        "Parse a GSR packet provided as an iterable of uint8 values.");

    m.def(
        "compute_gsr_statistics",
        &ircamera::compute_gsr_statistics,
        py::arg("samples"),
        "Compute descriptive statistics for a sequence of conductance samples in micro-siemens.");

    m.attr("__version__") = "0.1.0";
}

