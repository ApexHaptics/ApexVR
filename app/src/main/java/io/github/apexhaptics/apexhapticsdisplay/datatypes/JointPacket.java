package io.github.apexhaptics.apexhapticsdisplay.datatypes;

import java.util.HashMap;
import java.util.Map;

import static android.R.attr.x;

/**
 * Created by Jaden on 2017-02-15.
 */

public class JointPacket extends BluetoothDataPacket {
    public static final String packetString = "JLoc";
    public static final String separator = "JNT";
    private HashMap<Joint.JointType, Joint> jointMap = new HashMap<>();

    public void addJoint(Joint.JointType type, Joint.JointTrackingState state, float x, float y, float z) {
        Joint joint = new Joint(x,y,z);
        joint.state = state;
        jointMap.put(type, joint);
    }

    public Joint getJoint(Joint.JointType type) {
        return jointMap.get(type);
    }
}
