<script setup lang="ts">
import CharacterPortrait from './CharacterPortrait.vue'
import { characterArtEnabled, type Character } from '../character-art'
defineProps<{ eyebrow: string; title: string; description: string; symbol?: string; character?: Character }>()
</script>
<template>
  <section class="page-intro" :class="{ 'with-character': character && characterArtEnabled }">
    <span class="eyebrow">{{ eyebrow }}</span>
    <h1>{{ title }}<span>✧</span></h1>
    <p>{{ description }}</p>
    <span v-if="symbol" class="intro-mark" aria-hidden="true">{{ symbol }}</span>
    <div class="ornament-line"><span></span><i>◇</i><span></span></div>
    <CharacterPortrait v-if="character" :character="character" />
  </section>
</template>

<style scoped>
.with-character {
  padding: 58px 310px 45px 55px;
  min-height: 307px;
  text-align: left;
  margin-bottom: 28px;
  border-bottom: 1px solid #ac91c91c;
  background: radial-gradient(ellipse at 80% 70%, #55354d21, transparent 64%);
}
.with-character .ornament-line {
  justify-content: flex-start;
}
.with-character .intro-mark {
  right: 280px;
  top: 30px;
}
@media (max-width: 900px) and (min-width: 701px) {
  .with-character {
    padding-left: 16px;
    padding-right: 215px;
  }
  .with-character h1 {
    font-size: 35px;
  }
}
@media (max-width: 700px) {
  .with-character {
    padding: 30px 132px 28px 0;
    min-height: 226px;
    margin-bottom: 20px;
  }
  .with-character > .eyebrow {
    font-size: 6px;
    letter-spacing: 1px;
    line-height: 1.8;
  }
  .with-character h1 {
    font-size: 28px;
    letter-spacing: 3px;
    margin-top: 15px;
  }
  .with-character h1 > span {
    display: none;
  }
  .with-character > p {
    font-size: 10px;
    margin-top: 11px;
    letter-spacing: 0.4px;
  }
  .with-character .ornament-line {
    margin-top: 16px;
  }
  .with-character .intro-mark {
    display: none;
  }
}
</style>
