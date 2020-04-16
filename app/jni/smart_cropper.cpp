//
// Created by qiulinmin on 8/1/17.
//
#include <jni.h>
#include <string>
#include <android/log.h>
#include <android_utils.h>
#include <rect_scanner.h>
#include <Scanner.h>

using namespace std;

static const char* const kClassDocScanner = "com/wegene/docdetect/SmartCropper";
#define LOG_TAG "DocDetect/DocDetect"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

static struct {
    jclass jClassPoint;
    jmethodID jMethodInit;
    jfieldID jFieldIDX;
    jfieldID jFieldIDY;
} gPointInfo;

static void initClassInfo(JNIEnv *env) {
    gPointInfo.jClassPoint = reinterpret_cast<jclass>(env -> NewGlobalRef(env -> FindClass("android/graphics/Point")));
    gPointInfo.jMethodInit = env -> GetMethodID(gPointInfo.jClassPoint, "<init>", "(II)V");
    gPointInfo.jFieldIDX = env -> GetFieldID(gPointInfo.jClassPoint, "x", "I");
    gPointInfo.jFieldIDY = env -> GetFieldID(gPointInfo.jClassPoint, "y", "I");
}

static jobject createJavaPoint(JNIEnv *env, Point point_) {
    return env -> NewObject(gPointInfo.jClassPoint, gPointInfo.jMethodInit, point_.x, point_.y);
}

static void native_scan(JNIEnv *env, jclass type, jobject srcBitmap, jobjectArray outPoint_, jboolean canny) {
    if (env -> GetArrayLength(outPoint_) != 4) {
        return;
    }

    Mat srcBitmapMat;
    bitmap_to_mat(env, srcBitmap, srcBitmapMat);
    Mat bgrData(srcBitmapMat.rows, srcBitmapMat.cols, CV_8UC3);
    cvtColor(srcBitmapMat, bgrData, COLOR_RGBA2BGR);
    scanner::Scanner docScanner(bgrData, canny);
    std::vector<Point> scanPoints = docScanner.scanPoint();
    if (scanPoints.size() == 4) {
        for (int i = 0; i < 4; ++i) {
            env -> SetObjectArrayElement(outPoint_, i, createJavaPoint(env, scanPoints[i]));
        }
    }
}

static void process_edge(JNIEnv *env, jclass type, jobject srcBitmap, jobject hedBitmap, jobjectArray outPoint_) {
    if (env -> GetArrayLength(outPoint_) != 4) {
        return;
    }
    LOGD("process_edge");
    // convert from bitmap to mat
    Mat hedMat;
    bitmap_to_mat(env, hedBitmap, hedMat);
    LOGD("hedMat type=%d", hedMat.type());
    Mat greyImage;
    cvtColor(hedMat, greyImage, COLOR_BGRA2GRAY);
    LOGD("greyImage type=%d", greyImage.type());
    // convert pixel type from int to float, and value range from (0, 255) to (0.0, 1.0)
    cv::Mat floatRgbImage;
    /**
     void convertTo( OutputArray m, int rtype, double alpha=1, double beta=0 ) const;
     */
    greyImage.convertTo(floatRgbImage, CV_32FC1, 1.0 / 255);
    LOGD("floatRgbImage type=%d", floatRgbImage.type());

    scanner::RectDetect edgeDetect;
    std::vector<cv::Point> cv_points = edgeDetect.processEdgeImage(floatRgbImage);

    if (cv_points.size() == 4) {
        std::vector<cv::Point> scaled_points;
        int original_height, original_width;
        int HED_IMAGE_WIDTH = 256;
        AndroidBitmapInfo outBitmapInfo;
        AndroidBitmap_getInfo(env, srcBitmap, &outBitmapInfo);
        original_height = outBitmapInfo.height;
        original_width = outBitmapInfo.width;

        for (int i = 0; i < cv_points.size(); i++) {
            cv::Point cv_point = cv_points[i];

            cv::Point scaled_point = cv::Point(cv_point.x * original_width / HED_IMAGE_WIDTH, cv_point.y * original_height / HED_IMAGE_WIDTH);
            scaled_points.push_back(scaled_point);

        }
        if (scaled_points.size() == 4) {
            for (int i = 0; i < 4; ++i) {
                env -> SetObjectArrayElement(outPoint_, i, createJavaPoint(env, scaled_points[i]));
            }
        }
        return;
    }
}

static vector<Point> pointsToNative(JNIEnv *env, jobjectArray points_) {
    int arrayLength = env->GetArrayLength(points_);
    vector<Point> result;
    for(int i = 0; i < arrayLength; i++) {
        jobject point_ = env -> GetObjectArrayElement(points_, i);
        int pX = env -> GetIntField(point_, gPointInfo.jFieldIDX);
        int pY = env -> GetIntField(point_, gPointInfo.jFieldIDY);
        result.push_back(Point(pX, pY));
    }
    return result;
}

static void native_crop(JNIEnv *env, jclass type, jobject srcBitmap, jobjectArray points_, jobject outBitmap) {
    std::vector<Point> points = pointsToNative(env, points_);
    if (points.size() != 4) {
        return;
    }
    Point leftTop = points[0];
    Point rightTop = points[1];
    Point rightBottom = points[2];
    Point leftBottom = points[3];

    Mat srcBitmapMat;
    bitmap_to_mat(env, srcBitmap, srcBitmapMat);

    AndroidBitmapInfo outBitmapInfo;
    AndroidBitmap_getInfo(env, outBitmap, &outBitmapInfo);
    Mat dstBitmapMat;
    int newHeight = outBitmapInfo.height;
    int newWidth = outBitmapInfo.width;
    dstBitmapMat = Mat::zeros(newHeight, newWidth, srcBitmapMat.type());

    std::vector<Point2f> srcTriangle;
    std::vector<Point2f> dstTriangle;

    srcTriangle.push_back(Point2f(leftTop.x, leftTop.y));
    srcTriangle.push_back(Point2f(rightTop.x, rightTop.y));
    srcTriangle.push_back(Point2f(leftBottom.x, leftBottom.y));
    srcTriangle.push_back(Point2f(rightBottom.x, rightBottom.y));

    dstTriangle.push_back(Point2f(0, 0));
    dstTriangle.push_back(Point2f(newWidth, 0));
    dstTriangle.push_back(Point2f(0, newHeight));
    dstTriangle.push_back(Point2f(newWidth, newHeight));

    Mat transform = getPerspectiveTransform(srcTriangle, dstTriangle);
    warpPerspective(srcBitmapMat, dstBitmapMat, transform, dstBitmapMat.size());

    mat_to_bitmap(env, dstBitmapMat, outBitmap);
}

static JNINativeMethod gMethods[] = {

        {
                "nativeScan",
                "(Landroid/graphics/Bitmap;[Landroid/graphics/Point;Z)V",
                (void*)native_scan
        },
        {
                "processEdge",
                "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;[Landroid/graphics/Point;)V",
                (void*)process_edge
        },
        {
                "nativeCrop",
                "(Landroid/graphics/Bitmap;[Landroid/graphics/Point;Landroid/graphics/Bitmap;)V",
                (void*)native_crop
        }

};

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env = NULL;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_FALSE;
    }
    jclass classDocScanner = env->FindClass(kClassDocScanner);
    if(env -> RegisterNatives(classDocScanner, gMethods, sizeof(gMethods)/ sizeof(gMethods[0])) < 0) {
        return JNI_FALSE;
    }
    initClassInfo(env);
    return JNI_VERSION_1_4;
}
