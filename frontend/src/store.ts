import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from './api'
import type { Site, Taxonomy } from './types'
export const useSite = defineStore('site', () => {
  const site = ref<Site>({
      title: '红魔馆手记',
      subtitle: '在永夜与晨曦之间，收藏一点微小的光。',
      owner: '',
      about: '',
      github: '',
      email: '',
      interests: '',
      timeline: '',
      projects: '',
    }),
    categories = ref<Taxonomy[]>([]),
    tags = ref<Taxonomy[]>([])
  const motion = ref(
    localStorage.getItem('scarlet-motion') !== 'off' &&
      !matchMedia('(prefers-reduced-motion: reduce)').matches,
  )
  const toast = ref<{ text: string; error: boolean } | null>(null)
  let timer: ReturnType<typeof setTimeout> | undefined
  function notify(text: string, error = false) {
    toast.value = { text, error }
    clearTimeout(timer)
    timer = setTimeout(() => (toast.value = null), 5000)
  }
  function toggleMotion() {
    motion.value = !motion.value
    localStorage.setItem('scarlet-motion', motion.value ? 'on' : 'off')
  }
  async function load() {
    const [s, c, t] = await Promise.all([
      api<Site>('/site'),
      api<Taxonomy[]>('/categories'),
      api<Taxonomy[]>('/tags'),
    ])
    site.value = s
    categories.value = c
    tags.value = t
  }
  return { site, categories, tags, motion, toast, notify, toggleMotion, load }
})
