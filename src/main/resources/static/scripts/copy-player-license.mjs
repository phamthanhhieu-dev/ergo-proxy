import fs from "node:fs/promises";
await fs.copyFile(
    "node_modules/jassub/LICENSE",
    "playback/dist/JASSUB-LICENSE",
);
