# 🔌 GitHub Copilot SDK - Plugin System

A powerful, production-ready plugin system for the GitHub Copilot SDK that enables extensibility through lifecycle hooks and dynamic plugin loading.

## ✨ Features

- **Lifecycle Hooks**: Intercept and modify behavior at every stage of the SDK lifecycle
- **Slash Commands**: Built-in `/plugins` command system for interactive plugin management
- **Dynamic Loading**: Install and enable plugins at runtime without restarting
- **Built-in Plugins**: 4 ready-to-use plugins for common use cases
- **Plugin Registry**: Extensible registry system for publishing and sharing plugins
- **Data Persistence**: Per-session plugin data storage
- **TypeScript Support**: Full type definitions for plugin development

## 🚀 Quick Start

### Using Built-in Plugins

```javascript
import { CopilotClient, BUILTIN_PLUGINS } from '@github/copilot-sdk';

const client = new CopilotClient({
  plugins: [],
  pluginManagerConfig: {
    availablePlugins: BUILTIN_PLUGINS
  }
});

await client.start();
const session = await client.createSession();

// Install a plugin dynamically
await session.send({ prompt: '/plugins install logger' });

// List installed plugins
await session.send({ prompt: '/plugins list' });
```

### Creating a Custom Plugin

```javascript
import type { Plugin, PluginContext } from '@github/copilot-sdk';

const myPlugin = {
  name: 'my-plugin',
  description: 'My awesome plugin',
  
  async onLoad() {
    console.log('Plugin loaded!');
  },
  
  async onSessionCreated(context) {
    context.data.set('startTime', Date.now());
  },
  
  async onBeforeSend(context, options) {
    console.log('Sending:', options.prompt);
    return options; // Can modify options here
  },
  
  async onSessionEvent(context, event) {
    console.log('Event:', event.type);
    return event;
  },
  
  async onSessionEnd(context) {
    const duration = Date.now() - context.data.get('startTime');
    console.log(`Session lasted ${duration}ms`);
  }
};

const client = new CopilotClient({
  plugins: [myPlugin]
});
```

## 📚 Plugin Lifecycle Hooks

Plugins can implement any of these optional hooks:

### `onLoad(): Promise<void> | void`
Called when the plugin is loaded (once per SDK instance).

### `onSessionCreated(context: PluginContext): Promise<void> | void`
Called when a new session is created.

### `onBeforeSend(context: PluginContext, options: MessageOptions): Promise<MessageOptions> | MessageOptions`
Called before sending a message. Can modify the message options.

### `onSessionEvent(context: PluginContext, event: SessionEvent): Promise<SessionEvent | void> | SessionEvent | void`
Called for every session event. Can modify the event.

### `onCompactionStart(context: PluginContext, data: CompactionData): Promise<void> | void`
Called when context compaction starts. Useful for preserving important data.

### `onCompactionComplete(context: PluginContext, data: CompactionResult): Promise<void> | void`
Called after context compaction completes.

### `onSessionEnd(context: PluginContext): Promise<void> | void`
Called when the session is destroyed. Cleanup hook.

### `onUnload(): Promise<void> | void`
Called when the plugin is unloaded.

## 🎯 Built-in Plugins

The SDK ships with 4 production-ready plugins:

### 1. Logger Plugin
Logs all session interactions for debugging.

```bash
/plugins install logger
```

Features:
- Session creation logging
- Message send/receive logging
- Configurable debug mode

### 2. Memory Preservation Plugin
Preserves important conversation data before context compaction.

```bash
/plugins install memory-preservation
```

Features:
- Tracks important messages
- Saves data before compaction
- Restores data after compaction

### 3. Analytics Plugin
Tracks usage statistics and message counts.

```bash
/plugins install analytics
```

Features:
- Message count tracking
- Token usage monitoring
- Session duration stats

### 4. Anti-Compaction Plugin
Monitors and preserves conversation history during context compaction.

```bash
/plugins install anti-compaction
```

Features:
- Compaction event monitoring
- Full conversation history preservation
- Token threshold warnings
- Configurable preservation options

## 💬 Slash Commands

The plugin system adds interactive `/plugins` commands:

### `/plugins` or `/plugins list`
List all installed plugins with their status (enabled/disabled).

### `/plugins available`
Browse available plugins in the registry.

### `/plugins install <name>`
Install and enable a plugin from the registry.

### `/plugins enable <name>`
Enable a disabled plugin.

### `/plugins disable <name>`
Temporarily disable a plugin without uninstalling.

### `/plugins uninstall <name>`
Completely remove a plugin.

### `/plugins help`
Show help for all plugin commands.

## 🔧 Plugin Context

Every hook receives a `PluginContext` object:

```typescript
interface PluginContext {
  /** Current session */
  session: CopilotSession;
  
  /** Plugin-specific data storage (persists for session lifetime) */
  data: Map<string, any>;
}
```

Use `context.data` to store plugin-specific data that persists across hook calls:

```javascript
async onSessionCreated(context) {
  context.data.set('messageCount', 0);
}

async onBeforeSend(context, options) {
  const count = context.data.get('messageCount') || 0;
  context.data.set('messageCount', count + 1);
  return options;
}
```

## 📦 Plugin Registry

Create a registry of available plugins:

```javascript
const MY_PLUGINS = new Map([
  ['my-plugin', () => import('./my-plugin.js').then(m => m.default)],
  ['another-plugin', async () => {
    // Can be async factory
    return {
      name: 'another-plugin',
      description: 'Another awesome plugin',
      async onLoad() { /* ... */ }
    };
  }]
]);

const client = new CopilotClient({
  pluginManagerConfig: {
    availablePlugins: MY_PLUGINS
  }
});
```

## 🧪 Testing

Run the comprehensive test suite:

```bash
cd nodejs
node test-plugin-system.js
```

The test suite validates:
- ✅ PluginManager initialization
- ✅ All slash commands
- ✅ All lifecycle hooks
- ✅ All 4 built-in plugins
- ✅ Plugin data persistence
- ✅ Multiple plugins working together
- ✅ Edge cases and error handling

**Current Status**: 33/33 tests passing (100% pass rate)

## 📖 Example: Session Logger Plugin

Here's a complete example plugin that logs session metrics:

```javascript
const sessionLoggerPlugin = {
  name: 'session-logger',
  description: 'Logs detailed session metrics',
  
  async onSessionCreated(context) {
    context.data.set('stats', {
      startTime: Date.now(),
      messagesSent: 0,
      eventsReceived: 0,
      errors: 0
    });
    console.log(`📊 Session started: ${context.session.sessionId}`);
  },
  
  async onBeforeSend(context, options) {
    const stats = context.data.get('stats');
    stats.messagesSent++;
    console.log(`📤 Message #${stats.messagesSent}: ${options.prompt}`);
    return options;
  },
  
  async onSessionEvent(context, event) {
    const stats = context.data.get('stats');
    stats.eventsReceived++;
    if (event.type === 'error') stats.errors++;
    return event;
  },
  
  async onSessionEnd(context) {
    const stats = context.data.get('stats');
    const duration = Date.now() - stats.startTime;
    
    console.log(`\n📊 Session Summary:`);
    console.log(`   Duration: ${(duration / 1000).toFixed(2)}s`);
    console.log(`   Messages sent: ${stats.messagesSent}`);
    console.log(`   Events received: ${stats.eventsReceived}`);
    console.log(`   Errors: ${stats.errors}`);
  }
};
```

## 🤝 Contributing Plugins

To contribute a plugin to the built-in registry:

1. Create your plugin following the `Plugin` interface
2. Add comprehensive tests
3. Document usage and features
4. Submit a PR to add it to `BUILTIN_PLUGINS`

## 🔒 Security Considerations

- Plugins run in the same process as the SDK
- Plugins have full access to the SDK API
- Only install plugins from trusted sources
- Review plugin code before installation
- Consider sandboxing for untrusted plugins

## 📝 License

MIT - Same as GitHub Copilot SDK

## 🏴‍☠️ Credits

Plugin System developed by Barrer Software (@barrersoftware)
Built on GitHub Copilot SDK (MIT License)

---

**Ready to extend GitHub Copilot? Start building plugins today!** 🚀
