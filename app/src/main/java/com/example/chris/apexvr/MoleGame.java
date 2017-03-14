package com.example.chris.apexvr;

import android.opengl.Matrix;

import io.github.apexhaptics.apexhapticsdisplay.datatypes.GameStatePacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.RobotKinPosPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.RobotPosPacket;

/**
 * Created by Chris on 3/12/2017.
 */

public class MoleGame {

    private static final float TABLE_HIGHT = 5.0f;
    private float[] tablelocation;
    private float[] tableRotation;
    private boolean ready = false;
    ApexGraphics graphics;

    public MoleGame(ApexGraphics graphics){
        tablelocation = new float[16];
        Matrix.setIdentityM(tablelocation,0);
        this.graphics = graphics;
    }


    public void upadte(RobotPosPacket robotPosPacket, GameStatePacket gameStatePacket, RobotKinPosPacket robotKinPosPacket){

        if(robotPosPacket != null){
            ready = true;

            tableRotation = robotPosPacket.rotMat;

            Matrix.setIdentityM(tablelocation,0);
            Matrix.translateM(tablelocation,0,tableRotation,0,
                    robotPosPacket.X,robotPosPacket.Y,robotPosPacket.Z);

        }

        if(!ready){
            return;
        }

        if(gameStatePacket != null){
            graphics.createMole(0,MoleColours.values()[gameStatePacket.getData()].getColour());
        }

        if(robotKinPosPacket != null){
            if(graphics.getNumberOfMoles() > 0){
                float[] moleTransform = new float[16];
                float[] moleDisplacement = new float[16];
                Matrix.setIdentityM(moleDisplacement,0);
                Matrix.translateM(moleDisplacement,0,robotKinPosPacket.getX(),TABLE_HIGHT,robotKinPosPacket.getZ());
                Matrix.multiplyMM(moleTransform,0,moleDisplacement,0,tableRotation,0);
                Matrix.scaleM(graphics.getMoleOrientation(0),0,moleTransform,0,
                        1.0f,robotKinPosPacket.getY()-TABLE_HIGHT,1.0f);
            }
        }




    }

    public float[] getTablelocation() {
        return tablelocation;
    }

    private enum MoleColours{

        Red (new float[]{1.0f,0.0f,0.0f});

        private float[] colour;

        MoleColours(float[] colour){
            this.colour = colour;
        }

        public float[] getColour(){
            return colour;
        }
    }
}
