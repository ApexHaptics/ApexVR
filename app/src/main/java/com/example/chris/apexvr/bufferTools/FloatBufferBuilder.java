package com.example.chris.apexvr.bufferTools;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Chris on 2/15/2017.
 */

public class FloatBufferBuilder {
    int cappacity;
    int length;
    int subIndex;

    protected static int MAX_CAP_PER_ARRAY = 1048576; //2^20

    LinkedList<float[]> data = new LinkedList<>();

    public FloatBufferBuilder(int size){
        length = 0;
        subIndex = 0;
        cappacity = size;

        data.add(new float[size]);
    }

    public FloatBufferBuilder(){
        this(64);
    }

    public void add(float value){

        if(length >= cappacity){
            int newCap =  cappacity >MAX_CAP_PER_ARRAY ? MAX_CAP_PER_ARRAY : cappacity;
            data.add(new float[newCap]);
            cappacity += newCap;
            subIndex = 0;
        }

        data.getLast()[subIndex++] = value;
        ++length;
    }

    public FloatBuffer createBuffer(){

        FloatBuffer buffer = FloatBuffer.allocate(length);

        Iterator<float[]> iterator = data.iterator();

        for(int i = 1; i < data.size(); ++i){
            buffer.put(iterator.next());
        }

        float[] last = iterator.next();
        buffer.put(last,0,length - cappacity + last.length);

        buffer.rewind();

        return buffer;
    }



}
