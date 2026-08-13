/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import java.util.concurrent.CompletableFuture;

import com.github.copilot.tool.CopilotTool;

/**
 * Fixture testing different return type patterns.
 */
public class MultiReturnTools {

    @CopilotTool("Returns a string")
    public String stringMethod() {
        return "hello";
    }

    @CopilotTool("Void method")
    public void voidMethod() {
        // side-effect only
    }

    @CopilotTool("Async method")
    public CompletableFuture<String> asyncMethod() {
        return CompletableFuture.completedFuture("async result");
    }
}
