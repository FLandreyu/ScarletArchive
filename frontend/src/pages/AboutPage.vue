<script setup lang="ts">
import { computed } from 'vue'
import { Github, ArrowUpRight, Mail, Code2, Heart } from 'lucide-vue-next'
import { useSite } from '../store'
import PageIntro from '../components/PageIntro.vue'
import MarkdownView from '../components/MarkdownView.vue'
const site = useSite(),
  timeline = computed(() =>
    site.site.timeline
      .split('\n')
      .filter(Boolean)
      .map((x) => x.split('|')),
  ),
  projects = computed(() =>
    site.site.projects
      .split('\n')
      .filter(Boolean)
      .map((x) => x.split('|')),
  )
const safe = (url: string) => /^https?:\/\//.test(url)
</script>
<template>
  <div class="wrap page-wrap about-page">
    <PageIntro
      eyebrow="REMILIA'S DRAWING ROOM · ABOUT THE KEEPER"
      title="馆主会客室"
      description="很高兴，在这个小小的幻想乡里与你相遇。"
    />
    <div class="about-layout">
      <aside class="about-identity">
        <img src="/art/crest.svg" alt="馆主玫瑰纹章" width="110" height="110" /><span class="eyebrow"
          >THE KEEPER</span
        >
        <h2>{{ site.site.owner }}</h2>
        <p>写一点代码，留一点幻想。</p>
        <div class="identity-links">
          <a v-if="site.site.github" :href="site.site.github" target="_blank" rel="noopener noreferrer"
            ><Github :size="17" />GitHub<ArrowUpRight :size="13" /></a
          ><a v-if="site.site.email" :href="'mailto:' + site.site.email"><Mail :size="17" />写信给我</a>
        </div>
        <div class="about-stamp">EST. 2026<br />SCARLET ARCHIVE</div>
      </aside>
      <div class="about-copy">
        <span class="eyebrow">HELLO, TRAVELER.</span>
        <h2>在代码与幻想之间，<br />慢慢收集自己的故事。</h2>
        <MarkdownView :source="site.site.about" />
        <section>
          <h3><Heart :size="17" />那些让我保持好奇的事</h3>
          <div class="interest-pills">
            <span v-for="t in site.site.interests.split('/').filter(Boolean)" :key="t">{{ t.trim() }}</span>
          </div>
        </section>
        <section>
          <h3><Code2 :size="17" />正在做的事情</h3>
          <a
            v-for="p in projects"
            :key="p[0]"
            :href="safe(p[2] || '') ? p[2] : undefined"
            target="_blank"
            rel="noopener noreferrer"
            class="project-entry"
            ><div>
              <h4>{{ p[0] }}</h4>
              <p>{{ p[1] }}</p>
            </div>
            <ArrowUpRight :size="17"
          /></a>
        </section>
        <section>
          <h3>一路走来</h3>
          <div class="about-timeline">
            <div v-for="t in timeline" :key="t[0]">
              <span>{{ t[0] }}</span>
              <p>{{ t[1] }}</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>
