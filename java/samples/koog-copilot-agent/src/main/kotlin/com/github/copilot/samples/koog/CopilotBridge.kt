package com.github.copilot.samples.koog

import com.github.copilot.CopilotClient
import com.github.copilot.generated.AssistantMessageEvent
import com.github.copilot.generated.ToolExecutionCompleteEvent
import com.github.copilot.generated.ToolExecutionStartEvent
import com.github.copilot.rpc.MessageOptions
import com.github.copilot.rpc.PermissionHandler
import com.github.copilot.rpc.SessionConfig
import java.nio.file.Path
import java.util.concurrent.ExecutionException

class CopilotBridge(
    private val timeoutMs: Long = 120_000,
) : AutoCloseable {
    private val client = CopilotClient()
    private var started = false

    @Synchronized
    fun ask(
        prompt: String,
        workingDirectory: Path,
        model: String,
    ): String {
        ensureStarted()

        val sessionConfig = SessionConfig()
            .setClientName("koog-copilot-agent-sample")
            .setModel(model)
            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
            .setWorkingDirectory(workingDirectory.toAbsolutePath().normalize().toString())
            .setEnableFileHooks(false)
            .setEnableHostGitOperations(false)
            .setEnableSkills(false)

        client.createSession(sessionConfig).get().use { session ->
            session.on(ToolExecutionStartEvent::class.java) { event ->
                println("Copilot SDK tool starting: ${event.data.toolName()}")
            }
            session.on(ToolExecutionCompleteEvent::class.java) { event ->
                println("Copilot SDK tool completed: ${event.data.toolCallId()}")
            }

            val response = session.sendAndWait(MessageOptions().setPrompt(prompt), timeoutMs).get()
            return response.contentOrEmpty()
        }
    }

    @Synchronized
    private fun ensureStarted() {
        if (started) return

        try {
            client.start().get()
            started = true
        } catch (error: ExecutionException) {
            throw IllegalStateException("Failed to start Copilot CLI: ${error.cause?.message ?: error.message}", error)
        }
    }

    override fun close() {
        client.close()
    }
}

private fun AssistantMessageEvent.contentOrEmpty(): String = data?.content().orEmpty()
