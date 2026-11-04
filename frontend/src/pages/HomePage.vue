<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import {
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  Feather,
  Clock3,
  ChevronDown,
  Quote,
  Code2,
  Flower2,
  MoonStar,
} from 'lucide-vue-next'
import gsap from 'gsap'
import { api, date } from '../api'
import type { Article, Page } from '../types'
import { useSite } from '../store'
import ArticleCard from '../components/ArticleCard.vue'
const site = useSite(),
  total = ref(0),
  posts = ref<Article[]>([]),
  featured = ref<Article[]>([]),
  error = ref(''),
  loaded = ref(false),
  hero = ref<HTMLElement>()
let context: gsap.Context | undefined
function animate() {
  context?.revert()
  if (site.motion && hero.value)
    context = gsap.context(() => {
      gsap.from('.hero-copy > *', {
        y: 18,
        opacity: 0,
        duration: 0.65,
        stagger: 0.09,
        ease: 'power2.out',
        clearProps: 'all',
      })
      const media = gsap.matchMedia()
      media.add('(min-width: 1000px) and (prefers-reduced-motion: no-preference)', () => {
        const move = (e: PointerEvent) => {
          gsap.to('.hero-image', {
            x: (e.clientX / innerWidth - 0.5) * -9,
            y: (e.clientY / innerHeight - 0.5) * -5,
            duration: 1.2,
            ease: 'power1.out',
          })
        }
        hero.value?.addEventListener('pointermove', move)
        return () => hero.value?.removeEventListener('pointermove', move)
      })
    }, hero.value)
}
async function load() {
  try {
    const [a, b] = await Promise.all([
      api<Page<Article>>('/articles?size=6'),
      api<Page<Article>>('/articles?featured=true&size=2'),
    ])
    posts.value = a.items
    total.value = a.total
    featured.value = b.items
    loaded.value = true
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  }
}
onMounted(() => {
  animate()
  void load()
})
watch(() => site.motion, animate)
onBeforeUnmount(() => context?.revert())
</script>
<template>
  <section ref="hero" class="home-hero">
    <img
      class="hero-image"
      src="/art/mansion-moonlight.png"
      alt="月色、雾之湖与灯火温暖的红魔馆"
      width="1672"
      height="941"
      fetchpriority="high"
    />
    <div class="hero-vignette"></div>
    <div class="hero-stars" aria-hidden="true"><i v-for="n in 12" :key="n" :style="{ '--i': n }"></i></div>
    <div class="hero-inner wrap">
      <div class="hero-copy">
        <span class="hero-kicker"><span></span> A LETTER FROM THE SCARLET MANSION</span>
        <p class="hero-jp">ようこそ、紅魔館へ。</p>
        <h1>今夜，<br />来红魔馆坐坐<span>。</span></h1>
        <p class="hero-description">
          {{ site.site.subtitle }}<br />代码、故事，以及生活里值得珍藏的小小瞬间。
        </p>
        <div class="hero-actions">
          <RouterLink to="/articles" class="button primary"
            >走进魔法图书馆 <ArrowRight :size="17" /></RouterLink
          ><RouterLink to="/about" class="hero-about">认识馆主 <ArrowUpRight :size="16" /></RouterLink>
        </div>
        <div class="host-note">
          <span class="host-line"></span>
          <p>
            「茶已经备好了。<br />　今夜的故事，想从哪一页开始？」<small>REMILIA SCARLET · 蕾米莉亚</small>
          </p>
        </div>
      </div>
      <div class="hero-coordinate">
        <span>45° · MOONLIGHT / 00:00</span><span>SCARLET DEVIL MANSION</span>
      </div>
    </div>
    <div class="hero-bottom wrap">
      <span><i></i> THE NIGHT IS STILL YOUNG</span
      ><a href="#library">向下，翻开新的篇章 <ChevronDown :size="15" /></a><span>01 — THE ENTRANCE</span>
    </div>
  </section>
  <section class="welcome-strip wrap">
    <span class="mini-crest">✦</span>
    <p>这里是我的一方小小幻想乡。<span>写代码，也写生活；收藏知识，也收藏心情。</span></p>
    <div><span>记录</span><i>·</i><span>探索</span><i>·</i><span>相遇</span></div>
  </section>
  <section id="library" class="home-library wrap">
    <div class="section-heading">
      <div>
        <span class="eyebrow">VOILE · THE MAGIC LIBRARY</span>
        <h2>图书馆的新藏书<span>New chapters</span></h2>
        <p>帕秋莉整理了最近的几页手记，或许有你想读的故事。</p>
      </div>
      <RouterLink to="/articles" class="text-link">全部藏书 <ArrowUpRight :size="16" /></RouterLink>
    </div>
    <div v-if="error" class="state-box">
      <p role="alert">{{ error }}</p>
      <button class="button outline" @click="load">重新翻阅</button>
    </div>
    <div v-else-if="!loaded" class="state-box">小恶魔正在整理书架…</div>
    <div v-else class="home-content">
      <div class="latest-grid">
        <ArticleCard v-for="post in posts.slice(0, 4)" :key="post.id" :article="post" />
      </div>
      <aside class="home-aside">
        <section class="keeper-card">
          <span class="eyebrow">THE KEEPER OF THIS CORNER</span>
          <div class="keeper-portrait">
            <img src="/art/crest.svg" alt="馆主玫瑰纹章" width="70" height="70" /><span></span>
          </div>
          <h3>{{ site.site.owner }}</h3>
          <span class="keeper-role">代码与幻想之间的记录者</span>
          <p>把微小的灵感写进书页，<br />把漫长的日常酿成故事。</p>
          <div class="keeper-stats">
            <span
              ><b>{{ total }}</b
              >篇手记</span
            ><span
              ><b>{{ site.categories.length }}</b
              >个书架</span
            ><span><b>∞</b>份好奇</span>
          </div>
          <RouterLink to="/about" class="button outline">走进会客室 <ArrowUpRight :size="14" /></RouterLink>
        </section>
        <section class="shelf-index">
          <header>
            <BookOpen :size="16" />
            <h3>藏书分区</h3>
            <span>INDEX</span>
          </header>
          <RouterLink
            v-for="(cat, i) in site.categories"
            :key="cat.id"
            :to="{ path: '/articles', query: { category: cat.slug } }"
            ><span class="index-number">0{{ i + 1 }}</span
            ><span>{{ cat.name }}</span
            ><small>{{ String(cat.articleCount).padStart(2, '0') }}</small
            ><ArrowUpRight :size="13"
          /></RouterLink>
        </section>
        <div class="night-note">
          <MoonStar :size="23" :stroke-width="1" />
          <p>月光不会催促你。<br />慢慢读，慢慢长大。</p>
          <span>A SMALL REMINDER</span>
        </div>
      </aside>
    </div>
  </section>
  <section class="featured-section">
    <div class="wrap">
      <div class="section-heading">
        <div>
          <span class="eyebrow">RECOMMENDED BY THE MISTRESS</span>
          <h2>被特别夹上书签的几页<span>Editor's picks</span></h2>
        </div>
        <span class="section-decor">✧</span>
      </div>
      <div class="featured-grid">
        <RouterLink v-for="(a, i) in featured" :key="a.id" :to="'/articles/' + a.slug" class="featured-story"
          ><span class="featured-no">0{{ i + 1 }}</span>
          <div>
            <span class="eyebrow">{{ a.categoryName }} · SELECTED CHAPTER</span>
            <h3>{{ a.title }}</h3>
            <p>{{ a.excerpt }}</p>
            <span class="text-link">翻开这页 <ArrowRight :size="15" /></span>
          </div>
          <img :src="a.cover" alt="" loading="lazy" width="250" height="180"
        /></RouterLink>
      </div>
    </div>
  </section>
  <section class="wander-section wrap">
    <div class="section-heading">
      <div>
        <span class="eyebrow">THERE IS MORE TO EXPLORE</span>
        <h2>在馆邸里，再逛一会儿<span>Wander a little</span></h2>
      </div>
    </div>
    <div class="wander-grid">
      <RouterLink to="/archive"
        ><Clock3 :size="27" :stroke-width="1" />
        <div>
          <span>SAKUYA · THE CLOCK CORRIDOR</span>
          <h3>时间回廊</h3>
          <p>沿着咲夜的指针，重访过去的文字。</p>
        </div>
        <ArrowUpRight :size="18" /></RouterLink
      ><RouterLink to="/guestbook"
        ><Flower2 :size="27" :stroke-width="1" />
        <div>
          <span>MEILING · THE ROSE GARDEN</span>
          <h3>庭院来信</h3>
          <p>在离开前，留下一点你的好心情。</p>
        </div>
        <ArrowUpRight :size="18" /></RouterLink
      ><RouterLink to="/friends"
        ><Feather :size="27" :stroke-width="1" />
        <div>
          <span>FRIENDS · BEYOND THE MANSION</span>
          <h3>友人的庭院</h3>
          <p>沿着一枚书签，遇见另一个世界。</p>
        </div>
        <ArrowUpRight :size="18"
      /></RouterLink>
    </div>
  </section>
  <section class="updates wrap">
    <span class="eyebrow">MANSION JOURNAL</span>
    <h3>馆邸近讯</h3>
    <div v-for="a in posts.slice(0, 3)" :key="a.id">
      <time>{{ date(a.publishedAt) }}</time
      ><span class="update-dot"></span
      ><RouterLink :to="'/articles/' + a.slug">新藏书入馆 · {{ a.title }}</RouterLink
      ><ArrowUpRight :size="13" />
    </div>
  </section>
</template>
