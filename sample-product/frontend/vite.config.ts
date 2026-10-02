import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Builds straight into the Spring Boot app's static resources, so `mvn package` (or the app run
// from an IDE) serves the SPA from the same origin as the REST API and WebSocket endpoint — no
// CORS/CSRF configuration needed for this sample, since it's genuinely one origin.
export default defineConfig({
  plugins: [react()],
  base: "./",
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: true,
  },
  server: {
    // Only used by `npm run dev` for frontend-only iteration; the built app (what Phase 7
    // actually verifies) is same-origin and needs no proxy at all.
    proxy: {
      "/api": "http://localhost:8090",
      "/ws": { target: "http://localhost:8090", ws: true },
      "/demo": "http://localhost:8090",
    },
  },
});
