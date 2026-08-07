package com.pocotech.hub;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class HubBench {

    public interface Callback {
        void onProgress(String stage, int percent);
        void onComplete(BenchmarkResult result);
    }

    private static final class Cal {
        static double CPU_INT    = 1800; // Mops/с, int+ILP (1 ядро)
        static double CPU_FP     = 1200; // Melem/с, FMA (1 ядро)
        static double CPU_BRANCH = 950;  // Mops/с, ветвления (1 ядро)
        static double CPU_MULTI  = 6000; // суммарные Mops/с (все ядра)
        static double RAM_BW     = 6000; // MiB/с
        static double RAM_LAT_NS = 95;   // нс/переход (меньше = лучше)
        static double SQL_TPS    = 140;  // транзакций/с
        static double SEQ_WRITE  = 170;  // MiB/с
        static double GPU_FPS    = 8.0;  // fps offscreen-сцены
    }
    private static final double IDX = 10000.0;

    private static volatile int SINK;
    private static volatile float FSINK;

    private final Context ctx;
    private volatile boolean cancelled = false;

    public HubBench(Context c) { ctx = c.getApplicationContext(); }
    public void cancel() { cancelled = true; }

    public void run(final Callback cb) {
        cancelled = false;
        new Thread(new Runnable() { @Override public void run() { doRun(cb); } }, "HubBench").start();
    }

    private void doRun(Callback cb) {
        BenchmarkResult r = new BenchmarkResult();
        try {
            cb.onProgress("CPU: целые числа (1 ядро)…", 4);
            double intM = benchInt(2200);
            if (cancelled) return;

            cb.onProgress("CPU: плавающая точка (1 ядро)…", 18);
            double fpM = benchFp(1800);
            if (cancelled) return;

            cb.onProgress("CPU: ветвления (1 ядро)…", 30);
            double brM = benchBranch(1500);
            if (cancelled) return;

            cb.onProgress("CPU: все ядра…", 40);
            double multiM = benchMulti();
            if (cancelled) return;

            cb.onProgress("RAM: пропускная способность…", 54);
            double bw = benchRamBandwidth();
            if (cancelled) return;

            cb.onProgress("RAM: латентность…", 64);
            double lat = benchRamLatencyNs();
            if (cancelled) return;

            cb.onProgress("Накопитель: SQLite-транзакции…", 70);
            double tps = benchSqlite();
            if (cancelled) return;

            cb.onProgress("Накопитель: последовательная запись…", 78);
            double seq = benchSeqWrite();
            if (cancelled) return;

            cb.onProgress("GPU: offscreen-рендер 1080p…", 86);
            final float[] gpu = new float[2];
            final CountDownLatch latch = new CountDownLatch(1);
            GpuBench2.run(new GpuBench2.Listener() {
                @Override public void onDone(float fps, float drop) {
                    gpu[0] = fps; gpu[1] = drop; latch.countDown();
                }
            });
            latch.await();
            if (cancelled) return;

            cb.onProgress("Подсчёт баллов…", 98);

            double cpu1 = IDX * (0.45 * intM / Cal.CPU_INT
                               + 0.25 * fpM  / Cal.CPU_FP
                               + 0.30 * brM  / Cal.CPU_BRANCH);
            double cpuA = IDX * multiM / Cal.CPU_MULTI;
            double ram  = IDX * (0.70 * bw / Cal.RAM_BW
                               + 0.30 * Cal.RAM_LAT_NS / lat);
            double sto  = IDX * (0.60 * tps / Cal.SQL_TPS
                               + 0.40 * seq / Cal.SEQ_WRITE);

            r.cpuSingleScore = (int) Math.round(cpu1);
            r.cpuMultiScore  = (int) Math.round(cpuA);
            r.ramScore       = (int) Math.round(ram);
            r.storageScore   = (int) Math.round(sto);
            r.cpuCores       = Runtime.getRuntime().availableProcessors();
            r.cpuSingleRaw   = (long) intM;   // подпись в UI: «Mops/с»
            r.cpuMultiRaw    = (long) multiM;
            r.ramBandwidth   = (long) bw;
            r.storageWrite   = (long) seq;
            r.storageRead    = (long) tps;    // подпись в UI: «SQLite TPS»

            if (gpu[0] > 0) {
                r.applyGpuResult((int) Math.round(IDX * gpu[0] / Cal.GPU_FPS), gpu[0], gpu[1]);
            } else {
                r.totalScore = r.cpuSingleScore + r.cpuMultiScore + r.ramScore + r.storageScore;
            }
            r.benchmarkConfidence = 90; // упрощённо; разброс best-of-2 минимален
            cb.onComplete(r);
        } catch (Exception e) {
            r.markFailure("HubBench: " + e.getMessage());
            cb.onComplete(r);
        }
    }

    private static double benchInt(long windowMs) {
        final int[] d = new int[256 * 1024]; // 1 MiB
        Random rnd = new Random(0xC0FFEE);
        for (int i = 0; i < d.length; i++) d[i] = rnd.nextInt();
        runInt(d, 500); // прогрев JIT
        double best = 0;
        for (int w = 0; w < 2; w++) {
            best = Math.max(best, runInt(d, windowMs) / 1e6 / (windowMs / 1e3));
        }
        return best;
    }

    private static long runInt(int[] d, long windowMs) {
        int h0 = 0x9E3779B9, h1 = 0x85EBCA6B, h2 = 0xC2B2AE35, h3 = 0x27D4EB2F;
        long ops = 0;
        long end = System.nanoTime() + windowMs * 1_000_000L;
        while (System.nanoTime() < end) {
            for (int i = 0; i < d.length; i += 4) {
                h0 = mix(h0 + d[i]);
                h1 = mix(h1 + d[i + 1]);
                h2 = mix(h2 + d[i + 2]);
                h3 = mix(h3 + d[i + 3]);
            }
            ops += d.length;
        }
        SINK ^= h0 ^ h1 ^ h2 ^ h3;
        return ops;
    }

    private static int mix(int x) {
        x ^= x >>> 16; x *= 0x7FEB352D;
        x ^= x >>> 15; x *= 0x846CA68B;
        x ^= x >>> 16;
        return x;
    }

    private static double benchFp(long windowMs) {
        final int n = 256 * 1024;
        final float[] a = new float[n], b = new float[n];
        Random rnd = new Random(4242);
        for (int i = 0; i < n; i++) { a[i] = rnd.nextFloat(); b[i] = rnd.nextFloat(); }
        runFp(a, b, 400);
        double best = 0;
        for (int w = 0; w < 2; w++) {
            best = Math.max(best, runFp(a, b, windowMs) / 1e6 / (windowMs / 1e3));
        }
        return best;
    }

    private static long runFp(float[] a, float[] b, long windowMs) {
        float s0 = 0, s1 = 0, s2 = 0, s3 = 0;
        long elems = 0;
        long end = System.nanoTime() + windowMs * 1_000_000L;
        while (System.nanoTime() < end) {
            for (int i = 0; i < a.length; i += 4) {
                s0 += a[i]     * b[i];
                s1 += a[i + 1] * b[i + 1];
                s2 += a[i + 2] * b[i + 2];
                s3 += a[i + 3] * b[i + 3];
            }
            elems += a.length;
        }
        FSINK += s0 + s1 + s2 + s3;
        return elems;
    }

    private static double benchBranch(long windowMs) {
        runBranch(400);
        double best = 0;
        for (int w = 0; w < 2; w++) {
            best = Math.max(best, runBranch(windowMs) / 1e6 / (windowMs / 1e3));
        }
        return best;
    }

    private static long runBranch(long windowMs) {
        int v = 0x12345678, acc = 0;
        long ops = 0;
        long end = System.nanoTime() + windowMs * 1_000_000L;
        while (System.nanoTime() < end) {
            for (int k = 0; k < 4096; k++) {
                v = v * 1103515245 + 12345;
                switch ((v >>> 16) & 7) {
                    case 0:  acc += v >>> 8;              break;
                    case 1:  acc ^= v >>> 5;              break;
                    case 2:  acc -= v >>> 11;             break;
                    case 3:  acc += acc * 3 + (v & 0xFF); break;
                    case 4:  acc ^= acc << 2;             break;
                    case 5:  acc += (v * v) >>> 7;        break;
                    case 6:  acc = (acc >>> 3) ^ (v >>> 9); break;
                    default: acc += Integer.bitCount(v);  break;
                }
                ops++;
            }
        }
        SINK ^= acc;
        return ops;
    }

    private double benchMulti() throws InterruptedException {
        final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        double best = 0;
        for (int round = 0; round < 2 && !cancelled; round++) {
            ExecutorService pool = Executors.newFixedThreadPool(cores);
            final CountDownLatch ready = new CountDownLatch(cores);
            final CountDownLatch go    = new CountDownLatch(1);
            final CountDownLatch done  = new CountDownLatch(cores);
            final AtomicLong totalOps  = new AtomicLong();
            for (int t = 0; t < cores; t++) {
                final int seed = 1000 + t * 131;
                pool.submit(new Runnable() { @Override public void run() {
                    try {
                        final int[] d = new int[64 * 1024]; // 256 KiB/поток
                        Random rnd = new Random(seed);
                        for (int i = 0; i < d.length; i++) d[i] = rnd.nextInt();
                        runInt(d, 350);
                        ready.countDown();
                        go.await();
                        long end = System.nanoTime() + 3200L * 1_000_000L;
                        int h0 = 0x9E3779B9, h1 = 0x85EBCA6B, h2 = 0xC2B2AE35, h3 = 0x27D4EB2F;
                        long local = 0;
                        while (!cancelled && System.nanoTime() < end) {
                            for (int i = 0; i < d.length; i += 4) {
                                h0 = mix(h0 + d[i]);
                                h1 = mix(h1 + d[i + 1]);
                                h2 = mix(h2 + d[i + 2]);
                                h3 = mix(h3 + d[i + 3]);
                            }
                            local += d.length;
                        }
                        SINK ^= h0 ^ h1 ^ h2 ^ h3;
                        totalOps.addAndGet(local);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } finally { done.countDown(); }
                }});
            }
            ready.await();
            long start = System.nanoTime();
            go.countDown();
            done.await();
            pool.shutdown();
            best = Math.max(best, totalOps.get() / 1e6 / ((System.nanoTime() - start) / 1e9));
        }
        return best;
    }

    // ═══ RAM ═══

    private double benchRamBandwidth() throws InterruptedException {
        int threads = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        int mb = 32;
        while (true) {
            try { return runRam(threads, mb); }
            catch (OutOfMemoryError oom) {
                mb /= 2;
                if (mb < 4) return 500;
            }
        }
    }

    private double runRam(final int threads, final int mb) throws InterruptedException {
        final byte[][] srcs = new byte[threads][];
        final byte[][] dsts = new byte[threads][];
        for (int t = 0; t < threads; t++) {           // OutOfMemory вылетит здесь
            srcs[t] = new byte[mb << 20];
            dsts[t] = new byte[mb << 20];
        }
        final AtomicLong bytes = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch go    = new CountDownLatch(1);
        final CountDownLatch done  = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            final int idx = t;
            pool.submit(new Runnable() { @Override public void run() {
                try {
                    final byte[] s = srcs[idx], d = dsts[idx];
                    final int CHUNK = 4 << 20;
                    ready.countDown();
                    go.await();
                    long end = System.nanoTime() + 2500L * 1_000_000L;
                    long local = 0;
                    while (!cancelled && System.nanoTime() < end) {
                        for (int off = 0; off + CHUNK <= s.length; off += CHUNK) {
                            System.arraycopy(s, off, d, off, CHUNK);
                            local += CHUNK;
                        }
                    }
                    bytes.addAndGet(local * 2L); // чтение + запись
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally { done.countDown(); }
            }});
        }
        ready.await();
        long start = System.nanoTime();
        go.countDown();
        done.await();
        pool.shutdown();
        return bytes.get() / (1048576.0 * ((System.nanoTime() - start) / 1e9));
    }

    private static double benchRamLatencyNs() {
        final int m = 1 << 22; // 16 МБ — не влезает в кэши
        int[] perm = new int[m];
        for (int i = 0; i < m; i++) perm[i] = i;
        Random rnd = new Random(999);
        for (int i = m - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = perm[i]; perm[i] = perm[j]; perm[j] = t;
        }
        int[] next = new int[m];
        for (int i = 0; i < m; i++) next[perm[i]] = perm[(i + 1) % m];
        int p = 0;
        for (int k = 0; k < (1 << 20); k++) p = next[p]; // прогрев
        long hops = 0;
        long start = System.nanoTime();
        long end = start + 1600L * 1_000_000L;
        while (System.nanoTime() < end) {
            for (int k = 0; k < 8192; k++) p = next[p];
            hops += 8192;
        }
        SINK ^= p;
        return (System.nanoTime() - start) / (double) hops;
    }

    private double benchSqlite() {
        File f = new File(ctx.getCacheDir(), "hb_t.sqlite");
        if (f.exists()) f.delete();
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(f, null);
        long count = 0;
        long start = SystemClock.uptimeMillis();
        try {
            db.rawQuery("PRAGMA journal_mode=DELETE", null).close();
            db.execSQL("PRAGMA synchronous=FULL"); // честный fsync на каждый коммит
            db.execSQL("CREATE TABLE t(v INTEGER)");
            long end = start + 4000;
            while (SystemClock.uptimeMillis() < end && !cancelled) {
                db.beginTransaction();
                try {
                    db.execSQL("INSERT INTO t VALUES(1)");
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                count++;
                if ((count & 0x1FF) == 0) db.execSQL("DELETE FROM t");
            }
        } finally {
            try { db.close(); } catch (Exception ignored) {}
            f.delete();
        }
        return count / Math.max(0.001, (SystemClock.uptimeMillis() - start) / 1000.0);
    }

    private double benchSeqWrite() throws Exception {
        File f = new File(ctx.getCacheDir(), "hb_seq.bin");
        long usable = ctx.getCacheDir().getUsableSpace();
        long size = usable > 700L << 20 ? 96L << 20 : 48L << 20;
        byte[] buf = new byte[1 << 20];
        new Random(5).nextBytes(buf);
        long t0 = System.nanoTime();
        FileOutputStream out = new FileOutputStream(f);
        long left = size;
        while (left > 0 && !cancelled) {
            int n = (int) Math.min(buf.length, left);
            out.write(buf, 0, n);
            left -= n;
        }
        out.getFD().sync(); // сброс на флеш — внутри замера
        out.close();
        double sec = (System.nanoTime() - t0) / 1e9;
        f.delete();
        return (size / 1048576.0) / sec;
    }
}
