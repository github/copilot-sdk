package com.github.copilot.samples.koog

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.LLMChoice
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.utils.time.KoogClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Path
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CopilotBackedLLMClient(
    private val bridge: CopilotBridge,
    private val workingDirectory: Path,
    private val timeoutModel: String,
    private val clock: KoogClock = KoogClock.System,
) : LLMClient() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun llmProvider(): LLMProvider = CopilotModels.Provider

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant {
        val raw = bridge.ask(
            prompt = buildCopilotPrompt(prompt, tools),
            workingDirectory = workingDirectory,
            model = model.id.ifBlank { timeoutModel },
        )

        return parseKoogAssistantMessage(raw, model.id)
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> = flow {
        throw UnsupportedOperationException("Streaming is not implemented in this small Copilot-backed Koog LLM sample.")
    }

    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): LLMChoice = listOf(execute(prompt, model, tools))

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        ModerationResult(isHarmful = false, categories = emptyMap())

    override suspend fun models(): List<LLModel> = listOf(CopilotModels.model(timeoutModel))

    override suspend fun embed(text: String, model: LLModel): List<Double> =
        throw UnsupportedOperationException("Embeddings are not implemented in this sample.")

    override suspend fun embed(inputs: List<String>, model: LLModel): List<List<Double>> =
        throw UnsupportedOperationException("Embeddings are not implemented in this sample.")

    override fun close() {
        // The shared CopilotBridge owns the SDK client lifecycle.
    }

    private fun buildCopilotPrompt(prompt: Prompt, tools: List<ToolDescriptor>): String = buildString {
        appendLine("You are acting as the LLM backend for a Koog agent.")
        appendLine("Return exactly one JSON object and no Markdown.")
        appendLine()
        appendLine("Allowed response shapes:")
        appendLine("""{"type":"text","content":"final answer for the user"}""")
        if (tools.isNotEmpty()) {
            appendLine("""{"type":"tool_call","tool":"tool_name","args":{"argument":"value"}}""")
            appendLine()
            appendLine("Available Koog tools:")
            tools.forEach { tool ->
                appendLine("- ${tool.name}: ${tool.description}")
                tool.requiredParameters.forEach { parameter ->
                    appendLine("  required ${parameter.name}: ${parameter.description}")
                }
                tool.optionalParameters.forEach { parameter ->
                    appendLine("  optional ${parameter.name}: ${parameter.description}")
                }
            }
            appendLine()
            appendLine("Tool-call rules:")
            appendLine("- If you call ask_copilot, set task to the user's actual request.")
            appendLine("- If you call ask_copilot and no narrower workspace is needed, set workspacePath to \".\".")
        }
        appendLine()
        appendLine("If a tool result is already present in the transcript, use it to return a text response.")
        appendLine("For workspace analysis requests, prefer one ask_copilot tool call before finalizing.")
        appendLine()
        appendLine("Transcript:")
        prompt.messages.forEach { message ->
            appendLine("[${message.role}]")
            message.parts.forEach { part -> appendLine(part.renderForCopilot()) }
            appendLine()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun parseKoogAssistantMessage(raw: String, modelId: String): Message.Assistant {
        val parsed = runCatching { json.parseToJsonElement(raw.extractJsonObject()).jsonObject }.getOrNull()
        val meta = ResponseMetaInfo.create(clock, modelId = modelId)

        if (parsed == null) {
            return Message.Assistant(raw, meta)
        }

        return when (parsed["type"]?.jsonPrimitive?.content) {
            "tool_call" -> {
                val tool = parsed["tool"]?.jsonPrimitive?.content.orEmpty()
                val args = parsed["args"] as? JsonObject ?: JsonObject(emptyMap())
                Message.Assistant(
                    MessagePart.Tool.Call(
                        id = "copilot-${Uuid.random()}",
                        tool = tool,
                        args = args,
                    ),
                    meta,
                    finishReason = "tool_calls",
                )
            }

            "text" -> Message.Assistant(
                parsed["content"]?.jsonPrimitive?.content.orEmpty(),
                meta,
                finishReason = "stop",
            )

            else -> Message.Assistant(raw, meta)
        }
    }
}

private fun MessagePart.renderForCopilot(): String = when (this) {
    is MessagePart.Text -> text
    is MessagePart.Attachment -> "[attachment: $source]"
    is MessagePart.Reasoning -> content.joinToString("\n")
    is MessagePart.Tool.Call -> "[tool call] $tool $args"
    is MessagePart.Tool.Result -> "[tool result] $tool ${output}"
}

private fun String.extractJsonObject(): String {
    val trimmed = trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed

    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
}
