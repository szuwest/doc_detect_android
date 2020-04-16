package com.wegene.docdetect;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.wegene.docdetect.view.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Copyright 2020 WeGene.Inc
 * Created by west on 2020-03-24.
 */
public class CropImageActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 3333;
    private CropImageView ivCrop;
    private Button btnCancel;
    private Button btnOk;

    private static Bitmap srcBitmap;
    private static Point[] fourPoints;

    public static void gotoCrop(Bitmap bitmap, Point[] fourPoints, Activity context) {
        CropImageActivity.srcBitmap = bitmap;
        CropImageActivity.fourPoints = fourPoints;
        context.startActivityForResult(new Intent(context, CropImageActivity.class), REQUEST_CODE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        setupViews();
    }

    private void setupViews() {
        ivCrop = (CropImageView) findViewById(R.id.iv_crop);
        ivCrop.setImageBitmap(srcBitmap);
        ivCrop.setCropPoints(fourPoints);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnOk = (Button) findViewById(R.id.btn_ok);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (ivCrop.canRightCrop()) {
                    Bitmap crop = ivCrop.crop();
                    if (crop != null) {
//                        File tempFile = new File(getExternalFilesDir("img"), "crop.jpg");
//                        saveImage(crop, tempFile);
//                        Intent intent = new Intent();
//                        intent.setData(Uri.fromFile(tempFile));
//                        setResult(RESULT_OK, intent);
//                        finish();
                        showImage(crop);
                    } else {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                } else {
                    Toast.makeText(CropImageActivity.this, "cannot crop correctly", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showImage(Bitmap bitmap) {
        findViewById(R.id.layout_show).setVisibility(View.VISIBLE);
        ImageView showImageView = findViewById(R.id.iv_show);
        showImageView.setImageBitmap(bitmap);
    }

    private void saveImage(Bitmap bitmap, File saveFile) {
        try {
            FileOutputStream fos = new FileOutputStream(saveFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        srcBitmap = null;
        fourPoints = null;
    }
}
