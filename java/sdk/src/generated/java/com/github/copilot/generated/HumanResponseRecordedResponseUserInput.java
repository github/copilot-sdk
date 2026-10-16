/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code user_input} of {@link HumanResponseRecordedResponse}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HumanResponseRecordedResponseUserInput extends HumanResponseRecordedResponse {

    @JsonProperty("responseKind")
    private final String responseKind = "user_input";

    @Override
    public String getResponseKind() { return responseKind; }

    /** Exact question displayed to the user. */
    @JsonProperty("question")
    private String question;

    /** Exact choices displayed to the user, when the request offered choices. */
    @JsonProperty("choices")
    private List<String> choices;

    /** Whether the displayed request allowed a free-form answer. */
    @JsonProperty("allowFreeform")
    private Boolean allowFreeform;

    /** Exact selected or free-form answer submitted by the user. */
    @JsonProperty("answer")
    private String answer;

    /** Whether the answer was typed as free-form text rather than selected from the displayed choices. */
    @JsonProperty("wasFreeform")
    private Boolean wasFreeform;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getChoices() { return choices; }
    public void setChoices(List<String> choices) { this.choices = choices; }

    public Boolean getAllowFreeform() { return allowFreeform; }
    public void setAllowFreeform(Boolean allowFreeform) { this.allowFreeform = allowFreeform; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Boolean getWasFreeform() { return wasFreeform; }
    public void setWasFreeform(Boolean wasFreeform) { this.wasFreeform = wasFreeform; }
}
