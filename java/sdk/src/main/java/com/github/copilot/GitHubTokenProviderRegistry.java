/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.copilot.rpc.GitHubTokenProvider;

final class GitHubTokenProviderRegistry {

    private final Map<String, GitHubTokenProvider> providers = new HashMap<>();
    private final Map<String, String> sessionOwners = new HashMap<>();

    synchronized Registration register(GitHubTokenProvider provider) {
        String id = UUID.randomUUID().toString();
        providers.put(id, provider);
        return new Registration(this, id);
    }

    synchronized GitHubTokenProvider get(String registrationId) {
        return providers.get(registrationId);
    }

    private synchronized void claim(String registrationId, String sessionId) {
        String previous = sessionOwners.put(sessionId, registrationId);
        if (previous != null && !previous.equals(registrationId)) {
            providers.remove(previous);
        }
    }

    private synchronized void unregister(String registrationId) {
        providers.remove(registrationId);
        sessionOwners.values().removeIf(registrationId::equals);
    }

    synchronized void retire(String sessionId) {
        String registrationId = sessionOwners.remove(sessionId);
        if (registrationId != null) {
            providers.remove(registrationId);
        }
    }

    synchronized void clear() {
        providers.clear();
        sessionOwners.clear();
    }

    static final class Registration implements AutoCloseable {

        private final GitHubTokenProviderRegistry registry;
        private final String id;
        private boolean closed;

        private Registration(GitHubTokenProviderRegistry registry, String id) {
            this.registry = registry;
            this.id = id;
        }

        String id() {
            return id;
        }

        void claim(String sessionId) {
            registry.claim(id, sessionId);
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                closed = true;
                registry.unregister(id);
            }
        }
    }
}
