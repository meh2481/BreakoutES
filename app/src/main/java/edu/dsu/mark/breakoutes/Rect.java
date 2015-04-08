package edu.dsu.mark.breakoutes;

/**
 * Created by Mark on 4/6/2015.
 */
public class Rect extends Shape
{
    public final static int rectType = 1;

    public float w, h;

    Rect()
    {
        type = rectType;
        w = h = 0;
    }

    public float getWidth()
    {
        return w;
    }

    public float getHeight()
    {
        return h;
    }

    @Override
    public boolean isInside(Point p)
    {
        //TODO: Rotated rect testing
        return (p.x > pos.x - w/2.0f &&
                p.x < pos.x + w/2.0f &&
                p.y > pos.y - h/2.0f &&
                p.y < pos.y + h/2.0f);
    }

    public ContactManifold collide(Shape s)
    {
        ContactManifold cm = new ContactManifold();

        if(s.type == Circle.circleType)
        {
            //Handle circle-square collision in the Circle class
            cm = s.collide(this);

            //Flip contactmanifold fields so they're correct
            Point tmp = cm.normal1;
            cm.normal1 = cm.normal2;
            cm.normal2 = tmp;

            Shape sTmp = cm.shape1;
            cm.shape1 = cm.shape2;
            cm.shape2 = sTmp;

            return cm;
        }

        //TODO: Rect/rect collision



        return cm;
    }

}
