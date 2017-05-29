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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.tzutalin.dlibtest.SphereView.GlRenderer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by darrenl on 2016/5/20.
 */
public class CameraActivity extends Activity {

    private static int OVERLAY_PERMISSION_REQ_CODE = 1;



    static {
        // If you use opencv 2.4, System.loadLibrary("opencv_java")
        Log.d("cunt", "before static gsidhg");
        System.loadLibrary("opencv_java3");

        if (OpenCVLoader.initDebug()) {
            Log.d("cunt", "loaded statically allgee");
        }
        else {
            Log.d("cunt", "didnt load allgee statically.");
        }
    }



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LayoutInflater li  = (LayoutInflater) this.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
       // View cam_activity = li.inflate(R.layout.activity_camera, null);
        setContentView(R.layout.activity_camera);




        // Handle touch events.
        //this.mGlSurfaceView.setOnTouchListener(touchListener(renderer));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }

        if (null == savedInstanceState) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, CameraConnectionFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                    Toast.makeText(CameraActivity.this, "CameraActivity\", \"SYSTEM_ALERT_WINDOW, permission not granted...", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        }
    }


}