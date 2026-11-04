<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Search,
  Menu,
  X,
  SunMoon,
  Clock3,
  ArrowUp,
  ArrowUpRight,
  Sparkles,
  CheckCircle2,
  AlertCircle,
} from 'lucide-vue-next'
import { useSite } from './store'
import SearchDialog from './components/SearchDialog.vue'
const site = useSite(),
  route = useRoute(),
  router = useRouter()
const menu = ref(false),
  search = ref(false),
  scrolled = ref(false),
  turn = ref(false),
  loadError = ref('')
let turnTimer: ReturnType<typeof setTimeout>
const nav = [
  ['/', '馆邸首页'],
  ['/articles', '魔法图书馆'],
  ['/archive', '时间回廊'],
  ['/guestbook', '庭院来信'],
  ['/about', '关于馆主'],
]
function scroll() {
  scrolled.value = scrollY > 80
}
function backTop() {
  window.scrollTo({ top: 0, behavior: site.motion ? 'smooth' : 'instant' })
}
function key(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    search.value = !search.value
  }
  if (e.key === 'Escape') menu.value = false
}
watch(
  () => route.fullPath,
  () => {
    menu.value = false
    if (site.motion) {
      turn.value = true
      clearTimeout(turnTimer)
      turnTimer = setTimeout(() => (turn.value = false), 450)
    }
  },
)
watch(
  () => site.motion,
  (value) => document.documentElement.classList.toggle('motion-off', !value),
  { immediate: true },
)
onMounted(() => {
  site.load().catch((e) => (loadError.value = (e as Error).message))
  window.addEventListener('scroll', scroll, { passive: true })
  window.addEventListener('keydown', key)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', scroll)
  window.removeEventListener('keydown', key)
  clearTimeout(turnTimer)
})
</script>
<template>
  <a href="#main" class="skip-link">跳转到正文</a>
  <div class="ambient-grain" aria-hidden="true"></div>
  <header class="site-header" :class="{ scrolled, home: route.path === '/' }">
    <div class="nav-wrap">
      <RouterLink to="/" class="brand"
        ><img src="/art/crest.svg" alt="" width="43" height="43" /><span
          >{{ site.site.title }}<small>SCARLET ARCHIVE</small></span
        ></RouterLink
      >
      <nav class="desktop-nav" aria-label="主导航">
        <RouterLink
          v-for="n in nav"
          :key="n[0]"
          :to="n[0]"
          :class="{
            selected: route.path === n[0] || (n[0] === '/articles' && route.path.startsWith('/articles/')),
          }"
          >{{ n[1] }}</RouterLink
        >
      </nav>
      <div class="nav-tools">
        <button class="icon-btn search-trigger" aria-label="打开藏书搜索" @click="search = true">
          <Search :size="18" /><kbd>⌘ K</kbd></button
        ><span class="nav-separator"></span
        ><button
          class="icon-btn motion-toggle"
          :aria-pressed="site.motion"
          :aria-label="site.motion ? '关闭动态效果' : '开启动画效果'"
          @click="site.toggleMotion"
        >
          <SunMoon :size="18" /></button
        ><button
          class="icon-btn mobile-toggle"
          :aria-expanded="menu"
          aria-label="打开导航菜单"
          @click="menu = !menu"
        >
          <X v-if="menu" :size="22" /><Menu v-else :size="22" />
        </button>
      </div>
    </div>
    <nav v-if="menu" class="mobile-menu" aria-label="手机导航">
      <RouterLink
        v-for="n in [...nav, ['/friends', '友人的庭院'], ['/categories', '藏书分类'], ['/tags', '标签星图']]"
        :key="n[0]"
        :to="n[0]"
        >{{ n[1] }}<ArrowUpRight :size="16"
      /></RouterLink>
    </nav>
  </header>
  <div class="clock-transition" :class="{ turn }" aria-hidden="true">
    <Clock3 :size="33" :stroke-width="1" />
  </div>
  <main id="main" :class="{ 'admin-main': route.path.startsWith('/admin') }">
    <p v-if="loadError" class="global-error" role="alert">
      {{ loadError }}
      <button
        @click="
          site
            .load()
            .then(() => (loadError = ''))
            .catch((e) => site.notify(e.message, true))
        "
      >
        重新连接
      </button>
    </p>
    <RouterView />
  </main>
  <footer v-if="!route.path.startsWith('/admin')" class="site-footer">
    <div class="footer-top wrap">
      <RouterLink class="brand footer-brand" to="/"
        ><img src="/art/crest.svg" alt="" width="40" height="40" /><span
          >{{ site.site.title }}<small>SCARLET ARCHIVE</small></span
        ></RouterLink
      >
      <p>文字是小小的魔法。<br /><span>愿每一次翻页，都有新的相遇。</span></p>
      <div>
        <RouterLink to="/friends">友情链接</RouterLink><RouterLink to="/tags">标签星图</RouterLink
        ><a href="/sitemap.xml">站点地图</a><RouterLink to="/admin">馆邸管理</RouterLink>
      </div>
      <button
        class="secret-entry"
        aria-label="探索星晶房间"
        title="似乎有什么在闪烁…"
        @click="router.push('/secret')"
      >
        <Sparkles :size="23" :stroke-width="1" />
      </button>
    </div>
    <div class="footer-bottom wrap">
      <span
        >© {{ new Date().getFullYear() }} {{ site.site.owner || 'Scarlet Archive' }} · Built with care & a
        little magic.</span
      >
      <p>非官方粉丝创作 · 东方Project角色及世界观归上海爱丽丝幻乐团所有，与官方无关联。</p>
    </div>
  </footer>
  <button
    v-if="scrolled && !route.path.startsWith('/admin')"
    class="back-top icon-btn"
    aria-label="返回顶部"
    @click="backTop"
  >
    <ArrowUp :size="19" />
  </button>
  <SearchDialog v-if="search" @close="search = false" />
  <Transition name="toast"
    ><div
      v-if="site.toast"
      class="toast"
      :class="{ error: site.toast.error }"
      :role="site.toast.error ? 'alert' : 'status'"
    >
      <AlertCircle v-if="site.toast.error" :size="18" /><CheckCircle2 v-else :size="18" /><span>{{
        site.toast.text
      }}</span
      ><button aria-label="关闭提示" class="icon-btn" @click="site.toast = null">
        <X :size="16" />
      </button></div
  ></Transition>
</template>
