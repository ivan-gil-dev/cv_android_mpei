package com.example.facerecognition

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.opencv.objdetect.FaceDetectorYN
import java.io.File
import java.io.FileOutputStream


class MainActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {

    //CameraView
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    //Каскад Хаара
    private var mClassifier : CascadeClassifier? = null

    //Нейросеть
    private var faceDetector :FaceDetectorYN? = null

    //Кнопка переключения алгоритмов
    private var toggleAlgorithm : Button? = null
    //Кнопка переключения между фронтальной и основной камерой
    private var toggleCamera : Button? = null
    //Кнопка включающая размытие
    private var toggleBlur : Button? = null

    private var useNeuralNet = true //Использовать нейросеть
    private var useFrontalCamera = 0 // Использовать фронтальную камеру
    private var blurBackground = true;// Замылить фон

    //Загрузка модели каскада из APK в OpenCV
    fun InitClassifier(){

        val `is` = resources.openRawResource(R.raw.haarcascade_frontalface_default)
        //Создать папку cascade
        val cascadeDir = getDir("cascade", MODE_PRIVATE)
        //Создать файл haarcascade_frontalface_default.xml
        var caseFile = File(cascadeDir, "haarcascade_frontalface_default.xml")

        //Передать байты из ресурса APK в caseFile
        val fos = FileOutputStream(caseFile)

        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (`is`.read(buffer).also { bytesRead = it } != -1) {
            fos.write(buffer, 0, bytesRead)
        }
        `is`.close()
        fos.close()

        //Считать файл caseFile с помощью OpenCV
        mClassifier = CascadeClassifier(caseFile.getAbsolutePath())
        if (mClassifier!!.empty()) {
            mClassifier = null
        } else {
            Log.i("Cascade", "Classifier loaded successfully")
        }
    }

    //Загрузка модели нейросети из APK в OpenCV
    fun InitDetector(){
        val `is` = resources.openRawResource(R.raw.face_detection_yunet_quantized)
        //Создать папку cascade
        val cascadeDir = getDir("cascade", MODE_PRIVATE)
        //Создать файл face_detection_yunet_quantized.onnx
        var caseFile = File(cascadeDir, "face_detection_yunet_quantized.onnx")

        //Передать байты из ресурса APK в caseFile
        val fos = FileOutputStream(caseFile)

        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (`is`.read(buffer).also { bytesRead = it } != -1) {
            fos.write(buffer, 0, bytesRead)
        }
        `is`.close()
        fos.close()

        //Считать файл caseFile с помощью OpenCV
        faceDetector = FaceDetectorYN.create(caseFile.getAbsolutePath(), "", Size(320.0, 320.0))
    }

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("facerecognition")

                    //Инициализировать нейросеть и каскад Хаара
                    InitClassifier()
                    InitDetector()
                    mOpenCvCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        //Полноэкранный режим
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        //Разрешение на использование камеры
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )

        setContentView(R.layout.activity_main)

        //Настройка cameraView
        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.java_camera_view)
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        mOpenCvCameraView!!.setCameraIndex(useFrontalCamera)

        toggleAlgorithm = findViewById(R.id.toggleAlgorithm)
        toggleCamera = findViewById(R.id.toggleCamera)
        toggleBlur = findViewById(R.id.toggleBlur)

        //По нажатию кнопки менять алгоритм
        toggleAlgorithm?.setOnClickListener {
            useNeuralNet = !useNeuralNet

            if (useNeuralNet)
                toggleAlgorithm?.text = "Нейросеть"
            else
                toggleAlgorithm?.text = "Каскад Хаара"
        }

        //По нажатию кнопки изменить камеру
        toggleCamera?.setOnClickListener {
            if(useFrontalCamera == 0)
                useFrontalCamera = 1
            else
                useFrontalCamera = 0

            mOpenCvCameraView!!.disableView();
            mOpenCvCameraView!!.setCameraIndex(useFrontalCamera);
            mOpenCvCameraView!!.enableView();
        }

        //Вкл/Выкл размытие
        toggleBlur?.setOnClickListener {
            blurBackground = !blurBackground
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView!!.setCameraPermissionGranted()
                } else {
                    val message = "Camera permission was not granted"
                    Log.e(TAG, message)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Log.e(TAG, "Unexpected permission request")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}

    override fun onCameraViewStopped() {}

    //Для полученного кадра из камеры
    override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat {

        // Получить текущий кадр камеры в rgba формате
        var mat = frame.rgba()

        // Вызов из C++ (не используется)
        //adaptiveThresholdFromJNI(mat.nativeObjAddr, mClassifier!!.nativeObjAddr)

        if(!useNeuralNet){


            val facedetections = MatOfRect()
            //Получить массив из найденных лиц
            mClassifier!!.detectMultiScale(mat, facedetections)

            if(blurBackground){
                var background = mat.clone()
                //Размылить исходное изображение
                Imgproc.GaussianBlur(background, background, Size(25.0, 25.0), 0.0)

                //Копировать фрагменты изображения с найденными лицами на размыленное исходное изображение
                for (react in facedetections.toArray()) {
                    var insetMat = mat.submat(Rect(react.x, react.y, react.width, react.height))
                    var insetMat2 = background.submat(Rect(react.x, react.y, react.width, react.height))
                    insetMat.copyTo(insetMat2)
                }

                return background;
            }

            //Выделить рамкой лица
            for (react in facedetections.toArray()) {
                Imgproc.rectangle(
                    mat, Point(react.x.toDouble(), react.y.toDouble()),
                    Point((react.x + react.width).toDouble(), (react.y + react.height).toDouble()),
                    Scalar(255.0, 0.0, 0.0)
                )
            }
        }else{

            var faces = Mat()
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR)
            faceDetector?.inputSize = mat.size()
            //Получить массив из найденных лиц
            faceDetector?.detect(mat, faces)

            if(blurBackground){
                var background = mat.clone()
                //Размылить исходное изображение
                Imgproc.GaussianBlur(background, background, Size(25.0, 25.0), 0.0)

                //Копировать фрагменты изображения с найденными лицами на размыленное исходное изображение
                if(faces.rows() > 0)
                    for (i in 0..faces.rows() - 1) {
                        var x1 = faces[i, 0][0]
                        var y1 = faces[i, 1][0]
                        var x2 = faces[i, 2][0]
                        var y2 = faces[i, 3][0]

                        var sizemat = mat.size()
                        if(x1 >= 0.0 && y1 >= 0.0 && x2 >= 0.0 && y2 >= 0.0 &&
                            x1 <= sizemat.width && y1 <= sizemat.height && x2 <= sizemat.width && y2 <= sizemat.height
                        ){
                            var insetMat = mat.submat(Rect(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt()))
                            var insetMat2 = background.submat(Rect(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt()))
                            insetMat.copyTo(insetMat2)
                        }
                    }

                return background;
            }

            //Выделить рамкой лица
            for (i in 0..faces.rows() - 1) {
                var x1 = faces[i, 0][0]
                var y1 = faces[i, 1][0]
                var x2 = faces[i, 2][0]
                var y2 = faces[i, 3][0]


                Imgproc.rectangle(mat,  Rect(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt()),
                    Scalar(0.0, 255.0, 0.0))
            }

        }



        // return processed frame for live preview
        return mat
    }

    private external fun adaptiveThresholdFromJNI(matAddr: Long, classifierAddr: Long)

    companion object {

        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST = 1
    }
}