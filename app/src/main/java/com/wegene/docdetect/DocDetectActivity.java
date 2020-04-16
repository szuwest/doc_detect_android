package com.wegene.docdetect;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.wegene.docdetect.utils.CropUtils;
import com.wegene.docdetect.utils.DetectHelper;

import java.util.Collections;
import java.util.List;

public class DocDetectActivity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "DocDetect";

    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView hedDetectImageView;
    private TextView infoTextView;

    private volatile boolean isStop = false;

    public DocDetectActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_doc_detect);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        hedDetectImageView = findViewById(R.id.iv_detect);
        infoTextView = findViewById(R.id.tv_info);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        isStop = true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            mOpenCvCameraView.enableView();
            isStop = false;
        } else {
            Log.d(TAG, "Internal OpenCV library not found.");
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    private static final Scalar LINE_COLOR     = new Scalar(0, 255, 0, 255);
    private static final int thickness = 4;
    private Handler handler = new Handler();
    private Point[] cornerPoints;
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final Mat matRbga = inputFrame.rgba();
        if (matRbga.height() == 0 || isStop) return matRbga;
        if (isFinishing() || isDestroyed()) return matRbga;

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                if (matRbga.height() == 0) return;

                long start = System.currentTimeMillis();
                Log.d(TAG, "camera frame width=" + matRbga.width() + " height=" + matRbga.height());
                final Bitmap bitmap = Bitmap.createBitmap(matRbga.width(), matRbga.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(matRbga, bitmap);
                Log.d(TAG, "matToBitmap spend " + (System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                final Bitmap hedBitmap = SmartCropper.hedDetect(bitmap);
                long hedSpend = System.currentTimeMillis() - start;
                Log.d(TAG, "hedImage width=" + hedBitmap.getWidth() + " height=" + hedBitmap.getHeight());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        hedDetectImageView.setImageBitmap(hedBitmap);
                    }
                });
                start = System.currentTimeMillis();
//                final Point[] fourPoint = SmartCropper.scanPoints(bitmap, hedBitmap);
                final Point[] fourPoint = SmartCropper.scanPoints2(bitmap, hedBitmap);
                long scanSpend = System.currentTimeMillis() - start;
                final String info = "hed spend " + hedSpend + "\n" + "scan spend " + scanSpend;
                Log.d(TAG, info);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        infoTextView.setText(info);
                    }
                });

//                final Point[] fourPoint = SmartCropper.scan(bitmap);

                if (CropUtils.checkPoints(fourPoint)) {
                    //上次检测的点跟这次基本一样，就裁剪
                    if (DetectHelper.isRectSimilar(cornerPoints, fourPoint) && !isStop) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                gotoCrop(bitmap, fourPoint);
                            }
                        });
                    }
                    cornerPoints = fourPoint;
                }

                if (cornerPoints != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (matRbga.height() == 0 || isStop) return;
                            Imgproc.line(matRbga, CropUtils.convert(cornerPoints[0]),CropUtils.convert(cornerPoints[1]),LINE_COLOR,thickness);
                            Imgproc.line(matRbga, CropUtils.convert(cornerPoints[1]),CropUtils.convert(cornerPoints[2]),LINE_COLOR,thickness);
                            Imgproc.line(matRbga, CropUtils.convert(cornerPoints[2]),CropUtils.convert(cornerPoints[3]),LINE_COLOR,thickness);
                            Imgproc.line(matRbga, CropUtils.convert(cornerPoints[3]),CropUtils.convert(cornerPoints[0]),LINE_COLOR,thickness);
                        }
                    });
                }
            }
        });
        if (cornerPoints != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Imgproc.line(matRbga, CropUtils.convert(cornerPoints[0]),CropUtils.convert(cornerPoints[1]),LINE_COLOR,thickness);
                    Imgproc.line(matRbga, CropUtils.convert(cornerPoints[1]),CropUtils.convert(cornerPoints[2]),LINE_COLOR,thickness);
                    Imgproc.line(matRbga, CropUtils.convert(cornerPoints[2]),CropUtils.convert(cornerPoints[3]),LINE_COLOR,thickness);
                    Imgproc.line(matRbga, CropUtils.convert(cornerPoints[3]),CropUtils.convert(cornerPoints[0]),LINE_COLOR,thickness);
                }
            });
        }
        return matRbga;
    }

    private void gotoCrop(Bitmap bitmap, Point[] fourPoints) {
        CropImageActivity.gotoCrop(bitmap, fourPoints, this);
    }
}
