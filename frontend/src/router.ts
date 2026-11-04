import { createRouter, createWebHistory } from 'vue-router'
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, saved) {
    if (saved) return saved
    if (to.hash) return { el: to.hash, top: 100, behavior: 'smooth' }
    return { top: 0 }
  },
  routes: [
    { path: '/', component: () => import('./pages/HomePage.vue'), meta: { title: '迎宾大厅' } },
    { path: '/articles', component: () => import('./pages/ArticlesPage.vue'), meta: { title: '魔法图书馆' } },
    {
      path: '/articles/:slug',
      component: () => import('./pages/ArticlePage.vue'),
      meta: { title: '阅读藏书' },
    },
    { path: '/archive', component: () => import('./pages/ArchivePage.vue'), meta: { title: '时间回廊' } },
    {
      path: '/:kind(categories|tags)',
      component: () => import('./pages/TaxonomyPage.vue'),
      meta: { title: '藏书索引' },
    },
    { path: '/about', component: () => import('./pages/AboutPage.vue'), meta: { title: '馆主会客室' } },
    { path: '/guestbook', component: () => import('./pages/GuestbookPage.vue'), meta: { title: '庭院来信' } },
    { path: '/friends', component: () => import('./pages/FriendsPage.vue'), meta: { title: '友人的庭院' } },
    { path: '/secret', component: () => import('./pages/SecretPage.vue'), meta: { title: '星晶房间' } },
    {
      path: '/admin/:section?',
      component: () => import('./pages/AdminPage.vue'),
      meta: { title: '馆邸管理' },
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('./pages/SecretPage.vue'),
      meta: { title: '迷路的访客' },
    },
  ],
})
router.afterEach((to) => {
  document.title = `${to.meta.title} · 红魔馆手记`
})
export default router
