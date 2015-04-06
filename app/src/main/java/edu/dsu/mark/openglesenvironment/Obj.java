package edu.dsu.mark.openglesenvironment;

import android.opengl.Matrix;

/**
 * Created by Mark on 4/3/2015.
 */
public class Obj
{

    

    private static final float pix_per_tex = 300.0f;    //Could use actual math to get this number, but why bother?

    public Quad q;
    public Point pos;
    //public float x, y;  // x,y screen pos in texels
    public float angle; //Rotation to draw at
    public float scale; //scale factor

    private float r, g, b, a;

    public float dir, speed;   //Used to update movement

    public Shape collide;

    public Obj()//Quad qd)
    {
        q = null;
        collide = null;
        pos = new Point();
        angle = 0.0f;
        scale = 1.0f;
        r = g = b = a = 1;

        dir = speed = 0.0f;
    }

    public void draw(float[] m)
    {
        if(q == null)
            return;

        float[] scratch = new float[16];
        float[] finalmtx = new float[16];

        Matrix.setIdentityM(scratch, 0);
        Matrix.translateM(scratch, 0, pos.x, pos.y, 0);
        Matrix.rotateM(scratch, 0, angle, 0, 0, 1.0f);

        float w = q.getWidth();
        float h = q.getHeight();

        w /= pix_per_tex;   //scale according to image size
        h /= pix_per_tex;

        Matrix.scaleM(scratch, 0, w * scale, h * scale, 1);

        Matrix.multiplyMM(finalmtx, 0, m, 0, scratch, 0);
        q.setColor(r, g, b, a);
        q.draw(finalmtx);
    }

    public void update(float dt)
    {
        pos.x += Math.cos(Point.TORAD * dir) * speed * dt;
        pos.y += Math.sin(Point.TORAD * dir) * speed * dt;

        if(collide != null)
        {
            collide.pos.x = pos.x;
            collide.pos.y = pos.y;
            collide.angle = angle;
        }
    }

    public void setColor(float cr, float cg, float cb, float ca)
    {
        r = cr;
        g = cg;
        b = cb;
        a = ca;
    }

    public void genCollision()
    {
        genCollision(false);
    }

    public void genCollision(boolean circle)
    {
        float w = q.getWidth();
        float h = q.getHeight();

        w /= pix_per_tex;   //scale according to image size
        h /= pix_per_tex;

        w *= scale;
        h *= scale;

        if(circle)
        {
            Circle c = new Circle();
            c.rad = Math.min(w/2.0f, h/2.0f);

            collide = c;
        }
        else
        {
            Rect r = new Rect();

            r.w = w;
            r.h = h;

            collide = r;
        }
    }

    public ContactManifold colliding(Obj b)
    {
        return collide.collide(b.collide);
    }

}
