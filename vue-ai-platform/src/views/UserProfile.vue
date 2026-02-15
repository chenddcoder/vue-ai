<template>
  <div class="user-profile-page">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <div class="header-left">
          <a-button type="text" @click="goBack" class="back-btn">
            <LeftOutlined /> 返回
          </a-button>
          <div class="logo" @click="goHome">Vue AI Platform</div>
        </div>
        <div class="header-right">
          <UserAvatar />
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <div class="profile-header">
          <div class="user-info">
            <a-avatar :size="80" :src="profile?.avatar">
              {{ profile?.username?.charAt(0) || 'U' }}
            </a-avatar>
            <div class="user-details">
              <h1>{{ profile?.username || '用户' }}</h1>
              <p class="bio">{{ profile?.bio || '暂无个人简介' }}</p>
              <div class="stats">
                <span><strong>{{ userApps.length }}</strong> 个应用</span>
                <span><strong>{{ followers.length }}</strong> 粉丝</span>
                <span><strong>{{ following.length }}</strong> 关注</span>
              </div>
            </div>
          </div>
          <div class="profile-actions">
            <a-button
              v-if="isSelf"
              type="default"
              @click="goEditProfile"
            >
              编辑个人资料
            </a-button>
            <a-button
              v-if="isSelf"
              type="default"
              @click="showPasswordModal"
              class="password-btn"
            >
              修改密码
            </a-button>
            <a-button
              v-else-if="userStore.currentUser"
              :type="isFollowing ? 'default' : 'primary'"
              @click="toggleFollow"
              :loading="followingLoading"
            >
              {{ isFollowing ? '已关注' : '关注' }}
            </a-button>
          </div>
        </div>

        <a-tabs v-model:activeKey="activeTab" class="profile-tabs">
          <a-tab-pane key="apps" tab="发布的应用">
            <div class="apps-grid" v-if="userApps.length > 0">
              <div
                v-for="app in userApps"
                :key="app.id"
                class="app-item"
                @click="goToApp(app.id)"
              >
                <div class="app-icon">
                  <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.name" />
                  <div v-else class="default-icon">
                    <AppstoreOutlined />
                  </div>
                </div>
                <div class="app-info">
                  <div class="app-name">{{ app.name }}</div>
                  <div class="app-stats">
                    <span><LikeOutlined /> {{ app.likes || 0 }}</span>
                    <span><EyeOutlined /> {{ app.views || 0 }}</span>
                  </div>
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无发布的应用" />
          </a-tab-pane>

          <a-tab-pane key="followers" tab="粉丝">
            <div class="users-list" v-if="followers.length > 0">
              <div
                v-for="user in followers"
                :key="user.id"
                class="user-item"
                @click="goToProfile(user.id)"
              >
                <a-avatar :size="48" :src="user.avatar">
                  {{ user.username?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="user-info">
                  <div class="username">{{ user.username || '用户' }}</div>
                  <div class="bio">{{ user.bio || '暂无个人简介' }}</div>
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无粉丝" />
          </a-tab-pane>

          <a-tab-pane key="following" tab="关注">
            <div class="users-list" v-if="following.length > 0">
              <div
                v-for="user in following"
                :key="user.id"
                class="user-item"
                @click="goToProfile(user.id)"
              >
                <a-avatar :size="48" :src="user.avatar">
                  {{ user.username?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="user-info">
                  <div class="username">{{ user.username || '用户' }}</div>
                  <div class="bio">{{ user.bio || '暂无个人简介' }}</div>
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无关注" />
          </a-tab-pane>
        </a-tabs>
      </a-layout-content>
    </a-layout>

    <a-modal
      v-model:open="passwordModalVisible"
      title="修改密码"
      :confirm-loading="changingPassword"
      @ok="handlePasswordChange"
      @cancel="closePasswordModal"
    >
      <a-form :model="passwordForm" layout="vertical">
        <a-form-item label="当前密码" required>
          <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
        </a-form-item>
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码（至少6位）" />
        </a-form-item>
        <a-form-item label="确认新密码" required>
          <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="profileModalVisible"
      title="编辑个人资料"
      :confirm-loading="savingProfile"
      @ok="handleProfileSave"
      @cancel="closeProfileModal"
    >
      <a-form :model="profileForm" layout="vertical">
        <a-form-item label="用户名" required>
          <a-input v-model:value="profileForm.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea 
            v-model:value="profileForm.bio" 
            placeholder="请输入个人简介"
            :rows="4"
            :maxlength="200"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  LeftOutlined,
  UserOutlined,
  LogoutOutlined,
  AppstoreOutlined,
  LikeOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import UserAvatar from '@/components/UserAvatar.vue'
import {
  getUserApps,
  getUserFollowers,
  getUserFollowing,
  checkUserFollow,
  toggleUserFollow,
  getUserProfile,
  changePassword,
  updateUserProfile
} from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref<any>(null)
const userApps = ref<any[]>([])
const followers = ref<any[]>([])
const following = ref<any[]>([])
const isFollowing = ref(false)
const followingLoading = ref(false)
const activeTab = ref('apps')

const passwordModalVisible = ref(false)
const changingPassword = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileModalVisible = ref(false)
const savingProfile = ref(false)
const profileForm = ref({
  username: '',
  bio: ''
})

const showPasswordModal = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordModalVisible.value = true
}

const closePasswordModal = () => {
  passwordModalVisible.value = false
}

const handlePasswordChange = async () => {
  if (!passwordForm.value.oldPassword) {
    message.warning('请输入当前密码')
    return
  }
  if (!passwordForm.value.newPassword || passwordForm.value.newPassword.length < 6) {
    message.warning('新密码至少需要6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }

  changingPassword.value = true
  try {
    const res = await changePassword({
      userId: userStore.currentUser.id,
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    }) as any
    if (res.code === 200) {
      message.success('密码修改成功')
      passwordModalVisible.value = false
      userStore.logout()
      router.push('/login')
    } else {
      message.error(res.message || '密码修改失败')
    }
  } catch (error) {
    message.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

const targetUserId = computed(() => parseInt(route.params.userId as string) || userStore.currentUser?.id)
const isSelf = computed(() => targetUserId.value === userStore.currentUser?.id)

const goBack = () => router.back()
const goHome = () => router.push('/project/new')

const goEditProfile = () => {
  profileForm.value = {
    username: profile.value?.username || '',
    bio: profile.value?.bio || ''
  }
  profileModalVisible.value = true
}

const closeProfileModal = () => {
  profileModalVisible.value = false
}

const handleProfileSave = async () => {
  if (!profileForm.value.username.trim()) {
    message.warning('请输入用户名')
    return
  }
  
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }

  savingProfile.value = true
  try {
    const res: any = await updateUserProfile({
      userId: userStore.currentUser.id,
      username: profileForm.value.username.trim(),
      bio: profileForm.value.bio.trim()
    })
    
    if (res.code === 200) {
      message.success('个人资料更新成功')
      profileModalVisible.value = false
      // 更新本地数据
      if (profile.value) {
        profile.value.username = res.data.username
        profile.value.bio = res.data.bio
      }
      // 更新store中的用户信息
      if (userStore.currentUser) {
        userStore.currentUser.username = res.data.username
        userStore.currentUser.bio = res.data.bio
      }
    } else {
      message.error(res.message || '更新失败')
    }
  } catch (error: any) {
    message.error(error.message || '更新失败')
  } finally {
    savingProfile.value = false
  }
}

const goToApp = (appId: number) => {
  router.push(`/market/app/${appId}`)
}

const goToProfile = (userId: number) => {
  router.push(`/profile/${userId}`)
}

const loadData = async () => {
  const userId = targetUserId.value
  if (!userId) {
    router.push('/login')
    return
  }

  try {
    const [appsRes, followersRes, followingRes, profileRes] = await Promise.all([
      getUserApps(userId),
      getUserFollowers(userId),
      getUserFollowing(userId),
      getUserProfile(userId)
    ])

    const appsData = appsRes.data as any
    const followersData = followersRes.data as any
    const followingData = followingRes.data as any
    const profileData = profileRes.data as any

    if (appsData) userApps.value = appsData
    if (followersData) followers.value = followersData
    if (followingData) following.value = followingData

    if (profileData) {
      profile.value = {
        id: userId,
        username: profileData.username || `用户${userId}`,
        avatar: profileData.avatar || null,
        bio: profileData.bio || '暂无个人简介'
      }
    } else {
      profile.value = {
        id: userId,
        username: userApps.value[0]?.author_name || `用户${userId}`,
        avatar: null,
        bio: '暂无个人简介'
      }
    }
  } catch (error) {
    message.error('加载数据失败')
  }

  if (!isSelf.value && userStore.currentUser?.id) {
    try {
      const followRes = await checkUserFollow(userId, userStore.currentUser.id) as any
      if (followRes.data?.following) isFollowing.value = followRes.data.following
    } catch (error) {
      console.error('检查关注状态失败', error)
    }
  }
}

const toggleFollow = async () => {
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }
  const userId = targetUserId.value
  if (!userId) return
  
  followingLoading.value = true
  try {
    const res = await toggleUserFollow(userId, userStore.currentUser.id) as any
    if (res.data) {
      isFollowing.value = res.data.following
      message.success(res.data.message)
      loadData()
    }
  } finally {
    followingLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.user-profile-page { min-height: 100vh; background: #f0f2f5; }
.header { display: flex; justify-content: space-between; align-items: center; padding: 0 24px; background: #001529; }
.header-left { display: flex; align-items: center; gap: 16px; }
.back-btn { color: white; }
.logo { color: white; font-size: 18px; font-weight: 600; cursor: pointer; }
.header-right { display: flex; align-items: center; }
.user-btn { color: white; }
.content { padding: 24px; max-width: 1000px; margin: 0 auto; }

.profile-header { display: flex; justify-content: space-between; align-items: center; background: white; border-radius: 12px; padding: 24px; margin-bottom: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.user-info { display: flex; align-items: center; gap: 20px; }
.profile-actions { display: flex; gap: 12px; }
.password-btn { color: #1890ff; border-color: #1890ff; }
.user-details h1 { margin: 0 0 4px 0; font-size: 24px; }
.bio { color: #666; margin-bottom: 8px; }
.stats { display: flex; gap: 20px; color: #666; }
.stats span strong { color: #333; }

.apps-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 20px; }
.app-item { background: white; border-radius: 12px; padding: 16px; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.app-item:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.15); }
.app-icon { width: 80px; height: 80px; border-radius: 16px; overflow: hidden; margin: 0 auto 12px; }
.app-icon img { width: 100%; height: 100%; object-fit: cover; }
.default-icon { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); font-size: 36px; color: white; }
.app-info { text-align: center; }
.app-name { font-weight: 500; margin-bottom: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.app-stats { display: flex; justify-content: center; gap: 12px; font-size: 12px; color: #999; }
.app-stats span { display: flex; align-items: center; gap: 4px; }

.users-list { display: flex; flex-direction: column; gap: 12px; }
.user-item { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 12px; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.user-item:hover { transform: translateX(4px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.user-info .username { font-weight: 500; margin-bottom: 4px; }
.user-info .bio { color: #999; font-size: 12px; }

.profile-tabs { background: white; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }

/* 深色模式样式 */
.dark-theme .user-profile-page { background: #141414; }
.dark-theme .profile-header { background: #1f1f1f; box-shadow: 0 2px 8px rgba(0,0,0,0.45); }
.dark-theme .user-details h1 { color: rgba(255, 255, 255, 0.85); }
.dark-theme .bio { color: rgba(255, 255, 255, 0.65); }
.dark-theme .stats { color: rgba(255, 255, 255, 0.65); }
.dark-theme .stats span strong { color: rgba(255, 255, 255, 0.85); }
.dark-theme .app-item { background: #1f1f1f; box-shadow: 0 2px 8px rgba(0,0,0,0.45); }
.dark-theme .app-item:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.65); }
.dark-theme .app-name { color: rgba(255, 255, 255, 0.85); }
.dark-theme .app-stats { color: rgba(255, 255, 255, 0.45); }
.dark-theme .user-item { background: #1f1f1f; box-shadow: 0 2px 8px rgba(0,0,0,0.45); }
.dark-theme .user-item:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.65); }
.dark-theme .user-info .username { color: rgba(255, 255, 255, 0.85); }
.dark-theme .user-info .bio { color: rgba(255, 255, 255, 0.45); }
.dark-theme .profile-tabs { background: #1f1f1f; box-shadow: 0 2px 8px rgba(0,0,0,0.45); }
</style>
