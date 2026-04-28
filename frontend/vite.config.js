import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // 代理API请求到后端
      '/api': {
        target: 'http://localhost:5501',
        changeOrigin: true
      },
      // 代理静态文件请求
      '/static': {
        target: 'http://localhost:5501',
        changeOrigin: true
      }
    }
  }
})
