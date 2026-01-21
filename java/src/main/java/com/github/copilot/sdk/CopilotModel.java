package com.github.copilot.sdk;

/**
 * Available Copilot models.
 *
 * <p>
 * The actual availability of models depends on your GitHub Copilot
 * subscription.
 */
public enum CopilotModel {
    /** Claude Sonnet 4.5 */
    CLAUDE_SONNET_4_5("claude-sonnet-4.5"),
    /** Claude Haiku 4.5 */
    CLAUDE_HAIKU_4_5("claude-haiku-4.5"),
    /** Claude Opus 4.5 */
    CLAUDE_OPUS_4_5("claude-opus-4.5"),
    /** Claude Sonnet 4 */
    CLAUDE_SONNET_4("claude-sonnet-4"),
    /** GPT-5.2 Codex */
    GPT_5_2_CODEX("gpt-5.2-codex"),
    /** GPT-5.1 Codex Max */
    GPT_5_1_CODEX_MAX("gpt-5.1-codex-max"),
    /** GPT-5.1 Codex */
    GPT_5_1_CODEX("gpt-5.1-codex"),
    /** GPT-5.2 */
    GPT_5_2("gpt-5.2"),
    /** GPT-5.1 */
    GPT_5_1("gpt-5.1"),
    /** GPT-5 */
    GPT_5("gpt-5"),
    /** GPT-5.1 Codex Mini */
    GPT_5_1_CODEX_MINI("gpt-5.1-codex-mini"),
    /** GPT-5 Mini */
    GPT_5_MINI("gpt-5-mini"),
    /** GPT-4.1 */
    GPT_4_1("gpt-4.1"),
    /** Gemini 3 Pro Preview */
    GEMINI_3_PRO_PREVIEW("gemini-3-pro-preview");

    private final String value;

    CopilotModel(String value) {
        this.value = value;
    }

    /**
     * Returns the model identifier string to use with the API.
     *
     * @return the model identifier
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the string representation of the model.
     *
     * @return the model identifier string
     */
    @Override
    public String toString() {
        return value;
    }
}
