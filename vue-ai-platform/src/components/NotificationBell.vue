<template>
  <a-badge :count="unreadCount" :overflowCount="99" :offset="[-2, 2]">
    <a-dropdown :trigger="['click']" v-if="userStore.currentUser">
      <a-button type="text" class="notification-btn" @click="loadNotifications">
        <BellOutlined />
      </a-button>
      <template #overlay>
        <div class="notification-dropdown">
          <div class="notification-header">
            <span>通知</span>
            <a-button type="link" size="small" @click="markAllRead" v-if="notifications.length > 0">
              全部已读
            </a-button>
          </div>
          <div class="notification-list" v-if="notifications.length > 0">
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notification-item"
              :class="{ unread: !item.is_read }"
              @click="handleNotificationClick(item)"
            >
              <div class="notification-icon">
                <MessageOutlined v-if="item.type === 'comment' || item.type === 'reply'" />
                <LikeOutlined v-else-if="item.type === 'like'" />
                <BellOutlined v-else />
              </div>
              <div class="notification-content">
                <div class="notification-title">{{ item.title }}</div>
                <div class="notification-text">{{ item.content }}</div>
                <div class="notification-time">{{ formatTime(item.create_time) }}</div>
              </div>
            </div>
          </div>
          <div class="notification-empty" v-else>
            <InboxOutlined style="font-size: 32px; color: #ccc" />
            <p>暂无通知</p>
          </div>
        </div>
      </template>
    </a-dropdown>
  </a-badge>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { BellOutlined, MessageOutlined, LikeOutlined, InboxOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { getNotifications, markNotificationRead, markAllNotificationsRead, getUnreadNotificationCount, deleteNotification } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const notifications = ref<any[]>([])
const unreadCount = ref(0)

const loadNotifications = async () => {
  if (!userStore.currentUser?.id) return
  
  try {
    const res: any = await getNotifications(userStore.currentUser.id, 1, 20)
    if (res.code === 200) {
      notifications.value = res.data?.list || []
    }
    
    const countRes: any = await getUnreadNotificationCount(userStore.currentUser.id)
    if (countRes.code === 200) {
      unreadCount.value = countRes.data || 0
    }
  } catch (error) {
    console.error('加载通知失败', error)
  }
}

const markAllRead = async () => {
  if (!userStore.currentUser?.id) return
  
  try {
    const res: any = await markAllNotificationsRead(userStore.currentUser.id)
    if (res.code === 200) {
      unreadCount.value = 0
      notifications.value = notifications.value.map(n => ({ ...n, is_read: 1 }))
      message.success('已全部标记为已读')
    }
  } catch (error) {
    console.error('标记已读失败', error)
  }
}

const handleNotificationClick = async (item: any) => {
  if (!item.is_read) {
    try {
      await markNotificationRead(item.id)
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      item.is_read = 1
    } catch (error) {
      console.error('标记已读失败', error)
    }
  }
  
  if (item.related_id) {
    router.push(`/market/app/${item.related_id}`)
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  if (userStore.currentUser?.id) {
    loadNotifications()
  }
})

watch(() => userStore.currentUser, (newUser) => {
  if (newUser?.id) {
    loadNotifications()
  }
})
</script>

<style scoped>
.notification-btn {
  color: white;
  font-size: 18px;
}
.notification-dropdown {
  width: 320px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 500;
}
.notification-list {
  max-height: 400px;
  overflow-y: auto;
}
.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;
}
.notification-item:hover {
  background: #fafafa;
}
.notification-item.unread {
  background: #e6f7ff;
}
.notification-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.notification-content {
  flex: 1;
  min-width: 0;
}
.notification-title {
  font-weight: 500;
  margin-bottom: 4px;
}
.notification-text {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notification-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
.notification-empty {
  padding: 32px;
  text-align: center;
  color: #999;
}
.notification-empty p {
  margin-top: 8px;
}

.dark-theme .notification-dropdown {
  background: #1f1f1f;
}
.dark-theme .notification-header {
  border-color: #303030;
}
.dark-theme .notification-item {
  border-color: #303030;
}
.dark-theme .notification-item:hover {
  background: #2a2a2a;
}
.dark-theme .notification-item.unread {
  background: #1a3a5a;
}
.dark-theme .notification-text {
  color: #999;
}
</style>
