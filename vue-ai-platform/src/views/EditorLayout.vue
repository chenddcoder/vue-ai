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
          <a-button type="primary" @click="handleSave" :loading="saving">
            <template #icon><SaveOutlined /></template>
            保存
          </a-button>
          
          <span v-if="projectStore.hasUnsavedChanges()" class="save-status">
            <ClockCircleOutlined style="color: #faad14; margin-right: 4px" />
            未保存
          </span>
          <span v-else-if="projectStore.lastAutoSaveTime" class="save-status">
            <CheckCircleOutlined style="color: #52c41a; margin-right: 4px" />
            已保存 {{ formatTime(projectStore.lastAutoSaveTime) }}
          </span>
          
          <a-button type="primary" @click="showPublishModal" :disabled="userStore.isGuest" danger>
            <template #icon><CloudUploadOutlined /></template>
            发布
          </a-button>
          
          <a-dropdown v-if="userStore.currentUser">
            <a-button type="text" class="user-btn">
              <a-avatar :src="userStore.currentUser.avatar" :size="24" style="margin-right: 8px">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              {{ userStore.currentUser.username }}
              <span v-if="userStore.isGuest" class="guest-badge">游客</span>
              <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="showAvatarModal">
                  <UserOutlined />
                  更换头像
                </a-menu-item>
                <a-menu-divider v-if="!userStore.isGuest" />
                <a-menu-item @click="goAIConfig" v-if="!userStore.isGuest">
                  <RobotOutlined />
                  AI配置
                </a-menu-item>
                <a-menu-item v-if="userStore.isGuest" @click="goLogin">
                  <LoginOutlined />
                  登录/注册
                </a-menu-item>
                <a-menu-divider v-if="userStore.isGuest" />
                <a-menu-item @click="logout">
                  <LogoutOutlined />
                  退出
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          
          <a-button v-else type="primary" @click="goLogin">登录</a-button>
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

  <!-- 头像上传弹窗 -->
  <a-modal
    v-model:visible="avatarModalVisible"
    title="更换头像"
    @ok="handleAvatarSave"
    :confirmLoading="avatarUploading"
    okText="保存"
  >
    <a-form layout="vertical">
      <a-form-item label="选择头像">
        <a-avatar :src="previewAvatar" :size="100" style="margin-bottom: 16px">
          <template #icon><UserOutlined /></template>
        </a-avatar>
        <a-input-search
          v-model:value="avatarUrl"
          placeholder="输入头像图片URL"
          enter-button="预览"
          @search="previewAvatar = avatarUrl"
        />
        <p style="margin-top: 8px; color: #999; font-size: 12px;">提示：可以直接粘贴图片链接，如 https://api.dicebear.com/7.x/avataaars/svg?seed=...</p>
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
  LogoutOutlined,
  LoginOutlined,
  AppstoreOutlined,
  RobotOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import FileTree from '@/components/FileTree.vue'
import MonacoEditor from '@/components/editor/MonacoEditor.vue'
import Preview from '@/components/preview/Preview.vue'
import AIAssistant from '@/components/editor/AIAssistant.vue'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { saveProject, publishApp, getProject, updateUserAvatar, getPublishedAppByProject } from '@/api'

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

const avatarModalVisible = ref(false)
const avatarUploading = ref(false)
const avatarUrl = ref('')
const previewAvatar = ref('')

let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

const formatTime = (date: Date) => {
  return new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const triggerAutoSave = () => {
  if (!projectStore.autoSaveEnabled || projectStore.isAutoSaving) return
  
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  
  autoSaveTimer = setTimeout(async () => {
    if (!projectStore.hasUnsavedChanges()) return
    
    projectStore.isAutoSaving = true
    try {
      const projectId = route.params.id === 'new' ? undefined : Number(route.params.id)
      if (!projectId && !projectStore.currentProjectId) {
        projectStore.isAutoSaving = false
        return
      }
      
      const res: any = await saveProject({
        id: projectStore.currentProjectId || projectId,
        name: projectStore.projectName || '未命名项目',
        description: 'Vue AI Project',
        ownerId: userStore.currentUser?.id || 0,
        content: projectStore.files
      })
      
      if (res.code === 200) {
        projectStore.lastAutoSaveTime = new Date()
        const savedProjectId = res.data?.id
        if (savedProjectId && !projectStore.currentProjectId) {
          projectStore.setCurrentProjectId(savedProjectId)
          projectStore.setProjectName(res.data.name)
          if (route.params.id === 'new') {
            router.replace(`/project/${savedProjectId}`)
          }
        }
      }
    } catch (err: any) {
      console.error('Auto save failed:', err.message)
    } finally {
      projectStore.isAutoSaving = false
    }
  }, projectStore.autoSaveDelay)
}

watch(() => projectStore.files, () => {
  triggerAutoSave()
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

// 跳转到登录
const goLogin = () => {
  userStore.logout()
  router.push('/login')
}

// 退出登录
const logout = () => {
  userStore.logout()
  router.push('/login')
  message.success('已退出登录')
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

const showAvatarModal = () => {
  avatarUrl.value = userStore.currentUser?.avatar || ''
  previewAvatar.value = avatarUrl.value
  avatarModalVisible.value = true
}

const handleAvatarSave = async () => {
  if (!avatarUrl.value.trim()) {
    message.error('请输入头像URL')
    return
  }
  
  avatarUploading.value = true
  try {
    const res: any = await updateUserAvatar(userStore.currentUser!.id, avatarUrl.value)
    if (res.code === 200) {
      message.success('头像更新成功！')
      userStore.setUser(res.data)
      avatarModalVisible.value = false
    } else {
      message.error(res.message || '头像更新失败')
    }
  } catch (err: any) {
    message.error('头像更新失败: ' + err.message)
  } finally {
    avatarUploading.value = false
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
</style>
