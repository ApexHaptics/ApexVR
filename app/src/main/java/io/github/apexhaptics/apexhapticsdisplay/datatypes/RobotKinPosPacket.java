package io.github.apexhaptics.apexhapticsdisplay.datatypes;

/**
 * Created by Jaden on 2017-02-15.
 */

public class RobotKinPosPacket extends BluetoothDataPacket {
    public static final String packetString = "RPos";
    float X;
    float Y;
    float Z;

    @Override
    public String getPacketString() {
        return packetString;
    }

    public void setPos(float x, float y, float z) {
        X = x;
        Y = y;
        Z = z;
    }
}
