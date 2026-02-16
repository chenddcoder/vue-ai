<template>
  <div class="git-manager">
    <a-modal
      v-model:open="visible"
      title="Git 版本管理"
      width="900px"
      :footer="null"
      @cancel="close"
    >
      <div class="git-container">
        <!-- 左侧分支列表 -->
        <div class="branch-sidebar">
          <div class="branch-header">
            <h4>分支</h4>
            <a-button type="primary" size="small" @click="showCreateBranchModal">
              <PlusOutlined />
            </a-button>
          </div>
          <a-list
            :data-source="branches"
            size="small"
            class="branch-list"
          >
            <template #renderItem="{ item }">
              <a-list-item
                :class="['branch-item', { active: currentBranch === item.name }]"
                @click="switchBranch(item.name)"
              >
                <div class="branch-info">
                  <BranchesOutlined />
                  <span class="branch-name">{{ item.name }}</span>
                  <a-tag v-if="item.is_default" color="blue" size="small">默认</a-tag>
                </div>
                <a-popconfirm
                  v-if="!item.is_default"
                  title="确定要删除这个分支吗？"
                  @confirm="deleteBranch(item.name)"
                >
                  <DeleteOutlined class="delete-icon" @click.stop />
                </a-popconfirm>
              </a-list-item>
            </template>
          </a-list>
        </div>

        <!-- 右侧提交历史 -->
        <div class="commit-main">
          <div class="commit-header">
            <h4>{{ currentBranch }} 分支的提交历史</h4>
            <a-space>
              <a-button type="primary" @click="showCommitModal">
                <PlusOutlined /> 提交
              </a-button>
              <a-button @click="initGit" v-if="!isInitialized">
                <DownloadOutlined /> 初始化Git
              </a-button>
            </a-space>
          </div>

          <div v-if="!isInitialized" class="not-initialized">
            <a-empty description="Git尚未初始化">
              <a-button type="primary" @click="initGit">初始化Git仓库</a-button>
            </a-empty>
          </div>

          <a-timeline v-else class="commit-timeline">
            <a-timeline-item
              v-for="commit in commits"
              :key="commit.commit_hash"
              :color="commit.commit_hash === currentCommitHash ? 'blue' : 'gray'"
            >
              <div class="commit-item">
                <div class="commit-header-line">
                  <a-tag class="commit-hash">{{ commit.commit_hash }}</a-tag>
                  <span class="commit-time">{{ formatTime(commit.create_time) }}</span>
                </div>
                <div class="commit-message">{{ commit.message }}</div>
                <div class="commit-author">
                  <a-avatar :src="commit.author_avatar" :size="16">
                    <UserOutlined />
                  </a-avatar>
                  <span>{{ commit.author_name }}</span>
                </div>
                <div class="commit-actions">
                  <a-button type="link" size="small" @click="checkoutCommit(commit.commit_hash)">
                    检出
                  </a-button>
                  <a-button type="link" size="small" @click="showDiff(commit)">
                    查看
                  </a-button>
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>

          <a-pagination
            v-if="isInitialized && total > 0"
            v-model:current="currentPage"
            v-model:pageSize="pageSize"
            :total="total"
            size="small"
            class="commit-pagination"
            @change="loadCommits"
          />
        </div>
      </div>
    </a-modal>

    <!-- 创建提交对话框 -->
    <a-modal
      v-model:open="commitModalVisible"
      title="创建提交"
      @ok="handleCommit"
      :confirmLoading="committing"
    >
      <a-form :model="commitForm" layout="vertical">
        <a-form-item label="提交信息" required>
          <a-textarea
            v-model:value="commitForm.message"
            placeholder="请输入提交信息，描述本次更改..."
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 创建分支对话框 -->
    <a-modal
      v-model:open="branchModalVisible"
      title="创建分支"
      @ok="handleCreateBranch"
      :confirmLoading="creatingBranch"
    >
      <a-form :model="branchForm" layout="vertical">
        <a-form-item label="分支名称" required>
          <a-input
            v-model:value="branchForm.name"
            placeholder="请输入分支名称"
          />
        </a-form-item>
        <a-form-item label="基于提交">
          <a-select
            v-model:value="branchForm.fromCommit"
            placeholder="选择基于的提交（默认最新）"
          >
            <a-select-option value="">最新提交</a-select-option>
            <a-select-option
              v-for="commit in commits.slice(0, 5)"
              :key="commit.commit_hash"
              :value="commit.commit_hash"
            >
              {{ commit.commit_hash }} - {{ commit.message.substring(0, 30) }}...
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看提交详情对话框 -->
    <a-modal
      v-model:open="diffModalVisible"
      title="提交详情"
      width="800px"
      :footer="null"
    >
      <div v-if="selectedCommit" class="commit-detail">
        <div class="detail-header">
          <div class="detail-hash">
            <span class="label">提交：</span>
            <a-tag>{{ selectedCommit.commit_hash }}</a-tag>
          </div>
          <div class="detail-parent" v-if="selectedCommit.parent_hash">
            <span class="label">父提交：</span>
            <a-tag>{{ selectedCommit.parent_hash }}</a-tag>
          </div>
        </div>
        <div class="detail-message">
          <span class="label">提交信息：</span>
          <p>{{ selectedCommit.message }}</p>
        </div>
        <div class="detail-author">
          <span class="label">作者：</span>
          <a-avatar :src="selectedCommit.author_avatar" :size="24">
            <UserOutlined />
          </a-avatar>
          <span>{{ selectedCommit.author_name }}</span>
          <span class="time">{{ formatTime(selectedCommit.create_time) }}</span>
        </div>
        <div class="detail-content">
          <span class="label">项目内容：</span>
          <pre class="content-preview">{{ formatContent(selectedCommit.content) }}</pre>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DeleteOutlined,
  BranchesOutlined,
  UserOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getCommits,
  createCommit,
  getBranches,
  createBranch,
  deleteBranch as apiDeleteBranch,
  checkoutCommit as apiCheckoutCommit,
  initGit,
  getCommitDetail
} from '@/api'

const props = defineProps<{
  visible: boolean
  projectId: number
  projectContent: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'checkout': [content: string]
}>()

const userStore = useUserStore()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const isInitialized = ref(false)
const commits = ref<any[]>([])
const branches = ref<any[]>([])
const currentBranch = ref('main')
const currentCommitHash = ref('')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const commitModalVisible = ref(false)
const committing = ref(false)
const commitForm = ref({
  message: ''
})

const branchModalVisible = ref(false)
const creatingBranch = ref(false)
const branchForm = ref({
  name: '',
  fromCommit: ''
})

const diffModalVisible = ref(false)
const selectedCommit = ref<any>(null)

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 格式化内容
const formatContent = (content: string) => {
  if (!content) return ''
  try {
    const parsed = JSON.parse(content)
    return JSON.stringify(parsed, null, 2).substring(0, 2000)
  } catch {
    return content.substring(0, 2000)
  }
}

// 加载提交历史
const loadCommits = async () => {
  if (!props.projectId) return
  
  try {
    const res: any = await getCommits(
      props.projectId,
      currentBranch.value,
      currentPage.value,
      pageSize.value
    )
    
    if (res.code === 200) {
      commits.value = res.data || []
      total.value = res.total || 0
      isInitialized.value = commits.value.length > 0
      
      if (commits.value.length > 0) {
        currentCommitHash.value = commits.value[0].commit_hash
      }
    }
  } catch (error: any) {
    message.error('加载提交历史失败: ' + error.message)
  }
}

// 加载分支列表
const loadBranches = async () => {
  if (!props.projectId) return
  
  try {
    const res: any = await getBranches(props.projectId)
    if (res.code === 200) {
      branches.value = res.data || []
      
      // 找到默认分支
      const defaultBranch = branches.value.find(b => b.is_default)
      if (defaultBranch && !currentBranch.value) {
        currentBranch.value = defaultBranch.name
      }
    }
  } catch (error: any) {
    message.error('加载分支列表失败: ' + error.message)
  }
}

// 切换分支
const switchBranch = (branchName: string) => {
  currentBranch.value = branchName
  currentPage.value = 1
  loadCommits()
}

// 显示创建提交对话框
const showCommitModal = () => {
  commitForm.value.message = ''
  commitModalVisible.value = true
}

// 处理提交
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
      branch: currentBranch.value
    })
    
    if (res.code === 200) {
      message.success('提交成功')
      commitModalVisible.value = false
      loadCommits()
    } else {
      message.error(res.message || '提交失败')
    }
  } catch (error: any) {
    message.error('提交失败: ' + error.message)
  } finally {
    committing.value = false
  }
}

// 显示创建分支对话框
const showCreateBranchModal = () => {
  branchForm.value = { name: '', fromCommit: '' }
  branchModalVisible.value = true
}

// 处理创建分支
const handleCreateBranch = async () => {
  if (!branchForm.value.name.trim()) {
    message.warning('请输入分支名称')
    return
  }
  
  creatingBranch.value = true
  try {
    const res: any = await createBranch({
      projectId: props.projectId,
      name: branchForm.value.name,
      fromCommit: branchForm.value.fromCommit || undefined
    })
    
    if (res.code === 200) {
      message.success('分支创建成功')
      branchModalVisible.value = false
      loadBranches()
    } else {
      message.error(res.message || '创建失败')
    }
  } catch (error: any) {
    message.error('创建分支失败: ' + error.message)
  } finally {
    creatingBranch.value = false
  }
}

// 删除分支
const deleteBranch = async (branchName: string) => {
  try {
    const res: any = await apiDeleteBranch(props.projectId, branchName)
    if (res.code === 200) {
      message.success('分支删除成功')
      if (currentBranch.value === branchName) {
        currentBranch.value = 'main'
      }
      loadBranches()
    } else {
      message.error(res.message || '删除失败')
    }
  } catch (error: any) {
    message.error('删除分支失败: ' + error.message)
  }
}

// 检出提交
const checkoutCommit = async (commitHash: string) => {
  try {
    const res: any = await apiCheckoutCommit({
      projectId: props.projectId,
      commitHash
    })
    
    if (res.code === 200) {
      message.success('检出成功')
      emit('checkout', res.data.content)
      close()
    } else {
      message.error(res.message || '检出失败')
    }
  } catch (error: any) {
    message.error('检出失败: ' + error.message)
  }
}

// 显示提交详情
const showDiff = async (commit: any) => {
  selectedCommit.value = commit
  diffModalVisible.value = true
}

// 初始化Git
const handleInitGit = async () => {
  try {
    const res: any = await initGit(props.projectId)
    if (res.code === 200) {
      message.success('Git初始化成功')
      loadCommits()
      loadBranches()
    } else {
      message.error(res.message || '初始化失败')
    }
  } catch (error: any) {
    message.error('初始化失败: ' + error.message)
  }
}

// 关闭对话框
const close = () => {
  visible.value = false
}

// 监听visible变化
watch(() => props.visible, (val) => {
  if (val) {
    loadCommits()
    loadBranches()
  }
})
</script>

<style scoped>
.git-container {
  display: flex;
  height: 600px;
}

.branch-sidebar {
  width: 200px;
  border-right: 1px solid #f0f0f0;
  padding-right: 16px;
  overflow-y: auto;
}

.branch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.branch-header h4 {
  margin: 0;
}

.branch-list {
  max-height: 520px;
  overflow-y: auto;
}

.branch-item {
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s;
}

.branch-item:hover {
  background: #f5f5f5;
}

.branch-item.active {
  background: #e6f7ff;
  border-right: 3px solid #1890ff;
}

.branch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.branch-name {
  font-weight: 500;
}

.delete-icon {
  color: #999;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
}

.branch-item:hover .delete-icon {
  opacity: 1;
}

.delete-icon:hover {
  color: #ff4d4f;
}

.commit-main {
  flex: 1;
  padding-left: 16px;
  overflow-y: auto;
}

.commit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.commit-header h4 {
  margin: 0;
}

.not-initialized {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.commit-timeline {
  max-height: 480px;
  overflow-y: auto;
}

.commit-item {
  padding: 8px 0;
}

.commit-header-line {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.commit-hash {
  font-family: monospace;
  font-size: 12px;
}

.commit-time {
  font-size: 12px;
  color: #999;
}

.commit-message {
  font-weight: 500;
  margin: 4px 0;
  word-break: break-all;
}

.commit-author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.commit-actions {
  display: flex;
  gap: 8px;
}

.commit-pagination {
  margin-top: 16px;
  text-align: right;
}

.commit-detail {
  padding: 16px;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-hash,
.detail-parent {
  margin-bottom: 8px;
}

.detail-message {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.detail-message p {
  margin: 8px 0 0 0;
  font-weight: 500;
}

.detail-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-author .time {
  color: #999;
  font-size: 12px;
}

.detail-content {
  margin-top: 16px;
}

.content-preview {
  margin-top: 8px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
  font-size: 12px;
  line-height: 1.5;
}

.label {
  color: #666;
  font-size: 12px;
}

/* 深色模式适配 */
.dark-theme .branch-sidebar {
  border-right-color: #303030;
}

.dark-theme .branch-header,
.dark-theme .commit-header {
  border-bottom-color: #303030;
}

.dark-theme .branch-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.dark-theme .branch-item.active {
  background: rgba(24, 144, 255, 0.15);
}

.dark-theme .delete-icon {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .commit-time,
.dark-theme .detail-author .time {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .commit-author {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .detail-message,
.dark-theme .content-preview {
  background: #2a2a2a;
}

.dark-theme .label {
  color: rgba(255, 255, 255, 0.65);
}
</style>
