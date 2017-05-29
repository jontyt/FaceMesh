/*
 * Copyright 2016-present Tzutalin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tzutalin.dlibtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.Trace;
import android.renderscript.Matrix3f;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;
import com.tzutalin.dlibtest.PoseDetection.PoseDetection;

import junit.framework.Assert;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static org.opencv.core.Core.norm;
import static org.opencv.core.Core.transpose;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_64FC1;


/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with dlib lib.
 */
public class OnGetImageListener implements OnImageAvailableListener {
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private static final int INPUT_SIZE = 224;
    private static final String TAG = "OnGetImageListener";

    private int mScreenRotation = 90;

    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;

    private boolean mIsComputing = false;
    private Handler mInferenceHandler;

    private Context mContext;
    private FaceDet mFaceDet;
    private TrasparentTitleView mTransparentTitleView;
    private FloatingCameraWindow mWindow;
    private Paint mFaceLandmardkPaint;
    private PoseDetection poseDetection = new PoseDetection();

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d("cunt", "initialised in get image listener");
        }
        else {
            Log.d("cunt", "fukn failed");
        }
    }

    public void initialize(
            final Context context,
            final AssetManager assetManager,
            final TrasparentTitleView scoreView,
            final Handler handler) {
        this.mContext = context;
        this.mTransparentTitleView = scoreView;
        this.mInferenceHandler = handler;
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        mWindow = new FloatingCameraWindow(mContext);

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
    }

    public void deInitialize() {
        synchronized (OnGetImageListener.this) {
            if (mFaceDet != null) {
                mFaceDet.release();
            }

            if (mWindow != null) {
                mWindow.release();
            }
        }
    }


    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {

        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        Log.d(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            mScreenRotation = 270; //before 90
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 0;
        }

        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            // No mutex needed as this method is not reentrant.
            if (mIsComputing) {
                image.close();
                return;
            }
            mIsComputing = true;

            Trace.beginSection("imageAvailable");

            final Plane[] planes = image.getPlanes();

            // Initialize the storage bitmaps once when the resolution is known.
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                Log.d(TAG, String.format("Initializing at size %dx%d", mPreviewWdith, mPreviewHeight));
                mRGBBytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

                mYUVBytes = new byte[planes.length][];
                for (int i = 0; i < planes.length; ++i) {
                    mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
                }
            }

            for (int i = 0; i < planes.length; ++i) {
                planes[i].getBuffer().get(mYUVBytes[i]);
            }

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888(
                    mYUVBytes[0],
                    mYUVBytes[1],
                    mYUVBytes[2],
                    mRGBBytes,
                    mPreviewWdith,
                    mPreviewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    false);

            image.close();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.e(TAG, "Exception!", e);
            Trace.endSection();
            return;
        }

        mRGBframeBitmap.setPixels(mRGBBytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);
        drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);

        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(mCroppedBitmap);
        }

        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                            mTransparentTitleView.setText("Copying landmark model to " + Constants.getFaceShapeModelPath());
                            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                        }

                        long startTime = System.currentTimeMillis();
                        List<VisionDetRet> results;
                        synchronized (OnGetImageListener.this) {
                            results = mFaceDet.detect(mCroppedBitmap);
                        }
                        long endTime = System.currentTimeMillis();
                        mTransparentTitleView.setText("Time cost: " + String.valueOf((endTime - startTime) / 1000f) + " sec");
                        // Draw on bitmap


                        Paint featuresPaint = new Paint();
                        featuresPaint.setColor(Color.CYAN);
                        featuresPaint.setStrokeWidth(1);
                        featuresPaint.setStyle(Paint.Style.STROKE);
                        // Loop result list

                        final Set<Integer> key_points = new HashSet<Integer>(Arrays.asList(
                                new Integer[] {
                                        //11, //chin
                                        17, //jaw
                                        22, //right brow
                                        27, //left_brow
                                        31, //nose bridge
                                        36, //nose bottom
                                        42, //right eye
                                        48, //left eye
                                        60, // mouth outer
                                        68 //mouth inner

                                }
                        ));

                        if (results != null) {
                            for (final VisionDetRet ret : results) {
                                float resizeRatio = 1.0f;
                                Rect bounds = new Rect();
                                bounds.left = (int) (ret.getLeft() * resizeRatio);
                                bounds.top = (int) (ret.getTop() * resizeRatio);
                                bounds.right = (int) (ret.getRight() * resizeRatio);
                                bounds.bottom = (int) (ret.getBottom() * resizeRatio);
                                Canvas canvas = new Canvas(mCroppedBitmap);
                                canvas.drawRect(bounds, mFaceLandmardkPaint);
                                Log.d("cunt", "rectangle points : "  +bounds.left + " r: " + bounds.right + "width : " + mCroppedBitmap.getWidth() + " height: " + mCroppedBitmap.getHeight());


                                mWindow.translateOrb(bounds);


                                // Draw landmark
                                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                //Old method
//                                for (Point point : landmarks) {
//                                    int pointX = (int) (point.x * resizeRatio);
//                                    int pointY = (int) (point.y * resizeRatio);
//                                    canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
//                                }

                                for (int p = 0 ;  p < 68 -1 ; p++) {
                                    if ( ! key_points.contains(p + 1) ) {
                                        int pointXS = (int) (landmarks.get(p).x * resizeRatio);
                                        int pointYS = (int) (landmarks.get(p).y * resizeRatio);
                                        int pointXF = (int) (landmarks.get(p+1).x * resizeRatio);
                                        int pointYF = (int) (landmarks.get(p+1).y * resizeRatio);
                                        canvas.drawLine(pointXS, pointYS, pointXF, pointYF, featuresPaint);
                                    }

                                }
                                canvas.drawCircle(landmarks.get(33).x, landmarks.get(33).y, 3, mFaceLandmardkPaint);

                            }
                            if(results.size() > 0) {

                                long beforePose = System.currentTimeMillis();

                                double[] rotationRads = poseDetection.estimatePose(results.get(0), mCroppedBitmap);
                                long aftrePOse = System.currentTimeMillis();

                                Log.d("timetopose", String.valueOf((aftrePOse - beforePose) /1000f));
                                mWindow.rotateOrb(rotationRads[0], rotationRads[1], rotationRads[2]);

                                //window width =
                                float widthWindow = mCroppedBitmap.getWidth();
                                //scale = 1 is half the width
                                //left checkbone ..

                                Point ear_r = results.get(0).getFaceLandmarks().get(0);
                                Point ear_l = results.get(0).getFaceLandmarks().get(16);

                                double faceWidth =
                                        Math.sqrt(
                                        Math.pow((ear_l.x - ear_r.x), 2.0f)
                                        + Math.pow((ear_l.y - ear_r.y), 2) );

                                //At 1 facewidth is 1 / 4 of the screen
                                double faceWidthScale =  faceWidth / (widthWindow / 2.0f ) ;

                                Point noseTip = results.get(0).getFaceLandmarks().get(33);
                                Point chin = results.get(0).getFaceLandmarks().get(8);
                                double faceHeight =
                                        Math.sqrt(
                                                Math.pow((noseTip.x - chin.x), 2.0f)
                                                        + Math.pow((noseTip.y - chin.y), 2) );


                                faceHeight = faceHeight * 2;


                                mWindow.scaleOrb(faceWidthScale * 1.20f, faceHeight * 1.20f, 1.0f );
                            }

                        }

                        mWindow.setRGBBitmap(mCroppedBitmap);
                        mIsComputing = false;
                    }


                });

        Trace.endSection();
    }






}
