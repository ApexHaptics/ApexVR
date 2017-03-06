package io.github.apexhaptics.apexhapticsdisplay.datatypes;

/**
 * Created by Jaden on 2017-01-31.
 */

public class Joint {
    /**
     * Taken from the C# Kinect class
     * In the C# code they are assigned the enum value of their ordinal int
     * Summary:
     *      This contains all of the possible joint types.
     */
    public enum JointType
    {
        HipCenter,          // 0
        Spine,              // 1
        ShoulderCenter,     // 2
        Head,               // 3
        ShoulderLeft,       // 4
        ElbowLeft,          // 5
        WristLeft,          // 6
        HandLeft,           // 7
        ShoulderRight,      // 8
        ElbowRight,         // 9
        WristRight,         // 10
        HandRight,          // 11
        HipLeft,            // 12
        KneeLeft,           // 13
        AnkleLeft,          // 14
        FootLeft,           // 15
        HipRight,           // 16
        KneeRight,          // 17
        AnkleRight,         // 18
        FootRight           // 19
    }

    /**
     * Taken from the C# Kinect class
     * In the C# code they are assigned the enum value of their ordinal int
     * Summary:
     *      The tracking state of a specific joint.
     */
    public enum JointTrackingState
    {
        NotTracked,
        Inferred,
        Tracked
    }

    public float X;
    public float Y;
    public float Z;
    public JointTrackingState state = null;

    // If true, this member is
    private boolean inferred;

    public Joint() {
        X=0;
        Y=0;
        Z=0;
    }
    public Joint(float x, float y, float z) {
        setCoords(x,y,z);
    }

    // Setters
    public void setCoords(float x, float y, float z) {
        X=x;
        Y=y;
        Z=z;
    }

    // Getters
    // Gets a float array {x,y,z}
    public float[] getCoordArray() {
        return new float[] { X, Y, Z };
    }
}
