

#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>
#include <pybind11/chrono.h>

#include "native_shimmer.hpp"
#include "native_webcam.hpp"

namespace py = pybind11;
using namespace ircamera::sensors;

PYBIND11_MODULE(ircamera_native_backend, m
) {
m.

doc() = "IRCamera Native Backend - High-performance sensor interfaces";

py::class_<ShimmerData>(m,
"ShimmerData")
.

def (py::init<>())

.def_readwrite("timestamp_ns", &ShimmerData::timestamp_ns)
.def_readwrite("gsr_raw", &ShimmerData::gsr_raw)
.def_readwrite("gsr_microsiemens", &ShimmerData::gsr_microsiemens)
.def_readwrite("ppg_raw", &ShimmerData::ppg_raw)
.def_readwrite("accel_x", &ShimmerData::accel_x)
.def_readwrite("accel_y", &ShimmerData::accel_y)
.def_readwrite("accel_z", &ShimmerData::accel_z)
.def_readwrite("valid", &ShimmerData::valid)
.def("__repr__", [](
const ShimmerData &data
) {
return "<ShimmerData timestamp=" +
std::to_string(data
.timestamp_ns) +
" gsr=" +
std::to_string(data
.gsr_microsiemens) + "μS valid=" +
(data.valid ? "true" : "false") + ">";
});

py::class_<NativeShimmer::Statistics>(m,
"ShimmerStatistics")
.

def (py::init<>())

.def_readwrite("total_samples", &NativeShimmer::Statistics::total_samples)
.def_readwrite("dropped_samples", &NativeShimmer::Statistics::dropped_samples)
.def_readwrite("data_rate_hz", &NativeShimmer::Statistics::data_rate_hz)
.def_readwrite("last_timestamp_ns", &NativeShimmer::Statistics::last_timestamp_ns);

py::class_<NativeShimmer>(m,
"NativeShimmer")
.

def (py::init<>())

.def("connect", &NativeShimmer::connect,
"Connect to Shimmer sensor via serial port",
py::arg("port_name"), py::arg("baud_rate") = 115200)
.def("disconnect", &NativeShimmer::disconnect,
"Disconnect from sensor")
.def("start_streaming", &NativeShimmer::start_streaming,
"Start data streaming",
py::arg("sample_rate") = 512)
.def("stop_streaming", &NativeShimmer::stop_streaming,
"Stop data streaming")
.def("is_connected", &NativeShimmer::is_connected,
"Check if sensor is connected")
.def("is_streaming", &NativeShimmer::is_streaming,
"Check if streaming is active")
.def("get_data", &NativeShimmer::get_data,
"Get data from queue (non-blocking)")
.def("get_queue_size", &NativeShimmer::get_queue_size,
"Get current queue size")
.def("clear_queue", &NativeShimmer::clear_queue,
"Clear data queue")
.def("get_sensor_info", &NativeShimmer::get_sensor_info,
"Get sensor information")
.def("set_gsr_range", &NativeShimmer::set_gsr_range,
"Set GSR range for calibration")
.def("get_last_error", &NativeShimmer::get_last_error,
"Get last error message")
.def("get_statistics", &NativeShimmer::get_statistics,
"Get capture statistics");

py::class_<WebcamFrame>(m,
"WebcamFrame")
.

def (py::init<>())

.def_readwrite("timestamp_ns", &WebcamFrame::timestamp_ns)
.def_readwrite("width", &WebcamFrame::width)
.def_readwrite("height", &WebcamFrame::height)
.def_readwrite("channels", &WebcamFrame::channels)
.def_readwrite("frame_number", &WebcamFrame::frame_number)
.def_readwrite("valid", &WebcamFrame::valid)
.def("get_data_array", [](
const WebcamFrame &frame
) {

return py::array_t<uint8_t>(
{ frame.height, frame.width, frame.channels},
{ sizeof(uint8_t) * frame.
width *frame
.channels,
sizeof(uint8_t) * frame.channels,
sizeof(uint8_t)},
frame.data.

data(),
        py::cast(frame)  // Keep frame alive
);
}, "Get frame data as NumPy array (zero-copy)")
.def("__repr__", [](
const WebcamFrame &frame
) {
return "<WebcamFrame " +
std::to_string(frame
.width) + "x" +
std::to_string(frame
.height) + " frame=" +
std::to_string(frame
.frame_number) + " valid=" +
(frame.valid ? "true" : "false") + ">";
});

py::class_<NativeWebcam::CameraProperties>(m,
"CameraProperties")
.

def (py::init<>())

.def_readwrite("width", &NativeWebcam::CameraProperties::width)
.def_readwrite("height", &NativeWebcam::CameraProperties::height)
.def_readwrite("fps", &NativeWebcam::CameraProperties::fps)
.def_readwrite("fourcc", &NativeWebcam::CameraProperties::fourcc)
.def_readwrite("codec_name", &NativeWebcam::CameraProperties::codec_name)
.def_readwrite("brightness", &NativeWebcam::CameraProperties::brightness)
.def_readwrite("contrast", &NativeWebcam::CameraProperties::contrast)
.def_readwrite("saturation", &NativeWebcam::CameraProperties::saturation)
.def_readwrite("gain", &NativeWebcam::CameraProperties::gain)
.def_readwrite("exposure", &NativeWebcam::CameraProperties::exposure);

py::class_<NativeWebcam::Statistics>(m,
"CameraStatistics")
.

def (py::init<>())

.def_readwrite("total_frames", &NativeWebcam::Statistics::total_frames)
.def_readwrite("dropped_frames", &NativeWebcam::Statistics::dropped_frames)
.def_readwrite("actual_fps", &NativeWebcam::Statistics::actual_fps)
.def_readwrite("last_timestamp_ns", &NativeWebcam::Statistics::last_timestamp_ns)
.def_readwrite("avg_frame_size_bytes", &NativeWebcam::Statistics::avg_frame_size_bytes);

py::class_<NativeWebcam>(m,
"NativeWebcam")
.

def (py::init<>())

.def("open", &NativeWebcam::open,
"Open webcam device",
py::arg("device_id") = 0, py::arg("width") = 1920,
py::arg("height") = 1080, py::arg("fps") = 30.0)
.def("close", &NativeWebcam::close,
"Close webcam device")
.def("start_capture", &NativeWebcam::start_capture,
"Start frame capture")
.def("stop_capture", &NativeWebcam::stop_capture,
"Stop frame capture")
.def("is_open", &NativeWebcam::is_open,
"Check if camera is open")
.def("is_capturing", &NativeWebcam::is_capturing,
"Check if capture is active")
.def("get_frame", &NativeWebcam::get_frame,
"Get frame from queue (non-blocking)")
.def("get_queue_size", &NativeWebcam::get_queue_size,
"Get current queue size")
.def("clear_queue", &NativeWebcam::clear_queue,
"Clear frame queue")
.def("get_properties", &NativeWebcam::get_properties,
"Get camera properties")
.def("set_property", &NativeWebcam::set_property,
"Set camera property")
.def("get_property", &NativeWebcam::get_property,
"Get camera property")
.def("get_statistics", &NativeWebcam::get_statistics,
"Get capture statistics")
.def("get_last_error", &NativeWebcam::get_last_error,
"Get last error message")
.def("set_auto_exposure", &NativeWebcam::set_auto_exposure,
"Enable/disable auto-exposure")
.def("set_auto_white_balance", &NativeWebcam::set_auto_white_balance,
"Enable/disable auto-white-balance")
.def("take_snapshot", &NativeWebcam::take_snapshot,
"Take a snapshot (single frame capture)");

m.def("get_version", []() {
return "IRCamera Native Backend v1.0.0";
}, "Get version information");

m.def("get_timestamp_ns", []() {
return

std::chrono::duration_cast<std::chrono::nanoseconds> (
std::chrono::steady_clock::now()

.

time_since_epoch()

).

count();

}, "Get current timestamp in nanoseconds");

m.attr("SHIMMER_SAMPLE_RATE_512") = 512;
m.attr("SHIMMER_SAMPLE_RATE_1024") = 1024;
m.attr("SHIMMER_GSR_RANGE_AUTO") = -1;
m.attr("WEBCAM_HD_WIDTH") = 1280;
m.attr("WEBCAM_HD_HEIGHT") = 720;
m.attr("WEBCAM_FHD_WIDTH") = 1920;
m.attr("WEBCAM_FHD_HEIGHT") = 1080;
}
