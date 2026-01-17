import { PluginManager, BUILTIN_PLUGINS } from './dist/index.js';

const manager = new PluginManager([], {
  availablePlugins: BUILTIN_PLUGINS,
  debug: true
});

console.log('\n📥 Testing: /plugins install logger\n');
const installResult = await manager.handleCommand('/plugins install logger');
console.log('Install result:', installResult);

console.log('\n📋 Testing: /plugins list\n');
const listResult = await manager.handleCommand('/plugins list');
console.log('List result:', listResult);

console.log('\n📦 Testing: /plugins available\n');
const availableResult = await manager.handleCommand('/plugins available');
console.log('Available result:', availableResult);
