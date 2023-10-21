package Renderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

import Model.Model;
import Vector.*;
import static java.lang.Math.*;
import static Vector.VecOperator.*;
import ppmWriter.*;
public class Renderer {
    //embed a buffered image in the renderer
    BufferedImage pixelBuffer;
    Model modelObject;
    //model data. stored such that index 0 corresponds to the 3 3D coordinates specified by face 0.
    Vec3f[][] vertexCoords;
    Vec3f[][] textureCoords;
    Vec3f[][] normalCoords;
    float[] depthBuffer;
    public final int width, height;
    public int[] colorBuffer;        //pixel buffer
    public Color[] textureData;
    public int texHeight, texWidth;
    public Light diffuse = new Light();
    public Light ambient = new Light();
    {
        //default values
        diffuse.lightColor= Color.white;
        diffuse.direction = new Vec3f(0.5f,0.0f,0.3f);
        ambient.lightColor = new Color(0.3f,0.15f,0.2f);
    }
    //testing
    int nrModelFaces;
    //TODO structure : probably better to use an entire array of lights. Improve the general structure.
    public Renderer(int screenWidth, int screenHeight)
    {
        width=screenWidth;
        height=screenHeight;
        colorBuffer = new int[width*height];
        pixelBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        //default color
        fill(new Color(0.2f,0.2f,0.2f));
        depthBuffer= new float[height*width];
        clearDepthBuffer();
    }
    public void loadModelData(Model modelObject)
    {   //loads vertex, normal, and texture coords in the order specified by faces
        this.modelObject=modelObject;
        nrModelFaces=modelObject.nFaces();
        vertexCoords = new Vec3f[modelObject.nFaces()][3];
        textureCoords = new Vec3f[modelObject.nFaces()][3];
        normalCoords = new Vec3f[modelObject.nFaces()][3];
        System.out.println("FACES : "+modelObject.nFaces());;
        for (int i = 0; i<modelObject.nFaces(); i++)
        {
            for (int j = 0; j<3;j++)
            {   //iterate through every face 3 times to get every index
                //indexV is a single index, representing one Vec3f object
                int indexV = modelObject.getVertexIndices(i)[j];
                vertexCoords[i][j] = new Vec3f (modelObject.getVertexCoords(indexV));
                if (modelObject.nTextures()!=0)
                {
                    int indexT = modelObject.getTextureIndices(i)[j];
                    textureCoords[i][j] = new Vec3f(modelObject.getTexCoords(indexT));
                }
                if (modelObject.nNormals()!=0)
                {
                    int indexN = modelObject.getNormalIndices(i)[j];
                    normalCoords[i][j] = new Vec3f(modelObject.getNormalCoords(indexN));
                }
            }
        }
    }
    public void renderModel()
    {
        //calls drawTriangle function on loaded model data
        //TODO error handling : assumes model data is loaded
        //TODO error handling : this function assumes nFaces() always matches coords size
        for (int i = 0; i<modelObject.nFaces(); i++)
        {
            //deliver data. sacrificing readability for some speed
            drawTriangle
            (
                    new Vec3f[]{vertexCoords[i][0], vertexCoords[i][1], vertexCoords[i][2]},
                    modelObject.nTextures()==0? null: new Vec3f[]{textureCoords[i][0], textureCoords[i][1], textureCoords[i][2]},
                    depthBuffer,
                    shadeDiffuseAmbient(cross(minus(vertexCoords[i][1], vertexCoords[i][0]),
                            minus(vertexCoords[i][2], vertexCoords[i][0])).getNormalized())
            );
        }

    }
    public Color shadeDiffuseAmbient (Vec3f normal)
    {   //returns intensity*diffuse+ambient
        return sumColor(diffuseDirectional(normal), ambient.lightColor);
    }

    public void clearDepthBuffer()
    {
        Arrays.fill(depthBuffer, -Float.MAX_VALUE);
    }
    public void fill (Color fillColor)
    {       //10-15ms per frame
        Arrays.fill(colorBuffer, fillColor.getRGB());
    }
    public void testTexture () throws IOException
    {
        ppmWriter myWriter = new ppmWriter(texWidth, texHeight);
        int[] colorBuffer = new int[texHeight*texWidth];
        for (int i = 0;i<texHeight*texWidth;i++)
        {
            colorBuffer[i]=textureData[i].getRGB();
        }
        myWriter.setTitle("TEXTURE_TEST");
        myWriter.writeToPPM(colorBufferToString(colorBuffer));
    }
    public void setPixel(Pixel p, Color RGBcolor)
    {
        // max range for p.x and p.y is width-1 and height-1 respectively
        // we load y in reverse so that (0, 0) is at the left bottom of the screen
         colorBuffer[p.x()+width*(height-p.y()-1)]= RGBcolor.getRGB();
    //    pixelBuffer.setRGB(p.x(), height-p.y()-1, RGBcolor.getRGB());
    }
    public void printBuffer()
    {
        pixelBuffer.setRGB(0,0,width,height, colorBuffer,0,width);
    }
    public void drawLine (Pixel p0, Pixel p1, Color color)
    {   // max range for p.x and p.y is width-1 and height-1 respectively
        int x0 = p0.x(), y0=p0.y(), x1=p1.x(), y1=p1.y();
        boolean steep = false;
        if (Math.abs(x0-x1)<Math.abs(y0-y1))    //if height>width
        {
            //transpose both points
            int temp = x0;
            x0=y0;
            y0=temp;
            temp = x1;
            x1=y1;
            y1=temp;
            steep = true;
        }
        if (x0>x1)  //swap the points if x0 is to the right of x1
        {
            int temp = x0;
            x0=x1;
            x1=temp;
            temp = y0;
            y0=y1;
            y1=temp;
        }
        int y = y0;
        int dy = y1-y0;
        int dx = x1-x0;
        int derror = Math.abs(2*dy);
        int error = 0;
        if (steep)
        {
            for (int x = x0; x<=x1; x++)
            {
                setPixel(new Pixel(y,x), color);
                error+=derror;
                if (error>dx)
                {
                    y+=y1>y0? 1:-1;
                    error -=2*dx;
                }
            }
        }
        else
        {
            for (int x = x0; x<=x1; x++)
            {
                setPixel(new Pixel(x,y), color);
                error+=derror;
                if (error>dx)
                {
                    y+=y1>y0? 1:-1;
                    error -=2*dx;
                }
            }
        }
    }
    public Color diffuseDirectional (Vec3f normal)
    {   //returns diffuse color multiplied by intensity according to angle with respect to normal
        float intensity =max(dot(normal.getNormalized(), diffuse.direction.getNormalized().neg()), 0.0f);
        return new Color((int) min(intensity*diffuse.lightColor.getRed(), 255),
                (int) min(intensity*diffuse.lightColor.getGreen(), 255),
                (int) min(intensity*diffuse.lightColor.getBlue(), 255), 255);
    }

    private float interpolate(float[]pts, Vec3f barycentric,float value)
    {//interpolates value between 3 points. Assumes value = 0 at start
        float[] screen = new float[]{barycentric.x(),barycentric.y(), barycentric.z()};
        for (int i = 0; i<pts.length; i++)
        {
            value+=pts[i]*screen[i];
        }
        return  value;
    }
    private Vec3f barycentric (Vec3f A, Vec3f B, Vec3f C, Vec3f P)
    {       //<1.10ms

        Vec3f[] intervals = new Vec3f[2];
        intervals[0]=new Vec3f(C.x()-A.x(), B.x()-A.x(), A.x()-P.x());
        intervals[1]=new Vec3f(C.y()-A.y(), B.y()-A.y(), A.y()-P.y());
        Vec3f orthogonalVector = VecOperator.cross(intervals[0], intervals[1]);

        if (abs(orthogonalVector.z())>1e-2)
        {
            return new Vec3f(1.0f-((orthogonalVector.x()+orthogonalVector.y())/orthogonalVector.z()),
                    orthogonalVector.y()/ orthogonalVector.z(), orthogonalVector.x()/ orthogonalVector.z());
        }
        //not returning this
        return new Vec3f(-1.f,1.f,1.f);
    }
    public float objectAmp = 120;
    public void drawTriangle (Vec3f[] pts, Vec3f[] texPts,float[] depthBuffer,Color color)
    {   //1900-1950ms
        //takes 3 object/world space points, 3 texture coordinates, the depthBuffer, and a color
        //and draws triangle (after depth testing) in the color specified scaled by interpolated texture color
        //TODO structure : this function is doing too much and should be broken up
        //mapping to screen coords
        for (int i = 0; i<3;i++)    //<2ms
        {   //map to screen coordinates first
            float[] screenSpaceCoords = new float[]{map(-objectAmp, objectAmp,0,width-1, pts[i].x()).floatValue(),
                    map(-objectAmp,objectAmp,0,height-1, pts[i].y()).floatValue(), pts[i].z()};
            //apply perspective projection
            //assume camera distance from origin is d
            int d = 10000;
            float[] perspectiveProjection = new float[]{screenSpaceCoords[0]/(1-screenSpaceCoords[2]/d),
                    screenSpaceCoords[1]/(1-screenSpaceCoords[2]/d), screenSpaceCoords[2]/(1-screenSpaceCoords[2]/d)};
            pts[i] = new Vec3f(perspectiveProjection);
        }
        //find bounding box <0.5ms
        float minX = width-1, minY = height-1;
        float maxX = 0, maxY = 0;
        for (int i =0;i<3;i++)
        {
            minX= max(0, min(minX, pts[i].x()));
            minY= max(0, min(minY, pts[i].y()));
            maxX = min(width-1, max(maxX, pts[i].x()));
            maxY = min(height-1, max(maxY, pts[i].y()));
        }



        //test every pixel in the bounding box and render those which pass. almost all the runtime is here
        Vec3f P;
        int traversalX, traversalY;
        for (traversalX=(int)minX; traversalX<maxX; traversalX++)
        {
            for (traversalY=(int) minY; traversalY<maxY; traversalY++)
            {
                //barycentric test  <650ms per face, <0.66ms per pixel
                P = new Vec3f(traversalX, traversalY, 0.0f);
                Vec3f test = barycentric(pts[0], pts[1], pts[2], P);
                if (test.x()<0||test.y()<0||test.z()<0)
                {
                    continue;
                }
                float sum = 0;
                //interpolate sum between the 3 Z coordinates of the face ~150ms per face
                sum = interpolate(new float[]{pts[0].z(), pts[1].z(), pts[2].z()}, test, sum);
                P.setDepth(sum);
                Color textureColor = new Color(0,0,0);
                if (texPts!=null)
                {
                    //interpolate texture X Y coordinate
                    //TODO bug fixing : the texture only works when I map() the interpolated coordinates, why?
                    float texX = 0, texY = 0;
                    texX = interpolate(new float[]{texPts[0].x(), texPts[1].x(), texPts[2].x()}, test, texX);
                    texY = interpolate(new float[]{texPts[0].y(), texPts[1].y(), texPts[2].y()}, test, texY);
                    Pixel fragmentScreenSpace = new Pixel(map(0,1,0,texWidth, texX).intValue(),
                            map(0,1,0,texHeight, texY).intValue() );
                    textureColor = textureData[fragmentScreenSpace.x()+fragmentScreenSpace.y()*texWidth];
                }
                //depth test ~180ms per face
                if (depthBuffer[(int)(P.x()+P.y()*width)]<P.z())
                {
                    depthBuffer[(int)(P.x()+P.y()*width)]=P.z();
                    //set pixel ~150ms per face. some overhead to check texture availability.
                    setPixel(new Pixel(P.x().intValue(), P.y().intValue()), texPts==null? color : VecOperator.mulColor(textureColor, color));
                }


            }
        }
    }
    int funcCounter=0;
    double sum = 0;
    public void averageRunTime(long time)
    {
        sum += time;
        double average = sum/funcCounter;
        System.out.println("average processing time :  " + (((double) average/1_000_000)*nrModelFaces + "ms"));
    }
    public int[] getColorBuffer() {
        return colorBuffer;
    }
    public BufferedImage getPixelBuffer(){return pixelBuffer;}

    public static  <N extends Number> Double map (N srcMin, N srcMax, N destMin, N destMax, N value)
    {
        //maps a Number from range [srcMin, srcMax] to [destMin, destMAX]

        //it's always safe to cast float or int to double then cast back
        //mapping value to [0, 1]
        double ratio = (value.doubleValue()-srcMin.doubleValue())/(srcMax.doubleValue()-srcMin.doubleValue());
        //TODO error handling: what if value is outside the bounds? what if max-min==0?
        //TODO structure : write map() as a functional interface for stream maps
        return ((destMax.doubleValue() * ratio) + ((1 - ratio) * destMin.doubleValue()));
    }
    public static String colorBufferToString(int[] colorBuffer) {
        StringBuilder bufferData=new StringBuilder();
        for (int i =0; i<colorBuffer.length;i++)
        {
            Color tempColor=new Color(colorBuffer[i]);
            //concatting strings is extremely slow, since each time we concat the string has to be copied, meaning
            //we are copying the same string hundreds of thousands of times.
            //for building strings in for loops, use string builder or string buffer.
            bufferData.append(tempColor.getRed()).append(" ").append(tempColor.getGreen()).append(" ").append(tempColor.getBlue()).append("\n");
        }
        return bufferData.toString();
    }
    //fields

}
