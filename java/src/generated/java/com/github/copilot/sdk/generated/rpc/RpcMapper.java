/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Package-private holder for the shared {@link com.fasterxml.jackson.databind.ObjectMapper}
 * used by session API classes when merging {@code sessionId} into call parameters.
 * <p>
 * {@link com.fasterxml.jackson.databind.ObjectMapper} is thread-safe and expensive to
 * instantiate, so a single shared instance is used across all generated API classes.
 * The configuration mirrors {@code JsonRpcClient}'s mapper (JavaTimeModule, lenient
 * unknown-property handling, ISO date format, NON_NULL inclusion).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class RpcMapper {

    static final com.fasterxml.jackson.databind.ObjectMapper INSTANCE = createMapper();

    private static com.fasterxml.jackson.databind.ObjectMapper createMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private RpcMapper() {}
}
