<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ArrowUpRight, Clock3 } from 'lucide-vue-next'
import { api, date } from '../api'
import type { Article } from '../types'
import PageIntro from '../components/PageIntro.vue'
const items = ref<Article[]>([]),
  error = ref(''),
  loading = ref(true)
onMounted(async () => {
  try {
    items.value = await api<Article[]>('/archive')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
const years = computed(() => [...new Set(items.value.map((a) => a.publishedAt!.slice(0, 4)))])
const months = (year: string) => [
  ...new Set(
    items.value.filter((a) => a.publishedAt?.startsWith(year)).map((a) => a.publishedAt!.slice(0, 7)),
  ),
]
const inMonth = (month: string) => items.value.filter((a) => a.publishedAt?.startsWith(month))
</script>
<template>
  <div class="wrap page-wrap archive-page">
    <PageIntro
      eyebrow="SAKUYA'S CLOCK CORRIDOR · THE ARCHIVE"
      title="时间回廊"
      description="有些时光会远去，有些文字会替我们记得。"
      symbol="◷"
      character="sakuya"
    />
    <div class="archive-note">
      <Clock3 :size="27" :stroke-width="1" />
      <p>
        「时钟可以停下，故事仍会继续。」<small>咲夜替你整理了 {{ items.length }} 份公开手记。</small>
      </p>
    </div>
    <div v-if="loading || error" class="state-box">{{ error || '正在对齐时间的指针…' }}</div>
    <div v-else-if="!items.length" class="state-box">时间回廊正在等待第一份手记。</div>
    <section v-for="year in years" :key="year" class="archive-year">
      <h2>
        {{ year }}<span>{{ items.filter((a) => a.publishedAt?.startsWith(year)).length }} CHAPTERS</span>
      </h2>
      <div v-for="month in months(year)" :key="month" class="archive-month">
        <h3>{{ Number(month.slice(5)) }}<span>月</span></h3>
        <div>
          <RouterLink v-for="a in inMonth(month)" :key="a.id" :to="'/articles/' + a.slug"
            ><time>{{ date(a.publishedAt).slice(5) }}</time
            ><strong>{{ a.title }}</strong
            ><span>{{ a.categoryName }}</span
            ><ArrowUpRight :size="16"
          /></RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>
