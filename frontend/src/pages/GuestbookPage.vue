<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Send, Flower2, ChevronLeft, ChevronRight, ShieldCheck } from 'lucide-vue-next'
import { api, send, date } from '../api'
import type { Message, Page } from '../types'
import { useSite } from '../store'
import PageIntro from '../components/PageIntro.vue'
const site = useSite(),
  data = ref<Page<Message>>({ items: [], total: 0, page: 1, size: 8 }),
  page = ref(1),
  nickname = ref(''),
  content = ref(''),
  website = ref(''),
  busy = ref(false),
  loading = ref(true),
  error = ref('')
async function load() {
  loading.value = true
  try {
    data.value = await api<Page<Message>>(`/guestbook?page=${page.value}&size=8`)
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
async function submit() {
  busy.value = true
  try {
    await send('/guestbook', 'POST', {
      nickname: nickname.value,
      content: content.value,
      website: website.value,
    })
    site.notify('留言已交给美铃，审核后会在庭院公开')
    content.value = ''
  } catch (e) {
    site.notify((e as Error).message, true)
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>
<template>
  <div class="wrap page-wrap">
    <PageIntro
      eyebrow="MEILING'S ROSE GARDEN · GUESTBOOK"
      title="庭院来信"
      description="风从庭院经过，也许会捎来你的问候。"
      symbol="❧"
      character="meiling"
    />
    <div class="guestbook-layout">
      <section class="guest-form-panel">
        <span class="eyebrow">LEAVE A LITTLE KINDNESS</span>
        <h2>在离开之前，留句话吧。</h2>
        <p>一个问候、一点建议，或是今天的好心情。</p>
        <form @submit.prevent="submit">
          <label
            >怎么称呼你<input
              v-model="nickname"
              required
              maxlength="30"
              autocomplete="nickname"
              placeholder="庭院中的旅人" /></label
          ><label
            >想留下的话<textarea
              v-model="content"
              required
              minlength="3"
              maxlength="800"
              rows="6"
              placeholder="这里的月光，或许让你想起了什么？"
            ></textarea
            ><small>{{ content.length }} / 800</small></label
          ><label class="honeypot" aria-hidden="true"
            >Website<input v-model="website" tabindex="-1" autocomplete="off" /></label
          ><button class="button primary" :disabled="busy">
            {{ busy ? '正在寄出…' : '把来信交给美铃' }}<Send :size="15" />
          </button>
        </form>
        <div class="moderation-note">
          <ShieldCheck :size="16" />
          <p>来信会先经过审核再公开。请不要留下隐私、广告或不友善的内容。</p>
        </div>
      </section>
      <section class="guest-messages">
        <header>
          <h2>风里的回信</h2>
          <span>{{ data.total }} 封已公开来信</span>
        </header>
        <p v-if="loading || error" class="state-box">{{ error || '正在收集庭院里的来信…' }}</p>
        <div v-else-if="!data.items.length" class="state-box">
          <Flower2 :size="30" />
          <p>庭院正在等第一声问候。</p>
        </div>
        <article v-for="m in data.items" v-else :key="m.id" class="guest-letter">
          <header>
            <span class="letter-avatar">{{ m.nickname.slice(0, 1) }}</span>
            <div>
              <strong>{{ m.nickname }}</strong
              ><time>{{ date(m.createdAt) }}</time>
            </div>
            <span class="letter-stamp">❧</span>
          </header>
          <p>{{ m.content }}</p>
        </article>
        <div v-if="data.total > 8" class="pagination">
          <button
            class="icon-btn"
            aria-label="上一页"
            :disabled="page <= 1"
            @click="
              () => {
                page--
                load()
              }
            "
          >
            <ChevronLeft :size="18" /></button
          ><span>{{ page }} / {{ Math.ceil(data.total / 8) }}</span
          ><button
            class="icon-btn"
            aria-label="下一页"
            :disabled="page >= Math.ceil(data.total / 8)"
            @click="
              () => {
                page++
                load()
              }
            "
          >
            <ChevronRight :size="18" />
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
