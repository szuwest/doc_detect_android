package com.wegene.docdetect.utils;

import android.graphics.Point;
import android.util.Log;

/**
 * Copyright 2020 WeGene.Inc
 * Created by west on 2020-03-24.
 */
public class DetectHelper {

    private static final float OFFSET = 10;

    public static boolean isRectSimilar(Point[] fourPoints, Point[] otherPoints) {
        if (CropUtils.checkPoints(fourPoints) && CropUtils.checkPoints(otherPoints)) {
            boolean similar = false;
            for (int i=0; i<4; i++) {
                float xOffset = Math.abs(fourPoints[i].x - otherPoints[i].x);
                float yOffset = Math.abs(fourPoints[i].y - otherPoints[i].y);
                Log.d("DetectHelper", " point[" + i + "]" + " x offset=" + xOffset + " y offset=" + yOffset);
                similar = xOffset <= OFFSET && yOffset <= OFFSET;
                if (!similar) {
                    break;
                }
            }
            return similar;
        }
        return false;
    }
}
