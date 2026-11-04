import manifest from '../character-art.json'

declare const __SCARLET_CHARACTER_ART__: boolean
export type Character = keyof typeof manifest.characters
export const characterArtEnabled = __SCARLET_CHARACTER_ART__
export const characters = manifest.characters
