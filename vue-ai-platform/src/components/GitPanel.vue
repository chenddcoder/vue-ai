<template>
  <div class="git-panel">
    <!-- Git状态头部 -->
    <div class="git-header">
      <div class="git-title" @click="expanded = !expanded">
        <DownOutlined v-if="expanded" class="expand-icon" />
        <RightOutlined v-else class="expand-icon" />
        <BranchesOutlined class="git-icon" />
        <span>{{ status.currentBranch || 'main' }}</span>
        <a-tag v-if="!status.isGitInitialized" color="orange" size="small">未初始化</a-tag>
      </div>
      <div class="git-actions">
        <a-tooltip title="刷新">
          <ReloadOutlined :spin="loading" @click="loadStatus" />
        </a-tooltip>
      </div>
    </div>

    <!-- 展开内容 -->
    <div v-if="expanded" class="git-content">
      <!-- 未初始化提示 -->
      <div v-if="!status.isGitInitialized" class="init-section">
        <div class="init-card">
          <div class="init-icon">
            <BranchesOutlined />
          </div>
          <div class="init-info">
            <div class="init-title">启用版本控制</div>
            <div class="init-desc">初始化Git以跟踪项目更改历史</div>
          </div>
          <a-button type="primary" size="small" @click="handleInitGit" :loading="initializing">
            初始化
          </a-button>
        </div>
      </div>

      <!-- 已初始化 -->
      <div v-else>
        <!-- 快捷操作 -->
        <div class="quick-actions">
          <a-tooltip title="提交">
            <a-button type="primary" size="small" @click="showCommitModal = true">
              <SendOutlined /> 提交
            </a-button>
          </a-tooltip>
          <a-tooltip title="拉取">
            <a-button size="small" @click="handlePull" :loading="pulling">
              <DownloadOutlined /> 拉取
            </a-button>
          </a-tooltip>
          <a-tooltip title="推送">
            <a-button size="small" @click="handlePush" :loading="pushing">
              <UploadOutlined /> 推送
            </a-button>
          </a-tooltip>
        </div>

        <!-- 分支信息 -->
        <div class="info-section">
          <div class="section-title">
            <BranchesOutlined /> 分支
            <span class="count">({{ status.branchCount }})</span>
          </div>
          <div class="branch-info">
            <a-tag color="blue">{{ status.currentBranch }}</a-tag>
            <a-button type="link" size="small" @click="showBranchesModal = true">
              管理
            </a-button>
          </div>
        </div>

        <!-- 远程仓库 -->
        <div class="info-section">
          <div class="section-title">
            <CloudOutlined /> 远程仓库
            <span class="count">({{ status.remoteCount }})</span>
            <a-button type="link" size="small" @click="showRemoteModal = true">
              <PlusOutlined />
            </a-button>
          </div>
          <div v-if="remotes.length > 0" class="remote-list">
            <div v-for="remote in remotes" :key="remote.id" class="remote-item">
              <span class="remote-name">
                <CloudServerOutlined />
                {{ remote.name }}
                <a-tag v-if="remote.is_default" size="small">默认</a-tag>
              </span>
              <a-button type="link" size="small" danger @click="handleDeleteRemote(remote.name)">
                <DeleteOutlined />
              </a-button>
            </div>
          </div>
          <div v-else class="no-remote">
            <a-button type="link" size="small" @click="showRemoteModal = true">
              添加远程仓库
            </a-button>
          </div>
        </div>

        <!-- 提交历史 -->
        <div class="info-section">
          <div class="section-title">
            <HistoryOutlined /> 最近提交
            <span class="count">({{ status.commitCount }})</span>
          </div>
          <div class="commit-list">
            <div
              v-for="commit in status.recentCommits"
              :key="commit.commit_hash"
              class="commit-item"
              @click="showCommitHistory"
            >
              <div class="commit-message">{{ commit.message }}</div>
              <div class="commit-meta">
                <a-tag class="commit-hash">{{ commit.commit_hash?.substring(0, 7) }}</a-tag>
                <span class="commit-time">{{ formatTime(commit.create_time) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 提交对话框 -->
    <a-modal
      v-model:open="showCommitModal"
      title="提交更改"
      @ok="handleCommit"
      :confirmLoading="committing"
    >
      <a-form :model="commitForm" layout="vertical">
        <a-form-item label="提交信息" required>
          <a-textarea
            v-model:value="commitForm.message"
            placeholder="输入提交信息，描述本次更改..."
            :rows="3"
          />
        </a-form-item>
        <a-alert type="info" show-icon>
          <template #message>
            即将提交 {{ changedFilesCount }} 个文件的更改
          </template>
        </a-alert>
      </a-form>
    </a-modal>

    <!-- 远程仓库对话框 -->
    <a-modal
      v-model:open="showRemoteModal"
      title="添加远程仓库"
      @ok="handleAddRemote"
      :confirmLoading="addingRemote"
    >
      <a-form :model="remoteForm" layout="vertical">
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            提示：当前为本地版本控制，推送功能需要手动使用Git命令或配置CI/CD
          </template>
        </a-alert>
        <a-form-item label="名称" required>
          <a-input v-model:value="remoteForm.name" placeholder="例如: origin" />
        </a-form-item>
        <a-form-item label="仓库URL" required>
          <a-input v-model:value="remoteForm.url" placeholder="例如: https://github.com/username/repo.git" />
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="remoteForm.isDefault">设为默认远程仓库</a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分支管理对话框 -->
    <a-modal
      v-model:open="showBranchesModal"
      title="分支管理"
      width="500px"
      :footer="null"
    >
      <div class="branches-modal">
        <div class="branch-actions">
          <a-button type="primary" size="small" @click="showCreateBranchModal = true">
            <PlusOutlined /> 新建分支
          </a-button>
        </div>
        <a-list :data-source="branches" size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <div class="branch-list-item">
                <BranchesOutlined />
                <span>{{ item.name }}</span>
                <a-tag v-if="item.is_default" color="blue">默认</a-tag>
              </div>
              <template #actions>
                <a-button
                  v-if="!item.is_default"
                  type="link"
                  size="small"
                  danger
                  @click="handleDeleteBranch(item.name)"
                >
                  删除
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </a-modal>

    <!-- 创建分支对话框 -->
    <a-modal
      v-model:open="showCreateBranchModal"
      title="创建分支"
      @ok="handleCreateBranch"
      :confirmLoading="creatingBranch"
    >
      <a-form :model="branchForm" layout="vertical">
        <a-form-item label="分支名称" required>
          <a-input v-model:value="branchForm.name" placeholder="例如: feature/new-feature" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  DownOutlined,
  RightOutlined,
  BranchesOutlined,
  ReloadOutlined,
  SendOutlined,
  DownloadOutlined,
  UploadOutlined,
  CloudOutlined,
  CloudServerOutlined,
  PlusOutlined,
  DeleteOutlined,
  HistoryOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  initGit,
  getGitStatus,
  getRemotes,
  addRemote,
  deleteRemote,
  pullRemote,
  pushRemote,
  getBranches,
  createBranch,
  deleteBranch as apiDeleteBranch,
  createCommit
} from '@/api'

const props = defineProps<{
  projectId: number
  projectContent: string
}>()

const emit = defineEmits<{
  'refresh': []
}>()

const userStore = useUserStore()

const expanded = ref(true)
const loading = ref(false)
const initializing = ref(false)
const pulling = ref(false)
const pushing = ref(false)
const committing = ref(false)
const addingRemote = ref(false)
const creatingBranch = ref(false)

const showCommitModal = ref(false)
const showRemoteModal = ref(false)
const showBranchesModal = ref(false)
const showCreateBranchModal = ref(false)

const status = ref<any>({
  currentBranch: 'main',
  branchCount: 0,
  commitCount: 0,
  remoteCount: 0,
  recentCommits: [],
  isGitInitialized: false
})

const remotes = ref<any[]>([])
const branches = ref<any[]>([])

const commitForm = ref({
  message: ''
})

const remoteForm = ref({
  name: 'origin',
  url: '',
  isDefault: true
})

const branchForm = ref({
  name: ''
})

const changedFilesCount = computed(() => {
  try {
    const files = JSON.parse(props.projectContent)
    return Object.keys(files).length
  } catch {
    return 0
  }
})

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const loadStatus = async () => {
  if (!props.projectId) return
  loading.value = true
  try {
    const [statusRes, remotesRes, branchesRes] = await Promise.all([
      getGitStatus(props.projectId),
      getRemotes(props.projectId),
      getBranches(props.projectId)
    ])
    
    const statusResAny = statusRes as any
    const remotesResAny = remotesRes as any
    const branchesResAny = branchesRes as any
    
    if (statusResAny.code === 200) {
      status.value = statusResAny.data
    }
    if (remotesResAny.code === 200) {
      remotes.value = remotesResAny.data || []
    }
    if (branchesResAny.code === 200) {
      branches.value = branchesResAny.data || []
    }
  } catch (error: any) {
    console.error('加载Git状态失败:', error)
  } finally {
    loading.value = false
  }
}

const handleInitGit = async () => {
  initializing.value = true
  try {
    const res: any = await initGit(props.projectId)
    if (res.code === 200) {
      message.success('Git初始化成功')
      loadStatus()
      emit('refresh')
    } else {
      message.error(res.message || '初始化失败')
    }
  } catch (error: any) {
    message.error('初始化失败: ' + error.message)
  } finally {
    initializing.value = false
  }
}

const handleCommit = async () => {
  if (!commitForm.value.message.trim()) {
    message.warning('请输入提交信息')
    return
  }
  committing.value = true
  try {
    const res: any = await createCommit({
      projectId: props.projectId,
      authorId: userStore.currentUser?.id || 0,
      message: commitForm.value.message,
      content: props.projectContent,
      branch: status.value.currentBranch
    })
    if (res.code === 200) {
      message.success('提交成功')
      showCommitModal.value = false
      commitForm.value.message = ''
      loadStatus()
      emit('refresh')
    } else {
      message.error(res.message || '提交失败')
    }
  } catch (error: any) {
    message.error('提交失败: ' + error.message)
  } finally {
    committing.value = false
  }
}

const handlePush = async () => {
  if (remotes.value.length === 0) {
    message.warning('请先添加远程仓库')
    showRemoteModal.value = true
    return
  }
  
  const remoteName = remotes.value.find(r => r.is_default)?.name || remotes.value[0]?.name
  const remoteUrl = remotes.value.find(r => r.is_default)?.url || remotes.value[0]?.url
  
  if (!remoteUrl) {
    message.warning('请先添加远程仓库URL')
    showRemoteModal.value = true
    return
  }
  
  // 检查是否为GitHub仓库
  if (remoteUrl.includes('github.com')) {
    message.info('GitHub推送需要配置Personal Access Token，请在远程仓库设置中添加')
    message.warning('当前推送功能为演示版本，请手动使用Git命令推送:\n' + 
      `git remote add origin ${remoteUrl}\n` +
      `git push -u origin ${status.value.currentBranch}`)
  } else if (remoteUrl.includes('gitee.com')) {
    message.info('Gitee推送需要配置仓库地址和用户名')
    message.warning('当前推送功能为演示版本，请手动使用Git命令推送')
  } else {
    message.info('推送功能演示中，请使用以下Git命令推送:\n' + 
      `git remote add ${remoteName} ${remoteUrl}\n` +
      `git push -u ${remoteName} ${status.value.currentBranch}`)
  }
  
  // 模拟成功反馈
  pushing.value = true
  try {
    const res: any = await pushRemote({
      projectId: props.projectId,
      remoteName: remoteName,
      branch: status.value.currentBranch
    })
    if (res.code === 200) {
      message.success('已记录推送操作（演示模式）')
    }
  } finally {
    pushing.value = false
  }
}

const handlePull = async () => {
  if (remotes.value.length === 0) {
    message.warning('请先添加远程仓库')
    showRemoteModal.value = true
    return
  }
  
  const remoteName = remotes.value.find(r => r.is_default)?.name || remotes.value[0]?.name
  const remoteUrl = remotes.value.find(r => r.is_default)?.url || remotes.value[0]?.url
  
  if (!remoteUrl) {
    message.warning('请先添加远程仓库URL')
    showRemoteModal.value = true
    return
  }
  
  message.info('拉取功能演示中，请使用以下Git命令拉取:\n' + 
    `git pull ${remoteName} ${status.value.currentBranch}`)
  
  pulling.value = true
  try {
    const res: any = await pullRemote({
      projectId: props.projectId,
      remoteName: remoteName
    })
    if (res.code === 200) {
      message.success('已记录拉取操作（演示模式）')
    }
  } finally {
    pulling.value = false
  }
}

const handleAddRemote = async () => {
  if (!remoteForm.value.name.trim() || !remoteForm.value.url.trim()) {
    message.warning('请填写远程仓库名称和URL')
    return
  }
  addingRemote.value = true
  try {
    const res: any = await addRemote({
      projectId: props.projectId,
      name: remoteForm.value.name,
      url: remoteForm.value.url,
      isDefault: remoteForm.value.isDefault
    })
    if (res.code === 200) {
      message.success('远程仓库添加成功')
      showRemoteModal.value = false
      remoteForm.value = { name: 'origin', url: '', isDefault: true }
      loadStatus()
    } else {
      message.error(res.message || '添加失败')
    }
  } catch (error: any) {
    message.error('添加失败: ' + error.message)
  } finally {
    addingRemote.value = false
  }
}

const handleDeleteRemote = async (name: string) => {
  try {
    const res: any = await deleteRemote(props.projectId, name)
    if (res.code === 200) {
      message.success('远程仓库已删除')
      loadStatus()
    } else {
      message.error(res.message || '删除失败')
    }
  } catch (error: any) {
    message.error('删除失败: ' + error.message)
  }
}

const handleCreateBranch = async () => {
  if (!branchForm.value.name.trim()) {
    message.warning('请输入分支名称')
    return
  }
  creatingBranch.value = true
  try {
    const res: any = await createBranch({
      projectId: props.projectId,
      name: branchForm.value.name
    })
    if (res.code === 200) {
      message.success('分支创建成功')
      showCreateBranchModal.value = false
      branchForm.value.name = ''
      loadStatus()
    } else {
      message.error(res.message || '创建失败')
    }
  } catch (error: any) {
    message.error('创建失败: ' + error.message)
  } finally {
    creatingBranch.value = false
  }
}

const handleDeleteBranch = async (name: string) => {
  try {
    const res: any = await apiDeleteBranch(props.projectId, name)
    if (res.code === 200) {
      message.success('分支已删除')
      loadStatus()
    } else {
      message.error(res.message || '删除失败')
    }
  } catch (error: any) {
    message.error('删除失败: ' + error.message)
  }
}

const showCommitHistory = () => {
  message.info('查看提交历史功能开发中')
}

watch(() => props.projectId, (val) => {
  if (val) {
    loadStatus()
  }
}, { immediate: true })
</script>

<style scoped>
.git-panel {
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}

.git-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  background: #fafafa;
  border-radius: 4px;
}

.git-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  font-size: 13px;
}

.git-icon {
  color: #f97316;
}

.expand-icon {
  font-size: 10px;
  color: #999;
}

.git-actions {
  display: flex;
  gap: 8px;
}

.git-actions span {
  cursor: pointer;
  color: #666;
  font-size: 14px;
}

.git-actions span:hover {
  color: #1890ff;
}

.git-content {
  padding: 8px 12px;
}

.init-section {
  margin-bottom: 8px;
}

.init-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f0f5ff 0%, #fafafa 100%);
  border: 1px solid #d6e4ff;
  border-radius: 8px;
}

.init-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1890ff;
  border-radius: 8px;
  font-size: 20px;
  color: white;
}

.init-info {
  flex: 1;
}

.init-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.init-desc {
  font-size: 11px;
  color: #999;
}

.quick-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.info-section {
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}

.section-title .count {
  color: #999;
  font-weight: normal;
}

.branch-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.remote-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.remote-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
}

.remote-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.no-remote {
  font-size: 12px;
  color: #999;
}

.commit-list {
  max-height: 200px;
  overflow-y: auto;
}

.commit-item {
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
}

.commit-item:hover {
  background: #f5f5f5;
}

.commit-message {
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.commit-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.commit-hash {
  font-size: 10px;
  font-family: monospace;
}

.commit-time {
  font-size: 10px;
  color: #999;
}

.branches-modal .branch-actions {
  margin-bottom: 12px;
}

.branch-list-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 深色模式适配 */
.dark-theme .git-panel {
  border-top-color: #303030;
}

.dark-theme .git-header {
  background: #2a2a2a;
}

.dark-theme .init-card {
  background: linear-gradient(135deg, #1a1a2e 0%, #2a2a2a 100%);
  border-color: #434343;
}

.dark-theme .init-icon {
  background: #1890ff;
}

.dark-theme .init-title {
  color: rgba(255, 255, 255, 0.9);
}

.dark-theme .init-desc {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .section-title {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .commit-message {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .remote-item {
  background: #2a2a2a;
}

.dark-theme .commit-item:hover {
  background: rgba(255, 255, 255, 0.05);
}
</style>
