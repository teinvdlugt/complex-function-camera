package com.teinvdlugt.android.complexfunctioncamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private static final int CAMERA_REQUEST_CODE = 0;
    private static final int SQUARE = 1;
    private static final int SQUARE_ROOT = 2;

    private ImageView photoIV, processedIV;
    private TextView originTV;
    private Point origin = new Point(0, 0);
    private Bitmap photo;
    private int function = SQUARE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photoIV = (ImageView) findViewById(R.id.imageViewUnprocessed);
        processedIV = (ImageView) findViewById(R.id.imageViewProcessed);
        originTV = (TextView) findViewById(R.id.origin_textView);

        photoIV.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        origin = new Point((int) event.getX(), (int) event.getY());
        originTV.setText("Origin: " + origin.toString());
        return true;
    }

    public void onClickGrid(View view) {
        photo = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        photoIV.setImageBitmap(photo);
    }

    public void onClickTakePicture(View view) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, R.string.no_camera_available, Toast.LENGTH_SHORT).show();
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
                final int[] colors = new int[(int) (width * width)];
                photo.getPixels(colors, 0, width, 0, 0, width, height);

                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        photoIV.setImageBitmap(Bitmap.createBitmap(colors, 0, width, width, (int) (.5 * width), Bitmap.Config.RGB_565));
                    }
                });*/

                int newWidth = width * 2;  // Make the output image twice as big in each dimension
                int[] newColors = new int[newWidth * newWidth];
                Arrays.fill(newColors, Color.WHITE);

                for (int i = 0; i < newColors.length; i++) {
                    int x = i % newWidth;
                    int y = i / newWidth;
                    double real = ((x * 1. / newWidth * 2) - 1) * 2;
                    double img = (1 - (y * 1. / newWidth * 2)) * 2;
                    double r = Math.sqrt(real * real + img * img);
                    double th = Math.atan(img / real);

                    double oldR, oldTh;
                    if (function == SQUARE_ROOT) {
                        oldR = r * r;
                        oldTh = th * 2;
                    } else {
                        oldR = Math.sqrt(r);
                        oldTh = th / 2;
                    }
                    double oldReal = Math.sin(oldTh) * oldR;
                    double oldImg = Math.cos(oldTh) * oldR;
                    int oldX = (int) (width * oldReal / 2 + origin.x);
                    int oldY = (int) (origin.y - oldImg / 2 * width);

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

                return Bitmap.createBitmap(newColors, 0, newWidth, newWidth, newWidth, Bitmap.Config.RGB_565);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                Toast.makeText(MainActivity.this, "Done processing", Toast.LENGTH_SHORT).show();
                processedIV.setImageBitmap(bitmap);
            }
        }.execute(photo);
    }

    public void onClickSquareRoot(View view) {
        function = SQUARE_ROOT;
    }

    public void onClickSquare(View view) {
        function = SQUARE;
    }
}
