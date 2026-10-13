import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server:{
    port:5173,//前端端口
    proxy:{
      '/api':{//拦截所有以/api开头的请求
        target:'http://localhost:8080',//转发给后端
        changeOrigin:true,
        rewrite:(path)=>path.replace(/^\/api/,'')//去掉/api前缀
      }
    }
  }
})
