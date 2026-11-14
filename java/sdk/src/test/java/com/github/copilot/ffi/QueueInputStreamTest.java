/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
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
        QueueInputStream stream = new QueueInputStream();

        CompletableFuture<Integer> readFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return stream.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(100);
        stream.enqueue(new byte[]{(byte) 'A'});

        assertEquals((int) 'A', readFuture.get(2, TimeUnit.SECONDS));
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
        QueueInputStream stream = new QueueInputStream();

        CompletableFuture<Integer> readFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return stream.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(100);
        stream.close();

        assertEquals(-1, readFuture.get(2, TimeUnit.SECONDS));
        assertTrue(readFuture.isDone());
    }
}
