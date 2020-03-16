package com.example.testcamera.second;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.testcamera.R;

public class MainActivity extends AppCompatActivity {


    SurfaceView sv;
    SurfaceHolder holder;
//    HolderCallback holderCallback;

    public final static String TAG = "SimpleZoomInActivity";
    private FrameLayout container;
    private static  CameraRenderer renderer;
    private TextureView textureView;
    public static Boolean updatePos = false;
    public static Boolean downKeyClicked = false;
    private View shadow;
    private LinearLayout topBarZoomIn, zoomInHelpers, selectLayout, selectCameraLayout, zoomInBar, sdCardRemovedLayout, lowBatteryLayout;
    private ImageView battery, card, headphones, zoom_in_photo, plusHelper, minusHelper, menuHelper, filterHelper, ocrHelper, pausePlayHelper;
    private Boolean doNothing = false, isStopPreview = false, sdCardExists = false;
    private TextView selectLayoutTextView, selectCameraLayoutTextView, zoomInBarText, sdCardWasRemovedTextView;
    private View grayBar, activeBar;
    private ConstraintLayout.LayoutParams params;
    public static int zoomValue = 0;
    private CountDownTimer zoomBarTimer;
    private Thread sdZoomThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //приложение на весь экран
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_main);



        container = findViewById(R.id.frame_layout);

        plusHelper = findViewById(R.id.zoom_in_plus);
        minusHelper = findViewById(R.id.zoom_in_minus);


        setupCameraPreviewView();

        plusHelper.setOnClickListener(v -> renderer.incrementZoom());
        minusHelper.setOnClickListener(v -> renderer.decrementZoom());
//        zoom_in_photo.setOnClickListener(v -> {
//            renderer.changeCamera();
//            mirroringCamera();
//        });


    }

    private void closeCamera() {

        if (renderer.camera != null) {
            renderer.camera.stopPreview();
            renderer.camera.release();
        }
        renderer.deleteCameraRendereThread();
    }

    /** убираем зеркальный эффект в короткофокусной комере*/
    private void mirroringCamera() {
        Matrix matrix = new Matrix();

        if (renderer.backFront == 1) {
            matrix.setScale(1, -1);
            matrix.postTranslate(0, 480);
        } else {
            matrix.setScale(1, 1);
        }
        textureView.setTransform(matrix);
    }

    /** убираем зеркальный эффект с фото, сделанного на короткофокусную камеру*/
    public static Bitmap mirror(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setScale(-1, 1);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }



    @SuppressLint("ClickableViewAccessibility")
    void setupCameraPreviewView() {
        renderer = new CameraRenderer(this, 0);
        textureView = new TextureView(this);
        container.addView(textureView);
//        SurfaceView s;
//        s.setListen
        textureView.setSurfaceTextureListener(renderer);
        mirroringCamera();

        textureView.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight()));

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 1 && event.getAction() == KeyEvent.ACTION_DOWN) {
            renderer.incrementZoom();
        }
        if (keyCode == 2 && event.getAction() == KeyEvent.ACTION_DOWN) {
            renderer.decrementZoom();
        }
        return true;
    }
}
