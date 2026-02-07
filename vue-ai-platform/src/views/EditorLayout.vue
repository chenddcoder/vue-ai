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
          
          <a-button type="primary" @click="showPublishModal" :disabled="userStore.isGuest" danger>
            <template #icon><CloudUploadOutlined /></template>
            发布
          </a-button>
          
          <a-dropdown v-if="userStore.currentUser">
            <a-button type="text" class="user-btn">
              <UserOutlined />
              {{ userStore.currentUser.username }}
              <span v-if="userStore.isGuest" class="guest-badge">游客</span>
              <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="goAIConfig">
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
    
    <a-layout>
      <a-layout-sider width="200" style="background: #fff">
        <FileTree />
      </a-layout-sider>
      <a-layout style="padding: 0 24px 24px">
        <a-layout-content :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px', display: 'flex' }">
          <div class="editor-pane">
            <MonacoEditor />
          </div>
          <div class="preview-pane">
            <Preview />
          </div>
          <div class="ai-pane" v-if="showAI">
            <AIAssistant />
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>

  <!-- 发布应用弹窗 -->
  <a-modal
    v-model:visible="publishModalVisible"
    title="发布应用到市场"
    @ok="handlePublish"
    :confirmLoading="publishing"
    okText="发布"
  >
    <a-form :model="publishForm" layout="vertical">
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
      <a-form-item label="标签">
        <a-select
          v-model:value="publishForm.tags"
          mode="tags"
          placeholder="添加标签，方便其他人搜索"
          :tokenSeparators="[',']"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
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
  RobotOutlined
} from '@ant-design/icons-vue'
import FileTree from '@/components/FileTree.vue'
import MonacoEditor from '@/components/editor/MonacoEditor.vue'
import Preview from '@/components/preview/Preview.vue'
import AIAssistant from '@/components/editor/AIAssistant.vue'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { saveProject, publishApp } from '@/api'

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const userStore = useUserStore()

const showAI = ref(true)
const saving = ref(false)
const publishing = ref(false)
const publishModalVisible = ref(false)

const publishForm = ref({
  name: '',
  description: '',
  tags: [] as string[]
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
const handleSave = async () => {
  saving.value = true
  try {
    await saveProject({
      id: route.params.id === 'new' ? undefined : Number(route.params.id),
      name: 'Demo Project',
      description: 'Vue AI Project',
      ownerId: userStore.currentUser?.id || 0,
      content: projectStore.files
    })
    message.success('项目保存成功！')
  } catch (err: any) {
    message.error('保存失败: ' + err.message)
  } finally {
    saving.value = false
  }
}

// 显示发布弹窗
const showPublishModal = () => {
  if (userStore.isGuest) {
    message.warning('请先登录后再发布应用')
    return
  }
  publishModalVisible.value = true
}

// 发布应用
const handlePublish = async () => {
  if (!publishForm.value.name.trim()) {
    message.error('请输入应用名称')
    return
  }
  if (!publishForm.value.description.trim()) {
    message.error('请输入应用描述')
    return
  }
  
  publishing.value = true
  let hideSaveLoading: (() => void) | null = null
  let hidePublishLoading: (() => void) | null = null
  
  try {
    // 确定项目ID（来自我的应用或新建）
    const projectId = projectStore.fromMyApps && projectStore.currentProjectId 
      ? projectStore.currentProjectId 
      : (route.params.id === 'new' ? undefined : Number(route.params.id))
    
    // 先保存项目
    hideSaveLoading = message.loading('正在保存项目...', 0)
    const saveRes: any = await saveProject({
      id: projectId,
      name: publishForm.value.name,
      description: publishForm.value.description,
      ownerId: userStore.currentUser?.id || 1,
      content: projectStore.files
    })
    if (hideSaveLoading) hideSaveLoading()
    
    // 获取保存后的项目ID
    const savedProjectId = saveRes.data?.id || projectId
    
    // 然后发布应用
    hidePublishLoading = message.loading('正在发布应用...', 0)
    const publishRes: any = await publishApp({
      projectId: savedProjectId,
      name: publishForm.value.name,
      description: publishForm.value.description,
      tags: publishForm.value.tags,
      content: projectStore.files,
      authorId: userStore.currentUser?.id,
      authorName: userStore.currentUser?.username
    })
    if (hidePublishLoading) hidePublishLoading()
    
    if (publishRes.code === 200) {
      message.success('应用发布成功！')
      publishModalVisible.value = false
      // 清空表单和项目上下文
      publishForm.value = { name: '', description: '', tags: [] }
      projectStore.clearProjectContext()
      // 跳转到应用市场
      router.push('/market')
    } else {
      message.error(publishRes.message || '发布失败')
    }
  } catch (err: any) {
    if (hideSaveLoading) hideSaveLoading()
    if (hidePublishLoading) hidePublishLoading()
    message.error('发布失败: ' + (err.message || '未知错误'))
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
  border: 1px solid #eee;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-pane {
  width: 300px;
  height: 100%;
  border-left: 1px solid #ccc;
}
</style>
