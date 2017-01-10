package com.example.gaolf.simplecamera.shader;

import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by gaolf on 17/1/10.
 *
 * 继承并重写方法以提供特殊滤镜
 * 可以：
 *      提供自己的VERTEX_SHADER、FRAGMENT_SHADER来自定义管线
 *      并且重写bindVertexAttrib来绑定自定义的vertex data和uniform variable
 *
 */

public class BaseShader implements IShader {

    private static final String TAG = "BaseShader";

    private static final String VERTEX_SHADER =
            "attribute vec4 inPosition;\n" +
                    "attribute vec2 inTexCoord;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "\tgl_Position = vec4(inPosition);\n" +
                    "\ttexCoord = inTexCoord;\n" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "varying vec2 texCoord;\n" +
                    "uniform samplerExternalOES tex;\n" +
                    "void main() {\n" +
                    "\tgl_FragColor = texture2D(tex, texCoord);\n" +
                    "}";

    // todo, 为什么给camera设置orientation后，纹理图像还是相机默认的横向的，而不是竖向的？
    // todo, 如果相机默认不是横向，需要根据相机默认方向，更改这里的纹理坐标
    private static final float[] VERTEX_DATA = new float[] {
            -1.0f, 1.0f, 0.0f,      // 左上
            0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 0.0f,
    };

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8d65;

    private static final int IN_POSITION = 0;
    private static final int IN_TEX_COORD = 1;

    protected int program;
    protected int mSurfaceTextureName;


    @Override
    public final boolean apply() {
        compileProgram();
        bindVertexAttrib();
        bindTexture();

        return true;
    }

    @Override
    public void destroy() {

    }

    @Override
    public int getTextureName() {
        return mSurfaceTextureName;
    }


    @Override
    public void onSizeChanged(int width, int height) {

    }

    private void compileProgram() {

        int vShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vShader, getVertexShader());
        glCompileShader(vShader);
        String infoLog = glGetShaderInfoLog(vShader);
        if (!TextUtils.isEmpty(infoLog)) {
            Log.e(TAG, "vShader compile info log: " + infoLog);
        }

        int fShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fShader, getFragmentShader());
        glCompileShader(fShader);
        infoLog = glGetShaderInfoLog(fShader);
        if (!TextUtils.isEmpty(infoLog)) {
            Log.e(TAG, "fShader compile info log: " + infoLog);
        }

        program = glCreateProgram();
        glAttachShader(program, vShader);
        glAttachShader(program, fShader);
        glLinkProgram(program);
        infoLog = glGetProgramInfoLog(program);
        if (!TextUtils.isEmpty(infoLog)) {
            Log.e(TAG, "program link info log: " + infoLog);
        }
        glUseProgram(program);
    }

    protected String getVertexShader() {
        return VERTEX_SHADER;
    }

    protected String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    protected void bindVertexAttrib() {
        int[] attribLocArray = new int[2];
        int[] arrayBuffer = new int[1];

        attribLocArray[IN_POSITION] = glGetAttribLocation(program, "inPosition");
        attribLocArray[IN_TEX_COORD] = glGetAttribLocation(program, "inTexCoord");
        Log.d(TAG, "inPositionLoc: " + attribLocArray[IN_POSITION] +
                ", inTexCoordLoc: " + attribLocArray[IN_TEX_COORD]);

        FloatBuffer vertexDataBuffer = ByteBuffer.allocateDirect(Float.SIZE / 8 * VERTEX_DATA.length)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataBuffer.put(VERTEX_DATA).position(0);
        glGenBuffers(1, arrayBuffer, 0);
        glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer[0]);
        glBufferData(GL_ARRAY_BUFFER, Float.SIZE / 8 * VERTEX_DATA.length, vertexDataBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(attribLocArray[IN_POSITION], 3, GL_FLOAT, false, Float.SIZE / 8 * 5, 0);
        glVertexAttribPointer(attribLocArray[IN_TEX_COORD], 2, GL_FLOAT, false, Float.SIZE / 8 * 5, Float.SIZE / 8 * 3);
        glEnableVertexAttribArray(attribLocArray[IN_POSITION]);
        glEnableVertexAttribArray(attribLocArray[IN_TEX_COORD]);
    }

    private void bindTexture() {
        int texUniLoc = -1;
        int[] textures = new int[1];

        glActiveTexture(GL_TEXTURE0);
        glGenTextures(1, textures, 0);
        mSurfaceTextureName = textures[0];
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
        glGetUniformLocation(program, "tex");
        glUniform1i(texUniLoc, 0);

        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
}
