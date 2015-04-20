package edu.dsu.mark.breakoutes;

/**
 * Created by Mark on 4/6/2015.
 */
public class Circle extends Shape
{
    public final static int circleType = 2;
    public float rad;

    Circle()
    {
        type = circleType;
        rad = 0;
    }

    public float getWidth()
    {
        return rad;
    }

    public float getHeight()
    {
        return rad;
    }

    public boolean isInside(Point p)
    {
        Point test = p.subtract(pos);
        return (test.length() <= rad);
    }

    public ContactManifold collide(Shape s)
    {
        ContactManifold cm = new ContactManifold();
        cm.shape1 = this;
        cm.shape2 = s;

        if(s.type == Rect.rectType) //Circle/rect collision
        {
            //Math shamelessly stolen from http://www.migapro.com/circle-and-rotated-rectangle-collision-detection/
            Rect rc = (Rect) s;

            //The first step is to rotate the circle backwards the amount the rectangle is rotated
            float cx = (float) (Math.cos(-rc.angle * Point.TORAD) * (pos.x - rc.pos.x) -
                                Math.sin(-rc.angle * Point.TORAD) * (pos.y - rc.pos.y) + rc.pos.x);
            float cy = (float) (Math.sin(-rc.angle * Point.TORAD) * (pos.x - rc.pos.x) +
                                Math.cos(-rc.angle * Point.TORAD) * (pos.y - rc.pos.y) + rc.pos.y);


            float closestX, closestY;

            //Find the closest x point from the new circle center
            if (cx  < rc.pos.x - rc.w / 2.0f)
                closestX = rc.pos.x - rc.w / 2.0f;
            else if (cx  > rc.pos.x + rc.w / 2.0f)
                closestX = rc.pos.x + rc.w / 2.0f;
            else
                closestX = cx;

            //Same for y
            if (cy < rc.pos.y - rc.h / 2.0f)
                closestY = rc.pos.y - rc.h / 2.0f;
            else if (cy > rc.pos.y + rc.h / 2.0f)
                closestY = rc.pos.y + rc.h / 2.0f;
            else
                closestY = cy;

            //Determine collision
            cm.collide = false;

            //double distance = findDistance(unrotatedCircleX , unrotatedCircleY, closestX, closestY);
            /*public double findDistance(double fromX, double fromY, double toX, double toY){
    double a = Math.abs(fromX - toX);
    double b = Math.abs(fromY - toY);

    return Math.sqrt((a * a) + (b * b));
}*/

            cm.normal1.x = closestX - cx;
            cm.normal1.y = closestY - cy;

            float normLen = cm.normal1.length();

            // Collision
            cm.collide = normLen < rad;

            cm.normal1.rotate(rc.angle);

            normLen = Math.abs(rad - normLen);
            cm.normal1.normalize();
            cm.normal1.mul(normLen);

            cm.normal2.x = -cm.normal1.x;
            cm.normal2.y = -cm.normal1.y;
        }
        else if(s.type == circleType)
        {
            cm.collide = false;
            cm.normal1.x = s.pos.x - pos.x;
            cm.normal1.y = s.pos.y - pos.y;

            Circle c = (Circle) s;

            float normLen = (c.rad + rad) - cm.normal1.length();

            cm.collide = normLen > 0.0f;

            cm.normal1.normalize();
            cm.normal1.mul(Math.abs(normLen));

            cm.normal2.x = -cm.normal1.x;
            cm.normal2.y = -cm.normal1.y;
        }







        return cm;
    }
}
