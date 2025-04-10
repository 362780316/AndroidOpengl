#include "FaceTrack.h"

FaceTrack::FaceTrack(const char *model, const char *seeta) {
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(makePtr<CascadeClassifier>(model)); // OpenCV主探测器
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(makePtr<CascadeClassifier>(model)); // OpenCV跟踪探测器
    DetectionBasedTracker::Parameters detectorParams;
    // OpenCV创建追踪器，为了下面的（开始跟踪，停止跟踪）
    tracker = makePtr<DetectionBasedTracker>(mainDetector, trackingDetector, detectorParams);

    // TODO >>>>>>>>>>>>>>>>>>>>>>> 上面是OpenCV模板代码人脸追踪区域， 下面是Seeta人脸关键点代码+OpenCV >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    faceAlignment = makePtr<seeta::FaceAlignment>(seeta); // Seeta中科院关键特征点
}

void FaceTrack::startTracking() { // OpenCV开启追踪器
    tracker->run();
}

void FaceTrack::stopTracking() { // OpenCV关闭追踪器
    tracker->stop();
}

/**
 * 拿到数据 给 OpenGL着色器代码，对眼睛放大
 * @param src   待检测的图像数据
 * @param rects 检测成果的成果 保存Java需要的人脸坐标信息
 */
void FaceTrack::detector(Mat src, vector<Rect2f> &rects) {
    vector<Rect> faces;
    // src :灰度图（去除 不需要的色彩信息）
    tracker->process(src); // 处理灰度图(OpenCV的东西，灰度，色彩 影响我们人脸追踪)
    tracker->getObjects(faces); // 得到人脸框框的Rect - OpenCV的东西
    if (faces.size()) { // 判断true，说明非零，有人脸
        Rect face = faces[0]; // 有人脸就去第一个人脸，我没有去管，多个人脸了哦
        // 然后把跟踪出来的这个人脸，保存到rects里面去
        rects.push_back(Rect2f(face.x, face.y, face.width, face.height));

        // TODO 根据前面的OpenCV人脸最终成果， 做 人脸关键点定位
        seeta::ImageData image_data(src.cols, src.rows); // image_data就是图像数据
        image_data.data = src.data; // (人脸的信息 要送去检测的) = (把待检测图像)

        // 人脸追踪框 信息绑定  人脸关键点定位
        seeta::FaceInfo face_info; // 人脸的信息 要送去检测的
        seeta::Rect bbox; // 人脸框框的信息
        bbox.x = face.x;           // 把人脸信息的x 给 face_info
        bbox.y = face.y;           // 把人脸信息的y 给 face_info
        bbox.width = face.width;   // 把人脸信息的width 给 face_info
        bbox.height = face.height; // 把人脸信息的height 给 face_info
        face_info.bbox = bbox;     // 把人脸信息的bbox 给 face_info

        seeta::FacialLandmark points[5]; // 特征点的检测，固定了5个点

        // 执行采集出 五个点
        faceAlignment->PointDetectLandmarks(image_data, face_info, points);

        // 把五个点 转换 ，因为第二个参数需要 Rect2f
        for (int i = 0; i < 5; ++i) { // 为何不需要宽和高，只需要保存点就够了
            rects.push_back(Rect2f(points[i].x, points[i].y, 0, 0));
        }
    }
}