export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
  ) {
    super(message)
  }
}
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const token = sessionStorage.getItem('scarlet-token')
  if ((token && path.startsWith('/admin')) || (token && path.startsWith('/auth')))
    headers.set('Authorization', `Bearer ${token}`)
  let r: Response
  try {
    r = await fetch(`/api${path}`, { ...options, headers })
  } catch {
    throw new ApiError('NETWORK', '无法连接馆邸的服务，请稍后重试', 0)
  }
  const b = await r.json().catch(() => null)
  if (!r.ok) {
    if (r.status === 401 && path != '/auth/login') {
      sessionStorage.removeItem('scarlet-token')
      window.dispatchEvent(new Event('scarlet-expired'))
    }
    throw new ApiError(b?.code || 'ERROR', b?.message || '这次操作未能完成', r.status)
  }
  return b.data
}
export const send = <T>(path: string, method: string, data?: unknown) =>
  api<T>(path, { method, body: data === undefined ? undefined : JSON.stringify(data) })
export const date = (value: string | null) => (value ? value.slice(0, 10).replaceAll('-', '.') : '未发布')
