package io.github.apexhaptics.apexhapticsdisplay.datatypes;

/**
 * Created by Chris on 3/7/2017.
 */

public class MoleGamePacket extends BluetoothDataPacket {
    public static final String packetString = "MoleGame";
    public Mole[] moles;

    @Override
    public String getPacketString() {
        return packetString;
    }

    //inner types!

    public class Mole{
        float[] pos;
        float hight;
        float[] colour;
    }
}
