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
    private float[][] grid;
    private float edge;
    private float size;

    public GroundCreater(float size, int width){
        grid = new float[width][width];
        edge = size / width;
        this.width = width;
        this.size = size;
    }

    public ColouredInterleavedMesh getMesh(){
        FloatBuffer vertexes = FloatBuffer.allocate(9*width*width);
        IntBuffer indexes = IntBuffer.allocate(2* (width - 1) * (width - 1));

        float half = size/2.0f;

        for(int x = 0; x < width; ++x) {
            for (int y = 0; y < width; ++y) {
                vertexes.put(x * edge - half);
                vertexes.put(y * edge - half);
                vertexes.put(y * edge - half);
                vertexes.put(normal(x,y));
                if(gradient(x,y) > 0.6f){

                }
            }
        }

        return new ColouredInterleavedMesh(vertexes, indexes);
    }


    public float interpolate(float x, float y){
        int xUpper = (int)clamp((x / size + 1.0f) * width + 0.5f, 0.0f, size);
        int xLower = (int)clamp((x / size) * width + 0.5f, 0.0f, size);
        int yUpper = (int)clamp((y / size + 1.0f) * width + 0.5f, 0.0f, size);
        int yLower = (int)clamp((y / size) * width, 0.0f, size);

        float xScale = (x % edge) / edge;
        float yScale = (y % edge) / edge;

        float xHigh = grid[xLower][yUpper] * xScale + grid[xUpper][yUpper] * (1.0f - xScale);
        float xLow = grid[xLower][yLower] * xScale + grid[xUpper][yLower] * (1.0f - xScale);

        return xHigh * yScale + xLow * (1.0f - yScale);
    }

    public float gradient(int x, int y){
        float delta1 = (grid[clamp(x-1,0,width)][y] - grid[clamp(x+1,0,width)][y]) / edge;
        float delta2 = (grid[x][clamp(y-1,0,width)] - grid[x][clamp(y+1,0,width)]) / edge;

        return (float) Math.sqrt(delta1*delta1 + delta2*delta2);
    }

    public float gradient(int x1, int y1, int x2, int y2){
        float cross = (float) Math.sqrt(2.0f * edge * edge);

        float delta1 = (grid[x1][y1] - grid[x2][y2]) / cross;
        float delta2 = (grid[x2][y1] - grid[x1][y2]) / cross;

        return (float) Math.sqrt(delta1 * delta1 + delta2 * delta2);
    }

    public float[] normal(int x, int y){
        float[] normal = new float[3];

        float delta1 = grid[clamp(x-1,0,width)][y] - grid[clamp(x+1,0,width)][y];
        float delta2 = grid[x][clamp(y-1,0,width)] - grid[x][clamp(y+1,0,width)];

        normal[0] = delta1;
        normal[1] = edge * edge;
        normal[2] = delta2;

        float scalar = 0;

        for(float comp: normal){
            scalar += comp * comp;
        }

        scalar = (float) Math.sqrt(scalar);

        for(float comp: normal){
            comp /= scalar;
        }

        return normal;

    }

    public void perturb(float largest, float smallest, float highest, float lowest){
        float step = (largest - smallest) / 10.0f;
        float b = (float) (Math.log(highest - lowest) / (largest - smallest));

        Random random = new Random();

        for(float size = largest; size > smallest; size -= step){
            float dhm = highest - (float)Math.exp(largest-size * b);
            for(int i =  0; i < 10; ++i){
                float cx = random.nextFloat();
                float cy = random.nextFloat();
                float h = random.nextFloat() * dhm;
                float k = random.nextFloat() + 0.2f;
                float l = h * (3.0f * random.nextFloat() + 1.0f);

                for(int x = 0; x < width; ++x){
                    for(int y = 0; y < width; ++y){
                        float dx = x/width - cx;
                        float dy = y/width - cy;

                        float dist = (float) Math.sqrt(dx*dx - dy*dy);

                        grid[x][y] += h / (1.0f + Math.exp(-k * (dist - l)));

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
