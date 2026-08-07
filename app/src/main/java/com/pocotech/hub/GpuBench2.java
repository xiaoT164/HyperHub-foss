package com.pocotech.hub;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Off-screen GPU benchmark using a 1080p pbuffer surface and a heavy
 * fill/ALU-bound procedural shader scene.
 */
public final class GpuBench2 {

    public interface Listener { void onDone(float avgFps, float dropPercent); }

    private static final int W = 1920, H = 1080, LAYERS = 10;
    private static final long WARMUP_MS = 2000, TEST_MS = 10000;

    public static void run(final Listener l) {
        new Thread(new Runnable() { @Override public void run() {
            float fps = -1f, drop = -1f;
            try {
                float[] r = measure();
                fps = r[0]; drop = r[1];
            } catch (Throwable ignored) {}
            l.onDone(fps, drop);
        }}, "GpuBench2").start();
    }

    private static float[] measure() {
        EGLDisplay dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] ver = new int[2];
        if (!EGL14.eglInitialize(dpy, ver, 0, ver, 1)) throw new RuntimeException("eglInitialize");
        EGLConfig cfg = pick(dpy);
        EGLContext egl = EGL14.eglCreateContext(dpy, cfg, EGL14.EGL_NO_CONTEXT,
                new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
        EGLSurface surf = EGL14.eglCreatePbufferSurface(dpy, cfg,
                new int[]{EGL14.EGL_WIDTH, W, EGL14.EGL_HEIGHT, H, EGL14.EGL_NONE}, 0);
        if (!EGL14.eglMakeCurrent(dpy, surf, surf, egl)) throw new RuntimeException("makeCurrent");
        try {
            Scene s = new Scene();
            s.init();
            long warmEnd = System.nanoTime() + WARMUP_MS * 1_000_000L;
            while (System.nanoTime() < warmEnd) { s.draw(); GLES20.glFinish(); }

            final int CHUNKS = 4;
            long chunkNs = (TEST_MS / CHUNKS) * 1_000_000L;
            long[] frames = new long[CHUNKS];
            long t0 = System.nanoTime();
            for (int c = 0; c < CHUNKS; c++) {
                long ce = System.nanoTime() + chunkNs;
                while (System.nanoTime() < ce) { s.draw(); GLES20.glFinish(); frames[c]++; }
            }
            double sec = (System.nanoTime() - t0) / 1e9;
            long total = 0;
            for (long f : frames) total += f;
            float avg = (float) (total / sec);
            float first = frames[0] / (float) (chunkNs / 1e9);
            float last  = frames[CHUNKS - 1] / (float) (chunkNs / 1e9);
            float drop  = first > 0 ? Math.max(0, (1f - last / first) * 100f) : 0f;
            s.release();
            return new float[]{avg, drop};
        } finally {
            EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(dpy, surf);
            EGL14.eglDestroyContext(dpy, egl);
            EGL14.eglTerminate(dpy);
        }
    }

    private static EGLConfig pick(EGLDisplay dpy) {
        int[] attrs = {
            EGL14.EGL_SURFACE_TYPE,    EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE,   8, EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE,  8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0, EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
        };
        EGLConfig[] cfgs = new EGLConfig[1];
        int[] num = new int[1];
        if (!EGL14.eglChooseConfig(dpy, attrs, 0, cfgs, 0, 1, num, 0) || num[0] < 1)
            throw new RuntimeException("eglChooseConfig");
        return cfgs[0];
    }

    private static final String VS =
        "attribute vec2 aPos;\n" +
        "void main(){ gl_Position = vec4(aPos, 0.0, 1.0); }\n";

    private static final String FS =
        "precision mediump float;\n" +
        "uniform vec2 uRes; uniform float uTime; uniform float uSeed;\n" +
        "void main(){\n" +
        "  vec2 uv = (gl_FragCoord.xy / uRes) * 2.0 - 1.0;\n" +
        "  uv.x *= uRes.x / uRes.y;\n" +
        "  vec3 col = vec3(0.0);\n" +
        "  for (int i = 0; i < 6; i++) {\n" +
        "    float fi = float(i) + uSeed;\n" +
        "    float t = uTime * 0.22 + fi * 1.71;\n" +
        "    vec3 d = normalize(vec3(uv * (1.15 + 0.18 * fi), 1.35));\n" +
        "    float band = sin(d.x*8.0 + t) * sin(d.y*6.3 - t*1.31) * sin(d.z*10.1 + t*0.73);\n" +
        "    vec3 l = normalize(vec3(sin(t*0.9), cos(t*0.7), 0.62));\n" +
        "    float diff = max(dot(d, l), 0.0);\n" +
        "    float spec = pow(diff, 26.0);\n" +
        "    float fr = pow(1.0 - abs(d.z), 3.0);\n" +
        "    col += vec3(0.09+0.10*band, 0.13+0.11*band+0.09*diff, 0.19+0.15*band)\n" +
        "           + spec*0.38 + fr*0.16;\n" +
        "  }\n" +
        "  gl_FragColor = vec4(col, 1.0);\n" +
        "}\n";

    private static final class Scene {
        int prog, aPos, uRes, uTime, uSeed;
        long startNs;

        void init() {
            prog = build(VS, FS);
            aPos  = GLES20.glGetAttribLocation(prog, "aPos");
            uRes  = GLES20.glGetUniformLocation(prog, "uRes");
            uTime = GLES20.glGetUniformLocation(prog, "uTime");
            uSeed = GLES20.glGetUniformLocation(prog, "uSeed");
            FloatBuffer vb = ByteBuffer.allocateDirect(6 * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            vb.put(new float[]{-1f, -1f, 3f, -1f, -1f, 3f}).position(0);
            GLES20.glViewport(0, 0, W, H);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDisable(GLES20.GL_DITHER);
            GLES20.glUseProgram(prog);
            GLES20.glUniform2f(uRes, W, H);
            GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, vb);
            startNs = System.nanoTime();
        }

        void draw() {
            GLES20.glUniform1f(uTime, (System.nanoTime() - startNs) / 1e9f);
            for (int i = 0; i < LAYERS; i++) {
                GLES20.glUniform1f(uSeed, i * 0.37f);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
            }
        }

        void release() { if (prog != 0) GLES20.glDeleteProgram(prog); }
    }

    private static int build(String vs, String fs) {
        int v = compile(GLES20.GL_VERTEX_SHADER, vs);
        int f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) throw new RuntimeException("link: " + GLES20.glGetProgramInfoLog(p));
        return p;
    }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) throw new RuntimeException("shader: " + GLES20.glGetShaderInfoLog(s));
        return s;
    }
}
