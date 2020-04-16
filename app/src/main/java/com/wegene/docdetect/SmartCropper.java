package com.wegene.docdetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import com.wegene.docdetect.utils.CropUtils;

import java.io.IOException;


/**
 * Created by qiulinmin on 8/1/17.
 */

public class SmartCropper {

    private static ImageDetector sImageDetector = null;

    public static void buildImageDetector(Context context) {
        SmartCropper.buildImageDetector(context, null);
    }

    public static void buildImageDetector(Context context, String modelFile) {
        try {
            sImageDetector = new ImageDetector(context, modelFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  输入图片扫描边框顶点
     * @param srcBmp 扫描图片
     * @return 返回顶点数组，以 左上，右上，右下，左下排序
     */
    public static Point[] scan(Bitmap srcBmp) {
        if (srcBmp == null) {
            throw new IllegalArgumentException("srcBmp cannot be null");
        }
        boolean userCanny = true;
        if (sImageDetector != null) {
            Bitmap bitmap = sImageDetector.detectImage(srcBmp);
            if (bitmap != null) {
                srcBmp = Bitmap.createScaledBitmap(bitmap, srcBmp.getWidth(), srcBmp.getHeight(), false);
                userCanny = false;
            }
        }
        Point[] outPoints = new Point[4];
        nativeScan(srcBmp, outPoints, userCanny);
        return outPoints;
    }

    public static Bitmap hedDetect(Bitmap srcBmp) {
        return sImageDetector.detectImage(srcBmp);
    }

    //采用OpenCV的findContours等方法提取4个点
    public static Point[] scanPoints(Bitmap srcBitmap, Bitmap hedBitmap) {
        Point[] outPoints = new Point[4];
        nativeScan(Bitmap.createScaledBitmap(hedBitmap, srcBitmap.getWidth(), srcBitmap.getHeight(), false), outPoints, false);
        return outPoints;
    }


    private static final int HED_WIDHT = 256;
    //自己做数学算法计算4个点（从HED库移植过来）
    public static Point[] scanPoints2(Bitmap srcBitmap, Bitmap hedBitmap) {
        if (srcBitmap == null || hedBitmap == null) {
            throw new IllegalArgumentException("srcBitmap and hedBitmap cannot be null");
        }
        if (hedBitmap.getHeight() != HED_WIDHT || hedBitmap.getWidth() != HED_WIDHT) {
            throw new IllegalArgumentException("hedBitmap size must 256X256");
        }
        Point[] outPoints = new Point[4];
        processEdge(srcBitmap, hedBitmap, outPoints);
        return outPoints;
    }

    /**
     * 裁剪图片
     * @param srcBmp 待裁剪图片
     * @param cropPoints 裁剪区域顶点，顶点坐标以图片大小为准
     * @return 返回裁剪后的图片
     */
    public static Bitmap crop(Bitmap srcBmp, Point[] cropPoints) {
        if (srcBmp == null || cropPoints == null) {
            throw new IllegalArgumentException("srcBmp and cropPoints cannot be null");
        }
        if (cropPoints.length != 4) {
            throw new IllegalArgumentException("The length of cropPoints must be 4 , and sort by leftTop, rightTop, rightBottom, leftBottom");
        }
        Point leftTop = cropPoints[0];
        Point rightTop = cropPoints[1];
        Point rightBottom = cropPoints[2];
        Point leftBottom = cropPoints[3];

        int cropWidth = (int) ((CropUtils.getPointsDistance(leftTop, rightTop)
                + CropUtils.getPointsDistance(leftBottom, rightBottom))/2);
        int cropHeight = (int) ((CropUtils.getPointsDistance(leftTop, leftBottom)
                + CropUtils.getPointsDistance(rightTop, rightBottom))/2);

        Bitmap cropBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888);
        SmartCropper.nativeCrop(srcBmp, cropPoints, cropBitmap);
        return cropBitmap;
    }

    private static native void nativeScan(Bitmap srcBitmap, Point[] outPoints, boolean canny);

    private static native void processEdge(Bitmap srcBitmap, Bitmap hedBitmap, Point[] outPoints);

    private static native void nativeCrop(Bitmap srcBitmap, Point[] points, Bitmap outBitmap);

    static {
        try {
            System.loadLibrary("document_detect");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            Log.e("SmartCropper", "loadLibrary document_detect failed");
        }
    }

}
