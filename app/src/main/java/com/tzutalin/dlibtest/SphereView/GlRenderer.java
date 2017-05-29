package com.tzutalin.dlibtest.SphereView;

import android.content.Context;
import android.graphics.Paint;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.util.Log;
import android.view.MotionEvent;


import com.tzutalin.dlibtest.OnGetImageListener;
import com.tzutalin.dlibtest.R;

import org.opencv.core.Rect;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Simple class to render an object in a suitable view port.
 *
 * @author Jim Cornmell
 * @since July 2013
 */
public class GlRenderer implements Renderer {
    /** Factors to rotate sphere. */
    private static final float FACTOR_ROTATION_LONGITUDE = 100.0f;
    private static final float FACTOR_ROTATION_LATITUDE = 100.0f;

    /** Factors to scale sphere. */
    private static final float FACTOR_HYPOT = 500.0f;
    private static final float SPHERE_SCALE_MAX = 4.0f;
    private static final float SPHERE_SCALE_MIN = 0.5f;

    /** Object distance on the screen. */
    private static final float OBJECT_DISTANCE = -10.0f;

    /** Clear colour, alpha component. */
    private static final float CLEAR_RED = 0.0f;

    /** Clear colour, alpha component. */
    private static final float CLEAR_GREEN = 0.0f;

    /** Clear colour, alpha component. */
    private static final float CLEAR_BLUE = 0.0f;

    /** Clear colour, alpha component. */
    private static final float CLEAR_ALPHA = 0.5f;

    /** Perspective setup, field of view component. */
    private static final float FIELD_OF_VIEW_Y = 45.0f;

    /** Perspective setup, near component. */
    private static final float Z_NEAR = 0.1f;

    /** Perspective setup, far component. */
    private static final float Z_FAR = 100.0f;

    /** The earth's sphere. */
    private final Sphere mEarth;

    /** The context. */
    private final Context mContext;

    /** The spheres rotation angle. */
    private float mRotationAngle;

    /** The spheres tilt angle. */
    private float mAxialTiltAngle;

    private float mZAxistiltAngle;

    /** Scaling of the sphere. */
    private float mSphereScale = 1.0f;

    private float mSphereScaleX = 1.0f;
    private float mSphereScaleY = 1.0f;
    private float mSphereScaleZ = 1.0f;
    private float width, height;


    // Sphere coordinates
    private android.graphics.Rect orbRect;

    /**
     * Change the rotation angles for the sphere.
     *
     * @param pointA Start point.
     * @param pointB End point.
     */
    public void setMoveDelta(MotionEvent.PointerCoords pointA, MotionEvent.PointerCoords pointB) {
        mRotationAngle -= (pointA.x - pointB.x)/ FACTOR_ROTATION_LONGITUDE;
        mAxialTiltAngle -= (pointA.y - pointB.y)/ FACTOR_ROTATION_LATITUDE;
    }

    public void setMaskScale(double xScale, double yScale, double zScale) {
        //this.mSphereScaleX = (float) xScale;

    }

    public void setMaskRotation(double rx, double ry,double rz) {

        Log.d("trippy", "updating angles - x: " + -rx * 57.0f + " y: " + ry * 57.0f + " z: " + -rz * 57.0f);


//        mRotationAngle = (float) -rx * 57.0f;
//        mAxialTiltAngle  = (float) ry * 57.0f;//radians to degrees
//        mZAxistiltAngle = (float) -rz * 57.0f;
        mRotationAngle = updateAngle(mRotationAngle, (float)-rx * 57.0f);
        mAxialTiltAngle = updateAngle(mAxialTiltAngle, (float) ry * 57.0f);
        mZAxistiltAngle = updateAngle(mZAxistiltAngle, (float)-rz * 57.0f);

    }

    private float updateAngle(float angle, float newAngle) {


        //Assisting algorithm to reduce incorrect angle reading jumps
        float max_angle_change =  60.0f; //arbitrary value.

        //guard that the  new angle isn't greater than 60 degrees
        if ( newAngle > 90.0f || newAngle < -90.0f ) return angle;

        if (Math.abs(angle - newAngle) < max_angle_change) {
            return newAngle;
        }
        else {
            //angle += newAngle < angle ? -20.0f : 20.0f;
            return angle;
        }

    }

    public void setizeAndPosition(float ax, float ay, float bx, float by) {

    }

    public void updateOrbRect(android.graphics.Rect rect) {
        this.orbRect = rect;
    }


    /**
     * Constructor to set the handed over context.
     * @param context The context.
     */
    public GlRenderer(final Context context) {
        this.mContext = context;
        this.mEarth = new Sphere(3, 2);
        this.mRotationAngle = 0.0f;
        this.mAxialTiltAngle = 0.0f;
        this.mZAxistiltAngle = 0.0f;

        orbRect = new android.graphics.Rect();
        orbRect.set(0, 1, 0, 1);
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        //gl.glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, OBJECT_DISTANCE);

        //Update position with translation

        gl.glTranslatef(((float)orbRect.centerX() - 112) / 224 * 6.0f,
                (-(((float)orbRect.centerY() - 112)) / 224) * 6.0f, 0);

        //gl.glScalef(mSphereScaleX, mSphereScaleY, mSphereScaleZ);

        
        gl.glRotatef(this.mAxialTiltAngle, 1, 0, 0);
        gl.glRotatef(this.mRotationAngle, 0, 1, 0);
        gl.glRotatef(this.mZAxistiltAngle, 0, 0, 1);


        this.mEarth.draw(gl);
        gl.glTranslatef(( -((float)orbRect.centerX() - 112) ), 0.0f, 0);

    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        final float aspectRatio = (float) width / (float) (height == 0 ? 1 : height);

        this.width = width;
        this.height = height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, FIELD_OF_VIEW_Y, aspectRatio, Z_NEAR, Z_FAR);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

    }

    @Override
    public void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
        this.mEarth.loadGLTexture(gl, this.mContext, R.raw.eddy_g2);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        //gl.glEnable(GL10.GL_CULL_FACE); //test ?;
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glClearColor(0, 0, 0, 0);
        //gl.glClearColor(CLEAR_RED, CLEAR_GREEN, CLEAR_BLUE, CLEAR_ALPHA);
        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

    }
}
