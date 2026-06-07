package com.github.copilot.samples.koog

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

object CopilotModels {
    val Provider = LLMProvider("github-copilot", "GitHub Copilot")

    fun model(id: String): LLModel = LLModel(
        provider = Provider,
        id = id,
        capabilities = listOf(
            LLMCapability.Completion,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
        ),
    )
}
