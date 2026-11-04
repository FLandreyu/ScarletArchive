import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { existsSync } from 'node:fs'
import manifest from './character-art.json'
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  define: {
    __SCARLET_CHARACTER_ART__: JSON.stringify(
      process.env.SCARLET_CHARACTER_ART !== 'off' &&
        Object.keys(manifest.characters).every((key) =>
          existsSync(new URL(`public/art/characters/${key}.png`, import.meta.url)),
        ),
    ),
  },
  build: { outDir: '../backend/src/main/resources/static', emptyOutDir: true },
  server: {
    host: '127.0.0.1',
    port: 5180,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:8098',
      '/uploads': 'http://127.0.0.1:8098',
      '/sitemap.xml': 'http://127.0.0.1:8098',
    },
  },
})
