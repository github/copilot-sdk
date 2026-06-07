package com.github.copilot.samples.koog

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.message.MessagePart
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

fun main(args: Array<String>): Unit = runBlocking {
    val options = CliOptions.parse(args)
    val workspace = options.workspace.toAbsolutePath().normalize()

    CopilotBridge(timeoutMs = options.timeoutMs).use { bridge ->
        val copilotModel = CopilotModels.model(options.copilotModel)
        val copilotClient = CopilotBackedLLMClient(
            bridge = bridge,
            workingDirectory = workspace,
            timeoutModel = options.copilotModel,
        )

        MultiLLMPromptExecutor(copilotClient).use { executor ->
            val tools = ToolRegistry {
                tools(
                    CopilotToolSet(
                        bridge = bridge,
                        defaultWorkspace = workspace,
                        copilotModel = options.copilotModel,
                    ).asTools(),
                )
            }

            val agent = AIAgent<String, String>(
                promptExecutor = executor,
                llmModel = copilotModel,
                systemPrompt = """
                    You are a Koog agent powered by GitHub Copilot SDK.
                    For repository or workspace analysis, call ask_copilot once, then summarize the result.
                    Keep the final answer concise and do not modify files.
                """.trimIndent(),
                toolRegistry = tools,
                strategy = functionalStrategy {
                    var response = requestLLM(it)

                    while (getToolCalls(response).isNotEmpty()) {
                        val results = executeTools(getToolCalls(response), parallelTools = false)
                        response = sendToolResults(results)
                    }

                    response.parts.filterIsInstance<MessagePart.Text>().joinToString("\n") { part -> part.text }
                },
            ) {
                install(EventHandler) {
                    onToolCallStarting { context ->
                        println("Koog tool starting: ${context.toolName} ${context.toolArgs}")
                    }
                    onToolCallCompleted { context ->
                        println("Koog tool completed: ${context.toolName}")
                    }
                    onAgentExecutionFailed { context ->
                        println("Koog agent failed: ${context.error.message}")
                    }
                }
            }

            println(agent.run(options.task))
        }
    }
}

private data class CliOptions(
    val workspace: Path,
    val task: String,
    val copilotModel: String,
    val timeoutMs: Long,
) {
    companion object {
        fun parse(args: Array<String>): CliOptions {
            val values = args.toList().parseFlags()
            return CliOptions(
                workspace = Path.of(values["workspace"] ?: "../.."),
                task = values["task"] ?: "Summarize the Java SDK entry points without modifying files.",
                copilotModel = values["copilot-model"] ?: "auto",
                timeoutMs = values["timeout-ms"]?.toLongOrNull() ?: 120_000,
            )
        }
    }
}

private fun List<String>.parseFlags(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var index = 0
    while (index < size) {
        val key = this[index]
        if (!key.startsWith("--")) {
            index++
            continue
        }
        val normalized = key.removePrefix("--")
        val value = getOrNull(index + 1)?.takeUnless { it.startsWith("--") } ?: "true"
        result[normalized] = value
        index += if (value == "true") 1 else 2
    }
    return result
}
