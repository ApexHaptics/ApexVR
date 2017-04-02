package com.example.chris.apexvr;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

/**
 * Created by Chris on 3/19/2017.
 */

public class DataLogger {
    private static final int BUFFER_SIZE = 10000;
    private static final String TAG = "ApexDataLogger";
//    SimpleMatrix[] states, gains;
//    long[] stateTimes;
//    int stateIndex = 0;
//    boolean rapState = false;

    float[][] imu,sticker,skell;
    int imuIndex = 0,stickerIndex = 0,skellIndex = 0;
    long[] imuTimes,stickerTimes,skellTimes;
    boolean rapIMU = false, rapSticker = false, rapSkell = false;

    public DataLogger(){
//        states = new SimpleMatrix[BUFFER_SIZE];
//        gains = new SimpleMatrix[BUFFER_SIZE];
//        stateTimes = new long[BUFFER_SIZE];

        imu = new float[BUFFER_SIZE][16];
        sticker = new float[BUFFER_SIZE][16];
        skell = new float[BUFFER_SIZE][16];
        imuTimes = new long[BUFFER_SIZE];
        stickerTimes = new long[BUFFER_SIZE];
        skellTimes = new long[BUFFER_SIZE];
    }

//    public void logState(SimpleMatrix state, SimpleMatrix gain, long time){
//        states[stateIndex] = state;
//        gains[stateIndex] = gain;
//        stateTimes[stateIndex++] = time;
//
//        if(stateIndex >= BUFFER_SIZE){
//            stateIndex = 0;
//            rapState = true;
//        }
//    }

    public void logSticker(float[] pos, long time){

        sticker[stickerIndex] = pos;

        stickerTimes[stickerIndex++] = time;

        if(stickerIndex >= BUFFER_SIZE){
            stickerIndex = 0;
            rapSticker = true;
        }
    }

    public void logSticker(float[] rotation, float[] pos, long time){
        sticker[stickerIndex] = rotation;
        sticker[stickerIndex][12] = pos[0];
        sticker[stickerIndex][13] = pos[1];
        sticker[stickerIndex][14] = pos[2];

        stickerTimes[stickerIndex++] = time;

        if(stickerIndex >= BUFFER_SIZE){
            stickerIndex = 0;
            rapSticker = true;
        }
    }

    public void logSkell(float[] skell, long time){
        this.skell[skellIndex] = skell;
        skellTimes[skellIndex++] = time;

        if(skellIndex >= BUFFER_SIZE){
            skellIndex = 0;
            rapSkell = true;
        }
    }

    public void logIMU(float[] imu, long time){
        this.imu[imuIndex] = imu;
        imuTimes[imuIndex++] = time;

        if(imuIndex >= BUFFER_SIZE){
            imuIndex = 0;
            rapIMU = true;
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void saveLog() {
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "exteralFile not writable");
            return;
        }
        File docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        if (!docs.exists() && !docs.mkdirs()) {
            Log.e(TAG, "cannot create docs");
            return;
        }

        File logs = new File(docs,"ApexLogs");

        if (!logs.exists() && !logs.mkdirs()) {
            Log.e(TAG, "cannot create dir");
            return;
        }

        File file = new File(logs, "log.txt");
        int index = 0;
        while(file.exists()){
            file = new File(logs, "log" + ++index + ".txt");
        }


        try {
            file.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "cannot create file");
            return;
        }

        /*
        log document format (text):

        #IMU\n
        timestamp(ms):[4x4 transformation column major comma delimited]\n
        #sticker
        timestamp(ms):[4x4 transformation column major OR 3x1 pos Vec comma delimited]\n
        #skell\n
        timestamp(ms):[3x1 pos Vec comma delimited]\n



         */

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)))) {

            writer.write("#IMU\n");
            printArrayToStream(writer,imu,imuTimes,imuIndex,rapIMU);
            writer.write("#sticker\n");
            printArrayToStream(writer,sticker,stickerTimes,stickerIndex,rapSticker);
            writer.write("#skell\n");
            printArrayToStream(writer,skell,skellTimes,skellIndex,rapSkell);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "file not found");
            return;
        } catch (IOException e) {
            Log.e(TAG, "cannot write to file file");
            return;
        }


    }

    private void printArrayToStream(BufferedWriter writer,float[][] data,long[] time, int index, boolean rap) throws IOException {

        if(rap && index > 0){
            for(int i = index; i < BUFFER_SIZE; ++i){
                writer.write(time[i] + ":");
                writer.write(Arrays.toString(data[i]) + '\n');
            }
        }

        for(int i = 0; i < index; ++i){
            writer.write(time[i] + ":");
            writer.write(Arrays.toString(data[i]) + '\n');
        }
    }




}
