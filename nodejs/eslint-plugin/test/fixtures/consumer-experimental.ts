import { stableGreeting, startCanvas, CanvasOptions, StableClient } from "./sdk";

console.log(stableGreeting("world"));

const canvas = startCanvas();
console.log(canvas);

const options: CanvasOptions = { title: "demo" };
console.log(options);

const client = new StableClient();
client.greet();
client.enableMcpApps();
