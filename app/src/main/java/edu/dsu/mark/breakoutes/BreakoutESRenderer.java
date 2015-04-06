package edu.dsu.mark.breakoutes;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class BreakoutESRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "BreakoutESRenderer";
    private Obj oBall;
    private Obj oPaddle;
    private int blocksAdded;
    //Quad qPaddle;

    private HashMap<String, Quad> mImages;
    private LinkedList<Obj> mObjects;
    private LinkedList<Obj> mBlocks;

    private static final float paddleDist = 1.9f;   //Distance the paddle is from the center
    private static final float ballSpeed = 2.0f;

    private long lastTime;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private float mAngle;
    private float mAngleDest;

    private float camX, camY, camZ;
    private boolean tiltMove;
    private Context mContext;

    public void setContext(Context c)
    {
        mContext = c;
    }

    public BreakoutESRenderer()
    {
        mObjects = new LinkedList<Obj>();
        mBlocks = new LinkedList<Obj>();
        lastTime = SystemClock.uptimeMillis();

        oBall = new Obj();
        oBall.scale = 0.5f;
        oBall.speed = ballSpeed;
        oBall.dir = 0;
        oBall.pos.x = -1.5f;
        oBall.pos.y = 0;
        oPaddle = new Obj();

        Obj oWalls = new Obj();

        oWalls.sImg = "drawable/walls";
        oPaddle.sImg = "drawable/paddle";
        oBall.sImg = "drawable/pinball";

        oBall.type = Obj.typeBall;
        oPaddle.type = Obj.typePaddle;

        mObjects.add(oWalls);
        mObjects.add(oPaddle);
        mObjects.add(oBall);

        resetLevel();

    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        // Set the background frame color
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        mImages = new HashMap<String, Quad>();  //Dump any previous image memory

        //oBall.q = getImage("drawable/pinball");
        //oPaddle.q = getImage("drawable/paddle");
        //oWalls.q = getImage("drawable/walls");

        //Reload images
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            o.q = getImage(o.sImg);
        }

        for(Object i : mBlocks)
        {
            Obj o = (Obj) i;
            o.genCollision();
        }

        //Generate collision info
        oPaddle.genCollision();
        oBall.genCollision(true);

        camX = camY = 0;
        camZ = -6;
        tiltMove = false;
    }

    @Override
    public void onDrawFrame(GL10 unused)
    {

        long curTime = SystemClock.uptimeMillis();
        long diffTime = curTime - lastTime;
        float dt = (float) (diffTime) / 1000.0f;
        updateObjects(dt);

        if (tiltMove)
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

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        //Rotate and move paddle to the proper location
        oPaddle.angle = mAngle - 90.0f;
        oPaddle.pos.x = (float) -(Math.cos(Point.TORAD * mAngle) * paddleDist);
        oPaddle.pos.y = (float) -(Math.sin(Point.TORAD * mAngle) * paddleDist);

        //Draw objects
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            o.draw(mMVPMatrix);
        }
        //oWalls.draw(mMVPMatrix);
        //oPaddle.draw(mMVPMatrix);
        //oBall.draw(mMVPMatrix);

        lastTime = curTime;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
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
     * <p/>
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type       - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode)
    {

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
     * <p/>
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation)
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
        {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    /**
     * Returns the rotation angle of the triangle shape (mTriangle).
     *
     * @return - A float representing the rotation angle.
     */
    public float getAngle()
    {
        return mAngle;
    }

    /**
     * Sets the rotation angle of the triangle shape (mTriangle).
     */
    public void setAngle(float angle)
    {
        mAngle = angle * Point.TODEG;
    }

    public float wrapAngle(float angle)
    {
        if(angle < 0)
            return angle + 360;
        if(angle > 360)
            return angle - 360;
        return angle;
    }

    public float reflect(float angleToRef, float surfNormal)    //Assume reflection is possible yaay
    {
        angleToRef = wrapAngle(angleToRef + 180);
        float diff = surfNormal - angleToRef;
        if(diff > 90)
        {
            surfNormal -= 360;
            diff = surfNormal - angleToRef;
        }
        if(diff < -90)
        {
            surfNormal += 360;
            diff = surfNormal - angleToRef;
        }
        return wrapAngle(surfNormal + diff);
    }

    public void wallCheck(Obj o)
    {

        float objDist = o.pos.length();//(float) Math.sqrt(o.x*o.x + o.y*o.y);
        //float objAngle = o.pos.angle();//float) (Point.TODEG * Math.atan2(o.y, o.x));

        //Simple test to make ball bounce off circular walls
        if(objDist > paddleDist)
        {
            /*o.dir = reflect(o.dir, wrapAngle(objAngle + 180));
            o.pos.x = (float) (Math.cos(objAngle * Point.TORAD) * paddleDist);
            o.pos.y = (float) (Math.sin(objAngle * Point.TORAD) * paddleDist);*/
            oBall.dir = 0;
            oBall.pos.x = -1.5f;
            oBall.pos.y = 0;
            //oPaddle.setColor(1,0,0,1);  //TODO: Game over
        }
    }

    public void updateObjects(float dt)
    {
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            o.update(dt);
        }
        //oBall.update(dt);
        //oPaddle.update(dt);
        wallCheck(oBall);

        oBall.setColor(1,1,1,1);
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            if(o == oBall)
                continue;

            ContactManifold cm = oBall.colliding(o);
            if(cm.collide)  //If ball and something are hitting
            {
                oBall.setColor(1, 0, 0, 1);
                oBall.dir = reflect(oBall.dir, cm.normal2.angle());
                oBall.pos.x += cm.normal2.x;
                oBall.pos.y += cm.normal2.y;

                if(o.type == Obj.typeBlock)
                {
                    o.active = false; //Break block
                    //mObjects.remove(o);
                    //mBlocks.remove(o);
                    blocksAdded--;
                    if(blocksAdded <= 0)
                    {
                        //TODO: Won state
                        resetLevel();
                    }
                }
            }
        }

        /*ContactManifold cm = oBall.colliding(oPaddle);

        if(cm.collide)  //If ball and paddle are hitting
        {
            oBall.setColor(1, 0, 0, 1);
            oBall.dir = reflect(oBall.dir, cm.normal2.angle());
            oBall.pos.x += cm.normal2.x;
            oBall.pos.y += cm.normal2.y;
        }
        else*/



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

    private Quad getImage(String sImg)
    {
        if(mImages.containsKey(sImg))
            return mImages.get(sImg);
        Quad q = new Quad();
        q.loadImage(sImg, mContext);
        mImages.put(sImg, q);
        return q;
    }

    private void resetLevel()
    {
        blocksAdded = 0;
        for(int x = 0; x < 5; x++)
        {
            for(int y = 0; y < 5; y++)
            {
                Obj oBlock = new Obj();
                oBlock.scale = 0.25f;
                oBlock.sImg = "drawable/block";

                oBlock.pos.x = x * 0.25f;// - (2.5f * 0.25f);
                oBlock.pos.y = y * 0.25f;// - (2.5f * 0.25f);

                oBlock.setColor((float) Math.random(), (float) Math.random(), (float) Math.random(), 1.0f);
                oBlock.type = Obj.typeBlock;
                mObjects.add(oBlock);
                mBlocks.add(oBlock);
                blocksAdded++;
            }
        }
    }

}
