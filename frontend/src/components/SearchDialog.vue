<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { Search, ArrowUpRight } from 'lucide-vue-next'
import { api } from '../api'
import type { Article, Page } from '../types'
import Modal from './Modal.vue'
import CharacterPortrait from './CharacterPortrait.vue'
const emit = defineEmits<{ close: [] }>()
const query = ref(''),
  items = ref<Article[]>([]),
  busy = ref(false),
  error = ref('')
let timer: ReturnType<typeof setTimeout>,
  version = 0
watch(query, (value) => {
  clearTimeout(timer)
  const current = ++version
  if (!value.trim()) {
    items.value = []
    busy.value = false
    return
  }
  busy.value = true
  timer = setTimeout(async () => {
    try {
      const result = await api<Page<Article>>('/articles?q=' + encodeURIComponent(value) + '&size=6')
      if (current === version) {
        items.value = result.items
        error.value = ''
      }
    } catch (e) {
      if (current === version) error.value = (e as Error).message
    } finally {
      if (current === version) busy.value = false
    }
  }, 250)
})
onBeforeUnmount(() => {
  clearTimeout(timer)
  version++
})
</script>
<template>
  <Modal title="小恶魔 · 藏书检索" wide @close="emit('close')"
    ><div class="search-welcome">
      <CharacterPortrait character="koakuma" variant="search" />
      <div>
        <span class="eyebrow">A LITTLE HELP FROM KOAKUMA</span>
        <p>「想找哪一页？我来帮你翻翻。」</p>
      </div>
    </div>
    <div class="search-box">
      <Search :size="20" /><input
        v-model="query"
        aria-label="搜索藏书"
        placeholder="书名、内容或一点模糊的灵感…"
        maxlength="100"
      />
    </div>
    <p class="search-guide">在魔法图书馆已公开的藏书中寻找文字。</p>
    <div v-if="busy" class="state-box">正在翻找目录…</div>
    <p v-else-if="error" role="alert" class="form-error">{{ error }}</p>
    <div v-else-if="items.length" class="search-results">
      <RouterLink v-for="a in items" :key="a.id" :to="'/articles/' + a.slug" @click="emit('close')"
        ><div>
          <span>{{ a.categoryName }}</span>
          <h3>{{ a.title }}</h3>
          <p>{{ a.excerpt }}</p>
        </div>
        <ArrowUpRight :size="18"
      /></RouterLink>
    </div>
    <div v-else class="state-box">
      {{ query ? '没有找到这份藏书，换个关键词试试。' : '输入关键词，书页会为你展开。' }}
    </div></Modal
  >
</template>

<style scoped>
.search-welcome {
  display: flex;
  align-items: center;
  gap: 17px;
  margin: -6px 0 14px;
}
.search-welcome .eyebrow {
  font-size: 7px;
  letter-spacing: 1.7px;
  color: #b091b8;
}
.search-welcome p {
  font-family: var(--serif);
  font-size: 16px;
  color: #ddc9df;
  margin-top: 12px;
  line-height: 1.9;
}
@media (max-width: 700px) {
  .search-welcome {
    gap: 10px;
  }
  .search-welcome p {
    font-size: 13px;
  }
  .search-welcome .eyebrow {
    font-size: 6px;
    letter-spacing: 1px;
  }
}
</style>
