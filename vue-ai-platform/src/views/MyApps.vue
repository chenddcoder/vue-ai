<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header class="header">
      <div class="header-left">
        <div class="logo" @click="goHome">Vue AI Platform</div>
        <a-menu theme="dark" mode="horizontal" :selectedKeys="['my-apps']" :style="{ lineHeight: '64px' }">
          <a-menu-item key="editor" @click="goHome">编辑器</a-menu-item>
          <a-menu-item key="market" @click="goMarket">应用市场</a-menu-item>
          <a-menu-item key="my-apps">我的应用</a-menu-item>
        </a-menu>
      </div>
      
      <div class="header-right">
        <a-dropdown v-if="userStore.currentUser">
          <a-button type="text" class="user-btn">
            <UserOutlined />
            {{ userStore.currentUser.username }}
            <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="logout">
                <LogoutOutlined />
                退出
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </a-layout-header>

    <a-layout-content class="content">
      <div class="page-header">
        <h1>我的应用</h1>
        <a-button type="primary" @click="goHome">
          <PlusOutlined />
          创建新应用
        </a-button>
      </div>

      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <a-tab-pane key="projects" tab="我的项目">
          <div class="projects-grid">
            <a-spin :spinning="loading && activeTab === 'projects'">
              <a-row :gutter="[24, 24]">
                <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="project in projects" :key="project.id">
                  <a-card class="project-card" :title="project.name">
                    <p class="project-desc">{{ project.description || '暂无描述' }}</p>
                    <div class="project-meta">
                      <span>更新于 {{ project.updateTime }}</span>
                    </div>
                    <template #actions>
                      <a-button type="link" @click="editProject(project)">
                        <EditOutlined />
                        编辑
                      </a-button>
                      <a-button type="link" @click="openRenameModal(project)">
                        <FontSizeOutlined />
                        重命名
                      </a-button>
                      <a-button 
                        type="link" 
                        @click="publishProject(project)"
                        v-if="!project.published"
                      >
                        <CloudUploadOutlined />
                        发布
                      </a-button>
                      <a-button 
                        type="link" 
                        danger
                        @click="handleDeleteProject(project.id)"
                      >
                        <DeleteOutlined />
                        删除
                      </a-button>
                    </template>
                  </a-card>
                </a-col>
              </a-row>
              <div v-if="!loading && activeTab === 'projects' && projects.length === 0" class="empty-state">
                <a-empty description="暂无项目" />
              </div>
            </a-spin>
          </div>
        </a-tab-pane>

        <a-tab-pane key="published" tab="已发布应用">
          <div class="apps-grid">
            <a-spin :spinning="loading && activeTab === 'published'">
              <a-row :gutter="[24, 24]">
                <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="app in publishedApps" :key="app.id">
                  <a-card class="app-card" :title="app.name">
                    <p class="app-desc">{{ app.description || '暂无描述' }}</p>
                    <div class="app-stats">
                      <a-space>
                        <span><EyeOutlined /> {{ app.views }}</span>
                        <span><LikeOutlined /> {{ app.likes }}</span>
                      </a-space>
                    </div>
                    <div class="app-meta">
                      <span>发布于 {{ app.publishDate }}</span>
                    </div>
                    <template #actions>
                      <a-button type="link" @click="viewApp(app.id)">
                        <EyeOutlined />
                        查看
                      </a-button>
                      <a-button 
                        type="link" 
                        danger
                        @click="handleUnpublishApp(app.id)"
                      >
                        <CloudDownloadOutlined />
                        下架
                      </a-button>
                    </template>
                  </a-card>
                </a-col>
              </a-row>
              <div v-if="!loading && activeTab === 'published' && publishedApps.length === 0" class="empty-state">
                <a-empty description="暂无已发布应用" />
              </div>
            </a-spin>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-layout-content>
  </a-layout>

  <!-- 发布项目弹窗 -->
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

  <!-- 重命名项目弹窗 -->
  <a-modal
    v-model:visible="renameModalVisible"
    title="重命名项目"
    @ok="handleRename"
    :confirmLoading="renaming"
    okText="确认"
  >
    <a-form-item label="项目名称">
      <a-input v-model:value="renameForm.name" placeholder="输入新项目名称" @pressEnter="handleRename" />
    </a-form-item>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { 
  UserOutlined, 
  DownOutlined,
  LogoutOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CloudUploadOutlined,
  CloudDownloadOutlined,
  EyeOutlined,
  LikeOutlined,
  FontSizeOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import { getProjectList, saveProject, publishApp, unpublishApp as apiUnpublishApp, getMyMarketApps, deleteProject, renameProject } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const projectStore = useProjectStore()

const activeTab = ref('projects')
const projects = ref<any[]>([])
const publishedApps = ref<any[]>([])
const loading = ref(false)
const publishModalVisible = ref(false)
const publishing = ref(false)
const selectedProject = ref<any>(null)
const renameModalVisible = ref(false)
const renaming = ref(false)
const renameForm = ref({ id: 0, name: '' })

const publishForm = ref({
  name: '',
  description: '',
  tags: [] as string[]
})

// 跳转到首页
const goHome = () => {
  router.push('/project/new')
}

// 跳转到市场
const goMarket = () => {
  router.push('/market')
}

// 退出登录
const logout = () => {
  userStore.logout()
  router.push('/login')
  message.success('已退出登录')
}

// 加载项目列表
const loadProjects = async () => {
  loading.value = true
  try {
    const res: any = await getProjectList(userStore.currentUser?.id || 0)
    if (res.code === 200) {
      projects.value = res.data || []
    } else {
      message.error(res.message || '加载项目列表失败')
    }
  } catch (error: any) {
    message.error('加载项目列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 加载已发布应用
const loadPublishedApps = async () => {
  loading.value = true
  try {
    const res: any = await getMyMarketApps(userStore.currentUser?.id || 0)
    if (res.code === 200) {
      publishedApps.value = res.data || []
    } else {
      message.error(res.message || '加载已发布应用失败')
    }
  } catch (error: any) {
    message.error('加载已发布应用失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 编辑项目
const editProject = (project: any) => {
  // 设置项目上下文为来自我的应用
  projectStore.setCurrentProjectId(project.id)
  projectStore.setFromMyApps(true)
  projectStore.setProjectName(project.name)
  router.push(`/project/${project.id}`)
}

// 删除项目
const handleDeleteProject = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复，是否确认删除？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await deleteProject(id)
        if (res.code === 200) {
          message.success('项目已删除')
          loadProjects()
        } else {
          message.error(res.message || '删除失败')
        }
      } catch (error: any) {
        message.error('删除失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

// 打开重命名弹窗
const openRenameModal = (project: any) => {
  renameForm.value = { id: project.id, name: project.name }
  renameModalVisible.value = true
}

// 执行重命名
const handleRename = async () => {
  if (!renameForm.value.name.trim()) {
    message.error('请输入项目名称')
    return
  }
  renaming.value = true
  try {
    const res: any = await renameProject(renameForm.value.id, renameForm.value.name)
    if (res.code === 200) {
      message.success('重命名成功')
      renameModalVisible.value = false
      loadProjects()
    } else {
      message.error(res.message || '重命名失败')
    }
  } catch (error: any) {
    message.error('重命名失败: ' + (error.message || '未知错误'))
  } finally {
    renaming.value = false
  }
}

// 发布项目
const publishProject = (project: any) => {
  selectedProject.value = project
  publishForm.value = {
    name: project.name,
    description: project.description || '',
    tags: []
  }
  publishModalVisible.value = true
}

// 执行发布
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
  let hideLoading: (() => void) | null = null
  
  try {
    // 先保存项目
    hideLoading = message.loading('正在保存并发布...', 0)
    await saveProject({
      id: selectedProject.value.id,
      name: publishForm.value.name,
      description: publishForm.value.description,
      ownerId: userStore.currentUser?.id,
      content: selectedProject.value.content || {}
    })
    
    // 然后发布应用
    const res: any = await publishApp({
      projectId: selectedProject.value.id,
      name: publishForm.value.name,
      description: publishForm.value.description,
      tags: publishForm.value.tags,
      authorId: userStore.currentUser?.id,
      authorName: userStore.currentUser?.username
    })
    if (hideLoading) hideLoading()
    
    if (res.code === 200) {
      message.success('应用发布成功！')
      publishModalVisible.value = false
      publishForm.value = { name: '', description: '', tags: [] }
      selectedProject.value = null
      loadProjects()
      loadPublishedApps()
    } else {
      message.error(res.message || '发布失败')
    }
  } catch (err: any) {
    if (hideLoading) hideLoading()
    message.error('发布失败: ' + (err.message || '未知错误'))
  } finally {
    publishing.value = false
  }
}

// 下架应用
const handleUnpublishApp = (id: number) => {
  Modal.confirm({
    title: '确认下架',
    content: '下架后其他人将无法在市场中看到该应用，是否确认下架？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await apiUnpublishApp(id)
        if (res.code === 200) {
          message.success('应用已下架')
          loadPublishedApps()
        } else {
          message.error(res.message || '下架失败')
        }
      } catch (error: any) {
        message.error('下架失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

// 查看应用
const viewApp = (id: number) => {
  router.push(`/market/app/${id}`)
}

// 切换标签页
const handleTabChange = (key: string) => {
  if (key === 'projects') {
    loadProjects()
  } else if (key === 'published') {
    loadPublishedApps()
  }
}

onMounted(() => {
  loadProjects()
})
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

.content {
  background: #f0f2f5;
  padding: 24px 48px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
}

.projects-grid, .apps-grid {
  margin-top: 24px;
  min-height: 300px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.project-card, .app-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.project-desc, .app-desc {
  color: #666;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-meta, .app-meta {
  color: #999;
  font-size: 12px;
}

.app-stats {
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

:deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
