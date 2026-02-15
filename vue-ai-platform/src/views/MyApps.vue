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
        <UserAvatar />
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
                      <a-tag v-if="project.publishedAppId" color="green" style="margin-left: 8px">
                        已发布 v{{ project.publishedVersion }}
                      </a-tag>
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
                        @click="handlePublish(project)"
                        :class="{ 'published-btn': project.publishedAppId }"
                      >
                        <CloudUploadOutlined />
                        {{ project.publishedAppId ? '更新' : '发布' }}
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
                    <div class="app-meta-row">
                      <a-tag color="blue">v{{ app.version }}</a-tag>
                      <span class="app-stats">
                        <EyeOutlined /> {{ app.views }}
                        <LikeOutlined /> {{ app.likes }}
                      </span>
                    </div>
                    <div class="app-meta">
                      <span>发布于 {{ app.publishDate }}</span>
                    </div>
                    <template #actions>
                      <a-button type="link" @click="viewApp(app.id)">
                        <EyeOutlined />
                        查看
                      </a-button>
                      <a-button type="link" @click="openVersionHistory(app)">
                        <HistoryOutlined />
                        版本历史
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

  <!-- 发布/更新项目弹窗 -->
  <a-modal
    v-model:visible="publishModalVisible"
    :title="isUpdate ? '更新应用' : '发布应用到市场'"
    @ok="handlePublishSubmit"
    :confirmLoading="publishing"
    :okText="isUpdate ? '更新' : '发布'"
  >
    <a-form :model="publishForm" layout="vertical">
      <a-form-item :label="isUpdate ? '版本号' : '应用名称'" :required="!isUpdate">
        <template v-if="isUpdate">
          <a-space direction="vertical" style="width: 100%">
            <div class="version-info">
              <span class="current-version">当前版本: v{{ publishForm.currentVersion }}</span>
            </div>
            <a-radio-group v-model:value="publishForm.versionType">
              <a-radio value="patch">补丁版 (v{{ publishForm.currentVersion }} → v{{ nextPatchVersion }})</a-radio>
              <a-radio value="minor">次版本 (v{{ publishForm.currentVersion }} → v{{ nextMinorVersion }})</a-radio>
              <a-radio value="major">主版本 (v{{ publishForm.currentVersion }} → v{{ nextMajorVersion }})</a-radio>
            </a-radio-group>
          </a-space>
        </template>
        <a-input v-else v-model:value="publishForm.name" placeholder="给你的应用起个名字" />
      </a-form-item>
      
      <a-form-item label="应用描述" required>
        <a-textarea 
          v-model:value="publishForm.description" 
          :rows="4" 
          placeholder="描述一下你的应用是做什么的"
        />
      </a-form-item>
      
      <a-form-item label="更新内容" v-if="isUpdate">
        <a-textarea 
          v-model:value="publishForm.updateContent" 
          :rows="3" 
          placeholder="描述本次更新内容"
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
      
      <a-form-item label="开源设置">
        <a-switch v-model:checked="publishForm.isOpenSource" />
        <span style="margin-left: 10px">{{ publishForm.isOpenSource ? '开源（允许查看代码和使用模板）' : '闭源（仅允许预览和运行）' }}</span>
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 版本历史弹窗 -->
  <a-modal
    v-model:visible="versionModalVisible"
    title="版本历史"
    :footer="null"
    width="700px"
  >
    <a-timeline v-if="versionHistory.length > 0">
      <a-timeline-item
        v-for="(version, index) in versionHistory"
        :key="version.id"
        :color="index === 0 ? 'green' : 'gray'"
      >
        <template #dot>
          <TagOutlined v-if="index === 0" style="font-size: 16px" />
          <ClockCircleOutlined v-else style="font-size: 16px" />
        </template>
        <div class="version-item">
          <div class="version-header">
            <span class="version-tag">v{{ version.version }}</span>
            <span class="version-time">{{ version.create_time }}</span>
          </div>
          <div class="version-desc">{{ version.description || '暂无更新说明' }}</div>
          <div class="version-actions" v-if="index > 0">
            <a-button type="link" size="small" @click="handleRollback(version)">
              <UndoOutlined />
              回滚到此版本
            </a-button>
          </div>
        </div>
      </a-timeline-item>
    </a-timeline>
    <a-empty v-else description="暂无版本历史" />
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { 
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CloudUploadOutlined,
  CloudDownloadOutlined,
  EyeOutlined,
  LikeOutlined,
  FontSizeOutlined,
  HistoryOutlined,
  TagOutlined,
  ClockCircleOutlined,
  UndoOutlined
} from '@ant-design/icons-vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import { getProjectList, saveProject, publishApp, unpublishApp as apiUnpublishApp, getMyMarketApps, deleteProject, renameProject, getPublishedAppByProject, getAppVersions, rollbackApp } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()

const activeTab = ref('projects')
const projects = ref<any[]>([])
const publishedApps = ref<any[]>([])
const loading = ref(false)
const publishModalVisible = ref(false)
const publishing = ref(false)
const isUpdate = ref(false)
const selectedProject = ref<any>(null)
const renameModalVisible = ref(false)
const renaming = ref(false)
const renameForm = ref({ id: 0, name: '' })
const versionModalVisible = ref(false)
const versionHistory = ref<any[]>([])
const selectedApp = ref<any>(null)

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
  const parts = version.split('.').map((p: string) => parseInt(p.replace(/[^0-9]/g, '')) || 0)
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

const goHome = () => {
  router.push('/project/new')
}

const goMarket = () => {
  router.push('/market')
}

const loadProjects = async () => {
  loading.value = true
  try {
    const res: any = await getProjectList(userStore.currentUser?.id || 0)
    if (res.code === 200) {
      projects.value = res.data || []

      for (const project of projects.value) {
        try {
          const appRes: any = await getPublishedAppByProject(project.id)
          if (appRes.code === 200 && appRes.data) {
            project.publishedAppId = appRes.data.id
            project.publishedVersion = appRes.data.version
          }
        } catch (e) {
          project.publishedAppId = null
          project.publishedVersion = null
        }
      }
    } else {
      message.error(res.message || '加载项目列表失败')
    }
  } catch (error: any) {
    message.error('加载项目列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

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

const editProject = (project: any) => {
  projectStore.setCurrentProjectId(project.id)
  projectStore.setFromMyApps(true)
  projectStore.setProjectName(project.name)
  router.push(`/project/${project.id}`)
}

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

const openRenameModal = (project: any) => {
  renameForm.value = { id: project.id, name: project.name }
  renameModalVisible.value = true
}

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

const handlePublish = async (project: any) => {
  selectedProject.value = project
  
  // 检查是否已发布
  const appRes: any = await getPublishedAppByProject(project.id)
  
  if (appRes.code === 200 && appRes.data) {
    // 已发布，显示更新界面
    isUpdate.value = true
    publishForm.value = {
      name: appRes.data.name,
      description: appRes.data.description || '',
      tags: appRes.data.tags || [],
      isOpenSource: appRes.data.is_open_source === 1,
      appId: appRes.data.id,
      currentVersion: appRes.data.version,
      versionType: 'patch',
      updateContent: '',
      content: project.content || {}
    }
  } else {
    // 未发布，显示新发布界面
    isUpdate.value = false
    publishForm.value = {
      name: project.name,
      description: project.description || '',
      tags: [],
      isOpenSource: true,
      appId: 0,
      currentVersion: '1.0.0',
      versionType: 'patch',
      updateContent: '',
      content: project.content || {}
    }
  }
  
  publishModalVisible.value = true
}

const handlePublishSubmit = async () => {
  if (!isUpdate.value && !publishForm.value.name.trim()) {
    message.error('请输入应用名称')
    return
  }
  if (!publishForm.value.description.trim()) {
    message.error('请输入应用描述')
    return
  }
  if (isUpdate.value && !publishForm.value.updateContent.trim()) {
    message.error('请输入更新内容')
    return
  }
  
  publishing.value = true
  let hideLoading: (() => void) | null = null
  
  try {
    hideLoading = message.loading(isUpdate.value ? '正在更新...' : '正在保存并发布...', 0)
    
    // 保存项目
    await saveProject({
      id: selectedProject.value.id,
      name: publishForm.value.name,
      description: publishForm.value.description,
      ownerId: userStore.currentUser?.id,
      content: selectedProject.value.content || {}
    })
    
    // 发布/更新应用
    const res: any = await publishApp({
      projectId: selectedProject.value.id,
      appId: isUpdate.value ? publishForm.value.appId : undefined,
      name: publishForm.value.name,
      description: publishForm.value.description,
      tags: publishForm.value.tags,
      authorId: userStore.currentUser?.id,
      authorName: userStore.currentUser?.username,
      isOpenSource: publishForm.value.isOpenSource,
      updateContent: isUpdate.value ? publishForm.value.updateContent : undefined,
      versionType: isUpdate.value ? publishForm.value.versionType : undefined,
      content: selectedProject.value.content || {}
    })
    
    if (hideLoading) hideLoading()
    
    if (res.code === 200) {
      message.success(res.data.message || (isUpdate.value ? '应用更新成功！' : '应用发布成功！'))
      publishModalVisible.value = false
      publishForm.value = { name: '', description: '', tags: [], isOpenSource: true, appId: 0, currentVersion: '1.0.0', versionType: 'patch', updateContent: '', content: {} }
      selectedProject.value = null
      isUpdate.value = false
      loadProjects()
      loadPublishedApps()
    } else {
      message.error(res.message || (isUpdate.value ? '更新失败' : '发布失败'))
    }
  } catch (err: any) {
    if (hideLoading) hideLoading()
    message.error((isUpdate.value ? '更新失败: ' : '发布失败: ') + (err.message || '未知错误'))
  } finally {
    publishing.value = false
  }
}

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
          loadProjects()
        } else {
          message.error(res.message || '下架失败')
        }
      } catch (error: any) {
        message.error('下架失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

const openVersionHistory = async (app: any) => {
  selectedApp.value = app
  versionModalVisible.value = true
  loading.value = true
  
  try {
    const res: any = await getAppVersions(app.id)
    if (res.code === 200) {
      versionHistory.value = res.data || []
    } else {
      message.error(res.message || '获取版本历史失败')
    }
  } catch (error: any) {
    message.error('获取版本历史失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleRollback = (version: any) => {
  Modal.confirm({
    title: '确认回滚',
    content: `确定要回滚到版本 v${version.version} 吗？当前版本内容会被保存为新版本。`,
    okText: '确认回滚',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await rollbackApp(selectedApp.value.id, version.id)
        if (res.code === 200) {
          message.success('回滚成功')
          loadPublishedApps()
          loadProjects()
          // 刷新版本历史
          const historyRes: any = await getAppVersions(selectedApp.value.id)
          if (historyRes.code === 200) {
            versionHistory.value = historyRes.data || []
          }
        } else {
          message.error(res.message || '回滚失败')
        }
      } catch (error: any) {
        message.error('回滚失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

const viewApp = (id: number) => {
  router.push(`/market/app/${id}`)
}

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
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.app-stats {
  font-size: 14px;
  color: #666;
  display: flex;
  gap: 12px;
}

.published-btn {
  color: #52c41a !important;
}

:deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.version-info {
  margin-bottom: 12px;
}

.current-version {
  color: #1890ff;
  font-weight: 500;
}

.version-item {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 8px;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.version-tag {
  background: #1890ff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.version-time {
  color: #999;
  font-size: 12px;
}

.version-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.version-actions {
  margin-top: 8px;
}

/* 深色模式样式 */
.dark-theme .content {
  background: #141414;
}
.dark-theme .page-header h1 {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .project-desc,
.dark-theme .app-desc {
  color: rgba(255, 255, 255, 0.65);
}
.dark-theme .project-meta,
.dark-theme .app-meta {
  color: rgba(255, 255, 255, 0.45);
}
.dark-theme .version-item {
  background: #1f1f1f;
  border-color: #303030;
}
.dark-theme .version-tag {
  background: #1890ff;
  color: #fff;
}
.dark-theme .version-time {
  color: rgba(255, 255, 255, 0.45);
}
.dark-theme .version-desc {
  color: rgba(255, 255, 255, 0.65);
}
</style>
