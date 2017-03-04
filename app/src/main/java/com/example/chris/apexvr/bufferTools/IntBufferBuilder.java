package com.example.chris.apexvr.bufferTools;

import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Chris on 2/15/2017.
 */

public class IntBufferBuilder {
    int cappacity;
    int length;
    int subIndex;

    protected static int MAX_CAP_PER_ARRAY = 1048576; //2^20

    LinkedList<int[]> data = new LinkedList<>();

    public IntBufferBuilder(int size){
        length = 0;
        subIndex = 0;
        cappacity = size;

        data.add(new int[size]);
    }

    public IntBufferBuilder(){
        this(64);
    }

    public void add(int value){

        if(length >= cappacity){
            int newCap =  cappacity >MAX_CAP_PER_ARRAY ? MAX_CAP_PER_ARRAY : cappacity;
            data.add(new int[newCap]);
            cappacity += newCap;
            subIndex = 0;
        }

        data.getLast()[subIndex++] = value;
        ++length;
    }

    public IntBuffer createBuffer(){
        IntBuffer buffer = IntBuffer.allocate(length);

        Iterator<int[]> iterator = data.iterator();

        for(int i = 1; i < data.size(); ++i){
            buffer.put(iterator.next());
        }

        int[] last = iterator.next();
        buffer.put(last,0,length - cappacity + last.length);

        buffer.rewind();

        return buffer;
    }



}
