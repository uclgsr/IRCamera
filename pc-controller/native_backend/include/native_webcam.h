#pragma once

#include <opencv2/opencv.hpp>
#include <memory>
#include <string>
#include <vector>
#include <thread>
#include <queue>
#include <mutex>
#include <atomic>
#include <functional>

namespace ircamera {

/**
 * Video frame data structure for zero-copy memory sharing
 */
struct FrameData {
    uint64_t timestamp_ns;      // Nanosecond timestamp
    int width;                  // Frame width
    int height;                 // Frame height
    int channels;               // Number of channels (3 for BGR, 1 for grayscale)
    std::shared_ptr<uint8_t[]> data;  // Shared frame data buffer
    size_t data_size;           // Size of data buffer in bytes
    int frame_number;           // Sequential frame number
};

/**
 * Camera configuration structure
 */
struct CameraConfig {
    int device_id = 0;          // Camera device ID (0 for default)
    int width = 1920;           // Capture width
    int height = 1080;          // Capture height  
    double fps = 30.0;          // Frames per second
    int fourcc = cv::VideoWriter::fourcc('M','J','P','G');  // Video codec
    bool auto_exposure = true;  // Auto exposure control
    double exposure = -1;       // Manual exposure value (-1 for auto)
    double gain = -1;           // Manual gain value (-1 for auto)
};

/**
 * NativeWebcam class for high-performance local webcam capture
 * 
 * Features:
 * - OpenCV-based video capture with zero-copy frame sharing
 * - Dedicated C++ thread for continuous capture
 * - Thread-safe lock-free frame queue
 * - Multiple format support (MJPEG, YUYV, etc.)
 * - Configurable resolution and frame rate
 */
class NativeWebcam {
public:
    using FrameCallback = std::function<void(const FrameData&)>;
    
    explicit NativeWebcam(int device_id = 0);
    ~NativeWebcam();
    
    // Camera management
    bool open_camera(const CameraConfig& config = CameraConfig{});
    void close_camera();
    bool is_open() const;
    
    // Capture control
    bool start_capture();
    bool stop_capture();
    bool is_capturing() const;
    
    // Configuration
    bool set_resolution(int width, int height);
    bool set_fps(double fps);
    bool set_exposure(double exposure);
    bool set_gain(double gain);
    bool set_auto_exposure(bool enabled);
    
    // Data access
    void set_frame_callback(FrameCallback callback);
    FrameData get_latest_frame();
    std::vector<FrameData> get_buffered_frames();
    void clear_buffer();
    
    // Camera info and diagnostics
    std::vector<int> get_available_cameras();
    CameraConfig get_current_config() const;
    std::string get_camera_info() const;
    std::string get_last_error() const;
    bool test_camera_capture();
    
private:
    class Impl;
    std::unique_ptr<Impl> pimpl;
};

} // namespace ircamera