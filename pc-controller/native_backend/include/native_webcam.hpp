

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
            std::vector <uint8_t> data; // Frame data
            int frame_number;       // Sequential frame number
            bool valid;             // Frame validity flag
        };

        class NativeWebcam {
        public:
            NativeWebcam();

            ~NativeWebcam();

            NativeWebcam(const NativeWebcam &) = delete;

            NativeWebcam &operator=(const NativeWebcam &) = delete;

            bool open(int device_id = 0, int width = 1920, int height = 1080, double fps = 30.0);

            void close();

            bool start_capture();

            bool stop_capture();

            bool is_open() const { return opened_.load(); }

            bool is_capturing() const { return capturing_.load(); }

            bool get_frame(WebcamFrame &frame);

            size_t get_queue_size() const;

            void clear_queue();

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

            bool set_property(int property, double value);

            double get_property(int property) const;

            struct Statistics {
                uint64_t total_frames;
                uint64_t dropped_frames;
                double actual_fps;
                uint64_t last_timestamp_ns;
                size_t avg_frame_size_bytes;
            };

            Statistics get_statistics() const;

            std::string get_last_error() const { return last_error_; }

            bool set_auto_exposure(bool enable);

            bool set_auto_white_balance(bool enable);

            bool take_snapshot(WebcamFrame &frame);

        private:

            std::atomic<bool> opened_{false};
            std::atomic<bool> capturing_{false};
            std::atomic<bool> should_stop_{false};

            std::unique_ptr <cv::VideoCapture> cap_;
            int device_id_{0};

            int frame_width_{1920};
            int frame_height_{1080};
            double target_fps_{30.0};

            std::unique_ptr <std::thread> capture_thread_;

            std::unique_ptr <utils::ThreadSafeQueue<WebcamFrame>> frame_queue_;

            std::atomic<int> frame_counter_{0};

            mutable std::atomic <uint64_t> total_frames_{0};
            mutable std::atomic <uint64_t> dropped_frames_{0};
            mutable std::atomic <uint64_t> last_timestamp_ns_{0};
            mutable std::atomic <uint64_t> total_frame_size_{0};

            mutable std::string last_error_;

            std::chrono::steady_clock::time_point start_time_;
            std::chrono::steady_clock::time_point last_frame_time_;

            void capture_thread_func();

            uint64_t get_current_timestamp_ns();

            void update_statistics(size_t frame_size);

            bool validate_frame(const cv::Mat &mat);

            WebcamFrame mat_to_frame(const cv::Mat &mat);
        };

    } // namespace sensors
} // namespace ircamera
