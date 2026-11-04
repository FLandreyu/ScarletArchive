<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, ChevronLeft, ChevronRight, X } from 'lucide-vue-next'
import { api } from '../api'
import type { Article, Page } from '../types'
import { useSite } from '../store'
import ArticleCard from '../components/ArticleCard.vue'
import PageIntro from '../components/PageIntro.vue'
const route = useRoute(),
  router = useRouter(),
  site = useSite(),
  query = ref(''),
  data = ref<Page<Article>>({ items: [], total: 0, page: 1, size: 9 }),
  loading = ref(true),
  error = ref('')
let version = 0
const cat = computed(() => String(route.query.category || '')),
  tag = computed(() => String(route.query.tag || '')),
  page = computed(() => Math.max(1, Number(route.query.page) || 1))
function filter(values: Record<string, string | undefined>) {
  void router.push({ path: '/articles', query: { ...route.query, page: undefined, ...values } })
}
async function load() {
  const current = ++version
  loading.value = true
  query.value = String(route.query.q || '')
  const p = new URLSearchParams({
    q: query.value,
    category: cat.value,
    tag: tag.value,
    sort: String(route.query.sort || 'newest'),
    page: String(page.value),
    size: '9',
  })
  try {
    const result = await api<Page<Article>>('/articles?' + p)
    if (current === version) {
      data.value = result
      error.value = ''
    }
  } catch (e) {
    if (current === version) error.value = (e as Error).message
  } finally {
    if (current === version) loading.value = false
  }
}
watch(() => route.query, load, { immediate: true })
</script>
<template>
  <div class="wrap page-wrap">
    <PageIntro
      eyebrow="PATCHOULI'S COLLECTION · VOILE LIBRARY"
      title="魔法图书馆"
      description="每一篇手记，都是书架上等待被翻开的一页。"
      symbol="☾"
      character="patchouli"
    />
    <div class="library-tools">
      <form class="inline-search" @submit.prevent="filter({ q: query || undefined })">
        <Search :size="17" /><input
          v-model="query"
          aria-label="搜索文章"
          maxlength="100"
          placeholder="寻找一份藏书…"
        /><button type="submit">检索</button>
      </form>
      <select
        :value="route.query.sort || 'newest'"
        aria-label="文章排序"
        @change="filter({ sort: ($event.target as HTMLSelectElement).value })"
      >
        <option value="newest">最近入馆</option>
        <option value="oldest">最早的手记</option>
      </select>
    </div>
    <div class="category-tabs">
      <button :class="{ active: !cat }" @click="filter({ category: undefined })">全部藏书</button
      ><button
        v-for="c in site.categories"
        :key="c.id"
        :class="{ active: cat === c.slug }"
        @click="filter({ category: c.slug })"
      >
        {{ c.name }}<small>{{ c.articleCount }}</small>
      </button>
    </div>
    <div class="library-count">
      <span>共 {{ data.total }} 份藏书</span
      ><button v-if="tag" class="filter-tag" @click="filter({ tag: undefined })">
        # {{ site.tags.find((t) => t.slug === tag)?.name || tag }}<X :size="13" /></button
      ><RouterLink to="/tags">小恶魔的标签索引 ↗</RouterLink>
    </div>
    <div v-if="loading" class="state-box">正在翻阅藏书目录…</div>
    <div v-else-if="error" class="state-box">
      <p role="alert">{{ error }}</p>
      <button class="button outline" @click="load">重试</button>
    </div>
    <div v-else-if="!data.items.length" class="state-box empty-library">
      <span>✧</span>
      <h3>书架的这一格还空着</h3>
      <p>换一个关键词，或看看其他分类吧。</p>
      <button class="button outline" @click="router.push('/articles')">回到全部藏书</button>
    </div>
    <div v-else class="article-grid"><ArticleCard v-for="a in data.items" :key="a.id" :article="a" /></div>
    <div v-if="data.total > 9" class="pagination">
      <button
        class="icon-btn"
        :disabled="page <= 1"
        aria-label="上一页"
        @click="router.push({ query: { ...route.query, page: page - 1 } })"
      >
        <ChevronLeft :size="18" /></button
      ><span>{{ page }} / {{ Math.ceil(data.total / 9) }}</span
      ><button
        class="icon-btn"
        :disabled="page >= Math.ceil(data.total / 9)"
        aria-label="下一页"
        @click="router.push({ query: { ...route.query, page: page + 1 } })"
      >
        <ChevronRight :size="18" />
      </button>
    </div>
  </div>
</template>
