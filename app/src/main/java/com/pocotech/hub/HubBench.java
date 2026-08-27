package com.pocotech.hub;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
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

    private interface SampleTask {
        double run() throws Exception;
    }

    private static final class MetricStats {
        final double median;
        final float variationPct;

        MetricStats(double median, float variationPct) {
            this.median = median;
            this.variationPct = variationPct;
        }
    }

    private static final class Cal {
        static double CPU_INT    = 1800;
        static double CPU_FP     = 1200;
        static double CPU_BRANCH = 950;
        static double CPU_MULTI  = 6000;
        static double RAM_BW     = 6000;
        static double RAM_LAT_NS = 95;
        static double SQL_TPS    = 140;
        static double SEQ_WRITE  = 170;
        static double GPU_FPS    = 8.0;
    }

    private static final double IDX = 10000.0;

    private static volatile int SINK;
    private static volatile float FSINK;

    private final Context ctx;
    private volatile boolean cancelled = false;

    public HubBench(Context c) {
        ctx = c.getApplicationContext();
    }

    public void cancel() {
        cancelled = true;
    }

    public void run(final Callback cb) {
        cancelled = false;
        new Thread(new Runnable() {
            @Override public void run() {
                doRun(cb);
            }
        }, "HubBench").start();
    }

    private void doRun(Callback cb) {
        BenchmarkResult r = new BenchmarkResult();
        try {
            cb.onProgress("CPU: целые числа (6 окон)…", 3);
            MetricStats intStats = sampleMetric(6, 360L, new SampleTask() {
                @Override public double run() {
                    return benchIntOnce(560L);
                }
            });
            if (cancelled) return;

            cb.onProgress("CPU: плавающая точка (6 окон)…", 14);
            MetricStats fpStats = sampleMetric(6, 300L, new SampleTask() {
                @Override public double run() {
                    return benchFpOnce(500L);
                }
            });
            if (cancelled) return;

            cb.onProgress("CPU: ветвления (6 окон)…", 24);
            MetricStats brStats = sampleMetric(6, 250L, new SampleTask() {
                @Override public double run() {
                    return benchBranchOnce(420L);
                }
            });
            if (cancelled) return;

            cb.onProgress("CPU: все ядра (4 окна)…", 34);
            MetricStats multiStats = sampleMetric(4, 0L, new SampleTask() {
                @Override public double run() throws Exception {
                    return benchMultiOnce(1200L, 240L);
                }
            });
            if (cancelled) return;

            cb.onProgress("RAM: пропускная способность (7 окон)…", 46);
            MetricStats bwStats = sampleMetric(7, 0L, new SampleTask() {
                @Override public double run() throws Exception {
                    return benchRamBandwidthWindow();
                }
            });
            if (cancelled) return;

            cb.onProgress("RAM: латентность (7 окон)…", 58);
            MetricStats latStats = sampleMetric(7, 200L, new SampleTask() {
                @Override public double run() {
                    return benchRamLatencyNsWindow(240L);
                }
            });
            if (cancelled) return;

            cb.onProgress("Накопитель: SQLite (5 прогонов)…", 68);
            MetricStats sqliteStats = sampleMetric(5, 0L, new SampleTask() {
                @Override public double run() {
                    return benchSqliteWindow(650L);
                }
            });
            if (cancelled) return;

            cb.onProgress("Накопитель: запись (5 прогонов)…", 78);
            MetricStats seqStats = sampleMetric(5, 0L, new SampleTask() {
                @Override public double run() throws Exception {
                    return benchSeqWriteWindow();
                }
            });
            if (cancelled) return;

            cb.onProgress("GPU: offscreen-рендер 1080p…", 87);
            final float[] gpu = new float[2];
            final CountDownLatch latch = new CountDownLatch(1);
            GpuBench2.run(new GpuBench2.Listener() {
                @Override public void onDone(float fps, float drop) {
                    gpu[0] = fps;
                    gpu[1] = drop;
                    latch.countDown();
                }
            });
            latch.await();
            if (cancelled) return;

            cb.onProgress("Подсчёт медианы и доверия…", 98);

            double intM = intStats.median;
            double fpM = fpStats.median;
            double brM = brStats.median;
            double multiM = multiStats.median;
            double bw = bwStats.median;
            double lat = Math.max(1.0, latStats.median);
            double tps = sqliteStats.median;
            double seq = seqStats.median;

            double cpu1 = IDX * (0.45 * intM / Cal.CPU_INT
                    + 0.25 * fpM / Cal.CPU_FP
                    + 0.30 * brM / Cal.CPU_BRANCH);
            double cpuA = IDX * multiM / Cal.CPU_MULTI;
            double ram = IDX * (0.70 * bw / Cal.RAM_BW
                    + 0.30 * Cal.RAM_LAT_NS / lat);
            double sto = IDX * (0.60 * tps / Cal.SQL_TPS
                    + 0.40 * seq / Cal.SEQ_WRITE);

            r.cpuSingleScore = (int) Math.round(cpu1);
            r.cpuMultiScore = (int) Math.round(cpuA);
            r.ramScore = (int) Math.round(ram);
            r.storageScore = (int) Math.round(sto);
            r.cpuCores = Runtime.getRuntime().availableProcessors();
            r.cpuSingleRaw = Math.round(intM);
            r.cpuMultiRaw = Math.round(multiM);
            r.ramBandwidth = Math.round(bw);
            r.ramLatencyNs = Math.round(lat);
            r.storageWrite = Math.round(seq);
            r.storageRead = Math.round(tps);
            r.cpuSingleVariation = blend(intStats.variationPct, fpStats.variationPct, brStats.variationPct);
            r.cpuMultiVariation = multiStats.variationPct;
            r.ramVariation = blend(bwStats.variationPct, latStats.variationPct);
            r.storageVariation = blend(sqliteStats.variationPct, seqStats.variationPct);
            r.recomputeTotal();

            if (gpu[0] > 0f) {
                r.applyGpuResult((int) Math.round(IDX * gpu[0] / Cal.GPU_FPS), gpu[0], gpu[1]);
            }

            r.benchmarkConfidence = computeConfidence(r);
            cb.onComplete(r);
        } catch (Exception e) {
            r.markFailure("HubBench: " + e.getMessage());
            cb.onComplete(r);
        }
    }

    private MetricStats sampleMetric(int samples, long warmupMs, SampleTask task) throws Exception {
        if (warmupMs > 0) {
            task.run();
        }
        double[] values = new double[samples];
        for (int i = 0; i < samples; i++) {
            if (cancelled) return new MetricStats(0d, 100f);
            values[i] = Math.max(0.0001d, task.run());
        }
        return summarize(values);
    }

    private static MetricStats summarize(double[] values) {
        double[] copy = Arrays.copyOf(values, values.length);
        Arrays.sort(copy);
        double median = copy.length % 2 == 0
                ? (copy[copy.length / 2 - 1] + copy[copy.length / 2]) * 0.5d
                : copy[copy.length / 2];
        double variance = 0d;
        for (double v : values) {
            double diff = v - median;
            variance += diff * diff;
        }
        double stdev = Math.sqrt(variance / Math.max(1, values.length));
        float varPct = (float) Math.min(99d, (stdev / Math.max(1e-9d, median)) * 100d);
        return new MetricStats(median, varPct);
    }

    private static float blend(float... values) {
        if (values == null || values.length == 0) return 0f;
        float sum = 0f;
        for (float v : values) sum += v;
        return sum / values.length;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int computeConfidence(BenchmarkResult r) {
        float penalty = 0f;
        penalty += r.cpuSingleVariation * 1.15f;
        penalty += r.cpuMultiVariation * 0.95f;
        penalty += r.ramVariation * 1.05f;
        penalty += r.storageVariation * 0.85f;
        if (r.gpuTested) {
            penalty += Math.max(0f, r.gpuDropPercent - 10f) * 0.30f;
        } else {
            penalty += 8f;
        }
        return clamp(Math.round(99f - penalty), 48, 99);
    }

    private static double benchIntOnce(long windowMs) {
        final int[] data = new int[256 * 1024];
        Random rnd = new Random(0xC0FFEE);
        for (int i = 0; i < data.length; i++) data[i] = rnd.nextInt();
        long ops = runInt(data, windowMs);
        return ops / 1e6d / (windowMs / 1e3d);
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
        x ^= x >>> 16;
        x *= 0x7FEB352D;
        x ^= x >>> 15;
        x *= 0x846CA68B;
        x ^= x >>> 16;
        return x;
    }

    private static double benchFpOnce(long windowMs) {
        final int n = 256 * 1024;
        final float[] a = new float[n];
        final float[] b = new float[n];
        Random rnd = new Random(4242);
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextFloat();
            b[i] = rnd.nextFloat();
        }
        long elems = runFp(a, b, windowMs);
        return elems / 1e6d / (windowMs / 1e3d);
    }

    private static long runFp(float[] a, float[] b, long windowMs) {
        float s0 = 0f, s1 = 0f, s2 = 0f, s3 = 0f;
        long elems = 0;
        long end = System.nanoTime() + windowMs * 1_000_000L;
        while (System.nanoTime() < end) {
            for (int i = 0; i < a.length; i += 4) {
                s0 += a[i] * b[i];
                s1 += a[i + 1] * b[i + 1];
                s2 += a[i + 2] * b[i + 2];
                s3 += a[i + 3] * b[i + 3];
            }
            elems += a.length;
        }
        FSINK += s0 + s1 + s2 + s3;
        return elems;
    }

    private static double benchBranchOnce(long windowMs) {
        long ops = runBranch(windowMs);
        return ops / 1e6d / (windowMs / 1e3d);
    }

    private static long runBranch(long windowMs) {
        int v = 0x12345678;
        int acc = 0;
        long ops = 0;
        long end = System.nanoTime() + windowMs * 1_000_000L;
        while (System.nanoTime() < end) {
            for (int k = 0; k < 4096; k++) {
                v = v * 1103515245 + 12345;
                switch ((v >>> 16) & 7) {
                    case 0:  acc += v >>> 8; break;
                    case 1:  acc ^= v >>> 5; break;
                    case 2:  acc -= v >>> 11; break;
                    case 3:  acc += acc * 3 + (v & 0xFF); break;
                    case 4:  acc ^= acc << 2; break;
                    case 5:  acc += (v * v) >>> 7; break;
                    case 6:  acc = (acc >>> 3) ^ (v >>> 9); break;
                    default: acc += Integer.bitCount(v); break;
                }
                ops++;
            }
        }
        SINK ^= acc;
        return ops;
    }

    private double benchMultiOnce(final long windowMs, final long warmupMs) throws InterruptedException {
        final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(cores);
        final CountDownLatch ready = new CountDownLatch(cores);
        final CountDownLatch go = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(cores);
        final AtomicLong totalOps = new AtomicLong();

        for (int t = 0; t < cores; t++) {
            final int seed = 1000 + t * 131;
            pool.submit(new Runnable() {
                @Override public void run() {
                    try {
                        final int[] data = new int[64 * 1024];
                        Random rnd = new Random(seed);
                        for (int i = 0; i < data.length; i++) data[i] = rnd.nextInt();
                        runInt(data, warmupMs);
                        ready.countDown();
                        go.await();
                        long end = System.nanoTime() + windowMs * 1_000_000L;
                        int h0 = 0x9E3779B9, h1 = 0x85EBCA6B, h2 = 0xC2B2AE35, h3 = 0x27D4EB2F;
                        long local = 0;
                        while (!cancelled && System.nanoTime() < end) {
                            for (int i = 0; i < data.length; i += 4) {
                                h0 = mix(h0 + data[i]);
                                h1 = mix(h1 + data[i + 1]);
                                h2 = mix(h2 + data[i + 2]);
                                h3 = mix(h3 + data[i + 3]);
                            }
                            local += data.length;
                        }
                        SINK ^= h0 ^ h1 ^ h2 ^ h3;
                        totalOps.addAndGet(local);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }
            });
        }

        ready.await();
        long start = System.nanoTime();
        go.countDown();
        done.await();
        pool.shutdown();
        return totalOps.get() / 1e6d / ((System.nanoTime() - start) / 1e9d);
    }

    private double benchRamBandwidthWindow() throws InterruptedException {
        int threads = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        int mb = 16;
        while (true) {
            try {
                return runRamWindow(threads, mb, 420L);
            } catch (OutOfMemoryError oom) {
                mb /= 2;
                if (mb < 4) return 300d;
            }
        }
    }

    private double runRamWindow(final int threads, final int mb, final long windowMs) throws InterruptedException {
        final byte[][] srcs = new byte[threads][];
        final byte[][] dsts = new byte[threads][];
        for (int t = 0; t < threads; t++) {
            srcs[t] = new byte[mb << 20];
            dsts[t] = new byte[mb << 20];
        }
        final AtomicLong bytes = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch go = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            final int idx = t;
            pool.submit(new Runnable() {
                @Override public void run() {
                    try {
                        final byte[] s = srcs[idx];
                        final byte[] d = dsts[idx];
                        final int chunk = 2 << 20;
                        ready.countDown();
                        go.await();
                        long end = System.nanoTime() + windowMs * 1_000_000L;
                        long local = 0;
                        while (!cancelled && System.nanoTime() < end) {
                            for (int off = 0; off + chunk <= s.length; off += chunk) {
                                System.arraycopy(s, off, d, off, chunk);
                                local += chunk;
                            }
                        }
                        bytes.addAndGet(local * 2L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }
            });
        }
        ready.await();
        long start = System.nanoTime();
        go.countDown();
        done.await();
        pool.shutdown();
        return bytes.get() / (1048576d * ((System.nanoTime() - start) / 1e9d));
    }

    private static double benchRamLatencyNsWindow(long windowMs) {
        final int m = 1 << 21;
        int[] perm = new int[m];
        for (int i = 0; i < m; i++) perm[i] = i;
        Random rnd = new Random(999);
        for (int i = m - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }
        int[] next = new int[m];
        for (int i = 0; i < m; i++) next[perm[i]] = perm[(i + 1) % m];
        int p = 0;
        for (int k = 0; k < (1 << 18); k++) p = next[p];
        long hops = 0;
        long start = System.nanoTime();
        long end = start + windowMs * 1_000_000L;
        while (System.nanoTime() < end) {
            for (int k = 0; k < 8192; k++) p = next[p];
            hops += 8192;
        }
        SINK ^= p;
        return (System.nanoTime() - start) / Math.max(1d, (double) hops);
    }

    private double benchSqliteWindow(long windowMs) {
        File f = new File(ctx.getCacheDir(), "hb_t.sqlite");
        if (f.exists()) f.delete();
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(f, null);
        long count = 0;
        long start = SystemClock.uptimeMillis();
        try {
            db.rawQuery("PRAGMA journal_mode=DELETE", null).close();
            db.execSQL("PRAGMA synchronous=FULL");
            db.execSQL("CREATE TABLE t(v INTEGER)");
            long end = start + windowMs;
            while (SystemClock.uptimeMillis() < end && !cancelled) {
                db.beginTransaction();
                try {
                    db.execSQL("INSERT INTO t VALUES(1)");
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                count++;
                if ((count & 0x1FF) == 0) db.execSQL("DELETE FROM t");
            }
        } finally {
            try { db.close(); } catch (Exception ignored) {}
            f.delete();
        }
        return count / Math.max(0.001d, (SystemClock.uptimeMillis() - start) / 1000d);
    }

    private double benchSeqWriteWindow() throws Exception {
        File f = new File(ctx.getCacheDir(), "hb_seq.bin");
        long usable = ctx.getCacheDir().getUsableSpace();
        long size = usable > (256L << 20) ? (24L << 20) : (12L << 20);
        byte[] buf = new byte[1 << 20];
        new Random(5).nextBytes(buf);
        long left = size;
        long t0 = System.nanoTime();
        FileOutputStream out = new FileOutputStream(f);
        try {
            while (left > 0 && !cancelled) {
                int n = (int) Math.min(buf.length, left);
                out.write(buf, 0, n);
                left -= n;
            }
            out.getFD().sync();
        } finally {
            try { out.close(); } catch (Exception ignored) {}
            f.delete();
        }
        double sec = Math.max(0.001d, (System.nanoTime() - t0) / 1e9d);
        return (size / 1048576d) / sec;
    }
}
