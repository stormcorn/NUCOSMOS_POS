declare module "@vitejs/plugin-legacy" {
  import type { PluginOption } from "vite";

  export interface LegacyOptions {
    targets?: string | string[];
    modernPolyfills?: boolean;
    renderLegacyChunks?: boolean;
  }

  export default function legacy(options?: LegacyOptions): PluginOption;
}
