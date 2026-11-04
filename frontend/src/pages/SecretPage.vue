<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, RotateCcw, Sparkles } from 'lucide-vue-next'
import { useSite } from '../store'
import CharacterPortrait from '../components/CharacterPortrait.vue'
import { characterArtEnabled } from '../character-art'
const route = useRoute(),
  site = useSite(),
  collected = ref<number[]>([]),
  colors = ['#df6b83', '#e8ac6c', '#d7ce79', '#78b99a', '#78abc9', '#a28ada', '#cb86bb']
const found = computed(() => collected.value.length === 7),
  lost = computed(() => route.path != '/secret')
function pick(i: number) {
  if (!collected.value.includes(i)) collected.value.push(i)
  if (found.value) localStorage.setItem('scarlet-star-visitor', 'true')
}
</script>
<template>
  <section class="secret-page wrap">
    <span class="eyebrow">FLANDRE'S LITTLE ROOM · {{ lost ? '404' : 'A SECRET CHAPTER' }}</span>
    <h1>{{ lost ? '咦，你也走到这里了吗？' : '这里的星星，只为你亮着。' }}</h1>
    <p>
      {{
        lost
          ? '这条走廊没有你寻找的那一页。但不用着急，陪芙兰找一会儿星星吧。'
          : '不是每个房间都需要写满文字。有些地方，只要安静地待一会儿就好。'
      }}
    </p>
    <div class="secret-scene" :class="{ 'has-guest': characterArtEnabled }">
      <div class="crystal-chamber" :class="{ quiet: !site.motion }">
        <div class="chamber-window"></div>
        <div class="chamber-moon"></div>
        <svg viewBox="0 0 700 260" class="crystal-branch" aria-hidden="true">
          <path d="M80 160Q350-80 620 160M350 52v-30" fill="none" stroke="#a79178" stroke-width="3" /></svg
        ><button
          v-for="(color, i) in colors"
          :key="i"
          class="collect-crystal"
          :class="{ collected: collected.includes(i) }"
          :style="{
            '--crystal': color,
            '--delay': i * 0.32 + 's',
            left: 13 + i * 11.8 + '%',
            top: (i === 3 ? 25 : i === 2 || i === 4 ? 28 : i === 1 || i === 5 ? 35 : 45) + '%',
          }"
          :aria-label="`点亮第${i + 1}枚水晶`"
          :aria-pressed="collected.includes(i)"
          @click="pick(i)"
        >
          <span></span><i></i>
        </button>
        <div class="chamber-floor"></div>
        <span class="chamber-quote">「可以再多待一会儿吗？」<small>FLANDRE SCARLET</small></span>
      </div>
      <CharacterPortrait character="flandre" variant="secret" />
    </div>
    <div class="crystal-status">
      <Sparkles :size="18" /><span aria-live="polite">{{
        found ? '七枚星晶都亮了。谢谢你，今晚的房间不再孤单。' : `轻轻点亮水晶吧 · ${collected.length} / 7`
      }}</span
      ><button v-if="found" class="icon-btn" aria-label="重新点亮" @click="collected = []">
        <RotateCcw :size="16" />
      </button>
    </div>
    <RouterLink to="/" class="button outline"><ArrowLeft :size="16" />回到灯火明亮的大厅</RouterLink>
  </section>
</template>

<style scoped>
.secret-scene.has-guest {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 270px;
  gap: 15px;
  align-items: center;
  margin-top: 25px;
}
.has-guest .crystal-chamber {
  width: 100%;
  margin-top: 0;
}
@media (max-width: 700px) {
  .secret-scene.has-guest {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
    margin-top: 25px;
  }
  .has-guest :deep(.portrait-secret) {
    grid-row: 1;
    margin-bottom: -5px;
  }
}
</style>
