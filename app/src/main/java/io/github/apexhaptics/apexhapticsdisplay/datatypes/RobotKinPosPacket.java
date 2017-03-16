package io.github.apexhaptics.apexhapticsdisplay.datatypes;

/**
 * Created by Jaden on 2017-02-15.
 */

/**
 * A class for getting kinematic position updates
 */
public class RobotKinPosPacket extends BluetoothDataPacket {
    public static final String packetString = "RPos";
    private float X;
    private float Y;
    private float Z;

    @Override
    public String getPacketString() {
        return packetString;
    }

    public void setPos(float x, float y, float z) {
        X = x;
        Y = y;
        Z = z;
    }

    public float getZ() {
        return Z;
    }

    public float getY() {

        return Y;
    }

    public float getX() {

        return X;
    }

    public boolean removeMole() {
        return X == 0 && Y == 0 && Z == 0;
    }
}
