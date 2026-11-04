<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Save } from 'lucide-vue-next'
import { send } from '../../api'
import { useSite } from '../../store'
import type { Site } from '../../types'
const site = useSite(),
  form = reactive<Site>({ ...site.site }),
  busy = ref(false)
async function save() {
  busy.value = true
  try {
    await send('/admin/site', 'PUT', form)
    await site.load()
    site.notify('馆邸与馆主档案已更新')
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
      <h2>站点与馆主档案</h2>
      <p>所有公开个人信息在这里统一管理。</p>
    </div>
  </header>
  <form class="settings-form stack-form" @submit.prevent="save">
    <div class="form-grid">
      <label>站点名称<input v-model="form.title" required maxlength="60" /></label
      ><label>馆主称呼<input v-model="form.owner" required maxlength="40" /></label
      ><label class="span-two">首页欢迎短句<input v-model="form.subtitle" required maxlength="120" /></label
      ><label>GitHub地址<input v-model="form.github" type="url" maxlength="300" /></label
      ><label>联系邮箱<input v-model="form.email" type="email" maxlength="120" /></label
      ><label class="span-two"
        >个人介绍（Markdown）<textarea
          v-model="form.about"
          required
          maxlength="5000"
          rows="7"
        ></textarea></label
      ><label class="span-two"
        >技术栈与兴趣（用 / 分隔）<input v-model="form.interests" maxlength="500" /></label
      ><label class="span-two"
        >成长时间线（每行：年份|事件）<textarea
          v-model="form.timeline"
          rows="4"
          maxlength="3000"
        ></textarea></label
      ><label class="span-two"
        >项目经历（每行：名称|介绍|链接）<textarea
          v-model="form.projects"
          rows="4"
          maxlength="3000"
        ></textarea>
      </label>
    </div>
    <button class="button primary" :disabled="busy">
      <Save :size="15" />{{ busy ? '保存中…' : '保存站点设置' }}
    </button>
  </form>
</template>
