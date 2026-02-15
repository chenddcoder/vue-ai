<template>
  <a-dropdown v-if="userStore.currentUser" :trigger="['hover']">
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
        <a-menu-item @click="goToProfile">
          <UserOutlined />
          个人中心
        </a-menu-item>
        <a-menu-item @click="showAvatarModal" v-if="!userStore.isGuest">
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

  <a-modal
    v-model:open="avatarModalVisible"
    title="更换头像"
    :confirm-loading="avatarUploading"
    @ok="handleAvatarSave"
  >
    <a-form layout="vertical">
      <a-form-item label="头像URL" :rules="[{ required: true, message: '请输入头像URL' }]">
        <a-input v-model:value="avatarUrl" placeholder="请输入头像图片URL" />
      </a-form-item>
      <a-form-item label="预览">
        <a-avatar :src="previewAvatar" :size="100" style="margin-top: 8px">
          <template #icon><UserOutlined /></template>
        </a-avatar>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  DownOutlined,
  LogoutOutlined,
  LoginOutlined,
  RobotOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserAvatar } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const avatarModalVisible = ref(false)
const avatarUploading = ref(false)
const avatarUrl = ref('')
const previewAvatar = ref('')

const goToProfile = () => {
  if (userStore.currentUser?.id) {
    router.push(`/profile/${userStore.currentUser.id}`)
  }
}

const goAIConfig = () => {
  router.push('/ai-config')
}

const goLogin = () => {
  userStore.logout()
  router.push('/login')
}

const logout = () => {
  userStore.logout()
  router.push('/login')
  message.success('已退出登录')
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
.user-btn {
  color: white;
}
.guest-badge {
  background: #faad14;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 8px;
}

/* 深色模式样式 */
.dark-theme :deep(.ant-dropdown-menu) {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}
.dark-theme :deep(.ant-dropdown-menu-item) {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme :deep(.ant-dropdown-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.dark-theme :deep(.ant-dropdown-menu-item-disabled) {
  color: rgba(255, 255, 255, 0.25);
}
.dark-theme :deep(.ant-dropdown-menu-item-divider) {
  background: #303030;
}
</style>
