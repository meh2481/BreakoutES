package edu.dsu.mark.breakoutes;

/**
 * Created by Mark on 4/6/2015.
 */
public abstract class Shape
{
    public Point pos;

    public int type;
    public float angle;

    public Shape()
    {
        type = 0;
        angle = 0;
        pos = new Point();
    }

    public abstract ContactManifold collide(Shape s);
    public abstract float getWidth();
    public abstract float getHeight();
    public abstract boolean isInside(Point p);
}
