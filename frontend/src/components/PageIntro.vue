<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; description: string; symbol?: string }>()
</script>
<template>
  <section class="page-intro">
    <span class="eyebrow">{{ eyebrow }}</span>
    <h1>{{ title }}<span>✧</span></h1>
    <p>{{ description }}</p>
    <span v-if="symbol" class="intro-mark" aria-hidden="true">{{ symbol }}</span>
    <div class="ornament-line"><span></span><i>◇</i><span></span></div>
  </section>
</template>
