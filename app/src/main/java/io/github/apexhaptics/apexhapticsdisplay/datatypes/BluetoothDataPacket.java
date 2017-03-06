package io.github.apexhaptics.apexhapticsdisplay.datatypes;

import java.util.Date;

/**
 * Created by Jaden on 2017-02-15.
 */

public abstract class BluetoothDataPacket {
    public Date createDate;
    public int deltaT; // Time in MS since last. 0 if the first
    BluetoothDataPacket() {
        createDate = new Date();
    }
}
