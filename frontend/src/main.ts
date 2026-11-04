import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import 'highlight.js/styles/tokyo-night-dark.css'
const app = createApp(App).use(createPinia()).use(router)
// Resolve the initial lazy page before mounting the shell so the footer never flashes above it.
router.isReady().then(() => app.mount('#app'))
