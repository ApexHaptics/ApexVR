package com.example.chris.apexvr.apexGL.shader;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Chris on 2/20/2017.
 */

public class Texture {
    int[] textures;


    public Texture(Bitmap bitmap){
        textures = new int[1];
        GLES30.glGenTextures(1,textures,0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[0]);

        //GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, bitmap, 0);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public static Texture loadTexture(InputStream stream) throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Rect padding = new Rect();
        final Bitmap image = BitmapFactory.decodeStream(stream,padding,options);

        Texture texture = new Texture(image);

        image.recycle();

        return texture;

    }

    public void use(){

        //GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);

    }
}
