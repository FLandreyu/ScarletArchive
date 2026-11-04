import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
export default defineConfig({
  plugins: [vue(), tailwindcss()],
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
