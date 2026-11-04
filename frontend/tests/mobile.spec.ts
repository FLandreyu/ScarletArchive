import { test, expect } from '@playwright/test'
test('手机导航、文章、庭院与减少动态偏好', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /今夜/ })).toBeVisible()
  await expect(page.getByRole('button', { name: '开启动画效果' })).toBeVisible()
  await expect(page.locator('.latest-grid .article-card')).toHaveCount(4)
  await page.screenshot({
    path: '../docs/screenshots/07-mobile-home.png',
    fullPage: true,
    animations: 'disabled',
  })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  await page.getByRole('button', { name: '打开导航菜单' }).click()
  await page.getByRole('navigation', { name: '手机导航' }).getByRole('link', { name: '魔法图书馆' }).click()
  await expect(page.locator('.article-grid .article-card').first()).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  await page.goto('/articles/transactions-and-a-seat')
  await expect(page.locator('.prose h2').first()).toBeVisible()
  await page.screenshot({
    path: '../docs/screenshots/08-mobile-reading.png',
    fullPage: true,
    animations: 'disabled',
  })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  await page.goto('/guestbook')
  await expect(page.getByLabel('想留下的话')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
})
