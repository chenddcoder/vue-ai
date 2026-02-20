<template>
  <a-layout-header class="app-header">
    <div class="header-left">
      <div class="logo" @click="goHome">Vue AI Platform</div>
      <a-menu 
        theme="dark" 
        mode="horizontal" 
        :selectedKeys="[currentMenuKey]" 
        :style="{ lineHeight: '64px' }"
      >
        <a-menu-item key="editor" @click="goHome">编辑器</a-menu-item>
        <a-menu-item key="market" @click="goMarket">应用市场</a-menu-item>
      </a-menu>
      <div class="project-name" v-if="showProjectName && projectStore.projectName">
        <AppstoreOutlined />
        <span>{{ projectStore.projectName }}</span>
      </div>
    </div>
    
    <div class="header-right">
      <a-space>
        <template v-if="showEditorActions">
          <ThemeToggle />
          <NotificationBell v-if="userStore.currentUser" />
          
          <a-button type="primary" @click="handleSave" :loading="props.saving">
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
          
          <a-button type="primary" @click="handlePublish" :disabled="userStore.isGuest" danger>
            <template #icon><CloudUploadOutlined /></template>
            发布
          </a-button>
        </template>
        
        <UserAvatar />
      </a-space>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  SaveOutlined, 
  CloudUploadOutlined, 
  AppstoreOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'

interface Props {
  showEditorActions?: boolean
  showProjectName?: boolean
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showEditorActions: false,
  showProjectName: false,
  saving: false
})

interface Emits {
  (e: 'save'): void
  (e: 'publish'): void
}

const emit = defineEmits<Emits>()

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const userStore = useUserStore()

const currentMenuKey = computed(() => {
  if (route.path === '/market') return 'market'
  if (route.path === '/my-apps') return 'my-apps'
  if (route.path === '/favorites') return 'favorites'
  if (route.path === '/ai-config') return 'ai-config'
  return 'editor'
})

const goHome = () => {
  router.push('/project/new')
}

const goMarket = () => {
  router.push('/market')
}

const handleSave = () => {
  emit('save')
}

const handlePublish = () => {
  emit('publish')
}
</script>

<style scoped>
.app-header {
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

.save-status {
  color: #ffffff;
  font-size: 13px;
  margin-left: 12px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}

.dark-theme .app-header {
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

.dark-theme .project-name {
  background: rgba(255, 255, 255, 0.1);
}

.dark-theme .project-name span {
  color: rgba(255, 255, 255, 0.85);
}
</style>
