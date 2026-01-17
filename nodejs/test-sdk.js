/**
 * Test script to verify plugin system works with copilot SDK
 */

import { CopilotClient } from './dist/index.js';
import { testPlugin } from './test-plugin.js';

console.log('🏴‍☠️ Starting plugin test...\n');

// Create client with test plugin
const client = new CopilotClient({
    plugins: [testPlugin],
    logLevel: 'debug'
});

console.log('🏴‍☠️ Starting client...\n');
await client.start();

console.log('🏴‍☠️ Creating session...\n');
const session = await client.createSession({ model: 'claude-sonnet-4.5' });

console.log('🏴‍☠️ Sending test message...\n');

// Subscribe to events
session.on((event) => {
    console.log('📨 Event received:', event.type);
});

await session.send({ prompt: 'Say "Plugin system is working!" if you can read this.' });

// Wait a bit for events
await new Promise(resolve => setTimeout(resolve, 10000));

console.log('\n🏴‍☠️ Cleaning up...\n');
await session.destroy();
await client.stop();

console.log('🏴‍☠️ Test complete!');
