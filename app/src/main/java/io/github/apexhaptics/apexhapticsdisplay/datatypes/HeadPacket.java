package io.github.apexhaptics.apexhapticsdisplay.datatypes;

/**
 * Created by Jaden on 2017-02-15.
 */

public class HeadPacket extends BluetoothDataPacket {
    public static final String packetString = "MLoc";
    public static final String headString = "HED";
    public float X;
    public float Y;
    public float Z;
    public float yaw;
    public float pitch;

    public void setHeadPos(float x, float y, float z, float yaw, float pitch) {
        X=x;
        Y=y;
        Z=z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getPacketString() { return packetString; }
}
