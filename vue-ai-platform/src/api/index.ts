import request from '@/utils/request'

export const login = (data: any) => {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export const getProjectList = (userId: number) => {
  return request({
    url: '/api/project/list',
    method: 'get',
    params: { userId }
  })
}

export const getProject = (id: number) => {
  return request({
    url: `/api/project/${id}`,
    method: 'get'
  })
}

export const saveProject = (data: any) => {
  return request({
    url: '/api/project/save',
    method: 'post',
    data
  })
}

export const generateCode = (prompt: string) => {
  return request({
    url: '/magic/ai/generate',
    method: 'post',
    data: { prompt }
  })
}
