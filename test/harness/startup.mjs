/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

console.error(
  `[SDK proxy startup] Node entered at ${Math.round(performance.now())}ms`,
);
await import("tsx");
console.error(
  `[SDK proxy startup] TypeScript loader ready at ${Math.round(performance.now())}ms`,
);
await import("./server.ts");
