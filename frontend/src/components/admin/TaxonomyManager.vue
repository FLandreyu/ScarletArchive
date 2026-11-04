<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import { send } from '../../api'
import { useSite } from '../../store'
import type { Taxonomy } from '../../types'
import Modal from '../Modal.vue'
const site = useSite(),
  kind = ref<'categories' | 'tags'>('categories'),
  opened = ref(false),
  id = ref<number | null>(null),
  busy = ref(false),
  pending = ref<{ type: string; item: Taxonomy } | null>(null),
  form = reactive({ name: '', slug: '', description: '' })
function edit(type: 'categories' | 'tags', item?: Taxonomy) {
  kind.value = type
  id.value = item?.id ?? null
  Object.assign(form, {
    name: item?.name || '',
    slug: item?.slug || '',
    description: item?.description || '',
  })
  opened.value = true
}
async function save() {
  busy.value = true
  try {
    await send(
      '/admin/' + kind.value + (id.value === null ? '' : '/' + id.value),
      id.value === null ? 'POST' : 'PUT',
      form,
    )
    opened.value = false
    await site.load()
    site.notify('索引已更新')
  } catch (e) {
    site.notify((e as Error).message, true)
  } finally {
    busy.value = false
  }
}
async function remove() {
  if (!pending.value) return
  try {
    await send('/admin/' + pending.value.type + '/' + pending.value.item.id, 'DELETE')
    pending.value = null
    await site.load()
    site.notify('索引已删除')
  } catch (e) {
    site.notify((e as Error).message, true)
  }
}
</script>
<template>
  <header class="admin-section-heading">
    <div>
      <h2>分类与标签</h2>
      <p>建立清楚的书架索引，让文字更容易被找到。</p>
    </div>
  </header>
  <div class="taxonomy-admin-grid">
    <section v-for="type in ['categories', 'tags'] as const" :key="type" class="admin-panel">
      <header>
        <h3>{{ type === 'categories' ? '文章分类' : '文章标签' }}</h3>
        <button class="text-link" @click="edit(type)"><Plus :size="15" />新增</button>
      </header>
      <div
        v-for="item in type === 'categories' ? site.categories : site.tags"
        :key="item.id"
        class="taxonomy-row"
      >
        <div>
          <strong>{{ item.name }}</strong
          ><small>{{ item.slug }} · {{ item.articleCount }}篇公开文章</small>
        </div>
        <button class="icon-btn" :aria-label="'编辑' + item.name" @click="edit(type, item)">
          <Pencil :size="14" /></button
        ><button class="icon-btn danger" :aria-label="'删除' + item.name" @click="pending = { type, item }">
          <Trash2 :size="14" />
        </button>
      </div>
    </section>
  </div>
  <Modal
    v-if="opened"
    :title="(id === null ? '新增' : '编辑') + (kind === 'categories' ? '分类' : '标签')"
    @close="opened = false"
    ><form class="stack-form" @submit.prevent="save">
      <label>名称<input v-model="form.name" required :maxlength="kind === 'tags' ? 30 : 40" /></label
      ><label
        >短标识 slug<input
          v-model="form.slug"
          required
          pattern="[a-z0-9]+(-[a-z0-9]+)*"
          maxlength="60" /></label
      ><label v-if="kind === 'categories'"
        >介绍<textarea v-model="form.description" maxlength="200" rows="3"></textarea></label
      ><button class="button primary" :disabled="busy">{{ busy ? '保存中…' : '保存索引' }}</button>
    </form></Modal
  ><Modal v-if="pending" title="删除索引" @close="pending = null"
    ><p class="dialog-copy">
      确定删除“{{ pending.item.name }}”？<br />{{
        pending.type === 'categories'
          ? '仍被文章使用的分类无法删除。'
          : '该标签与文章的关联将一并移除，文章本身会保留。'
      }}
    </p>
    <button class="button danger-button" @click="remove">确认删除</button></Modal
  >
</template>
