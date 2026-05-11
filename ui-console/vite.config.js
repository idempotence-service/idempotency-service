import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/core': {
        target: 'http://localhost:18080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/core/, ''),
      },
      '/api/sender': {
        target: 'http://localhost:18081',
        changeOrigin: true,
      },
      '/api/receiver': {
        target: 'http://localhost:18082',
        changeOrigin: true,
      },
    },
  },
})
