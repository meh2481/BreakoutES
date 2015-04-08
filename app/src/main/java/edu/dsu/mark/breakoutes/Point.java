package edu.dsu.mark.breakoutes;

/**
 * Created by Mark on 4/6/2015.
 */
public class Point
{
    public static final float TODEG = (float) (180.0f / Math.PI);
    public static final float TORAD = (float) (Math.PI / 180.0f);

    public float x, y;

    public Point()
    {
        x = y = 0.0f;
    }
    public Point(float xp, float yp) {x = xp; y = yp;}

    public float length()
    {
        return (float) Math.sqrt(x*x + y*y);
    }

    public float lengthSquared()
    {
        return x*x + y*y;
    }

    public void normalize()
    {
        float len = length();
        x = x / len;
        y = y / len;
    }

    public float angle()
    {
        return (float) (Math.atan2(y, x) * TODEG);
    }

    public void rotate(float angle)
    {
        float c = (float) Math.cos(angle * Point.TORAD);
        float s = (float) Math.sin(angle * Point.TORAD);
        float origX = x;
        float origY = y;
        x = c*origX - s*origY;
        y = s*origX + c*origY;
    }

    public void mul(float amt)
    {
        x *= amt;
        y *= amt;
    }

    public Point subtract(Point p)
    {
        Point ret = new Point();

        ret.x = x - p.x;
        ret.y = y - p.y;

        return ret;
    }
}
