package edu.dsu.mark.breakoutes;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static java.lang.Math.atan2;

import java.util.List;
import java.util.concurrent.*;

public class BreakoutESRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener
{
    public static final String SWIPE_LEFT = "SWIPERL";
    public static final String SWIPE_RIGHT = "SWIPELR";
    public static final String SWIPE_UP = "SWIPEBT";
    public static final String SWIPE_DOWN = "SWIPETB";
    public static final String TAP = "TAP";

    private static boolean bDebug = false;///true; //TODO false
    private static final float tapTime = 0.25f;
    private static final float blockScale = 0.3f;
    private static final float blockGridSize = blockScale / 1.5625f;
    private Obj oBall;
    private Obj oPaddle;
    private Obj oEditorButton;
    private Obj oAddBlockButton;
    private Obj oRemBlockButton;
    private Obj oRSlider, oGSlider, oBSlider;
    private Obj lastDragged = null;

    private static final int TOTAL_LIVES = 5;
    private int mLives = TOTAL_LIVES;
    Quad qLifeDraw;

    public MediaPlayer music;
    private MediaPlayer newlevelSound;
    private MediaPlayer deathSound;
    private MediaPlayer hitblockSound;
    private MediaPlayer paddleSound;
    private MediaPlayer wallSound;

    private List<String> konamiCode;
    private Iterator<String> konamiIterator;

    private Line drawLine;
    private int[] mCamTex;
    private Camera mCamera = null;
    private SurfaceTexture mSTexture;
    private boolean mUpdateST = false;
    Quad qCamQuad = new Quad(); //We'll use a quad to draw the camera input (I'm sorry)

    public LinkedBlockingQueue<MotionEvent> motEvents;
    public LinkedBlockingQueue<String> miscEvents;

    private int blocksAdded;
    private int curLevel = 1;
    private boolean bEditor = false;

    private HashMap<String, Quad> mImages;
    private LinkedList<Obj> mObjects;
    private LinkedList<Obj> mBlocks;

    private static final float WALL_DIST = 1.9f;   //Distance the paddle is from the center
    private static final float PADDLE_DIST = 2.1f;
    private static final float MAX_PADDLE_ANGLE = 125.0f;
    private static final float MIN_PADDLE_ANGLE = 55.0f;
    private static final float BOARD_MAX_ANGLE = -45.0f;
    private static final float BOARD_MIN_ANGLE = -135.0f;
    private static final float ballSpeed = 2.3f;
    private static float sliderStartX = -2.1f;
    private static float sliderEndX = -2.8f;

    private long lastTime;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private float mAngle = 90;

    private float camX, camY, camZ;
    private float screenWidth, screenHeight;
    private Context mContext;

    private boolean ballLaunched = false;

    public BreakoutESRenderer(Context c)
    {
        konamiCode = new ArrayList<>();
        konamiCode.add(SWIPE_UP);
        konamiCode.add(SWIPE_UP);
        konamiCode.add(SWIPE_DOWN);
        konamiCode.add(SWIPE_DOWN);
        konamiCode.add(SWIPE_LEFT);
        konamiCode.add(SWIPE_RIGHT);
        konamiCode.add(SWIPE_LEFT);
        konamiCode.add(SWIPE_RIGHT);
        konamiCode.add(TAP);
        konamiCode.add(TAP);
        konamiIterator = konamiCode.iterator();

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
        if(!bDebug)
            oEditorButton.active = false;
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

        music = MediaPlayer.create(c, R.raw.theme);
        music.setLooping(true);
        music.start();

        newlevelSound = MediaPlayer.create(c, R.raw.newlevel);
        deathSound = MediaPlayer.create(c, R.raw.death);
        hitblockSound = MediaPlayer.create(c, R.raw.hitblock);
        paddleSound = MediaPlayer.create(c, R.raw.hitpaddle);
        wallSound = MediaPlayer.create(c, R.raw.wallhit);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        genCameraTexture();
        mSTexture = new SurfaceTexture ( mCamTex[0] );
        mSTexture.setOnFrameAvailableListener(this);

        if(mCamera == null)
            mCamera = Camera.open();
        try
        {
            mCamera.setPreviewTexture(mSTexture);
        }
        catch(IOException ioe)
        {
            Log.e("SURFCREATED", "couldn't open camera");
        }
        // Set the background frame color
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        mImages = new HashMap<>();  //Dump any previous image memory

        //Reload images
        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            o.q = getImage(o.sImg);
        }
        getImage("drawable/play");
        getImage("drawable/editor");
        getImage("drawable/block");
        qLifeDraw = getImage("drawable/pinball");

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

        drawLine = new Line();
    }

    @Override
    public void onDrawFrame(GL10 unused)
    {

        synchronized(this)
        {
            if(mUpdateST)
            {
                mSTexture.updateTexImage();
                mUpdateST = false;
            }
        }

        long curTime = SystemClock.uptimeMillis();
        long diffTime = curTime - lastTime;
        float dt = (float) (diffTime) / 1000.0f;
        updateObjects(dt);

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, camX, camY, camZ, 0f, 0f, 0f, 0f, 1.0f, 0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        //Draw camera image below everything else
        if(!bEditor)
        {
            Rect r = screenRect();
            float[] scratch = new float[16];
            float[] finalmtx = new float[16];
            Matrix.setIdentityM(scratch, 0);
            Matrix.scaleM(scratch, 0, -r.getWidth(), -r.getHeight(), 1);
            Matrix.multiplyMM(finalmtx, 0, mMVPMatrix, 0, scratch, 0);
            qCamQuad.draw(finalmtx);

            //Draw lives left
            Point p = screenToGL(new Point(screenWidth, 0));
            float liveDrawW = 0.25f;
            for(int i = 0; i < mLives; i++)
            {
                Matrix.setIdentityM(scratch, 0);
                Matrix.translateM(scratch, 0, p.x + liveDrawW / 2.0f, p.y - liveDrawW / 2.0f - (i * liveDrawW), 0);
                Matrix.scaleM(scratch, 0, liveDrawW, liveDrawW, 1);
                Matrix.multiplyMM(finalmtx, 0, mMVPMatrix, 0, scratch, 0);
                qLifeDraw.draw(finalmtx);
            }

        }

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

        lastTime = curTime;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1, 7);

        screenWidth = width;
        screenHeight = height;

        //Lay out editor buttons
        oEditorButton.pos = screenToGL(new Point(0,0));
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


        //Update camera size (Example from http://maninara.blogspot.co.uk/2012/09/render-camera-preview-using-opengl-es.html)
        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        if(psize.size() > 0)
        {
            int i;
            for ( i = 0; i < psize.size(); i++ )
            {
                if ( psize.get(i).width > width || psize.get(i).height > height )
                {
                    i--;
                    break;
                }
            }
            param.setPreviewSize(psize.get(i).width, psize.get(i).height);
            qCamQuad.width = psize.get(i).width;
            qCamQuad.height = psize.get(i).height;
        }
        param.set("orientation", "landscape");
        mCamera.setParameters ( param );
        mCamera.startPreview();

    }

    /*
     * Utility method for compiling a OpenGL shader.
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

    public float wrapAngle(float angle)
    {
        if(angle < 0)
            return angle + 360;
        if(angle > 360)
            return angle - 360;
        return angle;
    }

    public float reflect(float angleToRef, float surfNormal)
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

        float objDist = o.pos.length();
        float objAngle = o.pos.angle();

        //Simple test to make ball bounce off circular walls
        if(objDist > WALL_DIST)
        {
            if(objAngle < BOARD_MAX_ANGLE && objAngle > BOARD_MIN_ANGLE)
            {
                //TODO Game over stuff
                oBall.dir = 0;
                oBall.speed = 0;
                ballLaunched = false;
                //TODO Death sound
                mLives--;
                deathSound.start();
                if(mLives <= 0)
                {
                    mLives = TOTAL_LIVES;
                    curLevel = 1;
                    wipeLevel();
                }
            }
            else
            {
                o.dir = reflect(o.dir, wrapAngle(objAngle + 180));
                o.pos.x = (float) (Math.cos(objAngle * Point.TORAD) * WALL_DIST);
                o.pos.y = (float) (Math.sin(objAngle * Point.TORAD) * WALL_DIST);
                //TODO Ball hit wall sound
                wallSound.start();
            }
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
            eLast = e;
        }

        //Process other misc. input events synchronously as well
        while(true)
        {
            String s = miscEvents.poll();
            if (s == null)
                break;

            if (s.equals(SWIPE_LEFT) && bEditor)
            {
                curLevel++;
                wipeLevel();
            }
            else if(s.equals(SWIPE_RIGHT) && bEditor)
            {
                if(curLevel > 1)
                {
                    curLevel--;
                    wipeLevel();
                }
            }

            if(s.equals(konamiIterator.next()))
            {
                Log.e("CODEINPUT", "Success " + s);
                if(!konamiIterator.hasNext())
                {
                    konamiIterator = konamiCode.iterator();
                    Log.e("CODEINPUT", "Code input success");
                    bDebug = true;
                    oEditorButton.active = true;
                }
            }
            else
            {
                Log.e("CODEINPUT", "fail " + s);
                konamiIterator = konamiCode.iterator();
            }
        }

        if(bEditor)
            return;

        for(Object i : mObjects)
        {
            Obj o = (Obj) i;
            o.update(dt);
        }
        wallCheck(oBall);

        boolean toReset = false;
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
                    //Sanity checks, cause hitting the ball at the wrong angle seems to make it go NaN,NaN,NaN batman
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
                    if(hitblockSound.isPlaying())
                        hitblockSound.seekTo(0);
                    else
                        hitblockSound.start();
                    blocksAdded--;
                    if(blocksAdded <= 0)
                    {
                        //TODO: Won state
                        toReset = true;
                        curLevel++; //Incr level TODO Test if last level or such
                        newlevelSound.start();
                    }
                }
                else if(o.type == Obj.typePaddle)
                {
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
                    if(ballLaunched)
                        paddleSound.start();
                }
            }
        }

        if(toReset)
        {
            wipeLevel();
        }
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
        resetLevel();
        for(Object i : mBlocks)
        {
            Obj o = (Obj) i;
            o.q = getImage(o.sImg);
            o.genCollision();
        }
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
        ballLaunched = false;
        oBall.speed = 0;
        levelLoad(curLevel);
    }

    private Rect screenRect()
    {
        final float tan45 = (float) Math.tan(Math.PI/4.0);
        final float aspect = screenWidth / screenHeight;

        Rect rcCam = new Rect();

        rcCam.h = tan45 * camZ * 2;
        rcCam.w = rcCam.h * aspect;
        return rcCam;
    }

    private Point screenToGL(Point pos)
    {
        pos.x -= screenWidth / 2.0f;
        pos.y -= screenHeight / 2.0f;

        Rect rcCam = screenRect();

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
    public void onTouchEvent(MotionEvent e)
    {
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
                    if(clickAndDrag != null)
                    {
                        if(clickAndDrag.type == Obj.typeBlock)
                        {
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
                            mAngle = (float) (atan2(y,x) * Point.TODEG);
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
                if(bEditor)
                {
                    Obj testClick = objectInside(screenToGL(new Point(e.getX(), e.getY())));
                    if(testClick != null)
                    {
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
                                Obj oBlock = new Obj();
                                oBlock.scale = blockScale;
                                oBlock.sImg = "drawable/block";
                                oBlock.q = getImage("drawable/block");

                                oBlock.pos.x = x * blockGridSize;
                                oBlock.pos.y = y * blockGridSize;

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
                            }
                            else if(testClick == oRSlider || testClick == oGSlider || testClick == oBSlider)
                            {
                                clickAndDrag = testClick;
                                break;
                            }
                        }
                        else if(testClick.type == Obj.typeBlock)
                        {
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
                            oEditorButton.q  = getImage(oEditorButton.sImg);
                            oBall.speed = 0;
                            oBall.dir = 0;
                            oBall.pos.y = -1.5f;
                            oBall.pos.x = 0;
                            mAngle = 90;
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
                levelId = R.raw.level2;
                break;

            case 2:
                levelId = R.raw.level3;
                break;

            case 3:
                levelId = R.raw.level4;
                break;

            case 4:
                levelId = R.raw.level1; //Level 1 is too hard
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

    private void genCameraTexture()
    {
        mCamTex = new int[1];
        GLES20.glGenTextures(1, mCamTex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCamTex[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        qCamQuad.loadTex(mCamTex[0]);
    }

    public synchronized void onFrameAvailable(SurfaceTexture st)
    {
        mUpdateST = true;
    }

    public void close()
    {
        mUpdateST = false;
        mSTexture.release();
        mCamera.stopPreview();
        mCamera = null;
        GLES20.glDeleteTextures(1, mCamTex, 0);
    }

}
