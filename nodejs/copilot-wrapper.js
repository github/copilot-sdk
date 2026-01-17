#!/usr/bin/env node

/**
 * Copilot CLI Plugin Wrapper
 * 
 * This wrapper launches the Copilot CLI binary with plugin support.
 * It uses the plugin-enabled SDK to intercept all communication and
 * provide an interactive terminal interface.
 * 
 * Usage:
 *   copilot-with-plugins [options]
 * 
 * Plugins can be configured in ~/.copilot-plugins.json or passed as arguments
 */

import { CopilotClient } from './dist/index.js';
import { readFileSync, existsSync } from 'fs';
import { homedir } from 'os';
import { join } from 'path';
import readline from 'readline';

// Import built-in test plugins
import { testPlugin } from './test-plugin.js';

// Load plugin configuration from ~/.copilot-plugins.json
const pluginConfigPath = join(homedir(), '.copilot-plugins.json');
let plugins = [];

// For now, use test plugin
plugins = [testPlugin];

if (existsSync(pluginConfigPath)) {
    try {
        const config = JSON.parse(readFileSync(pluginConfigPath, 'utf8'));
        console.log('🏴‍☠️ Loaded plugin config from', pluginConfigPath);
        
        // TODO: Dynamic plugin loading from config
        // For now, we use statically imported plugins
    } catch (error) {
        console.error('⚠️  Failed to load plugin config:', error.message);
    }
}

console.log('🏴‍☠️ Starting Copilot CLI with plugin support...\n');

// Create client with plugins
const client = new CopilotClient({
    plugins,
    autoStart: true,
    useStdio: false, // Use TCP mode so we can intercept
    port: 0 // Random available port
});

console.log('🔌 Starting Copilot CLI server...');
await client.start();

console.log('✅ Connected to Copilot CLI');
console.log(`📦 Loaded ${plugins.length} plugin(s)\n`);

// Create a session
const session = await client.createSession();

console.log('🎯 Session created. Type your prompts (Ctrl+C to exit)\n');

// Setup readline for interactive input
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: '> '
});

// Handle session events
session.on((event) => {
    switch (event.type) {
        case 'user.message':
            // Check if this was a plugin command response
            if (event.data.content && event.data.content.includes('📦')) {
                console.log('\n' + event.data.content + '\n');
                rl.prompt();
            }
            break;
            
        case 'assistant.message':
            console.log('\n📝 Assistant:', event.data.content);
            console.log('');
            rl.prompt();
            break;
        
        case 'assistant.message_delta':
            // Stream response
            process.stdout.write(event.data.deltaContent);
            break;
        
        case 'session.idle':
            console.log('');
            rl.prompt();
            break;
        
        case 'assistant.intent':
            console.log(`💭 ${event.data.intent}`);
            break;
            
        // Add more event handlers as needed
    }
});

// Handle user input
rl.on('line', async (input) => {
    if (!input.trim()) {
        rl.prompt();
        return;
    }
    
    console.log('');
    await session.send({ prompt: input });
});

// Handle exit
rl.on('close', async () => {
    console.log('\n\n🏴‍☠️ Shutting down...');
    await session.destroy();
    await client.stop();
    process.exit(0);
});

// Start prompting
rl.prompt();
