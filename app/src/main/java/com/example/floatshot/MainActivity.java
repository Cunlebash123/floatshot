package com.example.floatshot;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView floatingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        floatingButton = findViewById(R.id.floatingButton);
        setupDraggableButton();
    }

    private void setupDraggableButton() {
        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY, startX, startY;
            private boolean isDragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - startX) > 10
                                || Math.abs(event.getRawY() - startY) > 10) {
                            isDragging = true;
                        }
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            captureScreen();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void captureScreen() {
        floatingButton.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            View rootView = getWindow().getDecorView().getRootView();
            final Bitmap bitmap = Bitmap.createBitmap(
                    rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(getWindow(), bitmap, copyResult -> {
                    floatingButton.setVisibility(View.VISIBLE);
                    if (copyResult == PixelCopy.SUCCESS) {
                        saveToGallery(bitmap);
                    } else {
                        toast("Capture failed (" + copyResult + ")");
                    }
                }, new Handler(Looper.getMainLooper()));
            } else {
                rootView.setDrawingCacheEnabled(true);
                Bitmap b = Bitmap.createBitmap(rootView.getDrawingCache());
                rootView.setDrawingCacheEnabled(false);
                floatingButton.setVisibility(View.VISIBLE);
                saveToGallery(b);
            }
        }, 100);
    }

    private void saveToGallery(@NonNull Bitmap bitmap) {
        String fileName = "FloatShot_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FloatShot");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri uri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            if (uri == null) {
                toast("Could not create file");
                return;
            }
            OutputStream out = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }
            toast("Saved to gallery: " + fileName);
        } catch (Exception e) {
            toast("Save failed: " + e.getMessage());
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
