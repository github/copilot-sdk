package com.github.copilot.samples.koog

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.nio.file.Path

@LLMDescription("Tools that delegate analysis-only work to GitHub Copilot SDK")
class CopilotToolSet(
    private val bridge: CopilotBridge,
    private val defaultWorkspace: Path,
    private val copilotModel: String,
) : ToolSet {
    @Tool("ask_copilot")
    @LLMDescription("Ask GitHub Copilot SDK to analyze a workspace and return an answer. This tool must not modify files.")
    fun askCopilot(
        @LLMDescription("Analysis task for Copilot SDK")
        task: String,
        @LLMDescription("Workspace path to analyze. Use the default workspace if unsure.")
        workspacePath: String,
    ): String {
        val workspace = workspacePath
            .takeIf { it.isNotBlank() }
            ?.let { Path.of(it) }
            ?: defaultWorkspace
        val resolvedWorkspace = when {
            workspace.isAbsolute -> workspace
            workspace.normalize().toString() == "." -> defaultWorkspace
            else -> defaultWorkspace.resolve(workspace)
        }.toAbsolutePath().normalize()

        val prompt = """
            You are being called from a Koog proof-of-concept through the GitHub Copilot SDK.

            Analyze the workspace only. Do not modify files, create commits, run destructive commands,
            or ask for additional user input.

            Task:
            $task
        """.trimIndent()

        return bridge.ask(
            prompt = prompt,
            workingDirectory = resolvedWorkspace,
            model = copilotModel,
        )
    }
}
