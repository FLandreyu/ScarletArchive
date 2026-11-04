import { test, expect } from '@playwright/test'

test('六位馆邸角色加载同源立绘，保留原作署名且手机布局不溢出', async ({ page }) => {
  const roles = [
    ['/', 'remilia', '71407907'],
    ['/articles', 'patchouli', '67199755'],
    ['/archive', 'sakuya', '39547480'],
    ['/guestbook', 'meiling', '55217436'],
    ['/tags', 'koakuma', '33672888'],
    ['/secret', 'flandre', '40346048'],
  ]
  for (const width of [1440, 820, 390]) {
    await page.setViewportSize({ width, height: 1000 })
    for (const [route, character, id] of roles) {
      await page.goto(route)
      const portrait = page.locator(`.character-portrait img[src="/art/characters/${character}.png"]`)
      await expect(portrait).toBeVisible()
      await portrait.scrollIntoViewIfNeeded()
      await portrait.evaluate((image: HTMLImageElement) => image.decode())
      expect(await portrait.evaluate((image: HTMLImageElement) => image.naturalWidth)).toBeGreaterThan(400)
      await expect(
        page.locator(`.character-portrait a[href="https://www.pixiv.net/artworks/${id}"]`),
      ).toHaveText(/dairi/)
      expect(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1)).toBeFalsy()
    }
  }
  await page.goto('/')
  await page.getByRole('button', { name: '打开藏书搜索' }).click()
  await expect(page.locator('.portrait-search img')).toBeVisible()
  await page.getByLabel('搜索藏书').fill('事务')
  await expect(page.locator('.search-results a').first()).toBeVisible()
})
