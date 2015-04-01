package edu.dsu.mark.openglesenvironment;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * A two-dimensional square for use as a drawn object in OpenGL ES 2.0.
 */
public class Quad {

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            /*"uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // The matrix must be included as a modifier of gl_Position.
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";*/
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 a_texCoord;" +
            "varying vec2 v_texCoord;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  v_texCoord = a_texCoord;" +
            "}";

    private final String fragmentShaderCode =
            /*"precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";*/
            "precision mediump float;" +
            "varying vec2 v_texCoord;" +
            "uniform sampler2D s_texture;" +
            "void main() {" +
            "  gl_FragColor = texture2D( s_texture, v_texCoord );" +
            "}";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    public static float uvs[];
    public FloatBuffer uvBuffer;
    public Context mContext;
    private int width, height;
    int texID;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float squareCoords[] = {
            -0.5f,  0.5f, 0.0f,   // top left
            -0.5f, -0.5f, 0.0f,   // bottom left
            0.5f, -0.5f, 0.0f,   // bottom right
            0.5f,  0.5f, 0.0f    // top right
    };

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices
    //private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    //float color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public Quad() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables

        texID = 0;
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param m - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] m)
    {
        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);

        // clear Screen and Depth Buffer, we have set the clear color as black.
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        // Get handle to texture coordinates location
        int mTexCoordLoc = GLES20.glGetAttribLocation(mProgram, "a_texCoord" );

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray ( mTexCoordLoc );

        // Prepare the texture coordinates
        GLES20.glVertexAttribPointer ( mTexCoordLoc, 2, GLES20.GL_FLOAT,
                false,
                0, uvBuffer);

        // Get handle to shape's transformation matrix
        int mHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mHandle, 1, false, m, 0);

        // Get handle to textures locations
        int mSamplerLoc = GLES20.glGetUniformLocation (mProgram, "s_texture" );

        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i ( mSamplerLoc, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);

    }

    public void setContext(Context c)
    {
        mContext = c;
    }

    public void loadImage(String s)
    {
        // Create our UV coordinates.
        uvs = new float[] {
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f
        };

        // The texture buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        // Generate Textures, if more needed, alter these numbers.
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        // Retrieve our image from resources.
        int id = mContext.getResources().getIdentifier(s, null, mContext.getPackageName());

        // Temporary create a bitmap
        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), id);

        width = bmp.getWidth();
        height = bmp.getHeight();

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        texID = textures[0];    //Save for later binding I guess?

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

        // We are done using the bitmap so we should recycle it.
        bmp.recycle();

    }
}