package com.wegene.docdetect.utils;

import android.graphics.Point;

/**
 * Created by qiulinmin on 8/3/17.
 */

public class CropUtils {

    public static double getPointsDistance(Point p1, Point p2) {
        return getPointsDistance(p1.x, p1.y, p2.x, p2.y);
    }

    public static double getPointsDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static boolean checkPoints(Point[] points) {
        return points != null && points.length == 4
                && points[0] != null && points[1] != null && points[2] != null && points[3] != null;
    }

    public static org.opencv.core.Point convert(Point p) {
        return new org.opencv.core.Point(p.x,p.y);
    }
}
