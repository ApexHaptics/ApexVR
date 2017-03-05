package com.example.chris.apexvr.apexGL.world;

import android.util.FloatMath;

import com.example.chris.apexvr.apexGL.mesh.ColouredInterleavedMesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

/**
 * Created by Chris on 3/4/2017.
 */

public class GroundCreater {
    private int width;
    private int lastIndex;
    private float[][] grid;
    private float edge;
    private float size;

    private static final float[] GRASS_COLOUR = {0.094625f, 0.063519f, 0.018978f};
    private static final float[] STONE_COLOUR = {0.3164f, 0.3047f, 0.2734f};

    public GroundCreater(float size, int width){

        this.width = width;
        lastIndex = width - 1;
        this.size = size;

        grid = new float[width][width];
        edge = size / lastIndex;
    }

    public ColouredInterleavedMesh getMesh(){
        FloatBuffer vertexes = FloatBuffer.allocate(9*width*width);
        IntBuffer indexes = IntBuffer.allocate(2 * 3 * lastIndex * lastIndex);

        float half = size/2.0f;

        for(int x = 0; x < width; ++x) {
            for (int y = 0; y < width; ++y) {
                vertexes.put(x * edge - half);
                vertexes.put(grid[x][y]);
                vertexes.put(y * edge - half);
                vertexes.put(normal(x,y));
                //vertexes.put(normal(x,y));
                if(gradient(x,y) > 0.5f){
                    vertexes.put(STONE_COLOUR);
                }else{
                    vertexes.put(GRASS_COLOUR);
                }
            }
        }

        for(int x = 0; x < lastIndex; ++x) {
            for (int y = 0; y < lastIndex; ++y) {
                indexes.put(x * width + y);
                indexes.put(x * width + y + 1);
                indexes.put((x+1) * width + y + 1);

                indexes.put((x+1) * width + y + 1);
                indexes.put((x+1) * width + y);
                indexes.put(x * width + y);
            }
        }


        vertexes.rewind();
        indexes.rewind();

        return new ColouredInterleavedMesh(vertexes, indexes);
    }


    public float interpolate(float x, float y){
        float xCLose = (x / size + 0.5f) * lastIndex;
        float yCLose = (y / size + 0.5f) * lastIndex;

        int xLower = clamp((int)xCLose,0,lastIndex);
        int yLower = clamp((int)yCLose,0,lastIndex);

        int xUpper = clamp((int)(xCLose + 1.0f),0,lastIndex);
        int yUpper = clamp((int)(yCLose + 1.0f),0,lastIndex);

        float xScale = (float) (xCLose - Math.floor(xCLose));
        float yScale = (float) (yCLose - Math.floor(yCLose));

        float xHigh = grid[xLower][yUpper] * xScale + grid[xUpper][yUpper] * (1.0f - xScale);
        float xLow = grid[xLower][yLower] * xScale + grid[xUpper][yLower] * (1.0f - xScale);

        return xHigh * yScale + xLow * (1.0f - yScale);
    }

    public float gradient(int x, int y){
        float delta1 = (grid[clamp(x-1,0,lastIndex)][y] - grid[clamp(x+1,0,lastIndex)][y]) / edge;
        float delta2 = (grid[x][clamp(y-1,0,lastIndex)] - grid[x][clamp(y+1,0,lastIndex)]) / edge;

        return (float) Math.sqrt(delta1*delta1 + delta2*delta2);
    }

    public float gradient(int x1, int y1, int x2, int y2){
        float cross = (float) Math.sqrt(2.0f * edge * edge);

        float delta1 = (grid[x1][y1] - grid[x2][y2]) / cross;
        float delta2 = (grid[x2][y1] - grid[x1][y2]) / cross;

        return (float) Math.sqrt(delta1 * delta1 + delta2 * delta2);
    }

    public float[] normal(float x, float y){
        float xCLose = (x / size + 0.5f) * lastIndex;
        float yCLose = (y / size + 0.5f) * lastIndex;

        int xLower = clamp((int)xCLose,0,lastIndex);
        int yLower = clamp((int)yCLose,0,lastIndex);

        int xUpper = clamp((int)(xCLose + 1.0f),0,lastIndex);
        int yUpper = clamp((int)(yCLose + 1.0f),0,lastIndex);

        float xScale = (float) (xCLose - Math.floor(xCLose));
        float yScale = (float) (yCLose - Math.floor(yCLose));

        float[] normal = new float[3];

        float[] normal11 = normal(xLower, yLower);
        float[] normal12 = normal(xLower, yUpper);

        float[] normal21 = normal(xUpper, yLower);
        float[] normal22 = normal(xUpper, yUpper);

        for(int i = 0; i < 3; ++i){
            float high = normal11[i] * xScale + normal12[i] * (1.0f - xScale);
            float low = normal21[i] * xScale + normal22[i] * (1.0f - xScale);

            normal[i] = low * yScale + high * (1.0f - yScale);
        }

        return normal;
    }

    public float[] normal(int x, int y){
        float[] normal = new float[3];

        float delta1 = grid[clamp(x-1,0,lastIndex)][y] - grid[clamp(x+1,0,lastIndex)][y];
        float delta2 = grid[x][clamp(y-1,0,lastIndex)] - grid[x][clamp(y+1,0,lastIndex)];

        normal[0] = delta1;
        normal[1] = edge * edge;
        normal[2] = delta2;

        float scalar = 0;

        for(float comp: normal){
            scalar += comp * comp;
        }

        scalar = (float) Math.sqrt(scalar);

        for(int i = 0; i < 3; ++i){
            normal[i] /= scalar;
        }

        return normal;

    }

    public void perturb(float largest, float smallest){

        int N = 10;
        int B = 10;

        float b = (largest - smallest) / (N - 1);

        Random random = new Random();

        for(int size = N; size > 0; --size){
            float dhm = size * b;

            for(int i =  0; i < B * (N - size - 1); ++i){
                float cx = random.nextFloat();
                float cy = random.nextFloat();
                float k = 20.0f * random.nextFloat() + 20.0f;
                float l = size / N;

                for(int x = 0; x < width; ++x){
                    for(int y = 0; y < width; ++y){
                        float dx = ((float)x)/lastIndex - cx;
                        float dy = ((float)y)/lastIndex - cy;

                        float dist = (float) Math.sqrt(dx*dx + dy*dy);

                        grid[x][y] += dhm / (1.0f + Math.exp(-k * (l - dist)));

                    }
                }
            }
        }
    }

     private float clamp(float x, float min, float max){

        if(x > max)
            return max;

        if(x < min)
            return min;

        return x;
    }

    private int clamp(int x, int min, int max){

        if(x > max)
            return max;

        if(x < min)
            return min;

        return x;
    }




}
