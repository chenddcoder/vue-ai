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

export const updateUserAvatar = (userId: number, avatar: string) => {
  return request.post<ApiResponse>('/api/auth/avatar', { userId, avatar })
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

export const deleteProject = (id: number) => {
  return request.delete<ApiResponse>(`/api/project/${id}`)
}

export const renameProject = (id: number, name: string) => {
  return request.put<ApiResponse>(`/api/project/${id}/rename`, { name })
}

// 应用市场相关
export const getMarketApps = (params?: { keyword?: string; category?: string; page?: number; pageSize?: number }) => {
  return request.get<ApiResponse>('/api/market/apps', { params })
}

export const getMarketAppDetail = (id: number) => {
  return request.get<ApiResponse>(`/api/market/apps/${id}`)
}

export const publishApp = (data: {
  projectId?: number
  appId?: number
  name: string
  description: string
  tags?: string[]
  thumbnail?: string
  content?: any
  authorId?: number
  authorName?: string
  isOpenSource?: boolean
  updateContent?: string
  versionType?: string
}) => {
  return request.post<ApiResponse>('/api/market/publish', data)
}

export const getAppVersions = (appId: number) => {
  return request.get<ApiResponse>(`/api/market/apps/${appId}/versions`)
}

export const rollbackApp = (appId: number, versionId: number) => {
  return request.post<ApiResponse>(`/api/market/apps/${appId}/rollback/${versionId}`, {})
}

export const getPublishedAppByProject = (projectId: number) => {
  return request.get<ApiResponse>(`/api/market/project/${projectId}/published`)
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

// ==================== 社区功能 API ====================

export const getAppComments = (appId: number) => {
  return request.get<ApiResponse>(`/api/market/apps/${appId}/comments`)
}

export const addAppComment = (data: {
  appId: number
  userId: number
  userName?: string
  userAvatar?: string
  content: string
  rating?: number
  parentId?: number
}) => {
  return request.post<ApiResponse>(`/api/market/apps/${data.appId}/comments`, data)
}

export const deleteComment = (commentId: number, userId: number) => {
  return request.delete<ApiResponse>(`/api/market/comments/${commentId}`, { data: { userId } })
}

export const toggleAppFavorite = (appId: number, userId: number) => {
  return request.post<ApiResponse>(`/api/market/apps/${appId}/favorite`, { userId })
}

export const getUserFavorites = (userId: number) => {
  return request.get<ApiResponse>('/api/market/favorites', { params: { userId } })
}

export const checkAppFavorite = (appId: number, userId: number) => {
  return request.get<ApiResponse>(`/api/market/apps/${appId}/favorite/check`, { params: { userId } })
}

export const toggleUserFollow = (userId: number, followerId: number) => {
  return request.post<ApiResponse>(`/api/market/users/${userId}/follow`, { followerId })
}

export const getUserFollowers = (userId: number) => {
  return request.get<ApiResponse>(`/api/market/users/${userId}/followers`)
}

export const getUserFollowing = (userId: number) => {
  return request.get<ApiResponse>(`/api/market/users/${userId}/following`)
}

export const checkUserFollow = (userId: number, followerId: number) => {
  return request.get<ApiResponse>(`/api/market/users/${userId}/follow/check`, { params: { followerId } })
}

export const getUserApps = (userId: number) => {
  return request.get<ApiResponse>(`/api/market/users/${userId}/apps`)
}

// AI代码生成
export const generateCode = (data: { prompt: string; provider?: string; model?: string; config?: any; context?: string; maxTokens?: number; temperature?: number; topP?: number }) => {
  return request.post<ApiResponse>('/magic/ai/generate', data)
}

// 智能模块代码生成
export const generateModuleCode = (data: { projectId?: number | null; requirement: string; userId: number; files?: Record<string, string> }) => {
  return request.post<ApiResponse>('/magic/ai/generate-module', data)
}

// AI配置管理
export const getAIConfigs = (userId: number) => {
  return request.get<ApiResponse>('/magic/ai/configs', { params: { userId } })
}

export const saveAIConfig = (data: {
  id?: number
  userId: number
  providerId: string
  modelId: string
  config: Record<string, any>
  isActive?: boolean
}) => {
  return request.post<ApiResponse>('/magic/ai/configs', data)
}

export const deleteAIConfig = (configId: number) => {
  return request.delete<ApiResponse>(`/magic/ai/configs/${configId}`)
}

export const setActiveAIConfig = (configId: number) => {
  return request.put<ApiResponse>(`/magic/ai/configs/${configId}/active`)
}

export const testAIConfig = (config: any, testPrompt?: string) => {
  return request.post<ApiResponse>('/magic/ai/configs/test', { config, testPrompt })
}

export const getAIProviders = () => {
  return request.get<ApiResponse>('/magic/ai/providers')
}
