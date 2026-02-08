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
          <a-dropdown v-if="userStore.currentUser">
            <a-button type="text" class="user-btn">
              <UserOutlined /> {{ userStore.currentUser.username }}
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="logout">
                  <LogoutOutlined /> 退出
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button v-else type="primary" @click="goLogin">登录</a-button>
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
          <a-button
            v-if="isSelf"
            type="default"
            @click="goEditProfile"
          >
            编辑个人资料
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
import {
  getUserApps,
  getUserFollowers,
  getUserFollowing,
  checkUserFollow,
  toggleUserFollow
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

const targetUserId = computed(() => parseInt(route.params.userId as string) || userStore.currentUser?.id)
const isSelf = computed(() => targetUserId.value === userStore.currentUser?.id)

const goBack = () => router.back()
const goHome = () => router.push('/project/new')
const goLogin = () => {
  userStore.logout()
  router.push('/login')
}

const logout = () => {
  userStore.logout()
  router.push('/login')
  message.success('已退出登录')
}

const goEditProfile = () => {
  message.info('编辑个人资料功能开发中')
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
    const [appsRes, followersRes, followingRes] = await Promise.all([
      getUserApps(userId),
      getUserFollowers(userId),
      getUserFollowing(userId)
    ])

    const appsData = appsRes.data as any
    const followersData = followersRes.data as any
    const followingData = followingRes.data as any

    if (appsData) userApps.value = appsData
    if (followersData) followers.value = followersData
    if (followingData) following.value = followingData

    profile.value = {
      id: userId,
      username: userApps.value[0]?.author_name || `用户${userId}`,
      avatar: null,
      bio: null
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
</style>
