package io.github.apexhaptics.apexhapticsdisplay.datatypes;

import static android.R.attr.x;
import static android.R.attr.y;

/**
 * Created by Jaden on 2017-02-15.
 */

public class Head {
    public float X;
    public float Y;
    public float Z;
    public float angle;

    public Head(float x, float y, float z, float angle) {
        X=x;
        Y=y;
        Z=z;
        this.angle = angle;
    }
}
