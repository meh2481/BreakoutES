package edu.dsu.mark.openglesenvironment;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "MyGLRenderer";
    //private Triangle mTriangle;
    private Quad qPaddle;
    private Quad testQuad;
    private Obj oBall;
    private Obj oPaddle;

    private static final float paddleDist = 1.9f;   //Distance the paddle is from the center
    private static final float TODEG = (float) (180.0f / Math.PI);
    private static final float TORAD = (float) (Math.PI / 180.0f);

    private long lastTime;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    //private final float[] mRotationMatrix = new float[16];

    private float mAngle;
    private float mAngleDest;

    private float camX, camY, camZ;
    private boolean tiltMove;
    private Context mContext;

    public void setContext(Context c)
    {
        mContext = c;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {

        lastTime = SystemClock.uptimeMillis();

        // Set the background frame color
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        //mTriangle = new Triangle();
        qPaddle = new Quad();
        qPaddle.loadImage("drawable/paddle", mContext);

        testQuad = new Quad();
        testQuad.loadImage("drawable/pinball", mContext);
        //testQuad.setColor(1, 0, 0, 1);

        oBall = new Obj(testQuad);
        oBall.scale = 0.5f;
        oPaddle = new Obj(qPaddle);


        camX = camY = 0;
        camZ = -6;
        tiltMove = false;
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        long curTime = SystemClock.uptimeMillis();

        if(tiltMove)
        {
            float ANGLE_MOVE = 10.5f;

            if (mAngle < mAngleDest) {
                float dist = mAngleDest - mAngle;
                mAngle += dist / 3.0f;
            } else if (mAngle > mAngleDest) {
                float dist = mAngle - mAngleDest;
                mAngle -= dist / 3.0f;
            }
        }

        //float[] scratch = new float[16];

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        //Matrix.setIdentityM(mRotationMatrix, 0);
        //Matrix.rotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);
        //Matrix.translateM(mRotationMatrix, 0, -2f, 0, 0);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        //Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        // Draw paddle test thing
        //qPaddle.draw(scratch);

        //Matrix.setIdentityM(mRotationMatrix, 0);
        //Matrix.scaleM(mRotationMatrix, 0, 2, 1, 1);
        //Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);
        //testQuad.draw(scratch);

        oPaddle.angle = mAngle - 90.0f;
        oPaddle.x = (float) -(Math.cos(TORAD * mAngle) * paddleDist);
        oPaddle.y = (float) -(Math.sin(TORAD * mAngle) * paddleDist);

        oBall.draw(mMVPMatrix);
        oPaddle.draw(mMVPMatrix);

        lastTime = curTime;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1, 7);

    }

    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    /**
     * Returns the rotation angle of the triangle shape (mTriangle).
     *
     * @return - A float representing the rotation angle.
     */
    public float getAngle() {
        return mAngle;
    }

    /**
     * Sets the rotation angle of the triangle shape (mTriangle).
     */
    public void setAngle(float angle)
    {
        mAngle = angle * TODEG;
    }

    public void setCam(float posX, float posY, float posZ)
    {
        //Vector3d vec = new Vector3d(posX, posY, posZ);
        //float len = (float) Math.sqrt(posX*posX + posY*posY + posZ*posZ);
        //len /= 2;
        camX = posX;// /len;
        camY = posY;// /len;
        camZ = posZ;// /len;

        mAngleDest = (float) ((float) Math.atan2(posX, posY) * 180 / Math.PI);


    }

}
