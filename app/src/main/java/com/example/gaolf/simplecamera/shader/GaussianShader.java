package com.example.gaolf.simplecamera.shader;

import static android.opengl.GLES20.*;

/**
 * Created by gaolf on 17/1/10.
 * 二次高斯滤波器，一次横向一次竖向
 */

public class GaussianShader extends BaseShader {

    /**
     * 缓存第一次滤波结果的临时纹理
     */
    private int mTexture;
    private int mTextureFrameBuffer;

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision highp float;\n" +
        "\n" +
        "varying vec2 texCoord;\n" +
        "uniform int textureId;\n" +
        "uniform samplerExternalOES tex;\n" +
        "uniform sampler2D fbTex;\n" +
        "\n" +
        "uniform vec2 direction;\n" +
        "uniform vec2 resolution;\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "  gl_FragColor = vec4(0.0);\n" +
        "\n" +
//        "  if (texCoord.y > 0.5) {\n" +
//        "    if (textureId == 1) {\n" +
//        "      gl_FragColor = texture2D(tex, texCoord);\n" +
//        "    } else {\n" +
//        "      gl_FragColor = texture2D(fbTex, texCoord);\n" +
//        "    }\n" +
//        "    return;\n" +
//        "  }" +
        "  vec2 off1 = vec2(1.411764705882353) * direction;\n" +
        "  vec2 off2 = vec2(3.2941176470588234) * direction;\n" +
        "  vec2 off3 = vec2(5.176470588235294) * direction;\n" +
        "  if (textureId == 1) {\n" +
        "    gl_FragColor += texture2D(tex, texCoord) * 0.1964825501511404;\n" +
        "    gl_FragColor += texture2D(tex, texCoord + (off1 / resolution)) * 0.2969069646728344;\n" +
        "    gl_FragColor += texture2D(tex, texCoord - (off1 / resolution)) * 0.2969069646728344;\n" +
        "    gl_FragColor += texture2D(tex, texCoord + (off2 / resolution)) * 0.09447039785044732;\n" +
        "    gl_FragColor += texture2D(tex, texCoord - (off2 / resolution)) * 0.09447039785044732;\n" +
        "    gl_FragColor += texture2D(tex, texCoord + (off3 / resolution)) * 0.010381362401148057;\n" +
        "    gl_FragColor += texture2D(tex, texCoord - (off3 / resolution)) * 0.010381362401148057;\n" +
        "  } else {\n" +
        "    gl_FragColor += texture2D(fbTex, texCoord) * 0.1964825501511404;\n" +
        "    gl_FragColor += texture2D(fbTex, texCoord + (off1 / resolution)) * 0.2969069646728344;\n" +
        "    gl_FragColor += texture2D(fbTex, texCoord - (off1 / resolution)) * 0.2969069646728344;\n" +
        "    gl_FragColor += texture2D(fbTex, texCoord + (off2 / resolution)) * 0.09447039785044732;\n" +
        "    gl_FragColor += texture2D(fbTex, texCoord - (off2 / resolution)) * 0.09447039785044732;\n" +
        "    gl_FragColor += texture2D(fbTex, texCoord + (off3 / resolution)) * 0.010381362401148057;\n" +
        "    gl_FragColor += texture2D(fbTex, texCoord - (off3 / resolution)) * 0.010381362401148057;\n" +
        "  }\n" +
        "}";


    @Override
    protected String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void onSizeChanged(int width, int height) {
        super.onSizeChanged(width, height);

        int resolutionLoc = glGetUniformLocation(program, "resolution");
        glUniform2f(resolutionLoc, width, height);

        int[] fbs = new int[1];
        glGenFramebuffers(1, fbs, 0);
        mTextureFrameBuffer = fbs[0];

        int textures[] = new int[1];
        glGenTextures(1, textures, 0);
        mTexture = textures[0];

        // 初始化纹理
        glActiveTexture(GL_TEXTURE0 + 1);
        glUniform1i(glGetUniformLocation(program, "fbTex"), 1);
        glBindTexture(GL_TEXTURE_2D, mTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // 绑定纹理到fb
        glBindFramebuffer(GL_FRAMEBUFFER, mTextureFrameBuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTexture, 0);

        // 解除绑定，避免其他地方的bug影响这里
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void draw() {
        int directionLoc = glGetUniformLocation(program, "direction");
        int textureIdLoc = glGetUniformLocation(program, "textureId");
//        // 1. render OES Texture horizontally into texture framebuffer
        glVertexAttribPointer(glGetAttribLocation(program, "inTexCoord"), 2, GL_FLOAT, false, 0,
                Float.SIZE / 8 * (3 * 4));
        glUniform1i(textureIdLoc, 1);
        glUniform2f(directionLoc, 1, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, mTextureFrameBuffer);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureName);
        super.draw();

        // 2. render framebuffer texture vertically into display framebuffer
        // this time, we don't have to rotate the texture to get portrait preview, so rebind texture coordinate
        glVertexAttribPointer(glGetAttribLocation(program, "inTexCoord"), 2, GL_FLOAT, false, 0,
                Float.SIZE / 8 * (3 * 4 + 2 * 4));
        glUniform1i(textureIdLoc, 2);
//        glUniform2f(directionLoc, 0, 1);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glActiveTexture(GL_TEXTURE0 + 1);
        glBindTexture(GL_TEXTURE_2D, mTexture);
        super.draw();
    }
}
