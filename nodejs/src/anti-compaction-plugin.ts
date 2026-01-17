/**
 * Anti-Compaction Plugin
 * 
 * Prevents automatic context compaction to preserve full conversation history.
 * Addresses GitHub Copilot CLI issue #947.
 * 
 * IMPORTANT: This plugin works by tracking compaction events but cannot
 * prevent compaction at the SDK level (no cancel mechanism exists).
 * Instead, it provides workarounds:
 * 
 * 1. Warning mode: Alerts user when compaction occurs
 * 2. Preservation mode: Saves full conversation history before compaction
 * 3. Token monitoring: Tracks context usage to predict compaction
 * 
 * Usage:
 *   /plugins install anti-compaction
 * 
 * For issue: https://github.com/github/copilot-cli/issues/947
 */

import type { Plugin, PluginContext } from './types.js';

export interface AntiCompactionOptions {
  /** Alert user when compaction occurs */
  warn?: boolean;
  /** Save full conversation history before compaction */
  preserve?: boolean;
  /** Maximum tokens before warning (default: 120000) */
  tokenThreshold?: number;
  /** File path to save preserved history (default: ~/.copilot-history.json) */
  historyPath?: string;
}

export class AntiCompactionPlugin implements Plugin {
  name = 'anti-compaction';
  private options: Required<AntiCompactionOptions>;
  private conversationHistory: any[] = [];
  private tokenCount: number = 0;
  private compactionCount: number = 0;

  constructor(options: AntiCompactionOptions = {}) {
    this.options = {
      warn: options.warn ?? true,
      preserve: options.preserve ?? true,
      tokenThreshold: options.tokenThreshold ?? 120000,
      historyPath: options.historyPath ?? `${process.env.HOME}/.copilot-history.json`
    };
  }

  async onLoad(): Promise<void> {
    console.log('🛡️  AntiCompactionPlugin loaded');
    console.log('   ⚠️  Note: SDK does not support canceling compaction');
    console.log('   💾 Preservation mode:', this.options.preserve ? 'ON' : 'OFF');
    console.log('   📢 Warning mode:', this.options.warn ? 'ON' : 'OFF');
    console.log(`   📊 Token threshold: ${this.options.tokenThreshold.toLocaleString()}`);
  }

  async onBeforeSend(context: PluginContext, options: any): Promise<any> {
    const message = options.message || options;
    
    // Track conversation in memory
    this.conversationHistory.push({
      type: 'user',
      content: message,
      timestamp: new Date().toISOString()
    });

    // Estimate tokens (rough: 1 token ≈ 4 chars)
    const messageStr = typeof message === 'string' ? message : JSON.stringify(message);
    this.tokenCount += Math.ceil(messageStr.length / 4);

    // Warn if approaching threshold
    if (this.tokenCount > this.options.tokenThreshold * 0.8) {
      console.log(`\n⚠️  WARNING: Approaching compaction threshold`);
      console.log(`   Current: ~${this.tokenCount.toLocaleString()} tokens`);
      console.log(`   Threshold: ${this.options.tokenThreshold.toLocaleString()} tokens`);
      console.log(`   Auto-compaction may trigger soon!`);
    }
    
    return options;
  }

  async onAfterReceive(context: PluginContext, response: any): Promise<void> {
    // Track assistant response
    const content = typeof response === 'string' ? response : JSON.stringify(response);
    this.conversationHistory.push({
      type: 'assistant',
      content,
      timestamp: new Date().toISOString()
    });

    this.tokenCount += Math.ceil(content.length / 4);
  }

  async onCompactionStart(context: PluginContext, data: any): Promise<void> {
    this.compactionCount++;

    if (this.options.warn) {
      console.log('\n⚠️  🚨 AUTO-COMPACTION TRIGGERED 🚨');
      console.log(`   This is compaction #${this.compactionCount} in this session`);
      console.log(`   Pre-compaction tokens: ${data.preCompactionTokens || this.tokenCount}`);
      console.log(`   Messages: ${data.preCompactionMessagesLength || this.conversationHistory.length}`);
      console.log('\n   ⛔ NOTE: SDK does not support preventing this compaction');
      console.log('   �� History is being preserved...\n');
    }

    if (this.options.preserve) {
      // Save conversation history to file
      const fs = await import('fs/promises');
      const historyData = {
        savedAt: new Date().toISOString(),
        compactionNumber: this.compactionCount,
        preCompactionTokens: data.preCompactionTokens || this.tokenCount,
        messagesCount: this.conversationHistory.length,
        history: this.conversationHistory
      };

      try {
        await fs.writeFile(
          this.options.historyPath,
          JSON.stringify(historyData, null, 2),
          'utf-8'
        );
        console.log(`   ✅ Full history saved to: ${this.options.historyPath}`);
      } catch (error) {
        console.error(`   ❌ Failed to save history: ${error}`);
      }

      // Also save to plugin context data
      context.data.set('full_conversation_history', [...this.conversationHistory]);
      context.data.set('compaction_metadata', {
        count: this.compactionCount,
        lastCompaction: new Date().toISOString(),
        tokensBeforeCompaction: data.preCompactionTokens || this.tokenCount
      });
    }
  }

  async onCompactionComplete(context: PluginContext, data: any): Promise<void> {
    if (this.options.warn) {
      if (data.success) {
        console.log('   ✅ Compaction complete');
        console.log(`   Tokens removed: ${data.tokensRemoved || 'unknown'}`);
        console.log(`   Messages removed: ${data.messagesRemoved || 'unknown'}`);
        
        if (data.summaryContent) {
          console.log('\n   📝 Compaction Summary:');
          console.log(`   "${data.summaryContent.substring(0, 100)}..."`);
        }

        console.log('\n   💡 TIP: Full history preserved in ~/.copilot-history.json');
        console.log('   📖 To disable auto-compaction, see: https://github.com/github/copilot-cli/issues/947\n');
      } else {
        console.error(`   ❌ Compaction failed: ${data.error}`);
      }
    }

    // Reset token counter (post-compaction count)
    if (data.postCompactionTokens) {
      this.tokenCount = data.postCompactionTokens;
    }
  }

  async onSessionEnd(): Promise<void> {
    console.log(`\n🛡️  AntiCompactionPlugin Session Summary:`);
    console.log(`   Total compactions: ${this.compactionCount}`);
    console.log(`   Messages tracked: ${this.conversationHistory.length}`);
    console.log(`   Final token count: ~${this.tokenCount.toLocaleString()}`);
    
    if (this.compactionCount > 0 && this.options.preserve) {
      console.log(`   💾 History saved to: ${this.options.historyPath}`);
    }
  }

  async onUnload(): Promise<void> {
    console.log('🛡️  AntiCompactionPlugin unloaded');
  }
}
