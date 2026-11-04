<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowUpRight, Link2, Send } from 'lucide-vue-next'
import { api, send } from '../api'
import type { Friend } from '../types'
import { useSite } from '../store'
import PageIntro from '../components/PageIntro.vue'
import Modal from '../components/Modal.vue'
const site = useSite(),
  friends = ref<Friend[]>([]),
  error = ref(''),
  apply = ref(false),
  name = ref(''),
  url = ref(''),
  description = ref(''),
  busy = ref(false)
onMounted(async () => {
  try {
    friends.value = await api<Friend[]>('/friends')
  } catch (e) {
    error.value = (e as Error).message
  }
})
async function submit() {
  busy.value = true
  try {
    await send('/friends/apply', 'POST', { name: name.value, url: url.value, description: description.value })
    apply.value = false
    site.notify('友链申请已寄出，审核后会公开')
    name.value = ''
    url.value = ''
    description.value = ''
  } catch (e) {
    site.notify((e as Error).message, true)
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <div class="wrap page-wrap">
    <PageIntro
      eyebrow="BEYOND THE MANSION · FRIENDS"
      title="友人的庭院"
      description="从这里出发，去拜访那些同样认真记录的世界。"
    />
    <p v-if="error" class="state-box" role="alert">{{ error }}</p>
    <div class="friend-grid">
      <a v-for="f in friends" :key="f.id" :href="f.url" target="_blank" rel="noopener noreferrer"
        ><span class="friend-initial">{{ f.name.slice(0, 1) }}</span>
        <div>
          <h2>{{ f.name }}</h2>
          <p>{{ f.description }}</p>
          <small>{{ f.url.replace(/^https?:\/\//, '').replace(/\/$/, '') }}</small>
        </div>
        <ArrowUpRight :size="20" /></a
      ><button class="friend-apply" @click="apply = true">
        <Link2 :size="30" :stroke-width="1" />
        <div>
          <h2>让两个世界，有一枚共同的书签。</h2>
          <p>欢迎留下你的站点，申请成为庭院的朋友。</p>
        </div>
        <ArrowUpRight :size="20" />
      </button>
    </div>
    <div class="friend-guidelines">
      <span class="eyebrow">A NOTE FOR NEW FRIENDS</span>
      <p>欢迎原创、持续更新、内容友善的个人站点。申请会经过审核，暂不收录广告站与非法内容。</p>
    </div>
    <Modal v-if="apply" title="寄出一份友链邀请" @close="!busy && (apply = false)"
      ><form class="stack-form" @submit.prevent="submit">
        <label>站点名称<input v-model="name" maxlength="60" required /></label
        ><label
          >站点地址<input v-model="url" type="url" maxlength="300" placeholder="https://…" required /></label
        ><label>一句介绍<textarea v-model="description" required maxlength="200" rows="3"></textarea></label
        ><button class="button primary" :disabled="busy">
          {{ busy ? '正在寄出…' : '提交申请' }}<Send :size="15" />
        </button></form
    ></Modal>
  </div>
</template>
