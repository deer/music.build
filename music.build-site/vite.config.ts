import { defineConfig } from "vite";
import { fresh } from "@fresh/plugin-vite";
import tailwindcss from "@tailwindcss/vite";
import { denoteHmr, denoteStyles } from "@denote/core/vite";
import { islandSpecifiers } from "@denote/core";

export default defineConfig({
  server: { port: 8000 },
  plugins: [
    // denoteStyles() inlines @import "@denote/core/styles.css" before
    // @tailwindcss/vite sees it — required because JSR packages aren't
    // installed into node_modules, so the CSS @import would not resolve.
    denoteStyles(),
    // denoteHmr() hot-reloads denote.config.ts without restarting Vite.
    denoteHmr(),
    fresh({
      serverEntry: "main.ts",
      clientEntry: "client.ts",
      islandSpecifiers,
    }),
    tailwindcss(),
  ],
});
