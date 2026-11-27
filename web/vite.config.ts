import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.FLOWOPS_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => `/flowOps${path.replace(/^\/api/, '')}`,
      },
    },
  },
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'vendor-vue',
              test: /[\\/]node_modules[\\/](?:vue|@vue|vue-router|pinia)[\\/]/,
              priority: 4,
            },
            {
              name: 'vendor-ui-utils',
              test: /[\\/]node_modules[\\/](?:@ctrl|@floating-ui|@popperjs|@sxzz|@vueuse|async-validator|dayjs|lodash(?:-es|-unified)?|memoize-one|normalize-wheel-es)[\\/]/,
              priority: 3,
            },
            {
              name: 'vendor-element-plus',
              test: /[\\/]node_modules[\\/](?:element-plus|@element-plus)[\\/]/,
              priority: 2,
            },
          ],
        },
      },
    },
  },
})
