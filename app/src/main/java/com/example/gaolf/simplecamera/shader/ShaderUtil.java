package com.example.gaolf.simplecamera.shader;

import android.text.TextUtils;
import android.util.Log;

import static android.content.ContentValues.TAG;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;

/**
 * Created by gaolf on 17/1/10.
 */

public class ShaderUtil {

    public static int loadProgram(String vertexShaderSource,
                                  String fragmentShaderSource) {
        int program;
        int vShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vShader, vertexShaderSource);
        glCompileShader(vShader);
        String infoLog = glGetShaderInfoLog(vShader);
        if (!TextUtils.isEmpty(infoLog)) {
            Log.e(TAG, "vShader compile info log: " + infoLog);
        }

        int fShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fShader, fragmentShaderSource);
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

        return program;
    }

}
