#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>
#include <pybind11/functional.h>


#include <fcntl.h>
#include <unistd.h>
#include <vector>
#include <string>

#include "native_shimmer.h"

#ifdef HAVE_OPENCV
#include "native_webcam.h"
#endif

namespace py = pybind11;

PYBIND11_MODULE(native_backend, m
) {
m.

doc() = "IRCamera Native Backend - High-performance sensor interfacing";

py::class_<ircamera::GSRData>(m,
"GSRData")
.

def (py::init<>())

.def_readwrite("timestamp_ns", &ircamera::GSRData::timestamp_ns)
.def_readwrite("raw_gsr_value", &ircamera::GSRData::raw_gsr_value)
.def_readwrite("gsr_microsiemens", &ircamera::GSRData::gsr_microsiemens)
.def_readwrite("raw_ppg_value", &ircamera::GSRData::raw_ppg_value)
.def_readwrite("ppg_normalized", &ircamera::GSRData::ppg_normalized)
.def_readwrite("packet_sequence", &ircamera::GSRData::packet_sequence)
.def("__repr__", [](
const ircamera::GSRData &data
) {
return "<GSRData timestamp=" +
std::to_string(data
.timestamp_ns) +
" gsr=" +
std::to_string(data
.gsr_microsiemens) + "µS" +
" raw=" +
std::to_string(data
.raw_gsr_value) + ">";
});

py::class_<ircamera::NativeShimmer>(m,
"NativeShimmer")
.

def(py::init<const std::string &>(), py::arg("port_name")

= "")
.def("connect", &ircamera::NativeShimmer::connect,
py::arg("port_name") = "",
"Connect to Shimmer device on specified port")
.def("disconnect", &ircamera::NativeShimmer::disconnect,
"Disconnect from Shimmer device")
.def("is_connected", &ircamera::NativeShimmer::is_connected,
"Check if device is connected")
.def("start_streaming", &ircamera::NativeShimmer::start_streaming,
"Start GSR data streaming")
.def("stop_streaming", &ircamera::NativeShimmer::stop_streaming,
"Stop GSR data streaming")
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

py::class_<ircamera::FrameData>(m,
"FrameData")
.

def (py::init<>())

.def_readwrite("timestamp_ns", &ircamera::FrameData::timestamp_ns)
.def_readwrite("width", &ircamera::FrameData::width)
.def_readwrite("height", &ircamera::FrameData::height)
.def_readwrite("channels", &ircamera::FrameData::channels)
.def_readwrite("data_size", &ircamera::FrameData::data_size)
.def_readwrite("frame_number", &ircamera::FrameData::frame_number)
.def("get_numpy_array", [](
const ircamera::FrameData &frame
) {
if (!frame.data || frame.data_size == 0) {
return

py::array_t<uint8_t>();

}

std::vector <size_t> shape;
if (frame.channels == 1) {
shape = {static_cast<size_t>(frame.height), static_cast<size_t>(frame.width)};
} else {
shape = {static_cast<size_t>(frame.height), static_cast<size_t>(frame.width),
         static_cast<size_t>(frame.channels)};
}

return
py::array_t<uint8_t>(
        shape,
        frame
.data.

get()

);
}, "Get frame data as numpy array")
.def("__repr__", [](
const ircamera::FrameData &frame
) {
return "<FrameData " +
std::to_string(frame
.width) + "x" +
std::to_string(frame
.height) +
"x" +
std::to_string(frame
.channels) + " frame=" +
std::to_string(frame
.frame_number) + ">";
});

py::class_<ircamera::CameraConfig>(m,
"CameraConfig")
.

def (py::init<>())

.def_readwrite("device_id", &ircamera::CameraConfig::device_id)
.def_readwrite("width", &ircamera::CameraConfig::width)
.def_readwrite("height", &ircamera::CameraConfig::height)
.def_readwrite("fps", &ircamera::CameraConfig::fps)
.def_readwrite("fourcc", &ircamera::CameraConfig::fourcc)
.def_readwrite("auto_exposure", &ircamera::CameraConfig::auto_exposure)
.def_readwrite("exposure", &ircamera::CameraConfig::exposure)
.def_readwrite("gain", &ircamera::CameraConfig::gain)
.def("__repr__", [](
const ircamera::CameraConfig &config
) {
return "<CameraConfig " +
std::to_string(config
.width) + "x" +
std::to_string(config
.height) +
" @ " +
std::to_string(config
.fps) + "fps>";
});

py::class_<ircamera::NativeWebcam>(m,
"NativeWebcam")
.

def(py::init<int>(), py::arg("device_id")

= 0)
.def("open_camera", &ircamera::NativeWebcam::open_camera,
py::arg("config") = ircamera::CameraConfig{
},
"Open camera with specified configuration")
.def("close_camera", &ircamera::NativeWebcam::close_camera,
"Close camera")
.def("is_open", &ircamera::NativeWebcam::is_open,
"Check if camera is open")
.def("start_capture", &ircamera::NativeWebcam::start_capture,
"Start video capture")
.def("stop_capture", &ircamera::NativeWebcam::stop_capture,
"Stop video capture")
.def("is_capturing", &ircamera::NativeWebcam::is_capturing,
"Check if capture is active")
.def("set_resolution", &ircamera::NativeWebcam::set_resolution,
py::arg("width"), py::arg("height"),
"Set camera resolution")
.def("set_fps", &ircamera::NativeWebcam::set_fps,
py::arg("fps"),
"Set camera frame rate")
.def("set_exposure", &ircamera::NativeWebcam::set_exposure,
py::arg("exposure"),
"Set camera exposure (-1 for auto)")
.def("set_gain", &ircamera::NativeWebcam::set_gain,
py::arg("gain"),
"Set camera gain")
.def("set_auto_exposure", &ircamera::NativeWebcam::set_auto_exposure,
py::arg("enabled"),
"Enable/disable auto exposure")
.def("set_frame_callback", &ircamera::NativeWebcam::set_frame_callback,
py::arg("callback"),
"Set callback function for real-time frames")
.def("get_latest_frame", &ircamera::NativeWebcam::get_latest_frame,
"Get latest captured frame")
.def("get_buffered_frames", &ircamera::NativeWebcam::get_buffered_frames,
"Get all buffered frames")
.def("clear_buffer", &ircamera::NativeWebcam::clear_buffer,
"Clear frame buffer")
.def("get_available_cameras", &ircamera::NativeWebcam::get_available_cameras,
"Get list of available camera device IDs")
.def("get_current_config", &ircamera::NativeWebcam::get_current_config,
"Get current camera configuration")
.def("get_camera_info", &ircamera::NativeWebcam::get_camera_info,
"Get camera information string")
.def("get_last_error", &ircamera::NativeWebcam::get_last_error,
"Get last error message")
.def("test_camera_capture", &ircamera::NativeWebcam::test_camera_capture,
"Test camera capture functionality");

m.def("get_shimmer_ports", []() {

std::vector <std::string> ports;

#ifdef _WIN32

for (int i = 1; i <= 20; i++) {
    std::string port = "COM" + std::to_string(i);

    HANDLE handle = CreateFileA(
        ("\\\\.\\"+port).c_str(),
        GENERIC_READ | GENERIC_WRITE,
        0, nullptr, OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL, nullptr
    );
    if (handle != INVALID_HANDLE_VALUE) {
        ports.push_back(port);
        CloseHandle(handle);
    }
}
#else

std::vector <std::string> candidates = {
        "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3",
        "/dev/ttyACM0", "/dev/ttyACM1", "/dev/ttyACM2", "/dev/ttyACM3",
        "/dev/cu.usbserial-*", "/dev/cu.usbmodem*"
};

for (
const auto &port
: candidates) {
int fd = open(port.c_str(), O_RDWR | O_NOCTTY | O_NONBLOCK);
if (fd >= 0) {
ports.
push_back(port);
close(fd);
}
}
#endif

return
ports;
}, "Get list of available serial ports for Shimmer devices");

m.def("get_available_cameras", []() {

ircamera::NativeWebcam webcam;
return webcam.

get_available_cameras();

}, "Get list of available camera device IDs");

m.attr("__version__") = "1.0.0";
m.attr("__author__") = "IRCamera Team";
m.attr("__description__") = "High-performance native backend for multi-modal physiological sensing";
}
