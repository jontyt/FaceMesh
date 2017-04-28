package com.tzutalin.dlibtest.SphereView;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.SphereView.Sphere;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SphereActivity extends AppCompatActivity {

    private OpenGLView mOpenGLView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_sphere);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Set screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mOpenGLView = new OpenGLView(this);
        setContentView(mOpenGLView);

    }


}
