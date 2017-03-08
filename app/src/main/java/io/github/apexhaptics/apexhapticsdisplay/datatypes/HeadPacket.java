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
    public float angle;

    public void setHeadPos(float x, float y, float z, float angle) {
        X=x;
        Y=y;
        Z=z;
        this.angle = angle;
    }

    public String getPacketString() { return packetString; }
}
