<script setup lang="ts">
import { ArrowUpRight, Clock3 } from 'lucide-vue-next'
import { date } from '../api'
import type { Article } from '../types'
defineProps<{ article: Article; horizontal?: boolean }>()
</script>
<template>
  <article class="article-card" :class="{ horizontal }">
    <RouterLink :to="`/articles/${article.slug}`" class="cover-link"
      ><img
        :src="article.cover || '/art/library.svg'"
        :alt="article.title + '封面'"
        loading="lazy"
        width="700"
        height="420" /><span class="cover-category">{{ article.categoryName }}</span
      ><ArrowUpRight class="cover-arrow" :size="20"
    /></RouterLink>
    <div class="article-card-copy">
      <div class="article-meta">
        <span>{{ date(article.publishedAt) }}</span
        ><i></i><span>{{ article.tags[0]?.name || '随笔' }}</span>
      </div>
      <h3>
        <RouterLink :to="`/articles/${article.slug}`">{{ article.title }}</RouterLink>
      </h3>
      <p>{{ article.excerpt }}</p>
      <footer>
        <span v-for="tag in article.tags.slice(0, 2)" :key="tag.id"># {{ tag.name }}</span
        ><RouterLink :to="`/articles/${article.slug}`">翻开这页 <ArrowUpRight :size="13" /></RouterLink>
      </footer>
    </div>
  </article>
</template>
