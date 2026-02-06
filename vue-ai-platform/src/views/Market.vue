<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header class="header">
      <div class="header-left">
        <div class="logo" @click="goHome">Vue AI Platform</div>
        <a-menu theme="dark" mode="horizontal" :selectedKeys="['market']" :style="{ lineHeight: '64px' }">
          <a-menu-item key="editor" @click="goHome">编辑器</a-menu-item>
          <a-menu-item key="market">应用市场</a-menu-item>
          <a-menu-item key="my-apps" @click="goMyApps" v-if="userStore.isLoggedIn">我的应用</a-menu-item>
        </a-menu>
      </div>
      
      <div class="header-right">
        <a-dropdown v-if="userStore.currentUser">
          <a-button type="text" class="user-btn">
            <UserOutlined />
            {{ userStore.currentUser.username }}
            <span v-if="userStore.isGuest" class="guest-badge">游客</span>
            <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu>
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
      </div>
    </a-layout-header>

    <a-layout-content class="market-content">
      <div class="market-header">
        <h1>应用市场</h1>
        <p>发现并体验社区创作的精彩应用</p>
      </div>

      <div class="search-section">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索应用..."
          size="large"
          @search="handleSearch"
          enterButton
        />
      </div>

      <div class="filter-section">
        <a-space>
          <span>分类：</span>
          <a-radio-group v-model:value="selectedCategory" @change="handleCategoryChange">
            <a-radio-button value="">全部</a-radio-button>
            <a-radio-button value="工具">工具</a-radio-button>
            <a-radio-button value="游戏">游戏</a-radio-button>
            <a-radio-button value="展示">展示</a-radio-button>
            <a-radio-button value="其他">其他</a-radio-button>
          </a-radio-group>
        </a-space>
      </div>

      <div class="apps-grid">
        <a-spin :spinning="loading">
          <a-row :gutter="[24, 24]">
            <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="app in apps" :key="app.id">
              <a-card hoverable class="app-card" @click="goAppDetail(app.id)">
                <template #cover>
                  <div class="app-cover">
                    <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.name" />
                    <div v-else class="default-cover">
                      <AppstoreOutlined style="font-size: 48px; color: #1890ff" />
                    </div>
                  </div>
                </template>
                <a-card-meta :title="app.name">
                  <template #description>
                    <div class="app-desc">{{ app.description }}</div>
                    <div class="app-meta">
                      <a-space>
                        <span><UserOutlined /> {{ app.author }}</span>
                        <span><LikeOutlined /> {{ app.likes }}</span>
                        <span><EyeOutlined /> {{ app.views }}</span>
                      </a-space>
                    </div>
                    <div class="app-tags" v-if="app.tags && app.tags.length">
                      <a-tag v-for="tag in app.tags.slice(0, 3)" :key="tag" size="small">{{ tag }}</a-tag>
                    </div>
                  </template>
                </a-card-meta>
              </a-card>
            </a-col>
          </a-row>
          <div v-if="!loading && apps.length === 0" class="empty-state">
            <a-empty description="暂无发布应用" />
          </div>
        </a-spin>
      </div>

      <div class="pagination-section">
        <a-pagination
          v-model:current="currentPage"
          v-model:pageSize="pageSize"
          :total="total"
          :pageSizeOptions="['12', '24', '48']"
          showSizeChanger
          showQuickJumper
          @change="handlePageChange"
        />
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  UserOutlined, 
  DownOutlined,
  LogoutOutlined,
  LoginOutlined,
  AppstoreOutlined,
  LikeOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMarketApps } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const apps = ref<any[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const selectedCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 跳转到首页
const goHome = () => {
  router.push('/project/new')
}

// 跳转到我的应用
const goMyApps = () => {
  router.push('/my-apps')
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

// 查看应用详情
const goAppDetail = (id: number) => {
  router.push(`/market/app/${id}`)
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadApps()
}

// 分类切换
const handleCategoryChange = () => {
  currentPage.value = 1
  loadApps()
}

// 分页变化
const handlePageChange = () => {
  loadApps()
}

// 加载应用列表
const loadApps = async () => {
  loading.value = true
  apps.value = [] // 清空当前数据
  try {
    console.log('Loading apps...')
    const res: any = await getMarketApps({
      keyword: searchKeyword.value,
      category: selectedCategory.value,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    
    console.log('API response:', res)
    
    if (res.code === 200) {
      apps.value = res.data?.list || []
      total.value = res.data?.total || 0
    } else {
      message.error(res.message || '加载应用列表失败')
    }
  } catch (error: any) {
    console.error('Load apps error:', error)
    message.error('加载应用列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadApps()
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

.guest-badge {
  background: #ff4d4f;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 4px;
}

.market-content {
  background: #f0f2f5;
  padding: 24px 48px;
}

.market-header {
  text-align: center;
  margin-bottom: 32px;
}

.market-header h1 {
  font-size: 2rem;
  margin-bottom: 8px;
}

.market-header p {
  color: #666;
  font-size: 1rem;
}

.search-section {
  max-width: 600px;
  margin: 0 auto 24px;
}

.filter-section {
  margin-bottom: 24px;
  text-align: center;
}

.apps-grid {
  margin-bottom: 32px;
  min-height: 300px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.app-card {
  cursor: pointer;
  transition: all 0.3s;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.app-cover {
  height: 160px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
}

.app-desc {
  color: #666;
  font-size: 13px;
  margin: 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.app-meta {
  margin: 8px 0;
  font-size: 12px;
  color: #999;
}

.app-tags {
  margin-top: 8px;
}

.pagination-section {
  text-align: center;
  padding: 24px 0;
}
</style>
