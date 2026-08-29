/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class TestProcess extends Process {

    private final CountDownLatch terminated = new CountDownLatch(1);

    @Override
    public OutputStream getOutputStream() {
        return OutputStream.nullOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return InputStream.nullInputStream();
    }

    @Override
    public InputStream getErrorStream() {
        return InputStream.nullInputStream();
    }

    @Override
    public int waitFor() throws InterruptedException {
        terminated.await();
        return 0;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    @Override
    public int exitValue() {
        if (isAlive()) {
            throw new IllegalThreadStateException("Process has not exited");
        }
        return 0;
    }

    @Override
    public void destroy() {
        terminated.countDown();
    }

    @Override
    public Process destroyForcibly() {
        destroy();
        return this;
    }

    @Override
    public boolean isAlive() {
        return terminated.getCount() > 0;
    }
}
