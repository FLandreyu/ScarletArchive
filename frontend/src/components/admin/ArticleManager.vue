<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Pencil, Trash2, ChevronLeft, ChevronRight, ArrowUpRight } from 'lucide-vue-next'
import { api, send, date } from '../../api'
import type { Article, Page } from '../../types'
import { useSite } from '../../store'
import Modal from '../Modal.vue'
import ArticleEditor from './ArticleEditor.vue'
const site = useSite(),
  editing = ref(false),
  editId = ref<number | null>(null),
  data = ref<Page<Article>>({ items: [], total: 0, page: 1, size: 10 }),
  page = ref(1),
  query = ref(''),
  status = ref(''),
  busy = ref(false),
  error = ref(''),
  pending = ref<Article | null>(null)
async function load() {
  busy.value = true
  try {
    data.value = await api<Page<Article>>(
      `/admin/articles?page=${page.value}&q=${encodeURIComponent(query.value)}&status=${status.value}`,
    )
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
function edit(id: number | null) {
  editId.value = id
  editing.value = true
}
async function remove() {
  if (!pending.value) return
  try {
    await send('/admin/articles/' + pending.value.id, 'DELETE')
    pending.value = null
    site.notify('文章已删除')
    await load()
    await site.load()
  } catch (e) {
    site.notify((e as Error).message, true)
  }
}
async function saved() {
  editing.value = false
  await load()
  await site.load()
}
onMounted(load)
</script>
<template>
  <ArticleEditor v-if="editing" :id="editId" @close="editing = false" @saved="saved" />
  <section v-else>
    <header class="admin-section-heading">
      <div>
        <h2>文章与手稿</h2>
        <p>共 {{ data.total }} 篇，已公开与草稿分别保管。</p>
      </div>
      <button class="button primary" @click="edit(null)"><Plus :size="16" />新建文章</button>
    </header>
    <form
      class="admin-filters"
      @submit.prevent="
        () => {
          page = 1
          load()
        }
      "
    >
      <input v-model="query" maxlength="100" placeholder="搜索标题或正文" aria-label="后台搜索文章" /><select
        v-model="status"
        aria-label="筛选文章状态"
        @change="
          () => {
            page = 1
            load()
          }
        "
      >
        <option value="">全部状态</option>
        <option value="PUBLISHED">已发布</option>
        <option value="DRAFT">草稿</option></select
      ><button class="button outline small">检索</button>
    </form>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <div v-if="busy" class="state-box">正在整理藏书…</div>
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>文章</th>
            <th>分类</th>
            <th>状态</th>
            <th>日期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in data.items" :key="a.id">
            <td>
              <strong>{{ a.title }}</strong
              ><small>/{{ a.slug }} {{ a.featured ? '· 首页推荐' : '' }}</small>
            </td>
            <td>{{ a.categoryName }}</td>
            <td>
              <span class="badge" :class="a.status">{{ a.status === 'PUBLISHED' ? '已发布' : '草稿' }}</span>
            </td>
            <td>{{ date(a.updatedAt) }}</td>
            <td>
              <div class="table-actions">
                <RouterLink
                  v-if="a.status === 'PUBLISHED'"
                  :to="'/articles/' + a.slug"
                  class="icon-btn"
                  aria-label="查看文章"
                  ><ArrowUpRight :size="15" /></RouterLink
                ><button class="icon-btn" :aria-label="'编辑' + a.title" @click="edit(a.id)">
                  <Pencil :size="15" /></button
                ><button class="icon-btn danger" :aria-label="'删除' + a.title" @click="pending = a">
                  <Trash2 :size="15" />
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!data.items.length">
            <td colspan="5" class="empty-cell">没有符合条件的手稿</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="data.total > 10" class="pagination">
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
      ><span>{{ page }} / {{ Math.ceil(data.total / 10) }}</span
      ><button
        class="icon-btn"
        aria-label="下一页"
        :disabled="page >= Math.ceil(data.total / 10)"
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
  </section>
  <Modal v-if="pending" title="删除这份藏书？" @close="pending = null"
    ><p class="dialog-copy">
      {{ pending.title }}<br />删除后不会保留历史版本。确认不再需要这份手稿后再删除。
    </p>
    <button class="button danger-button" @click="remove">确认删除</button></Modal
  >
</template>
