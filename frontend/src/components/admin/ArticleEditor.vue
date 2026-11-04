<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { Save, Eye, Upload, ArrowLeft } from 'lucide-vue-next'
import { api, send } from '../../api'
import type { Article } from '../../types'
import { useSite } from '../../store'
import MarkdownView from '../MarkdownView.vue'
const props = defineProps<{ id: number | null }>(),
  emit = defineEmits<{ close: []; saved: [] }>(),
  site = useSite()
const busy = ref(false),
  loading = ref(false),
  uploading = ref(false),
  preview = ref(true),
  error = ref('')
const form = reactive({
  title: '',
  slug: 'entry-' + Date.now().toString(36),
  excerpt: '',
  body: '## 新的篇章\n\n从这里写下你的故事。',
  cover: '/art/library.svg',
  categoryId: site.categories[0]?.id || 0,
  tagIds: [] as number[],
  status: 'DRAFT',
  featured: false,
  version: 0,
})
watch(
  () => site.categories,
  () => {
    if (!form.categoryId) form.categoryId = site.categories[0]?.id || 0
  },
)
onMounted(async () => {
  if (props.id === null) return
  loading.value = true
  try {
    const a = await api<Article>('/admin/articles/' + props.id)
    Object.assign(form, {
      title: a.title,
      slug: a.slug,
      excerpt: a.excerpt,
      body: a.body || '',
      cover: a.cover,
      categoryId: a.categoryId,
      tagIds: a.tags.map((t) => t.id),
      status: a.status,
      featured: a.featured,
      version: a.version,
    })
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
async function upload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    site.notify('封面不得超过5MB', true)
    return
  }
  uploading.value = true
  try {
    const data = new FormData()
    data.append('file', file)
    const result = await api<{ url: string }>('/admin/uploads', { method: 'POST', body: data })
    form.cover = result.url
    site.notify('封面已上传并安全重编码')
  } catch (e) {
    site.notify((e as Error).message, true)
  } finally {
    uploading.value = false
  }
}
async function save() {
  busy.value = true
  error.value = ''
  try {
    await send(
      '/admin/articles' + (props.id === null ? '' : '/' + props.id),
      props.id === null ? 'POST' : 'PUT',
      form,
    )
    site.notify(form.status === 'PUBLISHED' ? '文章已发布，可以在图书馆阅读' : '草稿已保存，仅后台可见')
    emit('saved')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <section class="editor">
    <header class="admin-section-heading">
      <button class="text-link" @click="emit('close')"><ArrowLeft :size="15" />返回文章</button>
      <h2>{{ id === null ? '新建一份藏书' : '编辑藏书' }}</h2>
      <button class="button outline small" @click="preview = !preview">
        <Eye :size="15" />{{ preview ? '收起预览' : '展开预览' }}
      </button>
    </header>
    <div v-if="loading" class="state-box">正在载入原稿…</div>
    <form v-else @submit.prevent="save">
      <div class="editor-fields">
        <label
          >文章标题<input
            v-model="form.title"
            required
            maxlength="160"
            placeholder="给这份手记起一个名字" /></label
        ><label
          >短链接 slug<input
            v-model="form.slug"
            required
            pattern="[a-z0-9]+(-[a-z0-9]+)*"
            maxlength="180"
            placeholder="a-new-chapter" /></label
        ><label class="span-two"
          >文章摘要<textarea v-model="form.excerpt" rows="2" required maxlength="400"></textarea></label
        ><label
          >分类<select v-model.number="form.categoryId" required>
            <option v-for="c in site.categories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select></label
        ><label
          >文章状态<select v-model="form.status">
            <option value="DRAFT">草稿 · 仅后台可见</option>
            <option value="PUBLISHED">已发布 · 前台公开</option>
          </select></label
        >
        <div class="span-two editor-tags">
          <span>标签</span
          ><label v-for="t in site.tags" :key="t.id"
            ><input v-model="form.tagIds" type="checkbox" :value="t.id" />{{ t.name }}</label
          >
        </div>
        <div class="span-two cover-editor">
          <img v-if="form.cover" :src="form.cover" alt="文章封面预览" width="160" height="90" />
          <div>
            <label
              >封面地址<input
                v-model="form.cover"
                maxlength="500"
                placeholder="/art/library.svg 或 HTTPS 图片" /></label
            ><label class="upload-button"
              ><Upload :size="14" />{{ uploading ? '上传中…' : '上传封面（PNG/JPEG，最大5MB）'
              }}<input
                type="file"
                accept="image/png,image/jpeg"
                aria-label="上传封面"
                :disabled="uploading"
                @change="upload"
            /></label>
          </div>
        </div>
      </div>
      <div class="markdown-editor" :class="{ split: preview }">
        <label
          >Markdown 正文<textarea
            v-model="form.body"
            required
            maxlength="100000"
            rows="24"
            spellcheck="false"
            aria-label="Markdown正文"
          ></textarea>
        </label>
        <section v-if="preview" class="editor-preview">
          <span class="eyebrow">安全预览 · HTML已禁用</span><MarkdownView :source="form.body" />
        </section>
      </div>
      <footer class="editor-footer">
        <label class="check-label"><input v-model="form.featured" type="checkbox" />首页推荐文章</label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="button primary" :disabled="busy || uploading">
          <Save :size="15" />{{
            busy ? '正在保存…' : form.status === 'PUBLISHED' ? '保存并发布' : '保存草稿'
          }}
        </button>
      </footer>
    </form>
  </section>
</template>
