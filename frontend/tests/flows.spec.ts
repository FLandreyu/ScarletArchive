import { test, expect, type Page } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
let token = ''
const suffix = Date.now().toString(36)
test.beforeAll(async ({ request }) => {
  await mkdir('../docs/screenshots', { recursive: true })
  const r = await request.post('/api/auth/login', { data: { username: 'admin', password: 'Scarlet123!' } })
  expect(r.ok()).toBeTruthy()
  token = (await r.json()).data.token
})
async function admin(page: Page, section = 'articles') {
  await page.addInitScript((t) => sessionStorage.setItem('scarlet-token', t), token)
  await page.goto('/admin/' + section)
  await expect(page.getByRole('navigation', { name: '后台导航' })).toBeVisible()
}
test('首页、搜索、代码复制与阅读目录', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', (e) => errors.push(e.message))
  page.on('console', (e) => {
    if (e.type() === 'error') errors.push(e.text())
  })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /今夜/ })).toBeVisible()
  await expect(page.locator('.latest-grid .article-card')).toHaveCount(4)
  await page.locator('.hero-image').evaluate((img: HTMLImageElement) => img.decode())
  await page.waitForFunction(() => getComputedStyle(document.querySelector('.host-note')!).opacity === '1')
  await page.screenshot({ path: '../docs/screenshots/01-home.png', fullPage: true, animations: 'disabled' })
  await page.getByRole('button', { name: '打开藏书搜索' }).click()
  await page.getByLabel('搜索藏书').fill('事务')
  await page.locator('.search-results a').first().click()
  await expect(page.locator('.reading-header h1')).toContainText('事务')
  await expect(page.locator('.reading-toc a')).toHaveCount(6)
  await page.locator('.copy-code').first().click()
  await expect(page.getByRole('status')).toContainText('代码已复制')
  expect(await page.evaluate(() => navigator.clipboard.readText())).toContain('@Transactional')
  await page.screenshot({
    path: '../docs/screenshots/03-reading.png',
    fullPage: true,
    animations: 'disabled',
  })
  expect(errors).toEqual([])
})
test('后台草稿隔离、预览安全、发布后前台可读', async ({ page, request }) => {
  await page.goto('/admin')
  await page.getByLabel('管理员密码').fill('Scarlet123!')
  await page.getByRole('button', { name: '进入书房' }).click()
  await expect(page.getByRole('navigation', { name: '后台导航' })).toBeVisible()
  await page.getByRole('navigation', { name: '后台导航' }).getByRole('link', { name: '文章管理' }).click()
  await page.getByRole('button', { name: '新建文章', exact: true }).click()
  await page.getByLabel('文章标题').fill('浏览器验收手记 ' + suffix)
  await page.getByLabel('短链接 slug').fill('browser-' + suffix)
  await page.getByLabel('文章摘要').fill('后台创建、草稿隔离与发布闭环的真实浏览器验证。')
  await page
    .getByLabel('Markdown正文')
    .fill(
      '## 一份真实手记\n\n通过真实接口发表。\n\n<script>window.__scarlet_xss=1</script>\n\n<img src=x onerror="window.__scarlet_xss=1">\n\n```java\nSystem.out.println("Hello");\n```',
    )
  await expect(page.locator('.editor-preview script,.editor-preview img[onerror]')).toHaveCount(0)
  const saved = page.waitForResponse(
    (r) => r.url().endsWith('/api/admin/articles') && r.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '保存草稿', exact: true }).click()
  const article = (await (await saved).json()).data
  expect((await request.get('/api/articles/' + article.slug)).status()).toBe(404)
  expect(await (await request.get('/sitemap.xml')).text()).not.toContain(article.slug)
  await page.getByRole('button', { name: '编辑' + article.title, exact: true }).click()
  await page.getByLabel('文章状态').selectOption('PUBLISHED')
  await page.screenshot({ path: '../docs/screenshots/04-editor.png', fullPage: true, animations: 'disabled' })
  await page.getByRole('button', { name: '保存并发布' }).click()
  await expect(page.getByText('文章已发布，可以在图书馆阅读')).toBeVisible()
  await page.goto('/articles/' + article.slug)
  await expect(page.locator('.reading-header h1')).toHaveText(article.title)
  await expect(page.locator('.prose script,.prose img[onerror]')).toHaveCount(0)
  expect(await page.evaluate(() => Object.hasOwn(window, '__scarlet_xss'))).toBeFalsy()
})
test('留言先审核再公开，友链申请也受审核约束', async ({ page, request }) => {
  await page.goto('/guestbook')
  await page.getByLabel('怎么称呼你').fill('庭院验收' + suffix)
  await page.getByLabel('想留下的话').fill('这封信在审核后才应该公开展示。')
  await page.getByRole('button', { name: '把来信交给美铃' }).click()
  await expect(page.getByRole('status')).toContainText('审核后')
  expect(JSON.stringify((await (await request.get('/api/guestbook')).json()).data)).not.toContain(
    '庭院验收' + suffix,
  )
  await admin(page, 'guestbook')
  const letter = page.locator('.moderation-list article').filter({ hasText: '庭院验收' + suffix })
  await letter.getByRole('button', { name: '审核通过' }).click()
  await expect(letter.getByText('已公开')).toBeVisible()
  await page.goto('/guestbook')
  await expect(page.locator('.guest-messages')).toContainText('庭院验收' + suffix)
  await page.screenshot({
    path: '../docs/screenshots/05-guestbook.png',
    fullPage: true,
    animations: 'disabled',
  })
  await page.goto('/friends')
  await page.locator('.friend-apply').click()
  await page.getByLabel('站点名称').fill('朋友' + suffix)
  await page.getByLabel('站点地址').fill('https://example.com')
  await page.getByLabel('一句介绍').fill('一份等待审核的友链申请')
  await page.getByRole('button', { name: '提交申请', exact: true }).click()
  expect(JSON.stringify((await (await request.get('/api/friends')).json()).data)).not.toContain(
    '朋友' + suffix,
  )
  await admin(page, 'friends')
  const friend = page.locator('.moderation-list article').filter({ hasText: '朋友' + suffix })
  await friend.getByRole('button', { name: '审核通过' }).click()
  await expect(friend.getByText('已公开')).toBeVisible()
})
test('后台标签管理与站点设置真实持久化', async ({ page, request }) => {
  await admin(page, 'taxonomy')
  const tagPanel = page
    .locator('.admin-panel')
    .filter({ has: page.getByRole('heading', { name: '文章标签', exact: true }) })
  await tagPanel.getByRole('button', { name: '新增', exact: true }).click()
  await page
    .getByRole('dialog')
    .getByLabel('名称', { exact: true })
    .fill('测试标签' + suffix)
  await page.getByLabel('短标识 slug').fill('tag-' + suffix)
  await page.getByRole('button', { name: '保存索引' }).click()
  await expect(tagPanel).toContainText('测试标签' + suffix)
  await tagPanel.getByRole('button', { name: '删除测试标签' + suffix, exact: true }).click()
  await page.getByRole('button', { name: '确认删除', exact: true }).click()
  await expect(tagPanel).not.toContainText('测试标签' + suffix)
  const original = (await (await request.get('/api/site')).json()).data
  await page.getByRole('navigation', { name: '后台导航' }).getByRole('link', { name: '站点设置' }).click()
  await page.getByLabel('首页欢迎短句').fill('真实浏览器设置验证 ' + suffix)
  await page.getByRole('button', { name: '保存站点设置' }).click()
  await expect
    .poll(async () => (await (await request.get('/api/site')).json()).data.subtitle)
    .toBe('真实浏览器设置验证 ' + suffix)
  await page.getByLabel('首页欢迎短句').fill(original.subtitle)
  await page.getByRole('button', { name: '保存站点设置' }).click()
  await expect
    .poll(async () => (await (await request.get('/api/site')).json()).data.subtitle)
    .toBe(original.subtitle)
})
test('封面上传解码重编码后可从真实资源地址读取', async ({ page, request }) => {
  await admin(page)
  await page.getByRole('button', { name: '新建文章' }).click()
  const upload = page.waitForResponse(
    (r) => r.url().endsWith('/api/admin/uploads') && r.request().method() === 'POST',
  )
  await page.getByLabel('上传封面', { exact: true }).setInputFiles('public/art/mansion-moonlight.png')
  const result = (await (await upload).json()).data
  expect(result.url).toMatch(/^\/uploads\/[a-f0-9-]+\.jpg$/)
  await expect(page.getByLabel('封面地址')).toHaveValue(result.url)
  const image = await request.get(result.url)
  expect(image.status()).toBe(200)
  expect(image.headers()['content-type']).toContain('image/jpeg')
})
test('公开页面、图片查看、隐藏房间与404', async ({ page }) => {
  for (const [path, title] of [
    ['/articles', '魔法图书馆'],
    ['/archive', '时间回廊'],
    ['/categories', '藏书分区'],
    ['/tags', '标签星图'],
    ['/about', '馆主会客室'],
    ['/friends', '友人的庭院'],
  ]) {
    await page.goto(path)
    await expect(page.locator('.page-intro h1')).toContainText(title)
  }
  await page.goto('/articles/a-light-for-every-story')
  await page.locator('.prose img').first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: '关闭对话框' }).click()
  await page.goto('/secret')
  for (let i = 1; i <= 7; i++) await page.getByRole('button', { name: `点亮第${i}枚水晶` }).click()
  await expect(page.locator('.crystal-status')).toContainText('七枚星晶都亮了')
  await page.screenshot({ path: '../docs/screenshots/06-secret.png', fullPage: true, animations: 'disabled' })
  await page.goto('/lost')
  await expect(page.locator('.secret-page h1')).toContainText('你也走到这里了吗')
})
test('退出后服务端拒绝原令牌', async ({ page, request }) => {
  await admin(page, 'overview')
  const old = await page.evaluate(() => sessionStorage.getItem('scarlet-token'))
  await page.getByRole('button', { name: '退出管理' }).click()
  await expect(page.getByRole('button', { name: '进入书房' })).toBeVisible()
  expect((await request.get('/api/auth/me', { headers: { Authorization: `Bearer ${old}` } })).status()).toBe(
    401,
  )
})
