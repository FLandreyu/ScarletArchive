<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  LockKeyhole,
  ArrowRight,
  LayoutDashboard,
  FileText,
  Tags,
  MessageSquare,
  Link2,
  Settings2,
  LogOut,
  ArrowUpRight,
} from 'lucide-vue-next'
import { api, send } from '../api'
import { useSite } from '../store'
import ArticleManager from '../components/admin/ArticleManager.vue'
import TaxonomyManager from '../components/admin/TaxonomyManager.vue'
import CommunityManager from '../components/admin/CommunityManager.vue'
import SiteSettings from '../components/admin/SiteSettings.vue'
const site = useSite(),
  route = useRoute(),
  router = useRouter(),
  logged = ref(false),
  booting = ref(true),
  username = ref('admin'),
  password = ref(''),
  busy = ref(false),
  error = ref(''),
  stats = ref<{ published: number; drafts: number; messages: number; pendingMessages: number }>()
const section = computed(() => String(route.params.section || 'overview'))
const nav = [
  { id: 'overview', text: '馆邸概览', icon: LayoutDashboard },
  { id: 'articles', text: '文章管理', icon: FileText },
  { id: 'taxonomy', text: '分类标签', icon: Tags },
  { id: 'guestbook', text: '留言审核', icon: MessageSquare },
  { id: 'friends', text: '友情链接', icon: Link2 },
  { id: 'settings', text: '站点设置', icon: Settings2 },
]
async function loadStats() {
  try {
    stats.value = await api('/admin/stats')
  } catch (e) {
    site.notify((e as Error).message, true)
  }
}
async function login() {
  busy.value = true
  error.value = ''
  try {
    const result = await send<{ token: string }>('/auth/login', 'POST', {
      username: username.value,
      password: password.value,
    })
    sessionStorage.setItem('scarlet-token', result.token)
    logged.value = true
    password.value = ''
    await loadStats()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
async function logout() {
  try {
    await send('/auth/logout', 'POST')
    sessionStorage.removeItem('scarlet-token')
    logged.value = false
    site.notify('已退出，原登录令牌已撤销')
  } catch (e) {
    site.notify((e as Error).message, true)
  }
}
function expired() {
  logged.value = false
  site.notify('管理登录已失效，请重新登录', true)
}
onMounted(async () => {
  window.addEventListener('scarlet-expired', expired)
  if (sessionStorage.getItem('scarlet-token'))
    try {
      await api('/auth/me')
      logged.value = true
      await loadStats()
    } catch {
      sessionStorage.removeItem('scarlet-token')
    }
  booting.value = false
})
onBeforeUnmount(() => window.removeEventListener('scarlet-expired', expired))
</script>
<template>
  <div v-if="booting" class="state-box page-wrap">正在核对馆邸通行证…</div>
  <div v-else-if="!logged" class="admin-login">
    <div class="admin-login-art">
      <img src="/art/crest.svg" alt="" width="85" height="85" /><span class="eyebrow">THE MANSION STUDY</span>
      <h1>让故事，<br />从这里继续。</h1>
      <p>写下新的篇章，照看庭院的来信，<br />让这座馆邸始终亮着一盏灯。</p>
    </div>
    <form @submit.prevent="login">
      <span class="eyebrow">AUTHORIZED KEEPER ONLY</span>
      <h2>馆邸管理</h2>
      <p>请出示你的管理员通行证。</p>
      <label>管理员账号<input v-model="username" autocomplete="username" required maxlength="32" /></label
      ><label
        >管理员密码<input
          v-model="password"
          type="password"
          autocomplete="current-password"
          required
          maxlength="64"
      /></label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="button primary" :disabled="busy">
        {{ busy ? '正在核验…' : '进入书房' }}<ArrowRight :size="16" /></button
      ><small>本地初始账号：admin / Scarlet123!<br />正式部署请用 ADMIN_PASSWORD 配置自己的密码。</small>
    </form>
  </div>
  <div v-else class="admin-shell wrap">
    <aside class="admin-nav">
      <span class="eyebrow">MANSION CONTROL</span>
      <h2>馆邸书房</h2>
      <nav aria-label="后台导航">
        <RouterLink v-for="n in nav" :key="n.id" :to="'/admin/' + n.id" :class="{ active: section === n.id }"
          ><component :is="n.icon" :size="17" />{{ n.text }}</RouterLink
        >
      </nav>
      <button class="text-link" @click="logout"><LogOut :size="16" />退出管理</button>
    </aside>
    <div class="admin-content">
      <template v-if="section === 'overview'"
        ><header class="admin-section-heading">
          <div>
            <span class="eyebrow">WELCOME BACK, KEEPER.</span>
            <h1>今晚，也写一点什么吧。</h1>
            <p>文字、来信与新的相遇，都在这里。</p>
          </div>
          <RouterLink class="button primary" to="/admin/articles"
            >管理藏书 <ArrowUpRight :size="16"
          /></RouterLink>
        </header>
        <div v-if="stats" class="admin-stat-grid">
          <div>
            <span>已公开藏书</span><strong>{{ stats.published }}</strong
            ><small>PUBLISHED CHAPTERS</small>
          </div>
          <div>
            <span>正在写的手稿</span><strong>{{ stats.drafts }}</strong
            ><small>PRIVATE DRAFTS</small>
          </div>
          <div>
            <span>庭院所有来信</span><strong>{{ stats.messages }}</strong
            ><small>LETTERS RECEIVED</small>
          </div>
          <div>
            <span>等待审核</span><strong>{{ stats.pendingMessages }}</strong
            ><small>WAITING AT THE GATE</small>
          </div>
        </div>
        <section class="admin-welcome">
          <img src="/art/library.svg" alt="魔法书装饰" width="700" height="420" />
          <div>
            <span class="eyebrow">A LITTLE ORDER, A LITTLE MAGIC.</span>
            <h2>一份好手记，<br />从一个小小的想法开始。</h2>
            <p>先保存草稿，慢慢打磨。确认发布后，文字才会出现在图书馆、归档和站点地图中。</p>
            <RouterLink class="text-link" to="/admin/articles"
              >前往文章管理 <ArrowRight :size="16"
            /></RouterLink>
          </div></section></template
      ><ArticleManager v-else-if="section === 'articles'" /><TaxonomyManager
        v-else-if="section === 'taxonomy'"
      /><CommunityManager
        v-else-if="section === 'guestbook' || section === 'friends'"
        :kind="section"
      /><SiteSettings v-else-if="section === 'settings'" />
      <div v-else class="state-box">
        <p>这间书房暂未开放。</p>
        <RouterLink to="/admin">回到概览</RouterLink>
      </div>
    </div>
  </div>
</template>
