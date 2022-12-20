#include <jni.h>
#include <string>
#include <jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect.hpp>
#define TAG "NativeLib"

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT void JNICALL
Java_com_example_facerecognition_MainActivity_adaptiveThresholdFromJNI(JNIEnv *env, jobject instance, jlong matAddr, jlong classifierAddr) {

// get Mat from raw address
Mat &mat = *(Mat *) matAddr;
//CascadeClassifier *classifier = (CascadeClassifier *) classifierAddr;
//
//std::vector<Rect> objects;
//
//if(classifier != nullptr)
//    classifier->detectMultiScale(mat, objects);
//
//for (auto rect : objects) {
//    rectangle(mat, Point(rect.x, rect.y), Point(rect.x + rect.width, rect.y + rect.height), Scalar(255, 0, 0), 5);
//}


clock_t begin = clock();

//cv::GaussianBlur(mat, mat, Size(9, 9), 0, 0);
//cv::adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 21, 5);


// log computation time to Android Logcat
//double totalTime = double(clock() - begin) / CLOCKS_PER_SEC;
//__android_log_print(ANDROID_LOG_INFO, TAG, "adaptiveThreshold computation time = %f seconds\n",
//totalTime);
}
}


