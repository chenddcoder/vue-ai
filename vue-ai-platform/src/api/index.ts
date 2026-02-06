import request from '@/utils/request'

interface ApiResponse<T = any> {
  code: number
  message?: string
  data?: T
  token?: string
}

// 认证相关
export const login = (data: { username: string; password: string }) => {
  return request.post<ApiResponse>('/api/auth/login', data)
}

export const register = (data: { username: string; password: string }) => {
  return request.post<ApiResponse>('/api/auth/register', data)
}

export const getUserInfo = () => {
  return request.get<ApiResponse>('/api/auth/user')
}

// 项目相关
export const getProjectList = (userId: number) => {
  return request.get<ApiResponse>('/api/project/list', { params: { userId } })
}

export const getProject = (id: number) => {
  return request.get<ApiResponse>(`/api/project/${id}`)
}

export const saveProject = (data: any) => {
  return request.post<ApiResponse>('/api/project/save', data)
}

// 应用市场相关
export const getMarketApps = (params?: { keyword?: string; category?: string; page?: number; pageSize?: number }) => {
  return request.get<ApiResponse>('/api/market/apps', { params })
}

export const getMarketAppDetail = (id: number) => {
  return request.get<ApiResponse>(`/api/market/apps/${id}`)
}

export const publishApp = (data: {
  projectId: number
  name: string
  description: string
  tags?: string[]
  thumbnail?: string
  content?: any
  authorId?: number
  authorName?: string
}) => {
  return request.post<ApiResponse>('/api/market/publish', data)
}

export const unpublishApp = (appId: number) => {
  return request.delete<ApiResponse>(`/api/market/apps/${appId}`)
}

export const toggleAppLike = (appId: number, userId?: number) => {
  return request.post<ApiResponse>(`/api/market/apps/${appId}/like`, { userId })
}

export const getMyMarketApps = (userId: number) => {
  return request.get<ApiResponse>('/api/market/my-apps', { params: { userId } })
}

// AI代码生成
export const generateCode = (prompt: string) => {
  return request.post<ApiResponse>('/magic/ai/generate', { prompt })
}
