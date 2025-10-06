#include "native_webcam.h"
#include <iostream>
#include <chrono>
#include <algorithm>
#include <thread>

namespace ircamera {

    class NativeWebcam::Impl {
    public:
        explicit Impl(int device_id)
                : device_id_(device_id), is_open_(false), is_capturing_(false), should_stop_(false),
                  frame_callback_(nullptr), frame_counter_(0) {

            config_.device_id = device_id;
        }

        ~Impl() {
            close_camera();
        }

        bool open_camera(const CameraConfig &config) {
            if (is_open_) {
                close_camera();
            }

            config_ = config;
            device_id_ = config.device_id;

            std::cout << "Attempting to open camera device " << device_id_ << std::endl;

            capture_.open(device_id_);
            if (!capture_.isOpened()) {
                last_error_ = "Failed to open camera device " + std::to_string(device_id_) + 
                              ". Possible causes: device not found, already in use, or insufficient permissions";
                std::cerr << last_error_ << std::endl;
                
                std::cout << "Attempting to open with different API..." << std::endl;
                capture_.open(device_id_, cv::CAP_V4L2);
                if (!capture_.isOpened()) {
                    capture_.open(device_id_, cv::CAP_ANY);
                    if (!capture_.isOpened()) {
                        std::cerr << "Failed with all available APIs" << std::endl;
                        return false;
                    }
                }
            }

            std::cout << "Camera device " << device_id_ << " opened successfully" << std::endl;

            if (!apply_configuration()) {
                std::cerr << "Configuration failed: " << last_error_ << std::endl;
                close_camera();
                return false;
            }

            is_open_ = true;

            cv::Mat test_frame;
            if (!capture_.read(test_frame) || test_frame.empty()) {
                last_error_ = "Camera opened but failed to capture test frame. Camera may be initializing, try again";
                std::cerr << last_error_ << std::endl;
                
                std::this_thread::sleep_for(std::chrono::milliseconds(500));
                if (!capture_.read(test_frame) || test_frame.empty()) {
                    close_camera();
                    return false;
                }
            }

            std::cout << "Test frame captured successfully: " << test_frame.cols << "x" << test_frame.rows << std::endl;
            return true;
        }

        void close_camera() {
            if (!is_open_) {
                return;
            }

            stop_capture();

            if (capture_.isOpened()) {
                capture_.release();
            }

            is_open_ = false;
        }

        bool start_capture() {
            if (!is_open_ || is_capturing_) {
                return false;
            }

            should_stop_ = false;
            capture_thread_ = std::thread(&Impl::capture_loop, this);

            is_capturing_ = true;
            return true;
        }

        bool stop_capture() {
            if (!is_capturing_) {
                return true;
            }

            should_stop_ = true;
            if (capture_thread_.joinable()) {
                capture_thread_.join();
            }

            is_capturing_ = false;
            return true;
        }

        bool set_resolution(int width, int height) {
            config_.width = width;
            config_.height = height;

            if (is_open_) {
                capture_.set(cv::CAP_PROP_FRAME_WIDTH, width);
                capture_.set(cv::CAP_PROP_FRAME_HEIGHT, height);

                int actual_width = static_cast<int>(capture_.get(cv::CAP_PROP_FRAME_WIDTH));
                int actual_height = static_cast<int>(capture_.get(cv::CAP_PROP_FRAME_HEIGHT));

                if (actual_width != width || actual_height != height) {
                    last_error_ = "Failed to set resolution to " +
                                  std::to_string(width) + "x" + std::to_string(height) +
                                  ". Actual: " + std::to_string(actual_width) + "x" +
                                  std::to_string(actual_height);
                    return false;
                }
            }

            return true;
        }

        bool set_fps(double fps) {
            config_.fps = fps;

            if (is_open_) {
                capture_.set(cv::CAP_PROP_FPS, fps);

                double actual_fps = capture_.get(cv::CAP_PROP_FPS);
                if (std::abs(actual_fps - fps) > 1.0) {

                    std::cout << "Warning: Requested FPS " << fps
                              << " but camera set to " << actual_fps << std::endl;
                }
            }

            return true;
        }

        bool set_exposure(double exposure) {
            config_.exposure = exposure;
            config_.auto_exposure = (exposure < 0);

            if (is_open_) {
                if (config_.auto_exposure) {
                    capture_.set(cv::CAP_PROP_AUTO_EXPOSURE, 0.75);
                } else {
                    capture_.set(cv::CAP_PROP_AUTO_EXPOSURE, 0.25);
                    capture_.set(cv::CAP_PROP_EXPOSURE, exposure);
                }
            }

            return true;
        }

        bool set_gain(double gain) {
            config_.gain = gain;

            if (is_open_ && gain >= 0) {
                capture_.set(cv::CAP_PROP_GAIN, gain);
            }

            return true;
        }

        bool set_auto_exposure(bool enabled) {
            config_.auto_exposure = enabled;

            if (is_open_) {
                capture_.set(cv::CAP_PROP_AUTO_EXPOSURE, enabled ? 0.75 : 0.25);
            }

            return true;
        }

        FrameData get_latest_frame() {
            std::lock_guard <std::mutex> lock(frame_mutex_);

            if (latest_frame_.data) {
                return latest_frame_;
            }

            FrameData empty_frame;
            empty_frame.timestamp_ns = 0;
            empty_frame.width = 0;
            empty_frame.height = 0;
            empty_frame.channels = 0;
            empty_frame.data = nullptr;
            empty_frame.data_size = 0;
            empty_frame.frame_number = -1;

            return empty_frame;
        }

        std::vector <FrameData> get_buffered_frames() {
            std::lock_guard <std::mutex> lock(frame_mutex_);
            std::vector <FrameData> result;

            while (!frame_queue_.empty()) {
                result.push_back(frame_queue_.front());
                frame_queue_.pop();
            }

            return result;
        }

        void clear_buffer() {
            std::lock_guard <std::mutex> lock(frame_mutex_);
            std::queue <FrameData> empty;
            frame_queue_.swap(empty);

            latest_frame_ = FrameData{};
        }

        std::vector<int> get_available_cameras() {
            std::vector<int> available_cameras;

            for (int i = 0; i < 10; i++) {
                cv::VideoCapture test_cap(i);
                if (test_cap.isOpened()) {
                    available_cameras.push_back(i);
                    test_cap.release();
                }
            }

            return available_cameras;
        }

        std::string get_camera_info() const {
            if (!is_open_) {
                return "Camera not open";
            }

            std::stringstream ss;
            ss << "Camera Device ID: " << device_id_ << std::endl;
            ss << "Resolution: " << capture_.get(cv::CAP_PROP_FRAME_WIDTH)
               << "x" << capture_.get(cv::CAP_PROP_FRAME_HEIGHT) << std::endl;
            ss << "FPS: " << capture_.get(cv::CAP_PROP_FPS) << std::endl;
            ss << "Format: " << capture_.get(cv::CAP_PROP_FORMAT) << std::endl;
            ss << "Auto Exposure: " << (config_.auto_exposure ? "Yes" : "No") << std::endl;

            if (!config_.auto_exposure) {
                ss << "Exposure: " << capture_.get(cv::CAP_PROP_EXPOSURE) << std::endl;
            }

            if (config_.gain >= 0) {
                ss << "Gain: " << capture_.get(cv::CAP_PROP_GAIN) << std::endl;
            }

            return ss.str();
        }

        bool test_camera_capture() {
            if (!is_open_) {
                last_error_ = "Camera not open";
                return false;
            }

            cv::Mat test_frame;
            if (!capture_.read(test_frame) || test_frame.empty()) {
                last_error_ = "Failed to capture test frame";
                return false;
            }

            return true;
        }

        bool is_open() const { return is_open_.load(); }

        bool is_capturing() const { return is_capturing_.load(); }

        void set_frame_callback(FrameCallback callback) { frame_callback_ = callback; }

        CameraConfig get_current_config() const { return config_; }

        std::string get_last_error() const { return last_error_; }

    private:
        bool apply_configuration() {

            if (config_.fourcc != 0) {
                capture_.set(cv::CAP_PROP_FOURCC, config_.fourcc);
            }

            capture_.set(cv::CAP_PROP_FRAME_WIDTH, config_.width);
            capture_.set(cv::CAP_PROP_FRAME_HEIGHT, config_.height);

            capture_.set(cv::CAP_PROP_FPS, config_.fps);

            if (config_.auto_exposure) {
                capture_.set(cv::CAP_PROP_AUTO_EXPOSURE, 0.75);
            } else {
                capture_.set(cv::CAP_PROP_AUTO_EXPOSURE, 0.25);
                if (config_.exposure >= 0) {
                    capture_.set(cv::CAP_PROP_EXPOSURE, config_.exposure);
                }
            }

            if (config_.gain >= 0) {
                capture_.set(cv::CAP_PROP_GAIN, config_.gain);
            }

            int actual_width = static_cast<int>(capture_.get(cv::CAP_PROP_FRAME_WIDTH));
            int actual_height = static_cast<int>(capture_.get(cv::CAP_PROP_FRAME_HEIGHT));

            if (actual_width <= 0 || actual_height <= 0) {
                last_error_ = "Invalid frame dimensions after configuration: " +
                              std::to_string(actual_width) + "x" + std::to_string(actual_height);
                return false;
            }

            config_.width = actual_width;
            config_.height = actual_height;
            config_.fps = capture_.get(cv::CAP_PROP_FPS);

            return true;
        }

        void capture_loop() {
            cv::Mat frame;
            auto target_frame_time = std::chrono::microseconds(
                    static_cast<int64_t>(1000000.0 / config_.fps));

            while (!should_stop_) {
                auto frame_start = std::chrono::high_resolution_clock::now();

                if (!capture_.read(frame) || frame.empty()) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(10));
                    continue;
                }

                FrameData frame_data;
                frame_data.timestamp_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(
                        frame_start.time_since_epoch()).count();
                frame_data.width = frame.cols;
                frame_data.height = frame.rows;
                frame_data.channels = frame.channels();
                frame_data.data_size = frame.total() * frame.elemSize();
                frame_data.frame_number = frame_counter_++;

                frame_data.data = std::shared_ptr<uint8_t[]>(new uint8_t[frame_data.data_size]);
                std::memcpy(frame_data.data.get(), frame.data, frame_data.data_size);

                {
                    std::lock_guard <std::mutex> lock(frame_mutex_);
                    latest_frame_ = frame_data;
                    frame_queue_.push(frame_data);

                    if (frame_queue_.size() > 100) {
                        frame_queue_.pop();
                    }
                }

                if (frame_callback_) {
                    frame_callback_(frame_data);
                }

                auto frame_end = std::chrono::high_resolution_clock::now();
                auto frame_duration = frame_end - frame_start;

                if (frame_duration < target_frame_time) {
                    std::this_thread::sleep_for(target_frame_time - frame_duration);
                }
            }
        }

        int device_id_;
        CameraConfig config_;
        cv::VideoCapture capture_;

        std::atomic<bool> is_open_;
        std::atomic<bool> is_capturing_;
        std::atomic<bool> should_stop_;

        std::string last_error_;

        std::thread capture_thread_;
        std::mutex frame_mutex_;
        std::queue <FrameData> frame_queue_;
        FrameData latest_frame_;
        FrameCallback frame_callback_;

        std::atomic<int> frame_counter_;
    };

    NativeWebcam::NativeWebcam(int device_id)
            : pimpl(std::make_unique<Impl>(device_id)) {
    }

    NativeWebcam::~NativeWebcam() = default;

    bool NativeWebcam::open_camera(const CameraConfig &config) {
        return pimpl->open_camera(config);
    }

    void NativeWebcam::close_camera() {
        pimpl->close_camera();
    }

    bool NativeWebcam::is_open() const {
        return pimpl->is_open();
    }

    bool NativeWebcam::start_capture() {
        return pimpl->start_capture();
    }

    bool NativeWebcam::stop_capture() {
        return pimpl->stop_capture();
    }

    bool NativeWebcam::is_capturing() const {
        return pimpl->is_capturing();
    }

    bool NativeWebcam::set_resolution(int width, int height) {
        return pimpl->set_resolution(width, height);
    }

    bool NativeWebcam::set_fps(double fps) {
        return pimpl->set_fps(fps);
    }

    bool NativeWebcam::set_exposure(double exposure) {
        return pimpl->set_exposure(exposure);
    }

    bool NativeWebcam::set_gain(double gain) {
        return pimpl->set_gain(gain);
    }

    bool NativeWebcam::set_auto_exposure(bool enabled) {
        return pimpl->set_auto_exposure(enabled);
    }

    void NativeWebcam::set_frame_callback(FrameCallback callback) {
        pimpl->set_frame_callback(callback);
    }

    FrameData NativeWebcam::get_latest_frame() {
        return pimpl->get_latest_frame();
    }

    std::vector <FrameData> NativeWebcam::get_buffered_frames() {
        return pimpl->get_buffered_frames();
    }

    void NativeWebcam::clear_buffer() {
        pimpl->clear_buffer();
    }

    std::vector<int> NativeWebcam::get_available_cameras() {
        return pimpl->get_available_cameras();
    }

    CameraConfig NativeWebcam::get_current_config() const {
        return pimpl->get_current_config();
    }

    std::string NativeWebcam::get_camera_info() const {
        return pimpl->get_camera_info();
    }

    std::string NativeWebcam::get_last_error() const {
        return pimpl->get_last_error();
    }

    bool NativeWebcam::test_camera_capture() {
        return pimpl->test_camera_capture();
    }

} 
