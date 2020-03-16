/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.testcamera.second;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.TextureView;

import java.io.IOException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class CameraRenderer implements Runnable, TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraRenderer";
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int DRAW_INTERVAL = 1000 / 30;
    private CameraFilter selectedFilter;
    private int selectedFilterId = FilterItem.FILTER_MAP.get("origin");
    private SparseArray<CameraFilter> cameraFilterMap = new SparseArray<>();
    protected Thread renderThread;
    private Context context;
    private SurfaceTexture surfaceTexture;
    private int gwidth, gheight;

    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLContext eglContext;
    private EGL10 egl10;

    protected Camera camera;
    private SurfaceTexture cameraSurfaceTexture;
    private int cameraTextureId;
    public int backFront = 0, defaultZoom, autoExposure;
    private Camera.Parameters parameters;



    public Camera.Parameters getCameraParameters(){
        return parameters;
    }

    public void setCameraParameters(Camera.Parameters p){
        this.parameters = p;
        Log.e(TAG, "setCameraParameters: " + parameters.getZoom() );
        Camera.Parameters pt = camera.getParameters();
        pt.setZoom(parameters.getZoom());
        camera.setParameters(pt);
        camera.startPreview();
    }

    public CameraRenderer(Context context, int cameraId) {
        this.context = context;
        this.backFront = cameraId;
    }

    /** Зум камеры */
    public void incrementZoom(){
        Log.e("TAG", "incrementZoom: " + camera.getParameters().getZoom());

        if(camera.getParameters().getZoom() < 40) {
            Log.e("TAG", "incrementZoom: ");
//            camera.stopPreview();
            Camera.Parameters parameters = camera.getParameters();
            int currZoom = camera.getParameters().getZoom();
            currZoom+=5;
            parameters.setZoom(currZoom);
            Camera.Parameters pt = camera.getParameters();
            pt.setZoom(parameters.getZoom());
            camera.setParameters(pt);
//            camera.startPreview();
        }
    }
    public void decrementZoom() {
        Log.e("TAG", "decrementZoom: " + camera.getParameters().getZoom());

        if (camera.getParameters().getZoom() >= 1) {
            Log.e("TAG", "decrementZoom: ");

//            camera.stopPreview();
            Camera.Parameters parameters = camera.getParameters();
            int currZoom = camera.getParameters().getZoom();
            currZoom -= 5;
            parameters.setZoom(currZoom);
            Camera.Parameters pt = camera.getParameters();
            pt.setZoom(parameters.getZoom());
            camera.setParameters(pt);
//            camera.startPreview();
        }
    }


    /** переключение камеры*/
    public void  changeCamera() {
        Log.e(TAG, "defaultZoom: " + defaultZoom );
        camera.stopPreview();
        camera.release();

        backFront = (backFront + 1) % 2; //id = 0 or 1
        Log.e(TAG, "changeCamera backFront: " + backFront );
        camera = Camera.open(backFront);

        cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        cameraSurfaceTexture = new SurfaceTexture(cameraTextureId);

        try {
            camera.setPreviewTexture(cameraSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        camera.stopPreview();
//        Camera.Parameters parameters = getCameraParameters();
//        SimpleZoomInActivity.zoomValue = defaultZoom;
//        Log.e(TAG, "SimpleZoomInActivity.zoomValue : " + SimpleZoomInActivity.zoomValue );
//
//        parameters.setZoom(defaultZoom);
//        setCameraParameters(parameters);

        camera.startPreview();
        camera.stopPreview();
        Camera.Parameters par = getCameraParameters();
        par.setZoom(defaultZoom);
        setCameraParameters(par);
        camera.startPreview();
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureUpdated: " + camera.getParameters().getZoom());
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged: " + width  + " " + height);
        gwidth = -width;
        gheight = -height;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed: " );
//        if (camera != null) {
//            camera.stopPreview();
//            camera.release();
//        }
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
//        this.onSurfaceTextureDestroyed(surface);
//
//        surface.release();
//        surfaceTexture.release();



        return true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
        renderThread = new Thread(this);

        surfaceTexture = surface;
        gwidth = -width;
        gheight = -height;

//         Open camera
        Pair<Camera.CameraInfo, Integer> backCamera = getBackCamera();
        Pair<Camera.CameraInfo, Integer> frontCamera = getFrontCamera();
        final int backCameraId = backCamera.second;
        final int frontCameraId = frontCamera.second;
        Log.e(TAG, "onSurfaceTextureAvailable backFront: " + backFront );
        if(backFront == 0){
            camera = Camera.open(backCameraId);
        } else{
            camera = Camera.open(frontCameraId);
            backFront = 1;
        }

        Camera.Parameters p = camera.getParameters();

        p.setZoom(defaultZoom);

        //автоэкспозиция
        if (autoExposure == 0) {
            p.setAutoExposureLock(true);
        } else { p.setAutoExposureLock(false); }

        camera.setParameters(p);
        parameters = camera.getParameters();
//        holderCallback = new HolderCallback();
//        holder.addCallback(holderCallback);
        camera.startPreview();


        renderThread.start();
    }






    public void setSelectedFilter(int id) {
        Log.e(TAG, "setSelectedFilter: "  + id);
        selectedFilterId = id;
        selectedFilter = cameraFilterMap.get(id);
        Log.e(TAG, "setSelectedFilter: "  + selectedFilter);

        if (selectedFilter != null)
            Log.e(TAG, "setSelectedFilter: " );

        selectedFilter.onAttach();
    }


    @Override
    public void run() {
        Log.e(TAG, "run: " );
        initGL(surfaceTexture);

        // Setup camera filters map


        cameraFilterMap.append(FilterItem.FILTER_MAP.get("origin"), new OriginalFilter(context));

        setSelectedFilter(selectedFilterId);

        // Create texture for camera preview
        cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        cameraSurfaceTexture = new SurfaceTexture(cameraTextureId);
        // Start camera preview
        if(camera != null){
            try {
                camera.setPreviewTexture(cameraSurfaceTexture);
//            camera.setDisplayOrientation(90);
                camera.startPreview();
            } catch (IOException ioe) {
                // Something bad happened
            }
        }


        // Render loop
        while (!Thread.currentThread().isInterrupted()) {
            try {


                if (gwidth < 0 && gheight < 0)
                    GLES20.glViewport(0, 0, gwidth = -gwidth, gheight = -gheight);

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Update the camera preview texture
                synchronized (this) {
                    cameraSurfaceTexture.updateTexImage();
                }

                // Draw camera preview
                selectedFilter.draw(cameraTextureId, gwidth, gheight);
//                Log.e(TAG, "zoomrun: cameraTextureId " + cameraTextureId );
//                Log.e(TAG, "zoomrun: gwidth " + gwidth );
//                Log.e(TAG, "zoomrun: gheight " + gheight );


                // Flush
                GLES20.glFlush();
                egl10.eglSwapBuffers(eglDisplay, eglSurface);



                Thread.sleep(DRAW_INTERVAL);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        cameraSurfaceTexture.release();
        GLES20.glDeleteTextures(1, new int[]{cameraTextureId}, 0);
    }

    private void initGL(SurfaceTexture texture) {
        Log.e(TAG, "initGL: " );
        egl10 = (EGL10) EGLContext.getEGL();

        eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] version = new int[2];
        if (!egl10.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        EGLConfig eglConfig = null;
        if (!egl10.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        } else if (configsCount[0] > 0) {
            eglConfig = configs[0];
        }
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        eglContext = egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        eglSurface = egl10.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null);

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            int error = egl10.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
                return;
            }
            throw new RuntimeException("eglCreateWindowSurface failed " +
                    android.opengl.GLUtils.getEGLErrorString(error));
        }

        if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }
    }

    private Pair<Camera.CameraInfo, Integer> getBackCamera() {
        Log.e(TAG, "getBackCamera: " );
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        final int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return new Pair<>(cameraInfo, i);
            }
        }
        return null;
    }
    private Pair<Camera.CameraInfo, Integer> getFrontCamera() {
        Log.e(TAG, "getFrontCamera: " );
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        final int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return new Pair<>(cameraInfo, i);
            }
        }
        return null;
    }

    protected void deleteCameraRendereThread(){
        if(Thread.currentThread() != null){
            Thread.currentThread().interrupt();
        }
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
    }
}