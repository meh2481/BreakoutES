package edu.dsu.mark.breakoutes;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static java.lang.Math.atan2;
import java.util.concurrent.*;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class BreakoutESRenderer implements GLSurfaceView.Renderer
{
    public static final String SWIPE_LEFT = "SWIPERL";
    public static final String SWIPE_RIGHT = "SWIPELR";

    private static final boolean bDebug = true; //TODO false
    private static final String TAG = "BreakoutESRenderer";
    private static final float tapTime = 0.25f;
    private static final float blockScale = 0.3f;
    private static final float blockGridSize = blockScale / 1.5625f;//0.16f;
    private Obj oBall;
    private Obj oPaddle;
    private Obj oEditorButton;
    private Obj oAddBlockButton;
    private Obj oRemBlockButton;
    private Obj oRSlider, oGSlider, oBSlider;
    private Obj lastDragged = null;

    private Line drawLine;

    public LinkedBlockingQueue<MotionEvent> motEvents;
    public LinkedBlockingQueue<String> miscEvents;

    private int blocksAdded;
    private int curLevel = 1;
    private boolean bEditor = false;
    //Quad qPaddle;

    private HashMap<String, Quad> mImages;
    private LinkedList<Obj> mObjects;
    private LinkedList<Obj> mBlocks;

    private static final float WALL_DIST = 1.9f;   //Distance the paddle is from the center
    private static final float PADDLE_DIST = 2.1f;
    private static final float MAX_PADDLE_ANGLE = 125.0f;
    private static final float MIN_PADDLE_ANGLE = 55.0f;
    private static final float BOARD_MAX_ANGLE = -45.0f;
    private static final float BOARD_MIN_ANGLE = -135.0f;
    private static final float ballSpeed = 3.0f;
    private static float sliderStartX = -2.1f;
    private static float sliderEndX = -2.8f;

    private long lastTime;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private float mAngle = 90;
    private float mAngleDest;

    private float camX, camY, camZ;
    private float screenWidth, screenHeight;
    private boolean tiltMove;
    private Context mContext;

    private boolean ballLaunched = false;

    public void setContext(Context c)
    {
        mContext = c;
    }

    public BreakoutESRenderer(Context c)
    {
        mContext = c;
        mObjects = new LinkedList<>();
        mBlocks = new LinkedList<>();
        motEvents = new LinkedBlockingQueue<>();
        miscEvents = new LinkedBlockingQueue<>();
        lastTime = SystemClock.uptimeMillis();

        oBall = new Obj();
        oBall.scale = 0.5f;
        oBall.speed = 0;
        oBall.dir = 0;
        oBall.pos.x = -1.5f;
        oBall.pos.y = 0;
        oPaddle = new Obj();

        Obj oWalls = new Obj();
        oEditorButton = new Obj();
        oAddBlockButton = new Obj();
        oRemBlockButton = new Obj();

        oRSlider = new Obj();
        oGSlider = new Obj();
        oBSlider = new Obj();
        oRSlider.setColor(1, 0, 0);
        oGSlider.setColor(0, 1, 0);
        oBSlider.setColor(0, 0, 1);
        oRSlider.pos.x = sliderEndX;
        oRSlider.pos.y = 1.75f;
        oGSlider.pos.x = sliderEndX;
        oGSlider.pos.y = 1.25f;
        oBSlider.pos.x = sliderEndX;
        oBSlider.pos.y = 0.75f;

        oWalls.sImg = "drawable/walls";
        oPaddle.sImg = "drawable/paddle2";
        oBall.sImg = "drawable/pinball";
        oEditorButton.sImg = "drawable/editor";
        oAddBlockButton.sImg = "drawable/blockadd";
        oRemBlockButton.sImg = "drawable/blockrem";
        oRSlider.sImg = "drawable/slider";
        oGSlider.sImg = "drawable/slider";
        oBSlider.sImg = "drawable/slider";

        oBall.type = Obj.typeBall;
        oPaddle.type = Obj.typePaddle;
        oEditorButton.type = Obj.typeButton;
        oAddBlockButton.type = Obj.typeButton;
        oAddBlockButton.active = false;
        oRemBlockButton.type = Obj.typeButton;
        oRemBlockButton.active = false;
        oRSlider.type = Obj.typeButton;
        oGSlider.type = Obj.typeButton;
        oBSlider.type = Obj.typeButton;
        oRSlider.active = false;
        oGSlider.active = false;
        oBSlider.active = false;

        mObjects.add(oWalls);
        mObjects.add(oPaddle);
        mObjects.add(oBall);
        mObjects.add(oEditorButton);
        mObjects.add(oAddBlockButton);
        mObjects.add(oRemBlockButton);
        mObjects.add(oRSlider);
        mObjects.add(oGSlider);
        mObjects.add(oBSlider);

        resetLevel();

    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        Log.e("BREAKOUTES", "onSurfaceCreated()");
        // Set the background frame color
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        mImages = new HashMap<>();  //Dump any previous image memory

        //oBall.q = getImage("drawable/pinball");
        //oPaddle.q = getImage("drawable/paddle");
        //oWalls.q = getImage("drawable/walls");

        //Reload images
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            o.q = getImage(o.sImg);
        }
        getImage("drawable/play");
        getImage("drawable/editor");
        getImage("drawable/block");

        for(Object i : mBlocks)
        {
            Obj o = (Obj) i;
            o.genCollision();
        }

        //Generate collision info
        oPaddle.genCollision(true);
        oBall.genCollision(true);
        oEditorButton.genCollision();
        oAddBlockButton.genCollision();
        oRemBlockButton.genCollision();
        oRSlider.genCollision();
        oGSlider.genCollision();
        oBSlider.genCollision();

        camX = camY = 0;
        camZ = -2;
        tiltMove = false;

        drawLine = new Line();
    }

    @Override
    public void onDrawFrame(GL10 unused)
    {

        long curTime = SystemClock.uptimeMillis();
        long diffTime = curTime - lastTime;
        float dt = (float) (diffTime) / 1000.0f;
        updateObjects(dt);

        if (tiltMove && !bEditor)
        {
            //float ANGLE_MOVE = 10.5f;

            if (mAngle < mAngleDest)
            {
                float dist = mAngleDest - mAngle;
                mAngle += dist / 3.0f;
            }
            else if (mAngle > mAngleDest)
            {
                float dist = mAngle - mAngleDest;
                mAngle -= dist / 3.0f;
            }
        }

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, camX, camY, camZ, 0f, 0f, 0f, 0f, 1.0f, 0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        //Rotate and move paddle to the proper location
        oPaddle.angle = mAngle - 90.0f;
        oPaddle.pos.x = (float) -(Math.cos(Point.TORAD * mAngle) * PADDLE_DIST);
        oPaddle.pos.y = (float) -(Math.sin(Point.TORAD * mAngle) * PADDLE_DIST);
        if(!ballLaunched)
        {
            Circle cPaddle = (Circle) oPaddle.collide;
            Circle cBall = (Circle) oBall.collide;
            oBall.pos.x = (float) (oPaddle.pos.x + (Math.cos(Point.TORAD * mAngle) * (cPaddle.rad + cBall.rad)));//0.145));
            oBall.pos.y = (float) (oPaddle.pos.y + (Math.sin(Point.TORAD * mAngle) * (cPaddle.rad + cBall.rad)));//0.145));
            oBall.dir = mAngle;
        }

        //Draw lines for sliders
        if(bEditor)
        {
            drawLine.setColor(1,1,1);
            drawLine.setVerts(sliderStartX, oRSlider.pos.y, sliderEndX, oRSlider.pos.y);
            drawLine.draw(mMVPMatrix);
            drawLine.setVerts(sliderStartX, oGSlider.pos.y, sliderEndX, oGSlider.pos.y);
            drawLine.draw(mMVPMatrix);
            drawLine.setVerts(sliderStartX, oBSlider.pos.y, sliderEndX, oBSlider.pos.y);
            drawLine.draw(mMVPMatrix);
        }

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
        //Log.e("BREAKOUTES", "onSurfaceChanged()");
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1, 7);

        screenWidth = width;
        screenHeight = height;

        //Lay out editor buttons
        oEditorButton.pos = screenToGL(new Point(0,0));
        //Log.e("THING", "pos: " + oEditorButton.pos.x + "," + oEditorButton.pos.y);
        oEditorButton.pos.x -= oEditorButton.collide.getWidth() / 2.0f;
        oEditorButton.pos.y -= oEditorButton.collide.getHeight() / 2.0f;
        oAddBlockButton.pos.x = oEditorButton.pos.x;
        oAddBlockButton.pos.y = oEditorButton.pos.y - oEditorButton.collide.getHeight() / 2.0f - oAddBlockButton.collide.getHeight() / 2.0f - 0.3f;

        oRSlider.pos.y = oAddBlockButton.pos.y - oAddBlockButton.collide.getHeight() / 2.0f - oRSlider.collide.getHeight() / 2.0f - 0.3f;
        oGSlider.pos.y = oRSlider.pos.y - 0.5f;
        oBSlider.pos.y = oGSlider.pos.y - 0.5f;

        float sliderLen = Math.abs(sliderStartX - sliderEndX);
        sliderStartX = oAddBlockButton.pos.x - (sliderLen / 2.0f);
        sliderEndX = sliderStartX + sliderLen;

        oRemBlockButton.pos.x = oAddBlockButton.pos.x;
        oRemBlockButton.pos.y = oBSlider.pos.y - 0.2f - oRemBlockButton.collide.getHeight() / 2.0f - oBSlider.collide.getHeight() / 2.0f;
        oRemBlockButton.scale = 0.8f;

        oEditorButton.updateCollision();
        oAddBlockButton.updateCollision();
        oRemBlockButton.updateCollision();
        oRSlider.pos.x = sliderStartX;
        oGSlider.pos.x = sliderStartX;
        oBSlider.pos.x = sliderStartX;
        oRSlider.updateCollision();
        oGSlider.updateCollision();
        oBSlider.updateCollision();

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
    /*public static void checkGlError(String glOperation)
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
        {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }*/

    /**
     * Returns the rotation angle of the triangle shape (mTriangle).
     *
     * @return - A float representing the rotation angle.
     */
    //public float getAngle()
    //{
    //    return mAngle;
    //}

    /**
     * Sets the rotation angle of the triangle shape (mTriangle).
     */
    //public void setAngle(float angle)
    //{
    //    mAngle = angle * Point.TODEG;
    //}

    public float wrapAngle(float angle)
    {
        if(angle < 0)
            return angle + 360;
        if(angle > 360)
            return angle - 360;
        return angle;
    }

    public float reflect(float angleToRef, float surfNormal)    //TODO: Make sure angles are correct, otherwise ignore
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
            if(diff > 90)
                return angleToRef;  //Ignore if too far apart
        }
        return wrapAngle(surfNormal + diff);
    }

    public void wallCheck(Obj o)
    {

        float objDist = o.pos.length();//(float) Math.sqrt(o.x*o.x + o.y*o.y);
        float objAngle = o.pos.angle();//float) (Point.TODEG * Math.atan2(o.y, o.x));

        //Simple test to make ball bounce off circular walls
        if(objDist > WALL_DIST)
        {
            //Log.e("WALLCHECK", "Ball Angle: "+objAngle);
            if(objAngle < BOARD_MAX_ANGLE && objAngle > BOARD_MIN_ANGLE)
            {
                //TODO Game over stuff
                oBall.dir = 0;
                oBall.speed = 0;
                ballLaunched = false;
            }
            else
            {
                o.dir = reflect(o.dir, wrapAngle(objAngle + 180));
                o.pos.x = (float) (Math.cos(objAngle * Point.TORAD) * WALL_DIST);
                o.pos.y = (float) (Math.sin(objAngle * Point.TORAD) * WALL_DIST);
            }
            //oBall.dir = 0;
            //oBall.pos.x = -1.5f;
            //oBall.pos.y = 0;
            //oPaddle.setColor(1,0,0,1);  //TODO: Game over
        }
    }

    public void updateObjects(float dt)
    {
        //Process touch events synchronously
        MotionEvent eLast = null;
        while(true)
        {
            MotionEvent e = motEvents.poll();
            if(e == null)
                break;
            //We're piling these ACTION_UP's on top of each other for some reason, ignore extras
            if(eLast == null || eLast.getAction() != e.getAction() || e.getAction() != MotionEvent.ACTION_UP)
                onTouchEvent(e);
            //else
            //    Log.e("UPDATEOBJ", "Duplicate event");
            eLast = e;
        }

        //Process other misc. input events synchronously as well
        while(true)
        {
            String s = miscEvents.poll();
            if (s == null)
                break;

            if (s == SWIPE_LEFT && bEditor)
            {
                curLevel++;
                wipeLevel();
                //Log.e("UPDATEOBJ", "Curlev: " + curLevel);
            }
            else if(s == SWIPE_RIGHT && bEditor)
            {
                if(curLevel > 1)
                {
                    curLevel--;
                    wipeLevel();
                }
                //Log.e("UPDATEOBJ", "Curlev: " + curLevel);
            }
        }

        if(bEditor)
        {
            //oEditorButton.q  = getImage(oEditorButton.sImg);
            return;
        }

        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            o.update(dt);
        }
        //oBall.update(dt);
        //oPaddle.update(dt);
        wallCheck(oBall);

        //oBall.setColor(1,1,1,1);
        boolean toReset = false;
        //boolean reflectedThisFrame = false;
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            if(o == oBall)
                continue;

            ContactManifold cm = oBall.colliding(o);
            if(cm.collide)  //If ball and something are hitting
            {


                if(o.type == Obj.typeBlock)
                {
                    //oBall.setColor(1, 0, 0, 1);

                    //Sanity checks, cause hitting the ball at the wrong angle seems to make it go NaN,NaN,NaN on posx, posy, dir
                    float tempdir = reflect(oBall.dir, cm.normal2.angle());
                    Point tempPos = new Point();
                    tempPos.x = oBall.pos.x + cm.normal2.x;
                    tempPos.y = oBall.pos.y + cm.normal2.y;
                    if(tempdir >= 0 && tempdir <= 360)
                        oBall.dir = tempdir;
                    if(tempPos.x > -500 && tempPos.x < 500)
                        oBall.pos.x = tempPos.x;
                    if(tempPos.y > -500 && tempPos.y < 500)
                        oBall.pos.y = tempPos.y;


                    o.active = false; //Break block
                    //mObjects.remove(o);
                    //mBlocks.remove(o);
                    blocksAdded--;
                    if(blocksAdded <= 0)
                    {
                        //TODO: Won state
                        toReset = true;
                        curLevel++; //Incr level TODO Test if last level or such
                    }
                }
                else if(o.type == Obj.typePaddle)
                {
                    //oBall.setColor(1, 0, 0, 1);

                    float tempdir = reflect(oBall.dir, cm.normal2.angle());
                    Point tempPos = new Point();
                    tempPos.x = oBall.pos.x + cm.normal2.x;
                    tempPos.y = oBall.pos.y + cm.normal2.y;
                    if(tempdir >= 0 && tempdir <= 360)
                        oBall.dir = tempdir;
                    if(tempPos.x > -500 && tempPos.x < 500)
                        oBall.pos.x = tempPos.x;
                    if(tempPos.y > -500 && tempPos.y < 500)
                        oBall.pos.y = tempPos.y;

                }
            }
        }

        if(toReset)
        {
            wipeLevel();
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

    public void wipeLevel()
    {
        for(Object i : mBlocks)
        {
            mObjects.remove(i);
        }
        mBlocks.clear();
        oBall.pos.x = -1.5f;
        oBall.pos.y = 0;
        oBall.dir = 0;
        //Log.e("THING", "Gonna reset nao " + blocksAdded);
        resetLevel();
        for(Object i : mBlocks)
        {
            Obj o = (Obj) i;
            o.q = getImage(o.sImg);
            o.genCollision();
        }
    }

    public void setCam(float posX, float posY, float posZ)
    {
        //Vector3d vec = new Vector3d(posX, posY, posZ);
        //float len = (float) Math.sqrt(posX*posX + posY*posY + posZ*posZ);
        //len /= 2;
        //camX = posX;// /len;
        //camY = posY;// /len;
        //camZ = posZ;// /len;

        mAngleDest = (float) ((float) Math.atan2(posX, posY) * 180 / Math.PI);


    }

    private Quad getImage(String sImg)
    {
        if(mImages.containsKey(sImg))
        {
            //Log.e("BREAKOUTES", "getImage() already has " + sImg);
            return mImages.get(sImg);
        }

        //Log.e("BREAKOUTES", "getImage() creating quad " + sImg);
        Quad q = new Quad();
        q.loadImage(sImg, mContext);
        mImages.put(sImg, q);
        return q;
    }

    private void resetLevel()
    {
        blocksAdded = 0;
        ballLaunched = false;
        oBall.speed = 0;
        levelLoad(curLevel);
        /*for(int x = 0; x < 2; x++)
        {
            for(int y = 0; y < 2; y++)
            {
                Obj oBlock = new Obj();
                oBlock.scale = blockScale;
                oBlock.sImg = "drawable/block";

                oBlock.pos.x = x * blockGridSize;// - (2.5f * 0.25f);
                oBlock.pos.y = y * blockGridSize;// - (2.5f * 0.25f);

                oBlock.setColor((float) Math.random(), (float) Math.random(), (float) Math.random(), 1.0f);
                oBlock.type = Obj.typeBlock;
                mObjects.add(oBlock);
                mBlocks.add(oBlock);
                blocksAdded++;
            }
        }*/
    }

    private Point screenToGL(Point pos)
    {
        pos.x -= screenWidth / 2.0f;
        pos.y -= screenHeight / 2.0f;

        final float tan45 = (float) Math.tan(Math.PI/4.0);
        final float aspect = screenWidth / screenHeight;

        Rect rcCam = new Rect();

        rcCam.h = tan45 * camZ * 2;
        rcCam.w = rcCam.h * aspect;

        Point ret = new Point();
        ret.x = pos.x * rcCam.w / screenWidth;
        ret.y = pos.y * rcCam.h / screenHeight;

        return ret;
    }

    private Obj objectInside(Point p)
    {
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            if(o.isInside(p))
                return o;
        }
        return null;
    }

    long lastDown = 0;
    Obj tappedOn = null;
    static final float paddleDistThreshold = 0.5f;
    Obj clickAndDrag = null;
    //Point tappedDown = new Point(0,0);
    public void onTouchEvent(MotionEvent e)
    {
        //Log.e("TOUCHEVENT", "evnt: "+e.getAction());

        float x = e.getX();
        float y = e.getY();

        y = (float) (y - screenHeight / 2.0);
        x = (float) (x - screenWidth / 2.0);

        switch (e.getAction())
        {
            case MotionEvent.ACTION_MOVE:
            {
                if(bEditor)
                {
                    //Log.e("ACTIONMOVE", "move");
                    if(clickAndDrag != null)
                    {
                        if(clickAndDrag.type == Obj.typeBlock)
                        {
                            //Log.e("ACTIONMOVE", "not null");
                            Point pos = screenToGL(new Point(e.getX(), e.getY()));
                            pos.x = (int) (pos.x / blockGridSize);
                            pos.x = pos.x * blockGridSize;
                            pos.y = (int) (pos.y / blockGridSize);
                            pos.y = pos.y * blockGridSize;

                            for(Object i : mBlocks)
                            {
                                Obj o = (Obj) i;
                                if(o == clickAndDrag)
                                    continue;

                                //Don't set block to here if there's already a block here (no overlapping blocks)
                                if(o.pos.x == pos.x && o.pos.y == pos.y)
                                {
                                    pos = clickAndDrag.pos;
                                    break;
                                }
                            }
                            clickAndDrag.pos = pos;
                        }
                        else    //Slider
                        {
                            Point newPos = screenToGL(new Point(e.getX(), e.getY()));
                            Log.e("ACTIONMOVE", newPos.x + "," + newPos.y);
                            clickAndDrag.pos.x = Math.min(sliderEndX, Math.max(newPos.x, sliderStartX));
                            if(clickAndDrag == oRSlider)
                            {
                                oAddBlockButton.r = (sliderEndX - clickAndDrag.pos.x)/Math.abs(sliderStartX-sliderEndX);
                                if(lastDragged != null)
                                    lastDragged.r = oAddBlockButton.r;
                            }
                            else if(clickAndDrag == oGSlider)
                            {
                                oAddBlockButton.g = (sliderEndX - clickAndDrag.pos.x)/Math.abs(sliderStartX-sliderEndX);
                                if(lastDragged != null)
                                    lastDragged.g = oAddBlockButton.g;
                            }
                            else if(clickAndDrag == oBSlider)
                            {
                                oAddBlockButton.b = (sliderEndX - clickAndDrag.pos.x)/Math.abs(sliderStartX-sliderEndX);
                                if(lastDragged != null)
                                    lastDragged.b = oAddBlockButton.b;
                            }
                        }
                    }
                }
                else
                {
                    long curTime = SystemClock.uptimeMillis();
                    long diffTime = curTime - lastDown;
                    float dt = (float) (diffTime) / 1000.0f;
                    if (ballLaunched || dt > tapTime)
                    {
                        if(e.getY() > screenHeight / 2.0f)
                        {
                            //setAngle((float) (atan2(y, x)));
                            mAngle = (float) (atan2(y,x) * Point.TODEG);
                            //Log.e("PADDLEANGLE", "Angle: " + mAngle);
                            if(mAngle > MAX_PADDLE_ANGLE)
                                mAngle = MAX_PADDLE_ANGLE;
                            if(mAngle < MIN_PADDLE_ANGLE)
                                mAngle = MIN_PADDLE_ANGLE;
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN:
            {
                //tappedDown.x = e.getX();
                //tappedDown.y = e.getY();
                //Log.e("TAPDOWN", e.getX() + "," + e.getY());
                if(bEditor)
                {
                    //Log.e("ACTIONDOWN", "Pressing down");
                    Obj testClick = objectInside(screenToGL(new Point(e.getX(), e.getY())));
                    if(testClick != null)
                    {
                        //Log.e("ACTIONDOWN", "not null");
                        if(testClick.type == Obj.typeButton)
                        {
                            if(testClick == oEditorButton)
                            {
                                tappedOn = testClick;
                                lastDown = SystemClock.uptimeMillis();
                                break;
                            }
                            else if(testClick == oRemBlockButton)
                            {
                                tappedOn = testClick;
                                lastDown = SystemClock.uptimeMillis();
                                break;
                            }
                            else if(testClick == oAddBlockButton)
                            {
                                //Obj o = new Obj();

                                Obj oBlock = new Obj();
                                oBlock.scale = blockScale;
                                oBlock.sImg = "drawable/block";
                                oBlock.q = getImage("drawable/block");

                                oBlock.pos.x = x * blockGridSize;// - (2.5f * 0.25f);
                                oBlock.pos.y = y * blockGridSize;// - (2.5f * 0.25f);

                                //oBlock.setColor((float) Math.random(), (float) Math.random(), (float) Math.random(), 1.0f);
                                oBlock.setColor(oAddBlockButton.r, oAddBlockButton.g, oAddBlockButton.b);
                                oBlock.type = Obj.typeBlock;
                                mObjects.add(oBlock);
                                mBlocks.add(oBlock);
                                oBlock.genCollision();
                                blocksAdded++;

                                clickAndDrag = oBlock;
                                lastDragged = clickAndDrag;
                                clickAndDrag.pos = screenToGL(new Point(e.getX(), e.getY()));
                                clickAndDrag.pos.x = (int)(clickAndDrag.pos.x / blockGridSize);
                                clickAndDrag.pos.x = clickAndDrag.pos.x * blockGridSize;
                                clickAndDrag.pos.y = (int)(clickAndDrag.pos.y / blockGridSize);
                                clickAndDrag.pos.y = clickAndDrag.pos.y * blockGridSize;
                                break;

                                //mBlocks.add(o);
                                //mObjects.add(o);
                            }
                            else if(testClick == oRSlider || testClick == oGSlider || testClick == oBSlider)
                            {
                                clickAndDrag = testClick;
                                //Log.e("ACTIONDOWN", "click on slider");
                                break;
                            }
                        }
                        else if(testClick.type == Obj.typeBlock)
                        {
                            //Log.e("ACTIONDOWN", "drag");
                            clickAndDrag = testClick;
                            lastDragged = testClick;
                            oAddBlockButton.setColor(testClick.r, testClick.g, testClick.b);
                            oRSlider.pos.x = sliderEndX - (testClick.r * (sliderEndX-sliderStartX));
                            oGSlider.pos.x = sliderEndX - (testClick.g * (sliderEndX-sliderStartX));
                            oBSlider.pos.x = sliderEndX - (testClick.b * (sliderEndX-sliderStartX));
                            oRSlider.updateCollision();
                            oGSlider.updateCollision();
                            oBSlider.updateCollision();
                        }
                    }
                    else if(screenToGL(new Point(e.getX(), e.getY())).length() <= WALL_DIST)
                    {
                        //Tapping on nothing inside of the circle, reset colorizing
                        lastDragged = null;
                    }
                }
                else
                {
                    Obj testClick = objectInside(screenToGL(new Point(e.getX(), e.getY())));
                    if(testClick != null)
                    {
                        if(testClick.type == Obj.typeButton && testClick == oEditorButton)
                        {
                            tappedOn = testClick;
                            lastDown = SystemClock.uptimeMillis();
                            break;
                        }
                    }
                    float dist = (screenToGL(new Point(e.getX(), e.getY())).subtract(oPaddle.pos)).length();
                    if (dist < paddleDistThreshold)
                        lastDown = SystemClock.uptimeMillis();
                    else
                        lastDown = 0;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            {
                if(bEditor)
                {
                    if(clickAndDrag != null)
                    {
                        if(clickAndDrag.type == Obj.typeBlock)
                        {
                            clickAndDrag.updateCollision();
                            //Test and see if outside playable area, if so delete
                            if (clickAndDrag.pos.length() > WALL_DIST)
                            {
                                mObjects.remove(clickAndDrag);
                                mBlocks.remove(clickAndDrag);
                                blocksAdded--;

                            }
                        }
                        else
                        {
                            clickAndDrag.updateCollision();
                        }
                        clickAndDrag = null;
                        break;
                    }
                    Obj testClick = objectInside(screenToGL(new Point(e.getX(), e.getY())));
                    if(testClick != null && testClick == tappedOn)
                    {
                        if(testClick == oEditorButton)
                        {
                            long curTime = SystemClock.uptimeMillis();
                            long diffTime = curTime - lastDown;
                            float dt = (float) (diffTime) / 1000.0f;
                            if (dt <= tapTime)
                            {
                                ballLaunched = false;
                                bEditor = false;
                                oAddBlockButton.active = false;
                                oRemBlockButton.active = false;
                                oRSlider.active = false;
                                oGSlider.active = false;
                                oBSlider.active = false;

                                //Save level
                                levelSave();

                                oEditorButton.sImg = "drawable/editor";
                                oEditorButton.q = getImage(oEditorButton.sImg);
                                //oEditorButton.genCollision();
                                break;
                            }
                        }
                        else if(testClick == oRemBlockButton)
                        {
                            for(Object i : mBlocks)
                            {
                                mObjects.remove(i);
                            }
                            mBlocks.clear();
                            blocksAdded = 0;
                        }
                    }
                }
                else
                {
                    Obj testClick = objectInside(screenToGL(new Point(e.getX(), e.getY())));
                    if(testClick != null && testClick == oEditorButton && testClick == tappedOn)
                    {
                        long curTime = SystemClock.uptimeMillis();
                        long diffTime = curTime - lastDown;
                        float dt = (float) (diffTime) / 1000.0f;
                        if (dt <= tapTime)
                        {
                            bEditor = true;
                            oAddBlockButton.active = true;
                            oRemBlockButton.active = true;
                            oRSlider.active = true;
                            oGSlider.active = true;
                            oBSlider.active = true;
                            oAddBlockButton.updateCollision();
                            oEditorButton.sImg = "drawable/play";
                            oEditorButton.q  = getImage(oEditorButton.sImg);   //TODO Why doesn't this work?
                            //oEditorButton.genCollision();
                            oBall.speed = 0;
                            oBall.dir = 0;
                            oBall.pos.y = -1.5f;
                            oBall.pos.x = 0;
                            mAngle = 90;
                            //oPaddle.pos.y = WALL_DIST;
                            //oPaddle.pos.x = 0;
                            break;
                        }
                    }
                    long curTime = SystemClock.uptimeMillis();
                    long diffTime = curTime - lastDown;
                    float dt = (float) (diffTime) / 1000.0f;
                    if (dt <= tapTime && !ballLaunched)
                    {
                        oBall.speed = ballSpeed;
                        ballLaunched = true;
                    }
                }
                break;
            }
        }
    }

    public void levelSave()
    {
        levelSave("level" + curLevel + ".sav");    //TODO
    }

    public void levelSave(String filename)
    {
        //TODO If player is in editor, save into this folder
        //File file = new File(mContext.getFilesDir(), filename);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        Log.e("LEVELSAVE", "Saving path: " + file.getAbsolutePath());
        //if (!file.mkdirs())
        //{
        //    Log.e("LEVELSAVE", "Directory not created");
        //}
        FileOutputStream outputStream;
        try
        {
            outputStream = new FileOutputStream(file);
            String s = "";
            s += mBlocks.size() + " ";
            for(Object i : mBlocks)
            {
                Obj o = (Obj) i;

                s += o.pos.x + " " + o.pos.y + " " + o.r + " " + o.g + " " + o.b + " ";
                outputStream.write(s.getBytes());
                s = "";
            }
            outputStream.close();
        } catch (java.io.IOException e)
        {
            e.printStackTrace();
        }

        //Broadcast so it shows up in Windows Explorer...
        if(bDebug)
        {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            mContext.sendBroadcast(intent);
        }

        //Log.e("LEVELSAVE", filename);
    }

    private void levelFromStr(String sLevelStr)
    {
        String[] splitValues = sLevelStr.split("\\s+");
        int curNum = 0;
        int numBlocks = Integer.parseInt(splitValues[curNum++]);
        for(int i = 0; i < numBlocks; i++)
        {
            Obj oBlock = new Obj();
            oBlock.scale = blockScale;
            oBlock.sImg = "drawable/block";

            oBlock.pos.x = Float.parseFloat(splitValues[curNum++]);
            oBlock.pos.y = Float.parseFloat(splitValues[curNum++]);

            oBlock.setColor(Float.parseFloat(splitValues[curNum++]),
                    Float.parseFloat(splitValues[curNum++]),
                    Float.parseFloat(splitValues[curNum++]));
            oBlock.type = Obj.typeBlock;
            mObjects.add(oBlock);
            mBlocks.add(oBlock);
            blocksAdded++;
        }
    }

    public void levelLoad(int levelNum)
    {
        int levelId;

        switch(levelNum)
        {
            case 1:
                levelId = R.raw.level1;
                break;

            case 2:
                levelId = R.raw.level2;
                break;

            case 3:
                levelId = R.raw.level3;
                break;

            case 4:
                levelId = R.raw.level4;
                break;

            //case 5:
            //    levelId = R.raw.level5;
            //    break;

            default:
                levelId = R.raw.level1;
                break;
        }

        String s = "";
        try
        {
            InputStream inputStream = mContext.getResources().openRawResource(levelId);
            int sz = inputStream.available();
            byte[] inputStr = new byte[sz];
            inputStream.read(inputStr, 0, sz);
            s = new String(inputStr, "UTF-8");
            inputStream.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        if(s.length() > 0)
            levelFromStr(s);
    }

    public void levelLoad(String sFilename)
    {
        String s = "";

        File file = new File(mContext.getFilesDir(), sFilename);
        //Log.e("LEVELLOAD", "Path: " + file.getAbsolutePath());
        FileInputStream inputStream;
        try
        {
            inputStream = new FileInputStream(file);
            int sz = inputStream.available();
            byte[] inputStr = new byte[sz];
            inputStream.read(inputStr, 0, sz);
            s = new String(inputStr, "UTF-8");
            inputStream.close();
        } catch (java.io.IOException e)
        {
            e.printStackTrace();
        }

        if(s.length() > 0)
            levelFromStr(s);
    }

}
