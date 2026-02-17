/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.MessageOptions;

class ConfigCloneTest {

    @Test
    void copilotClientOptionsCloneBasic() {
        CopilotClientOptions original = new CopilotClientOptions();
        original.setCliPath("/usr/local/bin/copilot");
        original.setLogLevel("debug");
        original.setPort(9000);
        
        CopilotClientOptions cloned = original.clone();
        
        assertEquals(original.getCliPath(), cloned.getCliPath());
        assertEquals(original.getLogLevel(), cloned.getLogLevel());
        assertEquals(original.getPort(), cloned.getPort());
    }

    @Test
    void copilotClientOptionsArrayIndependence() {
        CopilotClientOptions original = new CopilotClientOptions();
        String[] args = {"--flag1", "--flag2"};
        original.setCliArgs(args);
        
        CopilotClientOptions cloned = original.clone();
        cloned.getCliArgs()[0] = "--changed";
        
        assertEquals("--flag1", original.getCliArgs()[0]);
        assertEquals("--changed", cloned.getCliArgs()[0]);
    }

    @Test
    void sessionConfigCloneBasic() {
        SessionConfig original = new SessionConfig();
        original.setSessionId("my-session");
        original.setModel("gpt-4o");
        original.setStreaming(true);
        
        SessionConfig cloned = original.clone();
        
        assertEquals(original.getSessionId(), cloned.getSessionId());
        assertEquals(original.getModel(), cloned.getModel());
        assertEquals(original.isStreaming(), cloned.isStreaming());
    }

    @Test
    void sessionConfigListIndependence() {
        SessionConfig original = new SessionConfig();
        List<String> toolList = new ArrayList<>();
        toolList.add("grep");
        toolList.add("bash");
        original.setAvailableTools(toolList);
        
        SessionConfig cloned = original.clone();
        
        List<String> clonedTools = new ArrayList<>(cloned.getAvailableTools());
        clonedTools.add("web");
        cloned.setAvailableTools(clonedTools);
        
        assertEquals(2, original.getAvailableTools().size());
        assertEquals(3, cloned.getAvailableTools().size());
    }

    @Test
    void resumeSessionConfigCloneBasic() {
        ResumeSessionConfig original = new ResumeSessionConfig();
        original.setModel("o1");
        original.setStreaming(false);
        
        ResumeSessionConfig cloned = original.clone();
        
        assertEquals(original.getModel(), cloned.getModel());
        assertEquals(original.isStreaming(), cloned.isStreaming());
    }

    @Test
    void messageOptionsCloneBasic() {
        MessageOptions original = new MessageOptions();
        original.setPrompt("What is 2+2?");
        original.setMode("immediate");
        
        MessageOptions cloned = original.clone();
        
        assertEquals(original.getPrompt(), cloned.getPrompt());
        assertEquals(original.getMode(), cloned.getMode());
    }

    @Test
    void clonePreservesNullFields() {
        CopilotClientOptions opts = new CopilotClientOptions();
        CopilotClientOptions optsClone = opts.clone();
        assertNull(optsClone.getCliPath());
        
        SessionConfig cfg = new SessionConfig();
        SessionConfig cfgClone = cfg.clone();
        assertNull(cfgClone.getModel());
        
        MessageOptions msg = new MessageOptions();
        MessageOptions msgClone = msg.clone();
        assertNull(msgClone.getMode());
    }
}
