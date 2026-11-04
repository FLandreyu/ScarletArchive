import { readFile, writeFile, mkdir, rename } from 'node:fs/promises'
import { createHash } from 'node:crypto'
import { fetch, EnvHttpProxyAgent } from 'undici'

const root = new URL('../', import.meta.url)
const manifest = JSON.parse(await readFile(new URL('character-art.json', root), 'utf8'))
const sha = (value) => createHash('sha256').update(value).digest('hex')
const output = new URL('public/art/characters/', root)

if (process.env.SCARLET_CHARACTER_ART === 'off') {
  console.log('Character art disabled; the original symbol artwork will be used.')
  process.exit(0)
}

const pending = []
for (const [key, item] of Object.entries(manifest.characters)) {
  try {
    if (sha(await readFile(new URL(`${key}.png`, output))) === item.sha256) continue
  } catch {}
  pending.push([key, item])
}
if (!pending.length) {
  console.log('Six character illustrations verified from local cache.')
  process.exit(0)
}

const dispatcher = new EnvHttpProxyAgent()
async function request(url) {
  const parsed = new URL(url)
  if (parsed.protocol !== 'https:' || !['www.pixiv.net', 'i.pximg.net'].includes(parsed.hostname)) {
    throw new Error('Only the recorded Pixiv source hosts are allowed.')
  }
  const response = await fetch(url, {
    dispatcher,
    redirect: 'error',
    headers: { 'User-Agent': 'Mozilla/5.0', Referer: 'https://www.pixiv.net/' },
    signal: AbortSignal.timeout(30000),
  })
  if (!response.ok) throw new Error(`Pixiv returned HTTP ${response.status}.`)
  return response
}

try {
  // Recheck the author's usage terms on a fresh install, before retrieving artwork.
  const profile = await (await request(`https://www.pixiv.net/ajax/user/${manifest.authorId}?full=1`)).json()
  const terms = profile.body?.comment?.split('※フリー素材に関する利用規約※')[1]?.trim()
  if (!terms || sha(terms) !== manifest.termsSha256) {
    throw new Error(
      'The artist usage terms have changed. Review the source profile before updating the manifest.',
    )
  }
  await mkdir(output, { recursive: true })
  for (const [key, item] of pending) {
    const work = await (await request(`https://www.pixiv.net/ajax/illust/${item.id}`)).json()
    if (
      work.error ||
      work.body?.userId !== manifest.authorId ||
      work.body.xRestrict !== 0 ||
      !work.body.illustComment.includes('フリー素材') ||
      work.body.urls.original !== item.original
    ) {
      throw new Error(`The recorded source for ${key} requires review.`)
    }
    const bytes = Buffer.from(await (await request(item.original)).arrayBuffer())
    if (
      bytes.length > 5 * 1024 * 1024 ||
      bytes.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a' ||
      sha(bytes) !== item.sha256
    ) {
      throw new Error(`The illustration checksum for ${key} does not match.`)
    }
    const temporary = new URL(`${key}.png.pending`, output)
    await writeFile(temporary, bytes)
    await rename(temporary, new URL(`${key}.png`, output))
    console.log(`Fetched ${key} from ${item.source}`)
  }
} catch (error) {
  console.error(`Character art: ${error.message}`)
  console.error(
    'Check network/HTTPS_PROXY and docs/素材说明.md. For an art-free build, set SCARLET_CHARACTER_ART=off.',
  )
  process.exitCode = 1
} finally {
  await dispatcher.close()
}
