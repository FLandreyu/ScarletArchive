import { chromium } from '@playwright/test'
import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import assert from 'node:assert/strict'

const base = process.env.SCARLET_BASE_URL || 'http://127.0.0.1:8098'
const output = new URL('../../docs/screenshots/', import.meta.url)
const evidence = new URL('../../docs/evidence/', import.meta.url)
await mkdir(output, { recursive: true })
await mkdir(evidence, { recursive: true })
const browser = await chromium.launch({ headless: true })
const errors = []
const context = await browser.newContext({ viewport: { width: 1440, height: 1050 } })
await context.addInitScript(() => {
  window.__scarletMetrics = { lcp: 0, cls: 0 }
  new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) window.__scarletMetrics.lcp = entry.startTime
  }).observe({ type: 'largest-contentful-paint', buffered: true })
  new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
      if (!entry.hadRecentInput) window.__scarletMetrics.cls += entry.value
    }
  }).observe({ type: 'layout-shift', buffered: true })
})
const page = await context.newPage()
page.on('pageerror', (e) => errors.push(e.message))
page.on('console', (m) => {
  if (m.type() === 'error') errors.push(m.text())
})
async function visit(path, selector) {
  await page.goto(base + path)
  await page.locator(selector).first().waitFor()
  await page.evaluate(() => document.fonts.ready)
}
async function capture(p, name) {
  await p.evaluate(() => {
    document.activeElement?.blur()
    window.scrollTo({ top: 0, behavior: 'instant' })
  })
  await p.waitForFunction(() => scrollY === 0 && !document.querySelector('.clock-transition.turn'))
  await p.screenshot({ path: fileURLToPath(new URL(name, output)), fullPage: true, animations: 'disabled' })
}
try {
  await visit('/', '.latest-grid .article-card')
  await page.locator('.hero-image').evaluate((img) => img.decode())
  await page.waitForFunction(() => getComputedStyle(document.querySelector('.host-note')).opacity === '1')
  await capture(page, '01-home.png')
  const performance = await page.evaluate(() => {
    const n = window.performance.getEntriesByType('navigation')[0]
    return { domContentLoadedMs: Math.round(n.domContentLoadedEventEnd), ...window.__scarletMetrics }
  })
  await visit('/articles', '.article-grid .article-card')
  await capture(page, '02-library.png')
  await visit('/articles/transactions-and-a-seat', '.reading-toc a')
  assert.equal(await page.locator('.code-wrap pre').count(), 2)
  assert.equal(await page.locator('.prose pre pre').count(), 0)
  await capture(page, '03-reading.png')
  for (const [path, selector, name] of [
    ['/guestbook', '.guest-letter', '05-guestbook.png'],
    ['/about', '.about-copy .prose p', '10-about.png'],
    ['/archive', '.archive-month a', '11-archive.png'],
    ['/friends', '.friend-grid a', '12-friends.png'],
  ]) {
    await visit(path, selector)
    await capture(page, name)
  }
  await visit('/secret', '.collect-crystal')
  for (let i = 1; i <= 7; i++) await page.getByRole('button', { name: `点亮第${i}枚水晶` }).click()
  await capture(page, '06-secret.png')
  const auth = await context.request.post(base + '/api/auth/login', {
    data: { username: 'admin', password: 'Scarlet123!' },
  })
  assert.equal(auth.status(), 200)
  const token = (await auth.json()).data.token
  await page.addInitScript((value) => sessionStorage.setItem('scarlet-token', value), token)
  await visit('/admin', '.admin-stat-grid strong')
  await capture(page, '09-admin.png')
  await page.getByRole('navigation', { name: '后台导航' }).getByRole('link', { name: '文章管理' }).click()
  await page.getByRole('button', { name: '编辑从一张座位开始：理解事务与并发', exact: true }).click()
  await page.locator('.editor-preview .copy-code').first().waitFor()
  await capture(page, '04-editor.png')
  const mobile = await browser.newContext({
    viewport: { width: 390, height: 844 },
    isMobile: true,
    hasTouch: true,
    reducedMotion: 'reduce',
  })
  const mp = await mobile.newPage()
  await mp.goto(base)
  await mp.locator('.latest-grid .article-card').first().waitFor()
  await mp.locator('.hero-image').evaluate((img) => img.decode())
  await capture(mp, '07-mobile-home.png')
  assert.ok(await mp.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  await mp.goto(base + '/articles/transactions-and-a-seat')
  await mp.locator('.copy-code').first().waitFor()
  await capture(mp, '08-mobile-reading.png')
  assert.ok(await mp.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  const tablet = await browser.newContext({ viewport: { width: 820, height: 1180 }, reducedMotion: 'reduce' })
  const tp = await tablet.newPage()
  await tp.goto(base + '/articles')
  await tp.locator('.article-grid .article-card').first().waitFor()
  assert.ok(await tp.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  assert.deepEqual(errors, [])
  const report = {
    checkedAt: new Date().toISOString(),
    baseURL: base,
    desktopWidth: 1440,
    tabletWidth: 820,
    mobileWidth: 390,
    overflow: false,
    nestedCodeBlocks: false,
    scriptErrors: errors,
    localBrowserPerformance: performance,
    screenshots: 12,
  }
  await writeFile(new URL('final-smoke.json', evidence), JSON.stringify(report, null, 2) + '\n')
  console.log(JSON.stringify(report))
} finally {
  await browser.close()
}
