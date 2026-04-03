
import { defineConfig } from "vite";
import legacy from "@vitejs/plugin-legacy";
import vue from "@vitejs/plugin-vue";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  base: command === "build" ? "/erp/" : "/",
  plugins: [
    vue(),
    legacy({
      targets: ["defaults", "Android >= 8", "Chrome >= 61", "iOS >= 12"],
      renderLegacyChunks: true,
      modernPolyfills: true,
    }),
  ],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    allowedHosts: [
      "uxpilot.net",
      "host.uxpilot.net",
      "dev.host.uxpilot.net",
      "uxpilot.ai",
      "localhost",
      "127.0.0.1",
    ],
  },
}));
