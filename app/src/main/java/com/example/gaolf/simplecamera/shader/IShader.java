package com.example.gaolf.simplecamera.shader;

/**
 * Created by gaolf on 17/1/10.
 */

public interface IShader {
    boolean apply();
    void destroy();

    void onSizeChanged(int width, int height);

    int getTextureName();
}
