package edu.dsu.mark.openglesenvironment;

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
}
