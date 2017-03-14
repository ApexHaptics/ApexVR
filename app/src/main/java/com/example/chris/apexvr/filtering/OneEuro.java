package com.example.chris.apexvr.filtering;

/**
 * Created by Chris on 3/14/2017.
 */

public class OneEuro {

    float freq;
    float minCutoff;
    float beta;
    float dCutoff;
    LowPass x;
    LowPass dx;

    boolean ready = false;

    public OneEuro(float freq){
        this(freq, 1.0f, 0.0f, 1.0f);
    }

    public OneEuro(float freq, float mincutoff)  {
        this(freq, mincutoff, 0.0f, 1.0f);
    }

    public OneEuro(float freq, float mincutoff, float beta) {
        this(freq, mincutoff, beta, 1.0f);
    }

    public OneEuro(float freq, float mincutoff, float beta, float dcutoff)  {
        this.freq = freq;
        this.minCutoff = mincutoff;
        this.beta = beta;
        this.dCutoff = dcutoff;
        x = new LowPass(alpha(mincutoff));
        dx = new LowPass(alpha(dcutoff));
    }


    public float filter(float value, float dt){
        if(dt != 0){
            freq = 1.0f / dt;
        }
        return filter(value);
    }




    public float filter(float value){

        if(Float.isNaN(value) || Float.isInfinite(value)){
            return  x.lastValue();
        }

        // estimate the current variation per second
        float dvalue = x.isReady() ? (value - x.lastValue()) * freq : 0.0f;

        dx.setAlpha(alpha(dCutoff));
        float edvalue = dx.filter(dvalue);

        // use it to update the cutoff frequency
        float cutoff = minCutoff + beta * Math.abs(edvalue);
        // filter the given value
        x.setAlpha(alpha(cutoff));

        return x.filter(value);
    }


    private float alpha(float cutoff){
        float te = 1.0f / freq;
        float tau = 1.0f / (2.0f * (float) Math.PI * cutoff);
        return 1.0f / (1.0f + tau / te);
    }


    private class LowPass{
        float y,a;
        float last;
        boolean ready = false;


        public LowPass(float alpha){
            a = alpha;
            last = 0.0f;
        }

        public float filter(float v){

            last = v;

            if(!ready){
                ready = true;
                return y = v;
            }

            return y = a * v + (1.0f - a) * y;
        }

        public void setAlpha(float a) {
            this.a = a;
        }

        public float lastValue(){
            return last;
        }

        public boolean isReady() {
            return ready;
        }
    }
}
