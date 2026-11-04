<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import { Check, Trash2, Plus, Pencil, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { api, send, date } from '../../api'
import type { Message, Friend, Page } from '../../types'
import { useSite } from '../../store'
import Modal from '../Modal.vue'
const props = defineProps<{ kind: 'guestbook' | 'friends' }>(),
  site = useSite(),
  messages = ref<Message[]>([]),
  friends = ref<Friend[]>([]),
  total = ref(0),
  page = ref(1),
  loading = ref(true),
  error = ref(''),
  pending = ref<number | null>(null),
  opened = ref(false),
  editId = ref<number | null>(null),
  busy = ref(false),
  form = reactive({ name: '', url: '', description: '' })
async function load() {
  loading.value = true
  try {
    if (props.kind === 'guestbook') {
      const d = await api<Page<Message>>('/admin/guestbook?page=' + page.value)
      messages.value = d.items
      total.value = d.total
    } else friends.value = await api<Friend[]>('/admin/friends')
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
watch(
  () => props.kind,
  () => {
    page.value = 1
    void load()
  },
  { immediate: true },
)
async function moderate(id: number, status: string) {
  try {
    await send(`/admin/${props.kind}/${id}`, 'PATCH', { status })
    site.notify(status === 'APPROVED' ? '审核通过，前台已公开' : '已撤回到待审核')
    await load()
  } catch (e) {
    site.notify((e as Error).message, true)
  }
}
async function remove() {
  if (!pending.value) return
  try {
    await send(`/admin/${props.kind}/${pending.value}`, 'DELETE')
    pending.value = null
    await load()
    site.notify('记录已删除')
  } catch (e) {
    site.notify((e as Error).message, true)
  }
}
function edit(f?: Friend) {
  editId.value = f?.id ?? null
  Object.assign(form, { name: f?.name || '', url: f?.url || '', description: f?.description || '' })
  opened.value = true
}
async function save() {
  busy.value = true
  try {
    await send(
      '/admin/friends' + (editId.value === null ? '' : '/' + editId.value),
      editId.value === null ? 'POST' : 'PUT',
      form,
    )
    opened.value = false
    site.notify('友链已保存')
    await load()
  } catch (e) {
    site.notify((e as Error).message, true)
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <header class="admin-section-heading">
    <div>
      <h2>{{ kind === 'guestbook' ? '庭院来信审核' : '友情链接管理' }}</h2>
      <p>待审核内容不会展示在前台，确认后再公开。</p>
    </div>
    <button v-if="kind === 'friends'" class="button primary" @click="edit()">
      <Plus :size="15" />新增友链
    </button>
  </header>
  <div v-if="loading || error" class="state-box">{{ error || '正在收集记录…' }}</div>
  <div v-else-if="kind === 'guestbook'" class="moderation-list">
    <article v-for="m in messages" :key="m.id">
      <header>
        <h3>
          {{ m.nickname }}<small>{{ date(m.createdAt) }}</small>
        </h3>
        <span class="badge" :class="m.status">{{ m.status === 'APPROVED' ? '已公开' : '待审核' }}</span>
      </header>
      <p>{{ m.content }}</p>
      <footer>
        <button
          class="button outline small"
          @click="moderate(m.id, m.status === 'APPROVED' ? 'PENDING' : 'APPROVED')"
        >
          <Check :size="14" />{{ m.status === 'APPROVED' ? '撤回公开' : '审核通过' }}</button
        ><button class="icon-btn danger" :aria-label="'删除来信' + m.id" @click="pending = m.id">
          <Trash2 :size="15" />
        </button>
      </footer>
    </article>
    <p v-if="!messages.length" class="state-box">还没有收到来信。</p>
    <div v-if="total > 10" class="pagination">
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
        <ChevronLeft :size="17" /></button
      ><span>{{ page }} / {{ Math.ceil(total / 10) }}</span
      ><button
        class="icon-btn"
        aria-label="下一页"
        :disabled="page >= Math.ceil(total / 10)"
        @click="
          () => {
            page++
            load()
          }
        "
      >
        <ChevronRight :size="17" />
      </button>
    </div>
  </div>
  <div v-else class="moderation-list">
    <article v-for="f in friends" :key="f.id">
      <header>
        <h3>
          {{ f.name }}<small>{{ f.url }}</small>
        </h3>
        <span class="badge" :class="f.status">{{ f.status === 'APPROVED' ? '已公开' : '待审核' }}</span>
      </header>
      <p>{{ f.description }}</p>
      <footer>
        <button
          class="button outline small"
          @click="moderate(f.id, f.status === 'APPROVED' ? 'PENDING' : 'APPROVED')"
        >
          <Check :size="14" />{{ f.status === 'APPROVED' ? '撤回公开' : '审核通过' }}</button
        ><button class="icon-btn" :aria-label="'编辑' + f.name" @click="edit(f)"><Pencil :size="15" /></button
        ><button class="icon-btn danger" :aria-label="'删除友链' + f.id" @click="pending = f.id">
          <Trash2 :size="15" />
        </button>
      </footer>
    </article>
  </div>
  <Modal v-if="pending !== null" title="删除这条记录？" @close="pending = null"
    ><p class="dialog-copy">删除后将无法在前台或后台继续查看。</p>
    <button class="button danger-button" @click="remove">确认删除</button></Modal
  ><Modal v-if="opened" title="编辑友情链接" @close="opened = false"
    ><form class="stack-form" @submit.prevent="save">
      <label>站点名称<input v-model="form.name" maxlength="60" required /></label
      ><label>站点地址<input v-model="form.url" type="url" maxlength="300" required /></label
      ><label>简介<textarea v-model="form.description" rows="3" maxlength="200" required></textarea></label
      ><button class="button primary" :disabled="busy">{{ busy ? '保存中…' : '保存友链' }}</button>
    </form></Modal
  >
</template>
