<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { markdown } from '../markdown'
import { useSite } from '../store'
import Modal from './Modal.vue'
const props = defineProps<{ source: string }>()
const emit = defineEmits<{ toc: [value: { id: string; text: string; level: number }[]] }>()
const rendered = computed(() => markdown(props.source))
const zoom = ref<{ src: string; alt: string } | null>(null)
const site = useSite()
watch(
  () => rendered.value.toc,
  (t) => emit('toc', t),
  { immediate: true },
)
async function click(e: MouseEvent) {
  const el = e.target as HTMLElement
  if (el.closest('[data-copy-code]')) {
    const code = el.closest('.code-wrap')?.querySelector('code')?.textContent || ''
    try {
      await navigator.clipboard.writeText(code)
      site.notify('代码已复制')
    } catch {
      site.notify('复制失败，请手动选择代码', true)
    }
  }
  if (el instanceof HTMLImageElement) zoom.value = { src: el.currentSrc || el.src, alt: el.alt }
}
</script>
<template>
  <div class="prose" @click="click" v-html="rendered.html"></div>
  <Modal v-if="zoom" :title="zoom.alt || '查看图片'" wide @close="zoom = null"
    ><img class="zoom-image" :src="zoom.src" :alt="zoom.alt"
  /></Modal>
</template>
