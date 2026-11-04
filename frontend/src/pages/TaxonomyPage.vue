<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { BookOpen, ArrowUpRight, Hash } from 'lucide-vue-next'
import { useSite } from '../store'
import PageIntro from '../components/PageIntro.vue'
const site = useSite(),
  route = useRoute(),
  tags = computed(() => route.params.kind === 'tags')
</script>
<template>
  <div class="wrap page-wrap">
    <PageIntro
      :eyebrow="tags ? 'KOAKUMA’S STAR INDEX · TAGS' : 'PATCHOULI’S BOOKSHELVES · CATEGORIES'"
      :title="tags ? '标签星图' : '藏书分区'"
      :description="tags ? '一点灵感，也可能通往一整片星空。' : '循着自己的好奇，找到想停留的书架。'"
      symbol="✦"
    />
    <div v-if="tags" class="tag-constellation">
      <RouterLink
        v-for="(t, i) in site.tags"
        :key="t.id"
        :to="{ path: '/articles', query: { tag: t.slug } }"
        :style="{
          '--tag-size': 16 + Math.min(12, t.articleCount * 2) + 'px',
          '--rotation': (i % 2 ? 2 : -2) + 'deg',
        }"
        ><Hash :size="17" />{{ t.name }}<small>{{ t.articleCount }}</small
        ><ArrowUpRight :size="15"
      /></RouterLink>
    </div>
    <div v-else class="taxonomy-grid">
      <RouterLink
        v-for="(c, i) in site.categories"
        :key="c.id"
        :to="{ path: '/articles', query: { category: c.slug } }"
        ><span class="shelf-label">SHELF / 0{{ i + 1 }}</span
        ><BookOpen :size="43" :stroke-width="1" />
        <h2>{{ c.name }}</h2>
        <p>{{ c.description }}</p>
        <footer>
          <span>{{ c.articleCount }} 份藏书</span><ArrowUpRight :size="18" /></footer
      ></RouterLink>
    </div>
  </div>
</template>
