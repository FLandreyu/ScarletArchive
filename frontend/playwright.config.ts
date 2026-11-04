import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: 'tests',
  fullyParallel: false,
  workers: 1,
  timeout: 50000,
  retries: 0,
  reporter: [['list'], ['json', { outputFile: '../docs/evidence/browser-results.json' }]],
  use: {
    baseURL: process.env.SCARLET_BASE_URL || 'http://127.0.0.1:8099',
    headless: true,
    viewport: { width: 1440, height: 1050 },
    permissions: ['clipboard-read', 'clipboard-write'],
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'desktop', testIgnore: /mobile/ },
    {
      name: 'mobile',
      testMatch: /mobile/,
      use: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true },
    },
  ],
})
