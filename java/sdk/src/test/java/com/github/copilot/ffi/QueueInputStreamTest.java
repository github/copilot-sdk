/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class QueueInputStreamTest {

    @Test
    void readReturnsEnqueuedBytesAcrossMultipleChunks() throws Exception {
        QueueInputStream stream = new QueueInputStream();
        stream.enqueue("hello ".getBytes(StandardCharsets.UTF_8));
        stream.enqueue("world".getBytes(StandardCharsets.UTF_8));

        byte[] buffer = new byte[11];
        int first = stream.read(buffer, 0, 6);
        int second = stream.read(buffer, 6, 5);

        assertEquals(6, first);
        assertEquals(5, second);
        assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void readBlocksUntilDataArrives() throws Exception {
        var queue = new ObservedQueue();
        QueueInputStream stream = new QueueInputStream(queue);

        CompletableFuture<Integer> readFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return stream.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            assertTrue(queue.readStarted.await(2, TimeUnit.SECONDS), "Read must reach the empty queue");
            assertFalse(readFuture.isDone(), "Read must remain pending without data");
            stream.enqueue(new byte[]{(byte) 'A'});
            assertEquals((int) 'A', readFuture.get(2, TimeUnit.SECONDS));
        } finally {
            stream.close();
            readFuture.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void closeSignalsEndOfStream() throws Exception {
        QueueInputStream stream = new QueueInputStream();
        stream.enqueue("x".getBytes(StandardCharsets.UTF_8));

        assertEquals('x', stream.read());
        stream.close();
        assertEquals(-1, stream.read());
    }

    @Test
    void closeUnblocksPendingReadWithEof() throws Exception {
        var queue = new ObservedQueue();
        QueueInputStream stream = new QueueInputStream(queue);

        CompletableFuture<Integer> readFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return stream.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            assertTrue(queue.readStarted.await(2, TimeUnit.SECONDS), "Read must reach the empty queue");
            assertFalse(readFuture.isDone(), "Read must remain pending before close");
        } finally {
            stream.close();
        }

        assertEquals(-1, readFuture.get(2, TimeUnit.SECONDS));
        assertTrue(readFuture.isDone());
    }

    private static final class ObservedQueue extends LinkedBlockingQueue<byte[]> {
        private final CountDownLatch readStarted = new CountDownLatch(1);

        @Override
        public byte[] take() throws InterruptedException {
            readStarted.countDown();
            return super.take();
        }
    }
}
