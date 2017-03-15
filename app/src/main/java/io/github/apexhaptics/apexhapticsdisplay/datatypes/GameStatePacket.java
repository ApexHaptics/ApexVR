package io.github.apexhaptics.apexhapticsdisplay.datatypes;

/**
 * Created by Chris on 3/7/2017.
 */

public class GameStatePacket extends BluetoothDataPacket {
    public static final String packetString = "GStt";
    /**
     * The data value of the packet
     *      Only useful to indicate impedance in the ImpedanceSet state.
     *      The impedance then holds for the next PositionReached
     */
    int data;

    public int getData() {
        return data;
    }

    @Override
    public String getPacketString() {
        return packetString;
    }

    public void setGameState(int data) {
        this.data = data;
    }
}
