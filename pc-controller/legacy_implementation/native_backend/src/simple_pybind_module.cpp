#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>
#include <pybind11/functional.h>


#include <fcntl.h>
#include <unistd.h>
#include <vector>
#include <string>

#include "native_shimmer.h"

namespace py = pybind11;

PYBIND11_MODULE(native_backend, m) {
    m.doc() = "IRCamera Native Backend - High-performance sensor interfacing (Shimmer GSR only)";

    
    py::class_<ircamera::GSRData>(m, "GSRData")
        .def(py::init<>())
        .def_readwrite("timestamp_ns", &ircamera::GSRData::timestamp_ns)
        .def_readwrite("raw_gsr_value", &ircamera::GSRData::raw_gsr_value)
        .def_readwrite("gsr_microsiemens", &ircamera::GSRData::gsr_microsiemens)
        .def_readwrite("raw_ppg_value", &ircamera::GSRData::raw_ppg_value)
        .def_readwrite("ppg_normalized", &ircamera::GSRData::ppg_normalized)
        .def_readwrite("packet_sequence", &ircamera::GSRData::packet_sequence)
        .def("__repr__", [](const ircamera::GSRData &data) {
            return "<GSRData timestamp=" + std::to_string(data.timestamp_ns) +
                   " gsr=" + std::to_string(data.gsr_microsiemens) + "μS>";
        });

    
    py::class_<ircamera::NativeShimmer>(m, "NativeShimmer")
        .def(py::init<const std::string &>(), py::arg("port_name") = "")
        .def("connect", &ircamera::NativeShimmer::connect,
             "Connect to Shimmer sensor via serial port",
             py::arg("port_name") = "")
        .def("disconnect", &ircamera::NativeShimmer::disconnect,
             "Disconnect from sensor")
        .def("is_connected", &ircamera::NativeShimmer::is_connected,
             "Check if sensor is connected")
        .def("start_streaming", &ircamera::NativeShimmer::start_streaming,
             "Start data streaming")
        .def("stop_streaming", &ircamera::NativeShimmer::stop_streaming,
             "Stop data streaming")
        .def("is_streaming", &ircamera::NativeShimmer::is_streaming,
             "Check if data streaming is active")
        .def("set_sampling_rate", &ircamera::NativeShimmer::set_sampling_rate,
             py::arg("rate_hz"),
             "Set GSR sampling rate in Hz (1-1000)")
        .def("get_sampling_rate", &ircamera::NativeShimmer::get_sampling_rate,
             "Get current sampling rate")
        .def("set_gsr_range", &ircamera::NativeShimmer::set_gsr_range,
             py::arg("range"),
             "Set GSR measurement range")
        .def("calibrate_gsr", &ircamera::NativeShimmer::calibrate_gsr,
             py::arg("known_resistance_ohms"),
             "Calibrate GSR with known resistance")
        .def("set_data_callback", &ircamera::NativeShimmer::set_data_callback,
             py::arg("callback"),
             "Set callback function for real-time data")
        .def("get_buffered_data", &ircamera::NativeShimmer::get_buffered_data,
             "Get all buffered GSR data")
        .def("clear_buffer", &ircamera::NativeShimmer::clear_buffer,
             "Clear internal data buffer")
        .def("get_device_info", &ircamera::NativeShimmer::get_device_info,
             "Get device information string")
        .def("get_last_error", &ircamera::NativeShimmer::get_last_error,
             "Get last error message")
        .def("perform_self_test", &ircamera::NativeShimmer::perform_self_test,
             "Perform device self-test");

    
    m.def("get_available_serial_ports", []() {
        std::vector<std::string> ports;
        
        #ifdef __linux__
        
        std::vector<std::string> candidates = {
            "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3",
            "/dev/ttyACM0", "/dev/ttyACM1", "/dev/ttyACM2", "/dev/ttyACM3",
        };
        
        for (const auto &port : candidates) {
            int fd = open(port.c_str(), O_RDWR | O_NOCTTY | O_NONBLOCK);
            if (fd >= 0) {
                ports.push_back(port);
                close(fd);
            }
        }
        #elif _WIN32
        
        for (int i = 1; i <= 20; i++) {
            std::string port = "COM" + std::to_string(i);
            ports.push_back(port);  
        }
        #endif
        
        return ports;
    }, "Get list of available serial ports for Shimmer devices");

    
    m.def("detect_shimmer_device", []() {
        auto ports = []() {
            std::vector<std::string> ports;
            
            #ifdef __linux__
            std::vector<std::string> candidates = {
                "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3",
                "/dev/ttyACM0", "/dev/ttyACM1", "/dev/ttyACM2", "/dev/ttyACM3",
            };
            
            for (const auto &port : candidates) {
                int fd = open(port.c_str(), O_RDWR | O_NOCTTY | O_NONBLOCK);
                if (fd >= 0) {
                    ports.push_back(port);
                    close(fd);
                }
            }
            #endif
            
            return ports;
        }();
        
        if (!ports.empty()) {
            return ports[0];  
        }
        
        return std::string("");  
    }, "Detect if a Shimmer device is connected and return port");

    
    m.attr("__version__") = "1.0.0";
    m.attr("__author__") = "IRCamera Team";
    m.attr("__description__") = "High-performance native backend for GSR sensor processing";
}