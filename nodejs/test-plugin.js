/**
 * Simple test plugin to verify plugin system works with copilot CLI
 */

export const testPlugin = {
    name: 'test-plugin',
    
    async onLoad() {
        console.log('🏴‍☠️ TEST PLUGIN: onLoad() called');
    },
    
    async onSessionCreated(context) {
        console.log('🏴‍☠️ TEST PLUGIN: onSessionCreated() called - Session ID:', context.session.sessionId);
    },
    
    async onBeforeSend(context, options) {
        console.log('🏴‍☠️ TEST PLUGIN: onBeforeSend() called - Prompt:', options.prompt);
        return options;
    },
    
    async onSessionEvent(context, event) {
        console.log('🏴‍☠️ TEST PLUGIN: onSessionEvent() called - Type:', event.type);
        return event;
    },
    
    async onSessionEnd(context) {
        console.log('🏴‍☠️ TEST PLUGIN: onSessionEnd() called');
    }
};
