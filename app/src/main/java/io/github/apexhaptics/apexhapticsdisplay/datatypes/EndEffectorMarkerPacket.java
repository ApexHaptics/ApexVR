package io.github.apexhaptics.apexhapticsdisplay.datatypes;

import static android.R.attr.angle;

/**
 * Created by Jaden on 2017-02-15.
 */

public class EndEffectorMarkerPacket extends BluetoothDataPacket {
    public static final String packetString = "MLoc_EEF";
    public static final String eefString = "EEF";
    public float X;
    public float Y;
    public float Z;

    public void setEEPos(float x, float y, float z) {
        X=x;
        Y=y;
        Z=z;
    }

    public String getPacketString() { return packetString; }
}
