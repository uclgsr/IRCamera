package com.topdon.tc001.view;

/**
 * @author: CaiSongL
 * @date: 2023/6/3 14:43
 */
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLSurfaceView extends GLSurfaceView {
    private MyRenderer renderer;

    public MyGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new MyRenderer();
        setRenderer(renderer);
    }

    private class MyRenderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            // initializationOpenGL环境，settings背景色等
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            // 其他initialization操作...
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            // processing窗口大小变化，settings视口和投影矩阵
            GLES20.glViewport(0, 0, width, height);
            // 其他processing...
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            // 渲染场景，绘制point云
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            // 绘制point云...
        }
    }
}

