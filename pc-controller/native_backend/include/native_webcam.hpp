/**
 * High-performance webcam interface for C++ backend
 * 
 * Implements OpenCV-based webcam capture with zero-copy memory sharing
 * to Python for real-time video processing and display.
 */

#pragma once

#include <atomic>
#include <memory>
#include <string>
#include <thread>
#include <opencv2/opencv.hpp>

#include "thread_safe_queue.hpp"

namespace ircamera {
namespace sensors {

struct WebcamFrame {
    uint64_t timestamp_ns;  // Nanosecond timestamp
    int width;              // Frame width
    int height;             // Frame height
    int channels;           // Number of channels (typically 3 for BGR)
    std::vector<uint8_t> data; // Frame data
    int frame_number;       // Sequential frame number
    bool valid;             // Frame validity flag
};

class NativeWebcam {
public:
    NativeWebcam();
    ~NativeWebcam();

    // Non-copyable
    NativeWebcam(const NativeWebcam&) = delete;
    NativeWebcam& operator=(const NativeWebcam&) = delete;

    /**
     * Open webcam device
     * @param device_id Camera device ID (0 for default camera)
     * @param width Desired frame width (default: 1920)
     * @param height Desired frame height (default: 1080)
     * @param fps Desired frame rate (default: 30)
     * @return true if camera opened successfully
     */
    bool open(int device_id = 0, int width = 1920, int height = 1080, double fps = 30.0);

    /**
     * Close webcam device
     */
    void close();

    /**
     * Start frame capture
     * @return true if capture started successfully
     */
    bool start_capture();

    /**
     * Stop frame capture
     * @return true if capture stopped successfully
     */
    bool stop_capture();

    /**
     * Check if camera is open
     * @return true if open
     */
    bool is_open() const { return opened_.load(); }

    /**
     * Check if capture is active
     * @return true if capturing
     */
    bool is_capturing() const { return capturing_.load(); }

    /**
     * Get frame from queue (non-blocking)
     * @param frame Reference to store the frame
     * @return true if frame was available
     */
    bool get_frame(WebcamFrame& frame);

    /**
     * Get current queue size
     * @return Number of frames in queue
     */
    size_t get_queue_size() const;

    /**
     * Clear frame queue
     */
    void clear_queue();

    /**
     * Get camera properties
     */
    struct CameraProperties {
        int width;
        int height;
        double fps;
        int fourcc;
        std::string codec_name;
        double brightness;
        double contrast;
        double saturation;
        double gain;
        double exposure;
    };
    
    CameraProperties get_properties() const;

    /**
     * Set camera property
     * @param property OpenCV property ID (cv::CAP_PROP_*)
     * @param value Property value
     * @return true if property was set successfully
     */
    bool set_property(int property, double value);

    /**
     * Get camera property
     * @param property OpenCV property ID (cv::CAP_PROP_*)
     * @return Property value
     */
    double get_property(int property) const;

    /**
     * Get capture statistics
     */
    struct Statistics {
        uint64_t total_frames;
        uint64_t dropped_frames;
        double actual_fps;
        uint64_t last_timestamp_ns;
        size_t avg_frame_size_bytes;
    };
    
    Statistics get_statistics() const;

    /**
     * Get last error message
     * @return Error message string
     */
    std::string get_last_error() const { return last_error_; }

    /**
     * Enable/disable auto-exposure
     * @param enable true to enable auto-exposure
     * @return true if successful
     */
    bool set_auto_exposure(bool enable);

    /**
     * Enable/disable auto-white-balance
     * @param enable true to enable auto-white-balance
     * @return true if successful
     */
    bool set_auto_white_balance(bool enable);

    /**
     * Take a snapshot (single frame capture)
     * @param frame Reference to store the snapshot
     * @return true if snapshot was successful
     */
    bool take_snapshot(WebcamFrame& frame);

private:
    // Camera state
    std::atomic<bool> opened_{false};
    std::atomic<bool> capturing_{false};
    std::atomic<bool> should_stop_{false};

    // OpenCV video capture
    std::unique_ptr<cv::VideoCapture> cap_;
    int device_id_{0};

    // Capture settings
    int frame_width_{1920};
    int frame_height_{1080};
    double target_fps_{30.0};

    // Capture thread
    std::unique_ptr<std::thread> capture_thread_;
    
    // Frame queue
    std::unique_ptr<utils::ThreadSafeQueue<WebcamFrame>> frame_queue_;

    // Frame counter
    std::atomic<int> frame_counter_{0};

    // Statistics
    mutable std::atomic<uint64_t> total_frames_{0};
    mutable std::atomic<uint64_t> dropped_frames_{0};
    mutable std::atomic<uint64_t> last_timestamp_ns_{0};
    mutable std::atomic<uint64_t> total_frame_size_{0};

    // Error tracking
    mutable std::string last_error_;

    // Timing for FPS calculation
    std::chrono::steady_clock::time_point start_time_;
    std::chrono::steady_clock::time_point last_frame_time_;

    // Private methods
    void capture_thread_func();
    uint64_t get_current_timestamp_ns();
    void update_statistics(size_t frame_size);
    bool validate_frame(const cv::Mat& mat);
    WebcamFrame mat_to_frame(const cv::Mat& mat);
};

} // namespace sensors
} // namespace ircamera