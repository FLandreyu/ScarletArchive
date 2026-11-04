<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { X } from 'lucide-vue-next'
defineProps<{ title: string; wide?: boolean }>()
const emit = defineEmits<{ close: [] }>()
const box = ref<HTMLElement>()
const old = document.activeElement as HTMLElement
function key(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
  if (e.key !== 'Tab') return
  const list = box.value?.querySelectorAll<HTMLElement>(
    'button:not(:disabled),a[href],input,textarea,select,[tabindex="0"]',
  )
  if (!list?.length) return
  const a = list[0],
    b = list[list.length - 1]
  if (e.shiftKey && document.activeElement === a) {
    e.preventDefault()
    b.focus()
  } else if (!e.shiftKey && document.activeElement === b) {
    e.preventDefault()
    a.focus()
  }
}
onMounted(() => {
  document.addEventListener('keydown', key)
  box.value?.querySelector<HTMLElement>('input,button')?.focus()
  document.body.style.overflow = 'hidden'
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', key)
  document.body.style.overflow = ''
  old?.focus()
})
</script>
<template>
  <Teleport to="body"
    ><div class="modal-shade" @click.self="emit('close')">
      <section
        ref="box"
        class="modal"
        :class="{ wide }"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <header>
          <h2 id="modal-title">{{ title }}</h2>
          <button class="icon-btn" aria-label="关闭对话框" @click="emit('close')"><X :size="20" /></button>
        </header>
        <slot />
      </section></div
  ></Teleport>
</template>
