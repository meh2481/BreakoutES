package edu.dsu.mark.openglesenvironment;

import android.opengl.Matrix;

/**
 * Created by Mark on 4/3/2015.
 */
public class Obj
{

    private static final float TODEG = (float) (180.0f / Math.PI);
    private static final float TORAD = (float) (Math.PI / 180.0f);

    private static final float pix_per_tex = 300.0f;    //Could use actual math to get this number, but why bother?

    public Quad q;
    public float x, y;  // x,y screen pos in texels
    public float angle; //Rotation to draw at
    public float scale; //scale factor

    public float dir, speed;   //Used to update movement

    //TODO collision rect/circle

    public Obj()//Quad qd)
    {
        q = null;
        x = y = angle = 0.0f;
        scale = 1.0f;

        dir = speed = 0.0f;
    }

    public void draw(float[] m)
    {
        if(q == null)
            return;

        float[] scratch = new float[16];
        float[] finalmtx = new float[16];

        Matrix.setIdentityM(scratch, 0);
        Matrix.translateM(scratch, 0, x, y, 0);
        Matrix.rotateM(scratch, 0, angle, 0, 0, 1.0f);

        float w = q.getWidth();
        float h = q.getHeight();

        w /= pix_per_tex;   //scale according to image size
        h /= pix_per_tex;

        Matrix.scaleM(scratch, 0, w * scale, h * scale, 1);

        Matrix.multiplyMM(finalmtx, 0, m, 0, scratch, 0);
        q.draw(finalmtx);
    }

    public void update(float dt)
    {
        x += Math.cos(TORAD * dir) * speed * dt;
        y += Math.sin(TORAD * dir) * speed * dt;
    }

}
