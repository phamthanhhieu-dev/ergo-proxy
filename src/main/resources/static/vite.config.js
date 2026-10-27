import { defineConfig } from "vite";
import path from "node:path";

export default defineConfig({
    base: "/playback/dist/",
    publicDir: false,
    build: {
        outDir: "playback/dist",
        emptyOutDir: true,
        cssCodeSplit: false,
        assetsInlineLimit: 0,
        rolldownOptions: {
            input: {
                playback: path.resolve("playback/src/playback.js"),
                online: path.resolve("online/online.js"),
            },
            output: {
                entryFileNames: "[name].js",
                chunkFileNames: "assets/[name]-[hash].js",
                assetFileNames: (asset) =>
                    (asset.names || [asset.name]).some((name) =>
                        name?.endsWith(".css"),
                    )
                        ? "playback.css"
                        : "assets/[name]-[hash][extname]",
            },
        },
    },
});
