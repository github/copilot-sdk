import { stableGreeting, StableClient } from "./sdk";

console.log(stableGreeting("world"));

const client = new StableClient();
client.greet();
