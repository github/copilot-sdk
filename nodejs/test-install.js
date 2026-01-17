import { CopilotClient, BUILTIN_PLUGINS } from './dist/index.js';

const client = new CopilotClient({
  plugins: [],
  pluginManagerConfig: {
    availablePlugins: BUILTIN_PLUGINS,
    debug: true
  }
});

await client.start({ useStdio: false, port: 0 });
const session = await client.createSession();

// Test install command
console.log('\n📥 Testing: /plugins install logger\n');
const result = await session.send({ prompt: '/plugins install logger' });
console.log('Result:', result);

// Test list to see if logger is loaded
console.log('\n📋 Testing: /plugins list\n');
const listResult = await session.send({ prompt: '/plugins list' });
console.log('Result:', listResult);

await session.destroy();
await client.stop();
