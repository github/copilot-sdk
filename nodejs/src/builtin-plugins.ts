/**
 * Built-in plugins that ship with the plugin system
 * Users can install these via /plugins install <name>
 */

import type { Plugin, PluginContext } from './plugins.js';

/**
 * Memory Preservation Plugin
 * Preserves important conversation data before context compaction
 * 
 * Usage: /plugins install memory-preservation
 */
export class MemoryPreservationPlugin implements Plugin {
  name = 'memory-preservation';
  description = 'Preserves important conversation data before context compaction';
  private importantData: any[] = [];
  private debug: boolean;

  constructor(options: { debug?: boolean } = {}) {
    this.debug = options.debug || false;
  }

  async onLoad(): Promise<void> {
    if (this.debug) console.log('🧠 MemoryPreservationPlugin loaded');
  }

  async onBeforeSend(context: PluginContext, options: any): Promise<any> {
    // Track important user messages
    if (options.prompt) {
      this.importantData.push({ 
        type: 'user_message',
        content: options.prompt,
        timestamp: new Date().toISOString()
      });
    }
    return options;
  }

  async onCompactionStart(context: PluginContext, data: any): Promise<void> {
    console.log('\n⚠️  Context compaction starting');
    console.log(`   Pre-compaction tokens: ${data.preCompactionTokens}`);
    console.log(`   Messages: ${data.preCompactionMessagesLength}`);
    console.log(`   Preserving ${this.importantData.length} important items...`);
    
    // Save to context data (persists during session)
    context.data.set('preserved_memory', [...this.importantData]);
  }

  async onCompactionComplete(context: PluginContext, data: any): Promise<void> {
    if (data.success) {
      console.log('✅ Compaction complete');
      console.log(`   Tokens saved: ${data.tokensRemoved}`);
      console.log(`   Messages removed: ${data.messagesRemoved}`);
    } else {
      console.error(`❌ Compaction failed: ${data.error}`);
    }
  }

  async onUnload(): Promise<void> {
    if (this.debug) console.log('🧠 MemoryPreservationPlugin unloaded');
  }
}

/**
 * Logger Plugin
 * Simple logging of all interactions
 * 
 * Usage: /plugins install logger
 */
export class LoggerPlugin implements Plugin {
  name = 'logger';
  description = 'Logs all session interactions for debugging';
  private debug: boolean;

  constructor(options: { debug?: boolean } = {}) {
    this.debug = options.debug || false;
  }

  async onLoad(): Promise<void> {
    if (this.debug) console.log('📝 LoggerPlugin loaded');
  }

  async onSessionCreated(context: PluginContext): Promise<void> {
    console.log(`📝 Session created: ${context.session.sessionId}`);
  }

  async onBeforeSend(context: PluginContext, options: any): Promise<any> {
    console.log(`📤 → ${options.prompt?.substring(0, 100)}${options.prompt?.length > 100 ? '...' : ''}`);
    return options;
  }

  async onSessionEvent(context: PluginContext, event: any): Promise<any> {
    if (this.debug) {
      console.log(`📡 Event: ${event.type || 'unknown'}`);
    }
    return event;
  }

  async onAfterReceive(context: PluginContext, response: any): Promise<any> {
    const content = response?.content || response?.data?.content || 'No content';
    console.log(`📥 ← ${content.substring(0, 100)}${content.length > 100 ? '...' : ''}`);
    return response;
  }

  async onUnload(): Promise<void> {
    if (this.debug) console.log('📝 LoggerPlugin unloaded');
  }
}

/**
 * Analytics Plugin
 * Track usage statistics
 * 
 * Usage: /plugins install analytics
 */
export class AnalyticsPlugin implements Plugin {
  name = 'analytics';
  description = 'Tracks usage statistics and message counts';
  private messageCount = 0;
  private totalTokens = 0;
  private debug: boolean;

  constructor(options: { debug?: boolean } = {}) {
    this.debug = options.debug || false;
  }

  async onLoad(): Promise<void> {
    if (this.debug) console.log('📊 AnalyticsPlugin loaded');
  }

  async onBeforeSend(): Promise<any> {
    this.messageCount++;
    return arguments[1]; // Return options unchanged
  }

  async onCompactionComplete(context: PluginContext, data: any): Promise<void> {
    if (data.success && data.tokensRemoved) {
      this.totalTokens += data.tokensRemoved;
      console.log(`📊 Stats: ${this.messageCount} messages, ${this.totalTokens} tokens compacted`);
    }
  }

  async onSessionEnd(): Promise<void> {
    console.log(`\n📊 Session Stats:`);
    console.log(`   Messages sent: ${this.messageCount}`);
    console.log(`   Total tokens compacted: ${this.totalTokens}`);
  }

  async onUnload(): Promise<void> {
    if (this.debug) console.log('📊 AnalyticsPlugin unloaded');
  }
}

/**
 * Registry of built-in plugins
 * Used by PluginManager for /plugins available and /plugins install
 */
export const BUILTIN_PLUGINS = new Map<string, () => Plugin | Promise<Plugin>>([
  ['memory-preservation', () => new MemoryPreservationPlugin()],
  ['logger', () => new LoggerPlugin()],
  ['analytics', () => new AnalyticsPlugin()],
  ['anti-compaction', async () => {
    const { AntiCompactionPlugin } = await import('./anti-compaction-plugin.js');
    return new AntiCompactionPlugin();
  }]
]);
