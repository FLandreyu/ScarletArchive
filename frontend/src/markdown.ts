import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import sql from 'highlight.js/lib/languages/sql'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml'
import python from 'highlight.js/lib/languages/python'
import DOMPurify from 'dompurify'
for (const [name, language] of Object.entries({
  java,
  javascript,
  typescript,
  sql,
  json,
  bash,
  css,
  xml,
  python,
}))
  hljs.registerLanguage(name, language)
export function markdown(source: string) {
  const toc: { id: string; text: string; level: number }[] = []
  const counts = new Map<string, number>()
  const md: MarkdownIt = new MarkdownIt({ html: false, linkify: true, typographer: true })
  md.renderer.rules.fence = (tokens, index) => {
    const token = tokens[index],
      lang = token.info.trim().split(/\s+/)[0]
    const escaped =
      lang && hljs.getLanguage(lang)
        ? hljs.highlight(token.content, { language: lang }).value
        : md.utils.escapeHtml(token.content)
    return `<div class="code-wrap"><button type="button" class="copy-code" data-copy-code="true">复制代码</button><pre class="hljs"><code>${escaped}</code></pre></div>`
  }
  const heading =
    md.renderer.rules.heading_open ||
    ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
  md.renderer.rules.heading_open = (tokens, idx, options, env, self) => {
    const text = tokens[idx + 1]?.content || 'section'
    let id =
      text
        .toLowerCase()
        .replace(/[^\p{L}\p{N}]+/gu, '-')
        .replace(/^-|-$/g, '') || 'section'
    const n = counts.get(id) || 0
    counts.set(id, n + 1)
    if (n) id += '-' + n
    tokens[idx].attrSet('id', id)
    const level = Number(tokens[idx].tag.slice(1))
    if (level > 1 && level < 5) toc.push({ id, text, level })
    return heading(tokens, idx, options, env, self)
  }
  const image = md.renderer.rules.image!
  md.renderer.rules.image = (tokens, idx, options, env, self) => {
    tokens[idx].attrSet('loading', 'lazy')
    return image(tokens, idx, options, env, self)
  }
  return {
    html: DOMPurify.sanitize(md.render(source), {
      ADD_ATTR: ['data-copy-code', 'loading'],
      FORBID_TAGS: ['style', 'script', 'iframe', 'form', 'input'],
      FORBID_ATTR: ['style'],
    }),
    toc,
  }
}
