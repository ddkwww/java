import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    // 开发代理：前端以相对路径发请求/取图时，由 Vite 转发到后端 8081。
    // 这样无需 .env、无需操心 CORS，本地(localhost:5173)与局域网(192.168.68.100:5173)访问都通。
    proxy: {
      // 接口请求
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 静态图片（功法/法宝插画）：后端挂在 /images/manuals/**，
      // 前端 API_BASE 为空时图片地址是相对路径 /images/manuals/xxx.jpg，必须在此转发，否则打到 Vite 自身 404。
      '/images': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
