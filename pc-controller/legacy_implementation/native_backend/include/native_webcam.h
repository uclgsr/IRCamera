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

    struct FrameData {
        uint64_t timestamp_ns;
        int width;
        int height;
        int channels;
        std::shared_ptr<uint8_t[]> data;
        size_t data_size;
        int frame_number;
    };

    struct CameraConfig {
        int device_id = 0;
        int width = 1920;
        int height = 1080;
        double fps = 30.0;
        int fourcc = cv::VideoWriter::fourcc('M', 'J', 'P', 'G');
        bool auto_exposure = true;
        double exposure = -1;
        double gain = -1;
    };

    class NativeWebcam {
    public:
        using FrameCallback = std::function<void(const FrameData &)>;

        explicit NativeWebcam(int device_id = 0);

        ~NativeWebcam();

        bool open_camera(const CameraConfig &config = CameraConfig{});

        void close_camera();

        bool is_open() const;

        bool start_capture();

        bool stop_capture();

        bool is_capturing() const;

        bool set_resolution(int width, int height);

        bool set_fps(double fps);

        bool set_exposure(double exposure);

        bool set_gain(double gain);

        bool set_auto_exposure(bool enabled);

        void set_frame_callback(FrameCallback callback);

        FrameData get_latest_frame();

        std::vector <FrameData> get_buffered_frames();

        void clear_buffer();

        std::vector<int> get_available_cameras();

        CameraConfig get_current_config() const;

        std::string get_camera_info() const;

        std::string get_last_error() const;

        bool test_camera_capture();

    private:
        class Impl;

        std::unique_ptr <Impl> pimpl;
    };

} 
