package com.example.gaolf.simplecamera.shader;

/**
 * Created by gaolf on 17/1/10.
 */

public interface IShader {
    /**
     * 初始化shader，该方法不会改变context当前状态
     */
    void init();

    /**
     * 使用shader，根据shader修改context当前状态
     */
    void apply();
    void destroy();
    void draw();

    void onSizeChanged(int width, int height);

    int getTextureName();
}
