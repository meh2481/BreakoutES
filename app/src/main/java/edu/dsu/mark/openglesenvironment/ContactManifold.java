package edu.dsu.mark.openglesenvironment;

/**
 * Created by Mark on 4/6/2015.
 */
public class ContactManifold
{
    public Point normal1;
    public Point normal2;

    public Shape shape1;
    public Shape shape2;

    public boolean collide;

    public ContactManifold()
    {
        normal1 = new Point();
        normal2 = new Point();
    }
}
