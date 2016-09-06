package com.teinvdlugt.android.complexfunctioncamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 0;

    private ImageView photoIV, processedIV;
    private Bitmap photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photoIV = (ImageView) findViewById(R.id.imageViewUnprocessed);
        processedIV = (ImageView) findViewById(R.id.imageViewProcessed);
    }

    public void onClickTakePicture(View view) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");
            photoIV.setImageBitmap(photo);
        }
    }

    public void onClickProcess(View view) {
        if (photo == null) {
            Toast.makeText(MainActivity.this, "Please take a picture first", Toast.LENGTH_SHORT).show();
            return;
        }

        processedIV.setImageDrawable(null);

        new AsyncTask<Bitmap, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Bitmap... bitmaps) {
                Bitmap photo = bitmaps[0];
                final int width = photo.getWidth();
                final int height = photo.getHeight();
                final int[] colors = new int[(int) (.5 * width * width)];
                photo.getPixels(colors, 0, width, 0, height - (int) (.5 * width), width, (int) (.5 * width));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        photoIV.setImageBitmap(Bitmap.createBitmap(colors, 0, width, width, (int) (.5 * width), Bitmap.Config.RGB_565));
                    }
                });

                int[] newColors = new int[width * width];
                Arrays.fill(newColors, Color.WHITE);

                for (int i = 0; i < newColors.length; i++) {
                    int x = i % width;
                    int y = i / width;
                    double real = (x * 1. / width * 2) - 1;
                    double img = 1 - (y * 1. / width * 2);
                    double r = Math.sqrt(real * real + img * img);
                    double th = Math.atan(img / real);

                    double oldR = Math.sqrt(r);
                    double oldTh = th / 2;
                    double oldReal = Math.sin(oldTh) * oldR;
                    double oldImg = Math.cos(oldTh) * oldR;
                    int oldX = (int) ((oldReal + 1) / 2 * width);
                    int oldY = (int) ((1 - oldImg) / 2 * width);

                    if (oldX < width && oldY < height && oldX >= 0 && oldY >= 0) {
                        newColors[i] = photo.getPixel(oldX, oldY);
                    }
                }

                /*for (int i = 0; i < colors.length; i++) {
                    double real = i % width - 1;
                    double img = 1 - i / width;

                    // z -> z^2
                    // (a + bi)(a + bi) = a^2 + 2abi - b^2
                    double newReal = real * real - img * img;
                    double newImg = 2 * real * img;

                    int newX = (int) (newReal + 1);
                    int newY = (int) (1 - newImg);
                    int newI = newY * width + newX;
                    if (newI < newColors.length && newI >= 0) {
                        newColors[newI] = colors[i];
                    }
                }*/

                return Bitmap.createBitmap(newColors, 0, width, width, width, Bitmap.Config.RGB_565);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                Toast.makeText(MainActivity.this, "Done processing", Toast.LENGTH_SHORT).show();
                processedIV.setImageBitmap(bitmap);
            }
        }.execute(photo);
    }
}
