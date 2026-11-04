<script setup lang="ts">
import { computed, ref } from 'vue'
import { characterArtEnabled, characters, type Character } from '../character-art'

const props = withDefaults(
  defineProps<{
    character: Character
    variant?: 'hero' | 'intro' | 'secret' | 'search'
  }>(),
  { variant: 'intro' },
)
const artwork = computed(() => characters[props.character])
const failed = ref(false)
</script>

<template>
  <figure v-if="characterArtEnabled && !failed" class="character-portrait" :class="`portrait-${variant}`">
    <div class="portrait-light" aria-hidden="true"></div>
    <img
      :src="`/art/characters/${character}.png`"
      :alt="`${artwork.name}立绘，绘师 dairi`"
      :width="artwork.width"
      :height="artwork.height"
      :loading="variant === 'hero' ? 'eager' : 'lazy'"
      decoding="async"
      @error="failed = true"
    />
    <figcaption>
      <span>{{ artwork.name }}</span>
      <a :href="artwork.source" target="_blank" rel="noopener noreferrer">ILLUST / dairi ↗</a>
    </figcaption>
  </figure>
</template>

<style scoped>
.character-portrait {
  position: relative;
  margin: 0;
  isolation: isolate;
}
.character-portrait img {
  position: relative;
  display: block;
  width: 100%;
  height: calc(100% - 34px);
  object-fit: contain;
  object-position: center bottom;
  filter: drop-shadow(0 10px 24px #09071455);
}
.portrait-light {
  position: absolute;
  z-index: -1;
  inset: 8% -10% 8%;
  border-radius: 50%;
  background: radial-gradient(ellipse, #af628121, #7955a017 40%, transparent 70%);
}
figcaption {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 13px;
  height: 34px;
  white-space: nowrap;
  font-size: 9px;
  letter-spacing: 1px;
  color: #cfb8c7;
}
figcaption a {
  font:
    8px 'Segoe UI',
    sans-serif;
  letter-spacing: 1.3px;
  color: #c2a282;
  padding: 7px 0;
  text-decoration: underline;
  text-underline-offset: 4px;
}
figcaption a:hover {
  color: #f1d9ac;
}
.portrait-hero {
  position: absolute;
  z-index: 2;
  right: 1%;
  bottom: 86px;
  width: 390px;
  height: 530px;
}
.portrait-hero .portrait-light {
  border: 1px solid #d6b47a20;
  border-radius: 48% 48% 4px 4px;
  inset: 7% 10% 3%;
  background: radial-gradient(ellipse, #9b405126, #18112105 72%);
}
.portrait-intro {
  position: absolute;
  right: 64px;
  bottom: 15px;
  width: 220px;
  height: 275px;
}
.portrait-intro figcaption {
  gap: 8px;
  font-size: 8px;
}
.portrait-intro figcaption a {
  font-size: 7px;
}
.portrait-secret {
  width: 250px;
  height: 410px;
  align-self: center;
  justify-self: center;
}
.portrait-search {
  flex: 0 0 100px;
  height: 126px;
}
.portrait-search figcaption > span {
  display: none;
}
.portrait-search figcaption {
  height: 28px;
}
.portrait-search img {
  height: 98px;
}
@media (min-width: 1600px) {
  .portrait-hero {
    right: 3%;
    width: 430px;
    height: 580px;
  }
}
@media (max-width: 1100px) and (min-width: 701px) {
  .portrait-hero {
    right: -10px;
    width: 290px;
    height: 435px;
    bottom: 105px;
  }
  .portrait-intro {
    right: 15px;
    width: 190px;
  }
}
@media (max-width: 700px) {
  .portrait-hero {
    width: 228px;
    height: 325px;
    bottom: 80px;
    right: auto;
    left: 50%;
    transform: translateX(-50%);
  }
  .portrait-intro {
    right: -4px;
    bottom: 13px;
    width: 124px;
    height: 194px;
  }
  .portrait-intro figcaption {
    height: 28px;
  }
  .portrait-intro figcaption > span {
    display: none;
  }
  .portrait-intro img {
    height: calc(100% - 28px);
  }
  .portrait-secret {
    width: 170px;
    height: 275px;
  }
  .portrait-search {
    flex-basis: 78px;
    height: 108px;
  }
  .portrait-search img {
    height: 80px;
  }
}
</style>
