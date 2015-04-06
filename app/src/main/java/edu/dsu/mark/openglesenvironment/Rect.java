package edu.dsu.mark.openglesenvironment;

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
