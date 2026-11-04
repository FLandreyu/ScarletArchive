export interface Taxonomy {
  id: number
  name: string
  slug: string
  description?: string
  articleCount: number
}
export interface Article {
  id: number
  title: string
  slug: string
  excerpt: string
  body?: string
  cover: string
  categoryId: number
  categoryName: string
  categorySlug: string
  status: 'DRAFT' | 'PUBLISHED'
  featured: boolean
  version: number
  publishedAt: string | null
  createdAt: string
  updatedAt: string
  tags: Taxonomy[]
}
export interface Page<T> {
  items: T[]
  total: number
  page: number
  size: number
}
export interface Message {
  id: number
  nickname: string
  content: string
  status: 'PENDING' | 'APPROVED'
  createdAt: string
}
export interface Friend {
  id: number
  name: string
  url: string
  description: string
  status: 'PENDING' | 'APPROVED'
}
export interface Site {
  title: string
  subtitle: string
  owner: string
  about: string
  github: string
  email: string
  interests: string
  timeline: string
  projects: string
}
