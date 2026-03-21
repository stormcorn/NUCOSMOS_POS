
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(() => ({
  base: process.env.VITE_BASE || "/",
  plugins: [vue()],
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
