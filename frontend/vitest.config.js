import { defineConfig, mergeConfig } from 'vite'
import { configDefaults } from 'vitest/config'
import viteConfig from './vite.config.js'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/tests/setup.js'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        exclude: [
          ...configDefaults.exclude,
          'src/tests/**',
          '**/*.spec.js'
        ]
      }
    }
  })
)
