#ifndef FACETRACK_H
#define FACETRACK_H

#include <opencv2/opencv.hpp> // OpenCV人脸框框检测用的
#include <opencv2/objdetect.hpp>  // OpenCV人脸框框检测用的
#include "FaceAlignment/include/face_alignment.h" // 人脸5关键点定位用的
#include <vector> // 把最终成果 人脸关键点各个信息保存的容器

using namespace std; // C++标准STD的命名空间
using namespace cv; // OpenCV提供的命名空间

// 级联探测器适配器，下面的代码是从OpenCV实例中Copy过来的
class CascadeDetectorAdapter : public DetectionBasedTracker::IDetector {
public:
    CascadeDetectorAdapter(Ptr<CascadeClassifier> detector) :
            IDetector(),
            Detector(detector) {
        CV_Assert(detector);
    }
    // OpenCV的探测
    void detect(const cv::Mat &Image, std::vector<cv::Rect> &objects) {
        Detector->detectMultiScale(Image, objects, scaleFactor, minNeighbours, 0, minObjSize,maxObjSize);
    }
    virtual ~CascadeDetectorAdapter() {}

private:
    CascadeDetectorAdapter();
    Ptr<CascadeClassifier> Detector; // OpenCV的探测器
};

// TODO >>>>>>>>>>>>>>>>>>>>>>> 上面是OpenCV模板代码人脸追踪区域， 下面是Seeta人脸关键点代码+OpenCV >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

class FaceTrack {
public:
    /**
     * 传入模型数据进来
     * @param model  model: opencv的模型
     * @param seeta  seeta：seeta的模型 中科院的
     */
    FaceTrack(const char *model, const char *seeta);

    /**
     * 来检测人脸的函数
     * @param src   待检测的图像
     * @param rects 输出检测后的成果，人脸的框框
     */
    void detector(Mat src, vector<Rect2f> &rects);

    void startTracking(); // 开始跟踪 OpenCV

    void stopTracking(); // 停止跟踪 OpenCV

private:
    Ptr<DetectionBasedTracker> tracker; // OpenCV的人脸追踪器
    Ptr<seeta::FaceAlignment> faceAlignment; // Seeta中科院的人脸关键点
};

#endif //FACETRACK_H
