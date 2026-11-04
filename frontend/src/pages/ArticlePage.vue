<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowUpRight, Clock3, CalendarDays, BookOpen } from 'lucide-vue-next'
import { api, date } from '../api'
import type { Article, Page } from '../types'
import MarkdownView from '../components/MarkdownView.vue'
import ArticleCard from '../components/ArticleCard.vue'
const route = useRoute(),
  article = ref<Article | null>(null),
  related = ref<Article[]>([]),
  all = ref<Article[]>([]),
  toc = ref<{ id: string; text: string; level: number }[]>([]),
  error = ref(''),
  progress = ref(0)
let version = 0
const index = computed(() => all.value.findIndex((a) => a.id === article.value?.id)),
  previous = computed(() => all.value[index.value + 1]),
  next = computed(() => all.value[index.value - 1])
async function load() {
  const v = ++version
  article.value = null
  error.value = ''
  try {
    const a = await api<Article>('/articles/' + encodeURIComponent(String(route.params.slug)))
    if (v !== version) return
    article.value = a
    document.title = a.title + ' · 红魔馆手记'
    document.querySelector('meta[name="description"]')?.setAttribute('content', a.excerpt)
    document.querySelector('meta[property="og:title"]')?.setAttribute('content', a.title)
    document.querySelector('meta[property="og:description"]')?.setAttribute('content', a.excerpt)
    const [r, archive] = await Promise.all([
      api<Page<Article>>('/articles?category=' + a.categorySlug + '&size=4'),
      api<Article[]>('/archive'),
    ])
    if (v === version) {
      related.value = r.items.filter((x) => x.id !== a.id).slice(0, 2)
      all.value = archive
    }
  } catch (e) {
    if (v === version) error.value = (e as Error).message
  }
}
function scroll() {
  const el = document.querySelector('.reading-body')
  if (!el) {
    progress.value = 0
    return
  }
  const rect = el.getBoundingClientRect()
  progress.value = Math.max(0, Math.min(100, ((-rect.top + 110) / (rect.height - innerHeight + 150)) * 100))
}
watch(() => route.params.slug, load, { immediate: true })
onMounted(() => window.addEventListener('scroll', scroll, { passive: true }))
onBeforeUnmount(() => {
  window.removeEventListener('scroll', scroll)
  version++
})
</script>
<template>
  <div class="reading-progress" :style="{ width: progress + '%' }"></div>
  <div v-if="error" class="wrap state-box page-wrap">
    <h1>这份藏书尚未开放</h1>
    <p role="alert">{{ error }}</p>
    <RouterLink to="/articles" class="button outline">回到图书馆 <ArrowLeft :size="16" /></RouterLink>
  </div>
  <div v-else-if="!article" class="wrap state-box page-wrap">正在展开书页…</div>
  <article v-else class="reading-page wrap">
    <RouterLink to="/articles" class="text-link back-library"
      ><ArrowLeft :size="15" />返回魔法图书馆</RouterLink
    >
    <header class="reading-header">
      <RouterLink :to="{ path: '/articles', query: { category: article.categorySlug } }" class="eyebrow"
        >{{ article.categoryName }} / VOILE LIBRARY</RouterLink
      >
      <h1>{{ article.title }}</h1>
      <p>{{ article.excerpt }}</p>
      <div class="reading-meta">
        <span><CalendarDays :size="14" />{{ date(article.publishedAt) }}</span
        ><span
          ><Clock3 :size="14" />约 {{ Math.max(1, Math.ceil((article.body?.length || 0) / 400)) }} 分钟</span
        ><span><BookOpen :size="14" />{{ article.body?.length || 0 }} 字符</span>
      </div>
    </header>
    <img
      class="reading-cover"
      :src="article.cover || '/art/library.svg'"
      :alt="article.title + '封面'"
      width="1200"
      height="540"
    />
    <div class="reading-layout">
      <div class="reading-body">
        <MarkdownView :source="article.body || ''" @toc="toc = $event" />
        <div class="article-tags">
          <RouterLink
            v-for="t in article.tags"
            :key="t.id"
            :to="{ path: '/articles', query: { tag: t.slug } }"
            ># {{ t.name }}</RouterLink
          >
        </div>
        <div class="article-colophon">
          <img src="/art/crest.svg" alt="" width="38" height="38" />
          <p>这份手记已收录于红魔馆的书架。<br /><span>若文字给你带来一点灵感，欢迎在庭院留下回信。</span></p>
          <RouterLink to="/guestbook"><ArrowUpRight :size="20" /></RouterLink>
        </div>
        <nav class="adjacent-posts" aria-label="相邻文章">
          <RouterLink v-if="previous" :to="'/articles/' + previous.slug"
            ><span>← 上一篇</span><strong>{{ previous.title }}</strong></RouterLink
          ><RouterLink v-if="next" :to="'/articles/' + next.slug"
            ><span>下一篇 →</span><strong>{{ next.title }}</strong></RouterLink
          >
        </nav>
      </div>
      <aside class="reading-toc">
        <span class="eyebrow">ON THIS PAGE</span>
        <h3>本页的书签</h3>
        <a
          v-for="item in toc"
          :key="item.id"
          :href="'#' + item.id"
          :style="{ paddingLeft: (item.level - 2) * 13 + 'px' }"
          >{{ item.text }}</a
        >
        <div class="toc-note">
          「不必急着读完。<br />让文字陪你坐一会儿。」<span>PATCHOULI KNOWLEDGE</span>
        </div>
      </aside>
    </div>
    <section v-if="related.length" class="related-section">
      <div class="section-heading">
        <div>
          <span class="eyebrow">ANOTHER PAGE TO TURN</span>
          <h2>书架上，还有这些</h2>
        </div>
      </div>
      <div class="related-grid"><ArticleCard v-for="a in related" :key="a.id" :article="a" /></div>
    </section>
  </article>
</template>
