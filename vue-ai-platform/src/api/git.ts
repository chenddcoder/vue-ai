import request from '@/utils/request'

// Git管理相关API

// 获取项目提交历史
export const getCommits = (projectId: number, branch: string = 'main', page: number = 1, pageSize: number = 20) => {
  return request.get('/api/git/commits/' + projectId, {
    params: { branch, page, pageSize }
  })
}

// 创建提交
export const createCommit = (data: {
  projectId: number
  authorId: number
  message: string
  content: string
  branch?: string
}) => {
  return request.post('/api/git/commit', data)
}

// 获取提交详情
export const getCommitDetail = (commitHash: string) => {
  return request.get('/api/git/commit/' + commitHash)
}

// 获取分支列表
export const getBranches = (projectId: number) => {
  return request.get('/api/git/branches/' + projectId)
}

// 创建分支
export const createBranch = (data: {
  projectId: number
  name: string
  fromCommit?: string
}) => {
  return request.post('/api/git/branch', data)
}

// 删除分支
export const deleteBranch = (projectId: number, branchName: string) => {
  return request.delete('/api/git/branch/' + projectId + '/' + branchName)
}

// 检出提交
export const checkoutCommit = (data: {
  projectId: number
  commitHash: string
}) => {
  return request.post('/api/git/checkout', data)
}

// 对比两个提交
export const diffCommits = (data: {
  fromHash: string
  toHash: string
}) => {
  return request.post('/api/git/diff', data)
}

// 初始化Git
export const initGit = (projectId: number) => {
  return request.post('/api/git/init/' + projectId)
}

// 获取远程仓库列表
export const getRemotes = (projectId: number) => {
  return request.get('/api/git/remotes/' + projectId)
}

// 添加远程仓库
export const addRemote = (data: {
  projectId: number
  name: string
  url: string
  isDefault?: boolean
}) => {
  return request.post('/api/git/remote', data)
}

// 删除远程仓库
export const deleteRemote = (projectId: number, remoteName: string) => {
  return request.delete('/api/git/remote/' + projectId + '/' + remoteName)
}

// 拉取
export const pullRemote = (data: {
  projectId: number
  remoteName?: string
}) => {
  return request.post('/api/git/pull', data)
}

// 推送
export const pushRemote = (data: {
  projectId: number
  remoteName?: string
  branch?: string
}) => {
  return request.post('/api/git/push', data)
}

// 获取Git状态
export const getGitStatus = (projectId: number) => {
  return request.get('/api/git/status/' + projectId)
}
