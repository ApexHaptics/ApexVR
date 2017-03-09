/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.apexhaptics.apexhapticsdisplay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.apexhaptics.apexhapticsdisplay.datatypes.BluetoothDataPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.EndEffectorMarkerPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.HeadPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.Joint;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.JointPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.RobotKinPosPacket;

import static android.util.Log.d;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";

    // Name for the SDP record when creating server socket
    private static final String NAME = "ApexHapticsDisplay";

    // Unique UUID shared with the PC application
    private static final UUID MY_UUID =
            UUID.fromString("2611ba68-84e1-4842-a15e-0bfc7e096686");

    // Member fields
    private final BluetoothAdapter mAdapter;
//    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private ConcurrentMap<String, BluetoothDataPacket> dataPackets = new ConcurrentHashMap<>();

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * //@param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Context context) {
//    public BluetoothService(Context context, Handler handler) {
        mState = STATE_NONE;
//        mHandler = handler;

        // Connect to the bluetooth device
        // Because this is run on startup, the app must run after the computer. This can be changed
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            // Device does not support Bluetooth
            d(TAG, "Bluetooth unsupported");
            return;
        }

        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                boolean foundId = false;
                if(!deviceName.contains("DESKTOP")) continue;
                Log.d(TAG, "Bluetooth Device name: " + deviceName);
                d(TAG, "Bluetooth Device MAC: " + deviceHardwareAddress);
                connect(device); // Temporarily connecting to every device. This can be changed
                break;
            }
        }
    }


    private void handlePackets(List<BluetoothDataPacket> packets) {
        for (BluetoothDataPacket packet:packets) {
            dataPackets.put(packet.getPacketString(),packet);
        }
    }

    public BluetoothDataPacket getPacket(String name){
        BluetoothDataPacket packet = dataPackets.get(name);
        if(packet != null){
            dataPackets.put(name,null);
        }

        return packet;
    }

    public boolean hasPacket(String name){

        return dataPackets.get(name) != null;

    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        d(TAG,"State: " + state);
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Send the name of the connected device back to the UI Activity
//        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putString(Constants.DEVICE_NAME, device.getName());
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);
        d(TAG,"DEVICE_NAME: " + device.getName());

        setState(STATE_CONNECTED);

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
//        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(Constants.TOAST, "Unable to connect device");
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);
        d(TAG,"Unable to connect device");

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
//        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(Constants.TOAST, "Device connection was lost");
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);
        d(TAG,"Device connection was lost");

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            d(TAG, "Socket BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");

        }

        public void cancel() {
            d(TAG, "Socket cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                        MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BufferedReader mmBufferedReader;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmOutStream = tmpOut;
            mmBufferedReader = new BufferedReader(new InputStreamReader(tmpIn));
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            String[] packetData;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Send the obtained bytes to the UI Activity
//                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
//                            .sendToTarget();
                    packetData = mmBufferedReader.readLine().split(",");
                    List<BluetoothDataPacket> packets = null;
                    switch (packetData[0]) {
                        case JointPacket.packetString:
                            packets = parseJointPacket(packetData);
                            break;
                        case HeadPacket.packetString:
                            packets = parseMarkerPacket(packetData);
                            break;
                        case RobotKinPosPacket.packetString:
                            packets = parseRobotKinPosPacket(packetData);
                            break;
                        case "":
                            continue;
                        default:
                            Log.e(TAG, "Unknown packet type");
                            continue;
                    }
                    handlePackets(packets);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothService.this.start();
                    break;
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, "Incorrectly formatted message");
                }
            }
        }

        private ArrayList<BluetoothDataPacket> parseJointPacket(String[] data) {
            JointPacket packet = new JointPacket();
            ArrayList<BluetoothDataPacket> packets = new ArrayList<>();
            packets.add(packet);
            packet.deltaT = Integer.parseInt(data[1]);
            try {
                for (int i = 2; i < data.length; i+=6){
                    if(!data[i].equals(JointPacket.separator)) return packets;

                    packet.addJoint(Joint.JointType.values()[Integer.parseInt(data[i+1])],
                            Joint.JointTrackingState.values()[Integer.parseInt(data[i+2])],
                            Float.parseFloat(data[i+3]),
                            Float.parseFloat(data[i+4]),
                            Float.parseFloat(data[i+5]));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Incorrectly formatted JOINT message");
            }
            return packets;
        }

        private ArrayList<BluetoothDataPacket> parseMarkerPacket(String[] data) {
            ArrayList<BluetoothDataPacket> packets = new ArrayList<>();
            int i = 2;
            try {
                if(data[i].equals(HeadPacket.headString)) {
                    HeadPacket packet = new HeadPacket();
                    packet.deltaT = Integer.parseInt(data[1]);
                    float[] headRotMat = new float[] {
                            Float.parseFloat(data[i+4]), Float.parseFloat(data[i+5]),Float.parseFloat(data[i+6]),0,
                            Float.parseFloat(data[i+7]), Float.parseFloat(data[i+8]),Float.parseFloat(data[i+9]),0,
                            Float.parseFloat(data[i+10]), Float.parseFloat(data[i+11]),Float.parseFloat(data[i+12]),0,
                            0,0,0,1,
                    };
                    packet.setHeadPos(Float.parseFloat(data[i+1]),
                            Float.parseFloat(data[i+2]),
                            Float.parseFloat(data[i+3]),
                            headRotMat);
                    i += 13;
                    packets.add(packet);
                }
                if(data.length > i && data[i].equals(EndEffectorMarkerPacket.eefString)) {
                    EndEffectorMarkerPacket packet = new EndEffectorMarkerPacket();
                    packet.deltaT = Integer.parseInt(data[1]);
                    packet.setEEPos(Float.parseFloat(data[i+1]),
                            Float.parseFloat(data[i+2]),
                            Float.parseFloat(data[i+3]));
                    i += 4;
                    packets.add(packet);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Incorrectly formatted MARKER message");
            }
            return packets;
        }

        private ArrayList<BluetoothDataPacket> parseRobotKinPosPacket(String[] data) {
            RobotKinPosPacket packet = new RobotKinPosPacket();
            ArrayList<BluetoothDataPacket> packets = new ArrayList<>();
            packets.add(packet);
            packet.deltaT = Integer.parseInt(data[1]);
            try {
                packet.setPos(Float.parseFloat(data[2]),
                        Float.parseFloat(data[3]),
                        Float.parseFloat(data[4]));
                packets.add(packet);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Incorrectly formatted kinematic position message");
            }
            return packets;
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
//                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
//                        .sendToTarget();
                d(TAG,"MESSAGE_WRITE");
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}