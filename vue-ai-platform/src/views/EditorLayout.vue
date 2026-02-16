<template>
  <a-layout style="height: 100vh">
    <a-layout-header class="header">
      <div class="header-left">
        <div class="logo" @click="goHome">Vue AI Platform</div>
        <a-menu theme="dark" mode="horizontal" :selectedKeys="[currentMenuKey]" :style="{ lineHeight: '64px' }">
          <a-menu-item key="editor" @click="goHome">编辑器</a-menu-item>
          <a-menu-item key="market" @click="goMarket">应用市场</a-menu-item>
          <a-menu-item key="my-apps" @click="goMyApps" v-if="userStore.isLoggedIn">我的应用</a-menu-item>
          <a-menu-item key="ai-config" @click="goAIConfig">
            <template #icon>
              <RobotOutlined />
            </template>
            AI配置
          </a-menu-item>
        </a-menu>
        <div class="project-name" v-if="projectStore.projectName">
          <AppstoreOutlined />
          <span>{{ projectStore.projectName }}</span>
        </div>
      </div>
      
      <div class="header-right">
        <a-space>
          <ThemeToggle />
          
          <a-button type="primary" @click="handleSave" :loading="saving">
            <template #icon><SaveOutlined /></template>
            保存
          </a-button>
          
          <span v-if="projectStore.hasUnsavedChanges()" class="save-status">
            <ClockCircleOutlined style="color: #faad14; margin-right: 4px" />
            未保存
          </span>
          <span v-else class="save-status">
            <CheckCircleOutlined style="color: #52c41a; margin-right: 4px" />
            已保存
          </span>
          
          <a-button type="primary" @click="showPublishModal" :disabled="userStore.isGuest" danger>
            <template #icon><CloudUploadOutlined /></template>
            发布
          </a-button>
          
          <UserAvatar />
        </a-space>
      </div>
    </a-layout-header>
    
    <a-layout style="height: calc(100vh - 64px)">
      <a-layout-sider width="200" style="background: #fff; overflow: hidden; display: flex; flex-direction: column;">
        <FileTree />
      </a-layout-sider>
      <a-layout style="overflow: hidden">
        <a-layout-content style="padding: 0 24px 24px; height: 100%; display: flex; flex-direction: column">
          <a-spin :spinning="loading" style="flex: 1; display: flex; flex-direction: column">
            <div class="workspace" style="flex: 1; display: flex; min-height: 0">
              <div class="editor-pane" style="flex: 1; display: flex; flex-direction: column; min-width: 0">
                <MonacoEditor />
              </div>
              <div class="preview-pane" style="flex: 1; display: flex; flex-direction: column; min-width: 0">
                <Preview />
              </div>
              <div class="ai-pane" v-if="showAI" style="width: 300px; border-left: 1px solid #ccc; overflow: auto">
                <AIAssistant />
              </div>
            </div>
          </a-spin>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>

  <!-- 保存项目弹窗 -->
  <a-modal
    v-model:visible="saveModalVisible"
    title="保存项目"
    @ok="handleSaveConfirm"
    :confirmLoading="saving"
    okText="确认保存"
  >
    <a-form :model="saveForm" layout="vertical">
      <a-form-item label="项目名称" required>
        <a-input v-model:value="saveForm.name" placeholder="请输入项目名称" @pressEnter="handleSaveConfirm" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 发布应用弹窗 -->
  <a-modal
    v-model:visible="publishModalVisible"
    :title="isPublished ? '更新应用' : '发布应用到市场'"
    @ok="handlePublish"
    :confirmLoading="publishing"
    :okText="isPublished ? '更新' : '发布'"
  >
    <a-form :model="publishForm" layout="vertical">
      <template v-if="isPublished">
        <a-form-item label="版本号">
          <div class="version-info">
            <span class="current-version">当前版本: v{{ publishForm.currentVersion }}</span>
          </div>
          <a-radio-group v-model:value="publishForm.versionType">
            <a-radio value="patch">补丁版 (v{{ publishForm.currentVersion }} → v{{ nextPatchVersion }})</a-radio>
            <a-radio value="minor">次版本 (v{{ publishForm.currentVersion }} → v{{ nextMinorVersion }})</a-radio>
            <a-radio value="major">主版本 (v{{ publishForm.currentVersion }} → v{{ nextMajorVersion }})</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="更新内容" required>
          <a-textarea 
            v-model:value="publishForm.updateContent" 
            :rows="3" 
            placeholder="描述本次更新内容"
          />
        </a-form-item>
      </template>
      <template v-else>
        <a-form-item label="应用名称" required>
          <a-input v-model:value="publishForm.name" placeholder="给你的应用起个名字" />
        </a-form-item>
        <a-form-item label="应用描述" required>
          <a-textarea 
            v-model:value="publishForm.description" 
            :rows="4" 
            placeholder="描述一下你的应用是做什么的"
          />
        </a-form-item>
      </template>
      <a-form-item label="标签">
        <a-select
          v-model:value="publishForm.tags"
          mode="tags"
          placeholder="添加标签，方便其他人搜索"
          :tokenSeparators="[',']"
        />
      </a-form-item>
      <a-form-item label="开源设置">
        <a-switch v-model:checked="publishForm.isOpenSource" />
        <span style="margin-left: 10px">{{ publishForm.isOpenSource ? '开源（允许查看代码和使用模板）' : '闭源（仅允许预览和运行）' }}</span>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  SaveOutlined, 
  CloudUploadOutlined, 
  UserOutlined, 
  DownOutlined,
  AppstoreOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  RobotOutlined
} from '@ant-design/icons-vue'
import FileTree from '@/components/FileTree.vue'
import MonacoEditor from '@/components/editor/MonacoEditor.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import Preview from '@/components/preview/Preview.vue'
import AIAssistant from '@/components/editor/AIAssistant.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { saveProject, publishApp, getProject, getPublishedAppByProject } from '@/api'

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const userStore = useUserStore()

const showAI = ref(true)
const saving = ref(false)
const saveModalVisible = ref(false)
const saveForm = ref({
  name: ''
})
const publishing = ref(false)
const publishModalVisible = ref(false)
const loading = ref(false)
const isPublished = ref(false)

const publishForm = ref({
  name: '',
  description: '',
  tags: [] as string[],
  isOpenSource: true,
  appId: 0,
  currentVersion: '1.0.0',
  versionType: 'patch',
  updateContent: '',
  content: {}
})

const parseVersion = (version: string) => {
  const parts = version.split('.').map(p => parseInt(p.replace(/[^0-9]/g, '')) || 0)
  return { major: parts[0] || 0, minor: parts[1] || 0, patch: parts[2] || 0 }
}

const nextPatchVersion = computed(() => {
  const v = parseVersion(publishForm.value.currentVersion)
  return `${v.major}.${v.minor}.${v.patch + 1}`
})

const nextMinorVersion = computed(() => {
  const v = parseVersion(publishForm.value.currentVersion)
  return `${v.major}.${v.minor + 1}.0`
})

const nextMajorVersion = computed(() => {
  const v = parseVersion(publishForm.value.currentVersion)
  return `${v.major + 1}.0.0`
})

const formatTime = (date: Date) => {
  return new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

watch(() => projectStore.files, () => {
  // 自动保存已禁用
}, { deep: true })

const loadProject = async () => {
  const projectId = route.params.id
  if (projectId === 'new') {
    projectStore.clearProjectContext()
    projectStore.setProjectName('未命名项目')
    return
  }
  
  loading.value = true
  try {
    const res: any = await getProject(Number(projectId))
    if (res.code === 200 && res.data) {
      const project = res.data
      projectStore.setCurrentProjectId(project.id)
      projectStore.setProjectName(project.name)
      
      if (project.content) {
        try {
          const content = typeof project.content === 'string' ? JSON.parse(project.content) : project.content
          projectStore.setFiles(content)
          // Set first file as active if exists
          const fileKeys = Object.keys(content)
          if (fileKeys.length > 0) {
             // Prefer App.vue if exists
             if (content['App.vue']) {
               projectStore.setActiveFile('App.vue')
             } else {
               projectStore.setActiveFile(fileKeys[0])
             }
          }
        } catch (e) {
          console.error('Failed to parse project content:', e)
        }
      }
    }
  } catch (err: any) {
    message.error('加载项目失败: ' + err.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProject()
})

watch(() => route.params.id, () => {
  loadProject()
})

const currentMenuKey = computed(() => {
  if (route.path === '/market') return 'market'
  if (route.path === '/my-apps') return 'my-apps'
  if (route.path === '/ai-config') return 'ai-config'
  return 'editor'
})

// 跳转到首页
const goHome = () => {
  router.push('/project/new')
}

// 跳转到市场
const goMarket = () => {
  router.push('/market')
}

// 跳转到我的应用
const goMyApps = () => {
  router.push('/my-apps')
}

// 跳转到AI配置
const goAIConfig = () => {
  router.push('/ai-config')
}

// 保存项目
const executeSave = async () => {
  saving.value = true
  try {
    let projectId = route.params.id === 'new' ? undefined : Number(route.params.id)
    
    if (projectStore.fromMyApps && projectStore.currentProjectId) {
      projectId = projectStore.currentProjectId
    }
    
    const res: any = await saveProject({
      id: projectId,
      name: projectStore.projectName || '未命名项目',
      description: 'Vue AI Project',
      ownerId: userStore.currentUser?.id || 0,
      content: projectStore.files
    })
    
    if (res.code === 200) {
      message.success('项目保存成功！')
      const savedProjectId = res.data?.id
      
      if (savedProjectId) {
        projectStore.setCurrentProjectId(savedProjectId)
        projectStore.setProjectName(res.data.name)
        projectStore.markAsSaved()
        // If we were on 'new' route, replace with actual ID
        if (route.params.id === 'new') {
          router.replace(`/project/${savedProjectId}`)
        }
      }
    } else {
      message.error(res.message || '保存失败')
    }
  } catch (err: any) {
    message.error('保存失败: ' + err.message)
  } finally {
    saving.value = false
  }
}

const handleSave = async () => {
  const isNewProject = route.params.id === 'new'
  
  if (isNewProject) {
    saveForm.value.name = projectStore.projectName !== '未命名项目' ? projectStore.projectName : ''
    saveModalVisible.value = true
    return
  }
  
  await executeSave()
}

const handleSaveConfirm = async () => {
  if (!saveForm.value.name.trim()) {
    message.error('请输入项目名称')
    return
  }
  
  projectStore.setProjectName(saveForm.value.name)
  saveModalVisible.value = false
  await executeSave()
}

// 显示发布弹窗
const showPublishModal = async () => {
  if (userStore.isGuest) {
    message.warning('请先登录后再发布应用')
    return
  }
  
  // 检查是否已发布
  const projectId = projectStore.fromMyApps && projectStore.currentProjectId 
    ? projectStore.currentProjectId 
    : (route.params.id === 'new' ? undefined : Number(route.params.id))
  
  if (projectId) {
    try {
      const appRes: any = await getPublishedAppByProject(projectId)
      if (appRes.code === 200 && appRes.data) {
        // 已发布，显示更新界面
        isPublished.value = true
        publishForm.value = {
          name: appRes.data.name,
          description: appRes.data.description || '',
          tags: appRes.data.tags || [],
          isOpenSource: appRes.data.is_open_source === 1,
          appId: appRes.data.id,
          currentVersion: appRes.data.version,
          versionType: 'patch',
          updateContent: '',
          content: projectStore.files
        }
        publishModalVisible.value = true
        return
      }
    } catch (e) {
      console.error('检查发布状态失败:', e)
    }
  }
  
  // 未发布，显示新发布界面
  isPublished.value = false
  publishForm.value = {
    name: projectStore.projectName || '',
    description: '',
    tags: [],
    isOpenSource: true,
    appId: 0,
    currentVersion: '1.0.0',
    versionType: 'patch',
    updateContent: '',
    content: projectStore.files
  }
  publishModalVisible.value = true
}

// 发布应用
const handlePublish = async () => {
  if (!isPublished.value && !publishForm.value.name.trim()) {
    message.error('请输入应用名称')
    return
  }
  if (!publishForm.value.description.trim()) {
    message.error('请输入应用描述')
    return
  }
  if (isPublished.value && !publishForm.value.updateContent.trim()) {
    message.error('请输入更新内容')
    return
  }
  
  publishing.value = true
  let hideSaveLoading: (() => void) | null = null
  let hidePublishLoading: (() => void) | null = null
  
  try {
    const projectId = projectStore.fromMyApps && projectStore.currentProjectId 
      ? projectStore.currentProjectId 
      : (route.params.id === 'new' ? undefined : Number(route.params.id))
    
    hideSaveLoading = message.loading(isPublished.value ? '正在更新...' : '正在保存项目...', 0)
    const saveRes: any = await saveProject({
      id: projectId,
      name: publishForm.value.name,
      description: publishForm.value.description,
      ownerId: userStore.currentUser?.id || 1,
      content: projectStore.files
    })
    if (hideSaveLoading) hideSaveLoading()
    
    const savedProjectId = saveRes.data?.id || projectId
    
    hidePublishLoading = message.loading(isPublished.value ? '正在更新...' : '正在发布应用...', 0)
    const publishRes: any = await publishApp({
      projectId: savedProjectId,
      appId: isPublished.value ? publishForm.value.appId : undefined,
      name: publishForm.value.name,
      description: publishForm.value.description,
      tags: publishForm.value.tags,
      content: projectStore.files,
      authorId: userStore.currentUser?.id,
      authorName: userStore.currentUser?.username,
      isOpenSource: publishForm.value.isOpenSource,
      updateContent: isPublished.value ? publishForm.value.updateContent : undefined,
      versionType: isPublished.value ? publishForm.value.versionType : undefined
    })
    if (hidePublishLoading) hidePublishLoading()
    
    if (publishRes.code === 200) {
      message.success(publishRes.data.message || (isPublished.value ? '应用更新成功！' : '应用发布成功！'))
      publishModalVisible.value = false
      publishForm.value = { name: '', description: '', tags: [], isOpenSource: true, appId: 0, currentVersion: '1.0.0', versionType: 'patch', updateContent: '', content: {} }
      projectStore.clearProjectContext()
      router.push('/market')
    } else {
      message.error(publishRes.message || (isPublished.value ? '更新失败' : '发布失败'))
    }
  } catch (err: any) {
    if (hideSaveLoading) hideSaveLoading()
    if (hidePublishLoading) hidePublishLoading()
    message.error((isPublished.value ? '更新失败: ' : '发布失败: ') + (err.message || '未知错误'))
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
}

.project-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
  padding: 0 16px;
  height: 40px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  color: white;
  font-size: 14px;
}

.logo {
  color: white;
  font-size: 1.2rem;
  margin-right: 2rem;
  font-weight: 600;
  cursor: pointer;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-btn {
  color: white;
}

.guest-badge {
  background: #ff4d4f;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 4px;
}

.editor-pane, .preview-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.editor-pane :deep(.ant-card),
.preview-pane :deep(.ant-card) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-pane :deep(.ant-card-body),
.preview-pane :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-pane {
  width: 300px;
  border-left: 1px solid #ccc;
  overflow: hidden;
}

.ai-pane :deep(.ant-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-pane :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.version-info {
  margin-bottom: 12px;
}

.current-version {
  color: #1890ff;
  font-weight: 500;
}

.save-status {
  color: #ffffff;
  font-size: 13px;
  margin-left: 12px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}

/* 深色模式样式 */
.dark-theme .editor-layout :deep(.ant-layout-sider) {
  background: #1f1f1f !important;
}
.dark-theme .editor-layout :deep(.ant-tabs) {
  background: #1f1f1f;
}
.dark-theme .editor-layout :deep(.ant-tabs-tab) {
  color: rgba(255, 255, 255, 0.65);
}
.dark-theme .editor-layout :deep(.ant-tabs-tab-active) {
  color: #1890ff;
}
.dark-theme .editor-layout :deep(.ant-tabs-nav) {
  background: #1f1f1f;
  border-color: #303030;
}
.dark-theme .project-name {
  background: rgba(255, 255, 255, 0.1);
}
.dark-theme .project-name span {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .editor-container {
  background: #1e1e1e;
}
.dark-theme .preview-container {
  background: #f0f2f5;
}
.dark-theme .preview-frame {
  background: white;
}
.dark-theme .file-tree-header {
  background: #1f1f1f;
  border-color: #303030;
}
.dark-theme .file-tree-header .header-title {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .file-tree-content {
  background: #1f1f1f;
}
.dark-theme :deep(.ant-tree) {
  background: transparent;
}
.dark-theme :deep(.ant-tree-node-content-wrapper) {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme :deep(.ant-tree-node-selected) {
  background: #1890ff !important;
}
.dark-theme .header {
  background: #001529;
}
.dark-theme .logo {
  color: rgba(255, 255, 255, 0.95);
}
.dark-theme :deep(.ant-menu) {
  background: transparent;
}
.dark-theme :deep(.ant-menu-item) {
  color: rgba(255, 255, 255, 0.65);
}
.dark-theme :deep(.ant-menu-item:hover) {
  color: #fff;
}
.dark-theme :deep(.ant-menu-item-selected) {
  background: #1890ff;
  color: #fff;
}
.dark-theme .user-btn {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme :deep(.ant-btn-text) {
  color: rgba(255, 255, 255, 0.65);
}
.dark-theme :deep(.ant-btn-text:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}
.dark-theme :deep(.ant-divider) {
  border-color: #303030;
}
</style>
