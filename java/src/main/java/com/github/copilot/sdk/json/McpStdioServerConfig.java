/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a local/stdio MCP (Model Context Protocol) server.
 * <p>
 * Use this to configure an MCP server that is launched as a local subprocess
 * and communicates via standard input/output.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var server = new McpStdioServerConfig().setCommand("npx")
 * 		.setArgs(List.of("-y", "@modelcontextprotocol/server-filesystem", "/path")).setTools(List.of("*"));
 *
 * var config = new SessionConfig().setMcpServers(Map.of("filesystem", server));
 * }</pre>
 *
 * @see McpServerConfig
 * @see SessionConfig#setMcpServers(java.util.Map)
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class McpStdioServerConfig extends McpServerConfig {

    @JsonProperty("type")
    private final String type = "stdio";

    @JsonProperty("command")
    private String command;

    @JsonProperty("args")
    private List<String> args;

    @JsonProperty("env")
    private Map<String, String> env;

    @JsonProperty("workingDirectory")
    private String workingDirectory;

    /**
     * Gets the server type discriminator.
     *
     * @return always {@code "stdio"}
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the command to run the MCP server.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the command to run the MCP server.
     *
     * @param command
     *            the command
     * @return this config for method chaining
     */
    public McpStdioServerConfig setCommand(String command) {
        this.command = command;
        return this;
    }

    /**
     * Gets the arguments to pass to the command.
     *
     * @return the arguments list, or {@code null}
     */
    public List<String> getArgs() {
        return args == null ? null : Collections.unmodifiableList(args);
    }

    /**
     * Sets the arguments to pass to the command.
     *
     * @param args
     *            the arguments list
     * @return this config for method chaining
     */
    public McpStdioServerConfig setArgs(List<String> args) {
        this.args = args;
        return this;
    }

    /**
     * Gets the environment variables to pass to the server.
     *
     * @return the environment variables map, or {@code null}
     */
    public Map<String, String> getEnv() {
        return env == null ? null : Collections.unmodifiableMap(env);
    }

    /**
     * Sets the environment variables to pass to the server.
     *
     * @param env
     *            the environment variables map
     * @return this config for method chaining
     */
    public McpStdioServerConfig setEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    /**
     * Gets the working directory for the server process.
     *
     * @return the working directory path, or {@code null}
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the working directory for the server process.
     *
     * @param workingDirectory
     *            the working directory path
     * @return this config for method chaining
     */
    public McpStdioServerConfig setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    @Override
    public McpStdioServerConfig setTools(List<String> tools) {
        super.setTools(tools);
        return this;
    }

    @Override
    public McpStdioServerConfig setTimeout(Integer timeout) {
        super.setTimeout(timeout);
        return this;
    }
}
