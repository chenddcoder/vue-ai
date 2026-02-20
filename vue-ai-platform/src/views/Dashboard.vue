<template>
  <a-layout style="min-height: 100vh">
    <AppHeader />
    
    <a-layout-content class="dashboard-page">
      <div class="page-header">
        <h1>数据仪表盘</h1>
        <p>查看您的项目和应用统计</p>
      </div>

      <a-row :gutter="[24, 24]">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card">
            <a-statistic
              title="项目数量"
              :value="stats.projects"
              :prefix="h(ProjectOutlined)"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card">
            <a-statistic
              title="已发布应用"
              :value="stats.apps"
              :prefix="h(AppstoreOutlined)"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card">
            <a-statistic
              title="总收藏数"
              :value="stats.favorites"
              :prefix="h(HeartOutlined)"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card">
            <a-statistic
              title="总浏览量"
              :value="stats.views"
              :prefix="h(EyeOutlined)"
            />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[24, 24]" style="margin-top: 24px">
        <a-col :xs="24" :lg="12">
          <a-card title="最近活动" class="activity-card">
            <a-timeline>
              <a-timeline-item v-for="activity in activities" :key="activity.id" :color="activity.color">
                <p>{{ activity.content }}</p>
                <p class="activity-time">{{ activity.time }}</p>
              </a-timeline-item>
            </a-timeline>
            <a-empty v-if="activities.length === 0" description="暂无活动记录" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card title="应用评分分布" class="chart-card">
            <div class="rating-chart">
              <div class="rating-row" v-for="i in 5" :key="i">
                <span class="rating-label">{{ 6 - i }} 星</span>
                <div class="rating-bar">
                  <div 
                    class="rating-fill" 
                    :style="{ width: getRatingPercent(6 - i) + '%' }"
                  ></div>
                </div>
                <span class="rating-count">{{ ratingDistribution[6 - i] || 0 }}</span>
              </div>
            </div>
            <a-empty v-if="totalRatings === 0" description="暂无评分数据" />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[24, 24]" style="margin-top: 24px">
        <a-col :xs="24">
          <a-card title="热门应用" class="top-apps-card">
            <a-table
              :dataSource="topApps"
              :columns="appColumns"
              :pagination="false"
              size="small"
              rowKey="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <a @click="goToApp(record.id)">{{ record.name }}</a>
                </template>
                <template v-if="column.key === 'likes'">
                  <HeartOutlined style="color: #ff4d4f" /> {{ record.likes }}
                </template>
                <template v-if="column.key === 'views'">
                  <EyeOutlined /> {{ record.views }}
                </template>
              </template>
            </a-table>
            <a-empty v-if="topApps.length === 0" description="暂无发布的应用" />
          </a-card>
        </a-col>
      </a-row>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, h, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ProjectOutlined, AppstoreOutlined, HeartOutlined, EyeOutlined } from '@ant-design/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'
import { getProjectList, getMyMarketApps } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({
  projects: 0,
  apps: 0,
  favorites: 0,
  views: 0
})

const activities = ref<any[]>([])
const ratingDistribution = ref<Record<number, number>>({})
const topApps = ref<any[]>([])

const totalRatings = computed(() => {
  return Object.values(ratingDistribution.value).reduce((sum, count) => sum + count, 0)
})

const getRatingPercent = (rating: number) => {
  if (totalRatings.value === 0) return 0
  return ((ratingDistribution.value[rating] || 0) / totalRatings.value) * 100
}

const appColumns = [
  { title: '应用名称', dataIndex: 'name', key: 'name' },
  { title: '版本', dataIndex: 'version', key: 'version' },
  { title: '点赞', key: 'likes', width: 80 },
  { title: '浏览', key: 'views', width: 80 },
  { title: '发布时间', dataIndex: 'publish_time', key: 'publish_time' }
]

const loadDashboardData = async () => {
  if (!userStore.currentUser?.id) {
    loadMockData()
    return
  }

  try {
    // 加载项目
    const projectRes: any = await getProjectList()
    if (projectRes.code === 200) {
      stats.value.projects = projectRes.data?.length || 0
    }

    // 加载应用
    const appRes: any = await getMyMarketApps(userStore.currentUser.id)
    if (appRes.code === 200) {
      const apps = appRes.data || []
      stats.value.apps = apps.length
      stats.value.favorites = apps.reduce((sum: number, app: any) => sum + (app.likes || 0), 0)
      stats.value.views = apps.reduce((sum: number, app: any) => sum + (app.views || 0), 0)
      
      topApps.value = apps.sort((a: any, b: any) => (b.likes || 0) - (a.likes || 0)).slice(0, 5)
    }

    // 生成模拟活动数据
    generateActivities()
  } catch (e) {
    console.error('加载数据失败', e)
    loadMockData()
  }
}

const loadMockData = () => {
  stats.value = {
    projects: 5,
    apps: 3,
    favorites: 28,
    views: 156
  }
  topApps.value = [
    { id: 1, name: '待办事项', version: '1.0.0', likes: 15, views: 80, publish_time: '2026-02-10' },
    { id: 2, name: '计算器', version: '1.2.0', likes: 8, views: 45, publish_time: '2026-02-05' },
    { id: 3, name: '天气卡片', version: '1.0.0', likes: 5, views: 31, publish_time: '2026-01-20' }
  ]
  generateActivities()
}

const generateActivities = () => {
  activities.value = [
    { id: 1, content: '发布了新应用「待办事项」', time: '2小时前', color: 'green' },
    { id: 2, content: '更新了应用「计算器」', time: '1天前', color: 'blue' },
    { id: 3, content: '收到5个新收藏', time: '2天前', color: 'red' },
    { id: 4, content: '创建了新项目', time: '3天前', color: 'gray' }
  ]
}

const goToApp = (appId: number) => {
  router.push(`/market/app/${appId}`)
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
}

.stat-card {
  height: 100%;
}

.stat-card :deep(.ant-statistic-title) {
  font-size: 14px;
}

.stat-card :deep(.ant-statistic-content) {
  font-size: 28px;
}

.activity-card :deep(.ant-timeline-item-content) {
  margin-left: 20px;
}

.activity-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.rating-chart {
  padding: 16px 0;
}

.rating-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.rating-label {
  width: 50px;
  font-size: 13px;
}

.rating-bar {
  flex: 1;
  height: 16px;
  background: #f0f0f0;
  border-radius: 8px;
  margin: 0 12px;
  overflow: hidden;
}

.rating-fill {
  height: 100%;
  background: linear-gradient(90deg, #faad14, #ffc53d);
  border-radius: 8px;
  transition: width 0.3s;
}

.rating-count {
  width: 30px;
  text-align: right;
  font-size: 13px;
  color: #666;
}

.dark-theme .rating-bar {
  background: #2a2a2a;
}

.dark-theme .rating-count {
  color: rgba(255, 255, 255, 0.65);
}
</style>
