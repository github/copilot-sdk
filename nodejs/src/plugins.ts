/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Plugin System Extension (c) Barrer Software
 *--------------------------------------------------------------------------------------------*/

/**
 * Copilot SDK Plugin System
 * Extensibility hooks for the Copilot SDK
 */

import type { CopilotSession } from "./session.js";
import type { MessageOptions, SessionEvent } from "./types.js";

/**
 * Plugin context passed to plugin hooks
 */
export interface PluginContext {
    /** Current session */
    session: CopilotSession;
    /** Plugin-specific data storage */
    data: Map<string, any>;
}

/**
 * Base plugin interface
 */
export interface Plugin {
    /** Unique plugin identifier */
    name: string;

    /** Called when plugin is loaded */
    onLoad?(): Promise<void> | void;

    /** Called when a session is created */
    onSessionCreated?(context: PluginContext): Promise<void> | void;

    /** Called before a message is sent */
    onBeforeSend?(context: PluginContext, options: MessageOptions): Promise<MessageOptions> | MessageOptions;

    /** Called when a session event is received */
    onSessionEvent?(context: PluginContext, event: SessionEvent): Promise<SessionEvent | void> | SessionEvent | void;

    /**
     * Called when context compaction starts
     */
    onCompactionStart?(
        context: PluginContext,
        data: {
            preCompactionTokens?: number;
            preCompactionMessagesLength?: number;
        }
    ): Promise<void> | void;

    /**
     * Called when context compaction completes
     */
    onCompactionComplete?(
        context: PluginContext,
        data: {
            success: boolean;
            error?: string;
            preCompactionTokens?: number;
            postCompactionTokens?: number;
            messagesRemoved?: number;
            tokensRemoved?: number;
            summaryContent?: string;
        }
    ): Promise<void> | void;

    /** Called when session ends */
    onSessionEnd?(context: PluginContext): Promise<void> | void;

    /** Called when plugin is unloaded */
    onUnload?(): Promise<void> | void;
}

/**
 * Plugin manager that handles plugin lifecycle and hooks
 */
export class PluginManager {
    private plugins: Map<string, Plugin> = new Map();
    private pluginData: Map<string, Map<string, any>> = new Map();
    private enabledPlugins: Set<string> = new Set();
    private availablePlugins: Map<string, () => Plugin | Promise<Plugin>>;
    private debug: boolean;

    constructor(plugins: Plugin[] = [], config: { availablePlugins?: Map<string, () => Plugin | Promise<Plugin>>; debug?: boolean } = {}) {
        console.log('🔧 PluginManager: Constructor called with', plugins.length, 'plugins');
        this.debug = config.debug || false;
        this.availablePlugins = config.availablePlugins || new Map();
        
        for (const plugin of plugins) {
            this.registerPlugin(plugin);
        }
    }

    /**
     * Handle /plugins slash commands
     * Returns response message or null if not a plugin command
     */
    async handleCommand(prompt: string): Promise<string | null> {
        const trimmed = prompt.trim();
        
        if (!trimmed.startsWith('/plugins')) {
            return null;
        }

        const parts = trimmed.split(/\s+/);
        const command = parts[1]?.toLowerCase();

        try {
            switch (command) {
                case undefined:
                case 'list':
                    return this.listPlugins();
                
                case 'available':
                    return this.listAvailable();
                
                case 'install':
                    const installName = parts[2];
                    if (!installName) return '❌ Usage: /plugins install <name>';
                    return await this.installPlugin(installName);
                
                case 'enable':
                    const enableName = parts[2];
                    if (!enableName) return '❌ Usage: /plugins enable <name>';
                    return this.enablePlugin(enableName);
                
                case 'disable':
                    const disableName = parts[2];
                    if (!disableName) return '❌ Usage: /plugins disable <name>';
                    return this.disablePlugin(disableName);
                
                case 'uninstall':
                    const uninstallName = parts[2];
                    if (!uninstallName) return '❌ Usage: /plugins uninstall <name>';
                    return await this.uninstallPlugin(uninstallName);
                
                case 'help':
                    return this.showHelp();
                
                default:
                    return `❌ Unknown command: ${command}\nType /plugins help for available commands`;
            }
        } catch (error) {
            return `❌ Error: ${error instanceof Error ? error.message : String(error)}`;
        }
    }

    /**
     * List installed plugins
     */
    private listPlugins(): string {
        if (this.plugins.size === 0) {
            return '📦 No plugins installed\n\nType /plugins available to see available plugins';
        }

        let response = '📦 Installed Plugins:\n\n';
        
        for (const [name, plugin] of this.plugins) {
            const enabled = this.enabledPlugins.has(name);
            const status = enabled ? '✅ enabled' : '⏸️  disabled';
            response += `  ${status} ${name}\n`;
        }

        response += '\nType /plugins help for available commands';
        return response;
    }

    /**
     * List available plugins
     */
    private listAvailable(): string {
        if (this.availablePlugins.size === 0) {
            return '📦 No plugins available in registry';
        }

        let response = '📦 Available Plugins:\n\n';
        
        for (const name of this.availablePlugins.keys()) {
            const installed = this.plugins.has(name);
            const status = installed ? '✅ installed' : '📥 available';
            response += `  ${status} ${name}\n`;
        }

        response += '\nUse /plugins install <name> to install a plugin';
        return response;
    }

    /**
     * Install a plugin at runtime
     */
    private async installPlugin(name: string): Promise<string> {
        if (this.plugins.has(name)) {
            return `⚠️  Plugin "${name}" is already installed`;
        }

        const factory = this.availablePlugins.get(name);
        if (factory) {
            const plugin = await factory();
            this.registerPlugin(plugin);
            this.enabledPlugins.add(name);
            await plugin.onLoad?.();
            return `✅ Installed and enabled plugin: ${name}`;
        }

        return `❌ Plugin "${name}" not found\n\nAvailable: ${Array.from(this.availablePlugins.keys()).join(', ')}`;
    }

    /**
     * Enable a disabled plugin
     */
    private enablePlugin(name: string): string {
        if (!this.plugins.has(name)) {
            return `❌ Plugin "${name}" is not installed`;
        }
        
        if (this.enabledPlugins.has(name)) {
            return `⚠️  Plugin "${name}" is already enabled`;
        }

        this.enabledPlugins.add(name);
        return `✅ Enabled plugin: ${name}`;
    }

    /**
     * Disable an enabled plugin
     */
    private disablePlugin(name: string): string {
        if (!this.plugins.has(name)) {
            return `❌ Plugin "${name}" is not installed`;
        }
        
        if (!this.enabledPlugins.has(name)) {
            return `⚠️  Plugin "${name}" is already disabled`;
        }

        this.enabledPlugins.delete(name);
        return `✅ Disabled plugin: ${name}`;
    }

    /**
     * Uninstall a plugin
     */
    private async uninstallPlugin(name: string): Promise<string> {
        const plugin = this.plugins.get(name);
        if (!plugin) {
            return `❌ Plugin "${name}" not found`;
        }

        await plugin.onUnload?.();
        this.plugins.delete(name);
        this.enabledPlugins.delete(name);
        this.pluginData.delete(name);

        return `✅ Uninstalled plugin: ${name}`;
    }

    /**
     * Show help message
     */
    private showHelp(): string {
        return `📦 Plugin System Commands:

/plugins or /plugins list
  List installed plugins

/plugins available
  Browse available plugins in registry

/plugins install <name>
  Install a plugin at runtime

/plugins enable <name>
  Enable a disabled plugin

/plugins disable <name>
  Disable a plugin temporarily

/plugins uninstall <name>
  Uninstall a plugin

/plugins help
  Show this help message`;
    }

    /**
     * Register a plugin
     */
    registerPlugin(plugin: Plugin): void {
        if (this.plugins.has(plugin.name)) {
            throw new Error(`Plugin ${plugin.name} is already registered`);
        }
        this.plugins.set(plugin.name, plugin);
        this.pluginData.set(plugin.name, new Map());
        
        // Auto-enable plugins when registered
        this.enabledPlugins.add(plugin.name);
    }

    /**
     * Unregister a plugin
     */
    async unregisterPlugin(pluginName: string): Promise<void> {
        const plugin = this.plugins.get(pluginName);
        if (!plugin) return;

        await plugin.onUnload?.();
        this.plugins.delete(pluginName);
        this.pluginData.delete(pluginName);
    }

    /**
     * Get plugin context for a session
     */
    private getContext(session: CopilotSession, pluginName: string): PluginContext {
        return {
            session,
            data: this.pluginData.get(pluginName) || new Map(),
        };
    }

    /**
     * Execute onLoad hooks for all plugins
     */
    async executeOnLoad(): Promise<void> {
        console.log('🔧 PluginManager: executeOnLoad() called for', this.plugins.size, 'plugins');
        for (const [name, plugin] of this.plugins.entries()) {
            if (this.enabledPlugins.has(name)) {
                console.log('🔧 PluginManager: Calling onLoad for plugin:', plugin.name);
                await plugin.onLoad?.();
            }
        }
    }

    /**
     * Execute onSessionCreated hooks
     */
    async executeOnSessionCreated(session: CopilotSession): Promise<void> {
        for (const [pluginName, plugin] of this.plugins.entries()) {
            if (this.enabledPlugins.has(pluginName)) {
                const context = this.getContext(session, pluginName);
                await plugin.onSessionCreated?.(context);
            }
        }
    }

    /**
     * Execute onBeforeSend hooks
     */
    async executeOnBeforeSend(session: CopilotSession, options: MessageOptions): Promise<MessageOptions> {
        // Check if this is a slash command
        if (typeof options.prompt === 'string' && options.prompt.trim().startsWith('/plugins')) {
            const response = await this.handleCommand(options.prompt);
            if (response) {
                // Return modified options that will trigger a response
                return { ...options, prompt: response, _isPluginCommand: true } as any;
            }
        }

        let modifiedOptions = options;

        for (const [pluginName, plugin] of this.plugins.entries()) {
            if (this.enabledPlugins.has(pluginName) && plugin.onBeforeSend) {
                const context = this.getContext(session, pluginName);
                modifiedOptions = (await plugin.onBeforeSend(context, modifiedOptions)) || modifiedOptions;
            }
        }

        return modifiedOptions;
    }

    /**
     * Execute onSessionEvent hooks
     */
    async executeOnSessionEvent(session: CopilotSession, event: SessionEvent): Promise<SessionEvent> {
        let modifiedEvent = event;

        for (const [pluginName, plugin] of this.plugins.entries()) {
            if (this.enabledPlugins.has(pluginName) && plugin.onSessionEvent) {
                const context = this.getContext(session, pluginName);
                const result = await plugin.onSessionEvent(context, modifiedEvent);
                if (result) {
                    modifiedEvent = result;
                }
            }
        }

        return modifiedEvent;
    }

    /**
     * Execute onCompactionStart hooks
     */
    async executeOnCompactionStart(
        session: CopilotSession,
        data: { preCompactionTokens?: number; preCompactionMessagesLength?: number }
    ): Promise<void> {
        for (const [pluginName, plugin] of this.plugins.entries()) {
            if (this.enabledPlugins.has(pluginName) && plugin.onCompactionStart) {
                const context = this.getContext(session, pluginName);
                await plugin.onCompactionStart(context, data);
            }
        }
    }

    /**
     * Execute onCompactionComplete hooks
     */
    async executeOnCompactionComplete(
        session: CopilotSession,
        data: {
            success: boolean;
            error?: string;
            preCompactionTokens?: number;
            postCompactionTokens?: number;
            messagesRemoved?: number;
            tokensRemoved?: number;
            summaryContent?: string;
        }
    ): Promise<void> {
        for (const [pluginName, plugin] of this.plugins.entries()) {
            if (this.enabledPlugins.has(pluginName) && plugin.onCompactionComplete) {
                const context = this.getContext(session, pluginName);
                await plugin.onCompactionComplete(context, data);
            }
        }
    }

    /**
     * Execute onSessionEnd hooks
     */
    async executeOnSessionEnd(session: CopilotSession): Promise<void> {
        for (const [pluginName, plugin] of this.plugins.entries()) {
            if (this.enabledPlugins.has(pluginName)) {
                const context = this.getContext(session, pluginName);
                await plugin.onSessionEnd?.(context);
            }
        }
    }

    /**
     * Get all registered plugins
     */
    getPlugins(): Plugin[] {
        return Array.from(this.plugins.values());
    }
}
