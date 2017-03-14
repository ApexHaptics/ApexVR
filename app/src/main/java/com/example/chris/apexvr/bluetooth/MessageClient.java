package com.example.chris.apexvr.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Chris on 3/12/2017.
 */

public class MessageClient {

    private static final UUID MY_UUID = UUID.fromString("2611ba68-84e1-4842-a15e-0bfc7e096686");
    private static final String TAG = "Apex_Bluetooth";
    private static final String FRIST_HAND = "ARE YOU APEX MASTER?";
    private static final String SECOND_HAND = "I AM APEX MASTER";
    private boolean running;
    private boolean foundApex = false;

    private ConcurrentMap<String, Message> dataPackets;


    public MessageClient(){
        dataPackets = new ConcurrentHashMap<>();

    }

    private void parcLine(String s) {


    }




    public void start(){
        running = true;

        (new Thread(new BluetoothThread())).start();

    }

    private class BluetoothThread implements Runnable{

        private BluetoothAdapter bluetoothAdapter;

        @Override
        public void run() {

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bluetoothAdapter == null){
                throw new RuntimeException("Bluetooth not supported");
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            while(running){
                for (BluetoothDevice device : pairedDevices) {
                    BluetoothSocket socket = null;

                    try {
                        socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    } catch (IOException e) {
                        Log.w(TAG,"Cannot create socket for " + device.getName());
                        continue;
                    }

                    try {
                        socket.connect();
                    } catch (IOException e) {
                        Log.w(TAG,"Cannot connect to " + device.getName());
                        continue;
                    }

                    try(final BufferedReader reader =
                                new BufferedReader(new InputStreamReader(socket.getInputStream()))){
                        try(BufferedWriter writer
                                    = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))){

                            Thread tester = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        while(!Thread.currentThread().isInterrupted()){
                                            writer.write(FRIST_HAND);
                                            foundApex = reader.readLine().equals(SECOND_HAND);

                                            if(foundApex){
                                                MessageClient.this.notifyAll();
                                                return;
                                            }

                                        }
                                    } catch (IOException e) {
                                        return;
                                    }

                                }
                            });

                            tester.start();

                            try {
                                synchronized (MessageClient.this){
                                    MessageClient.this.wait(1000);
                                }
                            } catch (InterruptedException e) {}

                            while(tester.isAlive()){
                                tester.interrupt();
                                try {
                                    Thread.sleep(4);
                                } catch (InterruptedException e) {}
                            }

                            if(!foundApex){
                                continue;
                            }

                            Log.i(TAG,"Connected to " + device.getName());


                            while(running){
                                parcLine(reader.readLine());
                            }

                        }
                    } catch (IOException e) {
                        Log.w(TAG,"Stream failed on:  " + device.getName());
                        continue;
                    }

                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }
        }
    }
}
