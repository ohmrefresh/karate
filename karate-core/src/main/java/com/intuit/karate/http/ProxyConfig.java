/*
 * The MIT License
 *
 * Copyright 2022 Karate Labs Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.http;

/**
 * Configuration parameters for the Karate proxy server.
 * Centralizes all tunable parameters for easy maintenance and testing.
 *
 * @author pthomas3
 */
public class ProxyConfig {

    // Thread pool sizing
    private static final int DEFAULT_BOSS_THREADS = 1;
    private static final int DEFAULT_WORKER_THREADS_PER_CORE = 2;
    private static final int MIN_WORKER_THREADS = 4;
    private static final int HIGH_CORE_COUNT_THRESHOLD = 16;
    private static final double HIGH_CORE_COUNT_MULTIPLIER = 1.5;

    // Timeout configuration (in seconds)
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_WRITE_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_IDLE_TIMEOUT_SECONDS = 120;

    // HTTP configuration
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 1048576; // 1MB

    private final int bossThreads;
    private final int workerThreads;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int writeTimeoutSeconds;
    private final int idleTimeoutSeconds;
    private final int maxContentLength;

    /**
     * Creates a default configuration with CPU-adaptive thread sizing.
     */
    public ProxyConfig() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a configuration for the specified number of CPU cores.
     *
     * @param cpuCores number of available CPU cores
     */
    public ProxyConfig(int cpuCores) {
        this(DEFAULT_BOSS_THREADS,
             calculateOptimalWorkerThreads(cpuCores),
             DEFAULT_CONNECT_TIMEOUT_SECONDS,
             DEFAULT_READ_TIMEOUT_SECONDS,
             DEFAULT_WRITE_TIMEOUT_SECONDS,
             DEFAULT_IDLE_TIMEOUT_SECONDS,
             DEFAULT_MAX_CONTENT_LENGTH);
    }

    /**
     * Creates a fully customized configuration.
     *
     * @param bossThreads number of boss threads (typically 1)
     * @param workerThreads number of worker threads
     * @param connectTimeoutSeconds connection timeout in seconds
     * @param readTimeoutSeconds read timeout in seconds
     * @param writeTimeoutSeconds write timeout in seconds
     * @param idleTimeoutSeconds idle timeout in seconds
     * @param maxContentLength maximum HTTP content length in bytes
     */
    public ProxyConfig(int bossThreads, int workerThreads,
                      int connectTimeoutSeconds, int readTimeoutSeconds,
                      int writeTimeoutSeconds, int idleTimeoutSeconds,
                      int maxContentLength) {
        this.bossThreads = validatePositive(bossThreads, "bossThreads");
        this.workerThreads = validatePositive(workerThreads, "workerThreads");
        this.connectTimeoutSeconds = validatePositive(connectTimeoutSeconds, "connectTimeoutSeconds");
        this.readTimeoutSeconds = validatePositive(readTimeoutSeconds, "readTimeoutSeconds");
        this.writeTimeoutSeconds = validatePositive(writeTimeoutSeconds, "writeTimeoutSeconds");
        this.idleTimeoutSeconds = validatePositive(idleTimeoutSeconds, "idleTimeoutSeconds");
        this.maxContentLength = validatePositive(maxContentLength, "maxContentLength");
    }

    /**
     * Calculates optimal worker thread count based on CPU cores.
     * Uses I/O-bound workload optimization strategy.
     *
     * Strategy:
     * - 1-2 cores: 4 threads minimum (ensures reasonable concurrency)
     * - 3-16 cores: 2× cores (optimal for I/O-bound workloads)
     * - 17+ cores: 1.5× cores (balanced, avoids thread explosion)
     *
     * @param cpuCores number of available CPU cores
     * @return optimal number of worker threads
     */
    public static int calculateOptimalWorkerThreads(int cpuCores) {
        if (cpuCores <= 0) {
            throw new IllegalArgumentException("cpuCores must be positive, got: " + cpuCores);
        }

        if (cpuCores <= 2) {
            return MIN_WORKER_THREADS;
        } else if (cpuCores <= HIGH_CORE_COUNT_THRESHOLD) {
            return cpuCores * DEFAULT_WORKER_THREADS_PER_CORE;
        } else {
            return (int) Math.ceil(cpuCores * HIGH_CORE_COUNT_MULTIPLIER);
        }
    }

    private static int validatePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, got: " + value);
        }
        return value;
    }

    // Getters

    public int getBossThreads() {
        return bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutSeconds * 1000;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public int getWriteTimeoutSeconds() {
        return writeTimeoutSeconds;
    }

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    @Override
    public String toString() {
        return String.format("ProxyConfig{boss=%d, workers=%d, connect=%ds, read=%ds, write=%ds, idle=%ds, maxContent=%d}",
                bossThreads, workerThreads, connectTimeoutSeconds, readTimeoutSeconds,
                writeTimeoutSeconds, idleTimeoutSeconds, maxContentLength);
    }
}
