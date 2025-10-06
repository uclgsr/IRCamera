#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>
#include <pybind11/functional.h>

#include "shimmer.h"
#include "data_processor.h"

namespace py = pybind11;

PYBIND11_MODULE(enhanced_native_backend, m
) {
m.

doc() = "Enhanced IRCamera Native Backend - High-performance sensor processing and data handling";

// GSRData structure
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

// ThermalData structure
py::class_<ircamera::ThermalData>(m,
"ThermalData")
.

def (py::init<>())

.def_readwrite("timestamp_ns", &ircamera::ThermalData::timestamp_ns)
.def_readwrite("raw_temperatures", &ircamera::ThermalData::raw_temperatures)
.def_readwrite("celsius_temperatures", &ircamera::ThermalData::celsius_temperatures)
.def_readwrite("min_temp_c", &ircamera::ThermalData::min_temp_c)
.def_readwrite("max_temp_c", &ircamera::ThermalData::max_temp_c)
.def_readwrite("avg_temp_c", &ircamera::ThermalData::avg_temp_c)
.def_readwrite("center_temp_c", &ircamera::ThermalData::center_temp_c)
.def_readwrite("frame_sequence", &ircamera::ThermalData::frame_sequence)
.def_readwrite("width", &ircamera::ThermalData::width)
.def_readwrite("height", &ircamera::ThermalData::height)
.def("__repr__", [](
const ircamera::ThermalData &data
) {
return "<ThermalData frame=" +
std::to_string(data
.frame_sequence) +
" size=" +
std::to_string(data
.width) + "x" +
std::to_string(data
.height) +
" temp_range=" +
std::to_string(data
.min_temp_c) + "-" +
std::to_string(data
.max_temp_c) + "°C>";
});

// EnhancedShimmer class
py::class_<ircamera::EnhancedShimmer>(m,
"EnhancedShimmer")
.

def(py::init<const std::string &>(), py::arg("port_name")

= "",
"Create EnhancedShimmer instance with optional port name")
.def("connect", &ircamera::EnhancedShimmer::connect, py::arg("port_name") = "",
"Connect to Shimmer device on specified port")
.def("disconnect", &ircamera::EnhancedShimmer::disconnect,
"Disconnect from Shimmer device")
.def("is_connected", &ircamera::EnhancedShimmer::is_connected,
"Check if device is connected")
.def("set_sampling_rate", &ircamera::EnhancedShimmer::set_sampling_rate, py::arg("rate_hz"),
"Set GSR sampling rate in Hz (1-512)")
.def("set_gsr_range", &ircamera::EnhancedShimmer::set_gsr_range, py::arg("range"),
"Set GSR measurement range (1-7)")
.def("enable_ppg", &ircamera::EnhancedShimmer::enable_ppg, py::arg("enable"),
"Enable or disable PPG sensor")
.def("start_streaming", &ircamera::EnhancedShimmer::start_streaming,
"Start GSR data streaming")
.def("stop_streaming", &ircamera::EnhancedShimmer::stop_streaming,
"Stop GSR data streaming")
.def("is_streaming", &ircamera::EnhancedShimmer::is_streaming,
"Check if device is currently streaming data")
.def("set_gsr_callback", &ircamera::EnhancedShimmer::set_gsr_callback, py::arg("callback"),
"Set callback function for GSR data reception")
.def("set_error_callback", &ircamera::EnhancedShimmer::set_error_callback, py::arg("callback"),
"Set callback function for error notifications")
.def("get_packets_processed", &ircamera::EnhancedShimmer::get_packets_processed,
"Get number of packets successfully processed")
.def("get_packets_dropped", &ircamera::EnhancedShimmer::get_packets_dropped,
"Get number of packets dropped due to errors")
.def("get_data_rate_hz", &ircamera::EnhancedShimmer::get_data_rate_hz,
"Get current data rate in Hz")
.def("process_raw_packet", &ircamera::EnhancedShimmer::process_raw_packet, py::arg("packet"),
"Manually process a raw data packet")
.def("get_firmware_version", &ircamera::EnhancedShimmer::get_firmware_version,
"Get device firmware version")
.def("get_device_id", &ircamera::EnhancedShimmer::get_device_id,
"Get unique device identifier")
.def("get_sampling_rate", &ircamera::EnhancedShimmer::get_sampling_rate,
"Get current sampling rate in Hz");

// DataProcessor class
py::class_<ircamera::DataProcessor>(m,
"DataProcessor")
.

def(py::init<>(),

"Create DataProcessor instance")
.def("parse_json_message", &ircamera::DataProcessor::parse_json_message, py::arg("json_str"),
"Parse JSON message string into key-value pairs")
.def("create_json_response", &ircamera::DataProcessor::create_json_response, py::arg("fields"),
"Create JSON response from key-value pairs")
.def("compress_data", &ircamera::DataProcessor::compress_data, py::arg("data"),
"Compress binary data")
.def("decompress_data", &ircamera::DataProcessor::decompress_data, py::arg("compressed_data"),
"Decompress binary data")
.def("encode_jpeg", &ircamera::DataProcessor::encode_jpeg,
py::arg("raw_image"), py::arg("width"), py::arg("height"), py::arg("quality") = 90,
"Encode raw image data to JPEG format")
.def("decode_jpeg", &ircamera::DataProcessor::decode_jpeg, py::arg("jpeg_data"),
"Decode JPEG data to raw image")
.def("update_statistics", &ircamera::DataProcessor::update_statistics, py::arg("value"),
"Update running statistics with new value")
.def("get_mean", &ircamera::DataProcessor::get_mean,
"Get current mean of processed values")
.def("get_variance", &ircamera::DataProcessor::get_variance,
"Get current variance of processed values")
.def("get_std_deviation", &ircamera::DataProcessor::get_std_deviation,
"Get current standard deviation of processed values")
.def("get_sample_count", &ircamera::DataProcessor::get_sample_count,
"Get number of samples processed")
.def("reset_statistics", &ircamera::DataProcessor::reset_statistics,
"Reset all statistics")
.def("add_sample", &ircamera::DataProcessor::add_sample,
py::arg("timestamp"), py::arg("value"),
"Add timestamped sample to buffer")
.def("get_recent_samples", &ircamera::DataProcessor::get_recent_samples, py::arg("time_window_seconds"),
"Get samples within specified time window")
.def("clear_buffer", &ircamera::DataProcessor::clear_buffer,
"Clear sample buffer")
.def("get_buffer_size", &ircamera::DataProcessor::get_buffer_size,
"Get current buffer size");

// ThermalProcessor class
py::class_<ircamera::ThermalProcessor>(m,
"ThermalProcessor")
.

def(py::init<>(),

"Create ThermalProcessor instance")
.def("raw_to_celsius", &ircamera::ThermalProcessor::raw_to_celsius,
py::arg("raw_data"), py::arg("emissivity") = 0.95,
"Convert raw thermal data to Celsius temperatures")
.def("apply_colormap", &ircamera::ThermalProcessor::apply_colormap,
py::arg("temp_data"), py::arg("colormap") = "jet",
"Apply colormap to temperature data")
.def("apply_gaussian_blur", &ircamera::ThermalProcessor::apply_gaussian_blur,
py::arg("image"), py::arg("width"), py::arg("height"), py::arg("sigma"),
"Apply Gaussian blur to thermal image")
.def("apply_histogram_equalization", &ircamera::ThermalProcessor::apply_histogram_equalization,
py::arg("image"),
"Apply histogram equalization to enhance thermal image")
.def("find_min_temperature", &ircamera::ThermalProcessor::find_min_temperature,
py::arg("temp_data"),
"Find minimum temperature in data")
.def("find_max_temperature", &ircamera::ThermalProcessor::find_max_temperature,
py::arg("temp_data"),
"Find maximum temperature in data")
.def("calculate_average_temperature", &ircamera::ThermalProcessor::calculate_average_temperature,
py::arg("temp_data"),
"Calculate average temperature")
.def("find_hotspot_location", &ircamera::ThermalProcessor::find_hotspot_location,
py::arg("temp_data"), py::arg("width"), py::arg("height"),
"Find location of hottest point in thermal image");

// MessageProcessor class
py::class_<ircamera::MessageProcessor>(m,
"MessageProcessor")
.

def(py::init<>(),

"Create MessageProcessor instance")
.def("create_hello_message", &ircamera::MessageProcessor::create_hello_message,
py::arg("device_id"), py::arg("capabilities"),
"Create HELLO protocol message")
.def("create_sync_request", &ircamera::MessageProcessor::create_sync_request,
py::arg("timestamp_ms"),
"Create time synchronization request message")
.def("create_sync_response", &ircamera::MessageProcessor::create_sync_response,
py::arg("pc_timestamp"), py::arg("device_timestamp"),
"Create time synchronization response message")
.def("create_ack_message", &ircamera::MessageProcessor::create_ack_message,
py::arg("command"),
"Create acknowledgment message")
.def("create_error_message", &ircamera::MessageProcessor::create_error_message,
py::arg("command"), py::arg("error_code"), py::arg("message"),
"Create error message")
.def("parse_message", &ircamera::MessageProcessor::parse_message,
py::arg("message"), py::arg("type"), py::arg("params"),
"Parse protocol message")
.def("validate_message_format", &ircamera::MessageProcessor::validate_message_format,
py::arg("message"),
"Validate message format")
.def("get_current_timestamp_ms", &ircamera::MessageProcessor::get_current_timestamp_ms,
"Get current timestamp in milliseconds")
.def("calculate_time_offset", &ircamera::MessageProcessor::calculate_time_offset,
py::arg("t1"), py::arg("t2"), py::arg("t3"), py::arg("t4"),
"Calculate time offset using NTP algorithm")
.def("calculate_round_trip_time", &ircamera::MessageProcessor::calculate_round_trip_time,
py::arg("t1"), py::arg("t3"),
"Calculate round-trip time");

// Processing utility functions
py::module_ processing = m.def_submodule("processing", "Signal processing utilities");

processing.def("apply_lowpass_filter", &ircamera::processing::apply_lowpass_filter,
py::arg("data"), py::arg("cutoff_hz"), py::arg("sample_rate"),
"Apply lowpass filter to signal data");

processing.def("apply_highpass_filter", &ircamera::processing::apply_highpass_filter,
py::arg("data"), py::arg("cutoff_hz"), py::arg("sample_rate"),
"Apply highpass filter to signal data");

processing.def("apply_notch_filter", &ircamera::processing::apply_notch_filter,
py::arg("data"), py::arg("notch_hz"), py::arg("sample_rate"),
"Apply notch filter to remove specific frequency");

processing.def("calculate_mean", &ircamera::processing::calculate_mean,
py::arg("data"),
"Calculate mean of data array");

processing.def("calculate_std", &ircamera::processing::calculate_std,
py::arg("data"),
"Calculate standard deviation of data array");

processing.def("calculate_rms", &ircamera::processing::calculate_rms,
py::arg("data"),
"Calculate RMS value of data array");

processing.def("detect_motion_artifacts", &ircamera::processing::detect_motion_artifacts,
py::arg("gsr_data"), py::arg("threshold") = 2.0,
"Detect motion artifacts in GSR data");

processing.def("detect_electrical_artifacts", &ircamera::processing::detect_electrical_artifacts,
py::arg("gsr_data"),
"Detect electrical artifacts in GSR data");

processing.def("validate_gsr_packet", &ircamera::processing::validate_gsr_packet,
py::arg("packet"),
"Validate GSR data packet format");

processing.def("calculate_signal_quality", &ircamera::processing::calculate_signal_quality,
py::arg("data"),
"Calculate signal quality metric (0-1)");

// Version and build information
m.attr("__version__") = "2.0.0";
m.attr("__build_date__") = __DATE__ " " __TIME__;

#ifdef HAVE_OPENCV
m.attr("opencv_available") = true;
#else
m.attr("opencv_available") = false;
#endif
}
