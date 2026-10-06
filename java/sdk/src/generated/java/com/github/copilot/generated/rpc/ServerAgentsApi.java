/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code agents} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerAgentsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerAgentsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Discovers custom agents across user, project, plugin, and remote sources.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AgentsDiscoverResult> discover(AgentsDiscoverParams params) {
        return caller.invoke("agents.discover", params, AgentsDiscoverResult.class);
    }

    /**
     * Returns the canonical directories where a client may create custom agents that the runtime will recognize, including ones that do not exist yet. Project directories become active once created.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AgentsGetDiscoveryPathsResult> getDiscoveryPaths(AgentsGetDiscoveryPathsParams params) {
        return caller.invoke("agents.getDiscoveryPaths", params, AgentsGetDiscoveryPathsResult.class);
    }

    /**
     * Lists the agents this runtime ships, by name. A consumer separating shipped agents from ones the user or a plugin authored should compare against these names rather than against `AgentInfo.source`: an authored agent may carry the `builtin` source while not being one of these, and the runtime treats the two as separate questions. `disableableNames` is the subset a user may turn off, which a client needs to decide whether to offer a toggle. `yamlBasedNames` is the subset backed by a shipped YAML definition, which a client needs before asking the runtime to load one.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentsGetBuiltinsResult> getBuiltins() {
        return caller.invoke("agents.getBuiltins", java.util.Map.of(), AgentsGetBuiltinsResult.class);
    }

    /**
     * Lists the shipped agents a client should offer right now, filtered by the feature flags it passes. `getBuiltins` names every agent the runtime knows about; some of those are gated, so a client rendering a picker wants this narrower list together with the description to show beside each name.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentsGetAvailableBuiltinsResult> getAvailableBuiltins(AgentsGetAvailableBuiltinsParams params) {
        return caller.invoke("agents.getAvailableBuiltins", params, AgentsGetAvailableBuiltinsResult.class);
    }

    /**
     * Loads one shipped agent's YAML definition, for a client that needs what the agent declares rather than only its name. `getBuiltins` reports which names have a definition to load: a name outside its `yamlBasedNames` is special-cased in code and has none. The definition crosses as its own JSON rather than as contract-typed fields, because the runtime parses it with the agent schema's tolerant shape and re-typing it here would drop the keys that shape accepts and this one does not. The projected `__nativeCustomAgent` view the runtime derives is included, so a caller reading the declared model and a caller rendering the agent see the same definition.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentsGetBuiltinDefinitionResult> getBuiltinDefinition(AgentsGetBuiltinDefinitionParams params) {
        return caller.invoke("agents.getBuiltinDefinition", params, AgentsGetBuiltinDefinitionResult.class);
    }

    /**
     * Projects one shipped agent the way a picker lists it, reading only the metadata at the head of the definition file and stopping before the prompt body. `getBuiltinDefinition` answers the whole definition instead, so a client listing every shipped agent should prefer this one: the cost of a listing grows with the number of agents, and the prompt body is the part a listing never shows. The two also differ in shape. This returns the projected custom agent on its own, whereas `getBuiltinDefinition` returns the authored definition with that projection nested under `__nativeCustomAgent`.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentsGetBuiltinListingDefinitionResult> getBuiltinListingDefinition(AgentsGetBuiltinListingDefinitionParams params) {
        return caller.invoke("agents.getBuiltinListingDefinition", params, AgentsGetBuiltinListingDefinitionResult.class);
    }

    /**
     * Resolves the model a custom agent asks for against the models actually available, and answers both the model to switch to and the warning a user should see when the agent's preference cannot be met. A custom agent may name several acceptable models in preference order, so the decision is a match rather than a lookup, and an agent whose preference is unavailable is a normal outcome that produces a warning rather than an error. A host must call this rather than pick the first available name itself, because the preference order and the wording of the warning are what keep one installation's agent selection the same as another's.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentsCustomAgentInitialModelDecisionResult> customAgentInitialModelDecision(AgentsCustomAgentInitialModelDecisionParams params) {
        return caller.invoke("agents.customAgentInitialModelDecision", params, AgentsCustomAgentInitialModelDecisionResult.class);
    }

}
