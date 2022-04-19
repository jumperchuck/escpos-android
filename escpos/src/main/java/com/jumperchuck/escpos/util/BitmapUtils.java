package com.jumperchuck.escpos.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;
import java.util.List;

public class BitmapUtils {

    public static List<Bitmap> splitByWidth(Bitmap bitmap, int width) {
        List<Bitmap> result = new ArrayList<>();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        double num = Math.ceil((double) bitmapWidth / (double) width);
        for (int i = 0; i < num; i++) {
            int x = i * width;
            int y = 0;
            int surplusHeight = bitmapHeight;
            int splitWidth = bitmapWidth - x;
            int splitHeight = Math.min(width, surplusHeight);
            Bitmap splitBitmap = Bitmap.createBitmap(bitmap, x, y, splitWidth, splitHeight);
            result.add(splitBitmap);
        }
        return result;
    }

    public static List<Bitmap> splitByHeight(Bitmap bitmap, int height) {
        List<Bitmap> result = new ArrayList<>();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        double num = Math.ceil((double) bitmapHeight / (double) height);
        for (int i = 0; i < num; i++) {
            int x = 0;
            int y = i * height;
            int surplusHeight = bitmapHeight - y;
            int splitWidth = bitmapWidth;
            int splitHeight = Math.min(height, surplusHeight);
            Bitmap splitBitmap = Bitmap.createBitmap(bitmap, x, y, splitWidth, splitHeight);
            result.add(splitBitmap);
        }
        return result;
    }
}
