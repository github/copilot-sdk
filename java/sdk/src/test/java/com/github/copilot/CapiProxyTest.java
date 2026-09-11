/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class CapiProxyTest {

    @Test
    void proxyInstancesShareHttpClientResources() throws Exception {
        var field = CapiProxy.class.getDeclaredField("HTTP_CLIENT");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "E2E contexts must not retain one HttpClient selector manager per test class");
    }
}
