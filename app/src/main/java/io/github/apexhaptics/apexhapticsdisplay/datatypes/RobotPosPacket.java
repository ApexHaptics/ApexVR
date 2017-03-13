package io.github.apexhaptics.apexhapticsdisplay.datatypes;

import static android.R.attr.angle;

/**
 * Created by Jaden on 2017-02-15.
 */

public class RobotPosPacket extends BluetoothDataPacket {
    public static final String packetString = "MLoc_ROB";
    public static final String robString = "ROB";
    public float X;
    public float Y;
    public float Z;
    public float[] rotMat;

    public void setRobotPos(float x, float y, float z, float[] rotMat) {
        X=x;
        Y=y;
        Z=z;
        this.rotMat = rotMat;
    }

    public String getPacketString() { return packetString; }
}
