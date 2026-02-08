<template>
  <a-modal
    v-model:open="dialogVisible"
    :title="app?.name || '应用详情'"
    :footer="null"
    width="800px"
    @cancel="handleCancel"
  >
    <div class="app-detail" v-if="app">
      <div class="app-header">
        <div class="app-thumbnail">
          <img v-if="app.thumbnail" :src="app.thumbnail" :alt="app.name" />
          <div v-else class="default-thumbnail">
            <AppstoreOutlined />
          </div>
        </div>
        <div class="app-info">
          <h2>{{ app.name }}</h2>
          <p class="author">
            <UserOutlined />
            {{ app.author_name }}
            <a-tag v-if="app.is_open_source === 1" color="blue">开源</a-tag>
          </p>
          <p class="version">版本: {{ app.version }}</p>
          <p class="tags" v-if="app.tags && app.tags.length">
            <a-tag v-for="tag in app.tags" :key="tag">{{ tag }}</a-tag>
          </p>
          <div class="stats">
            <span><EyeOutlined /> {{ app.views || 0 }}</span>
            <span><LikeOutlined /> {{ app.likes || 0 }}</span>
            <span><StarOutlined /> {{ avgRating?.avg_rating?.toFixed(1) || '暂无评分' }}</span>
          </div>
        </div>
      </div>

      <div class="app-actions">
        <a-button type="primary" @click="launchApp">
          <PlayCircleOutlined /> 启动应用
        </a-button>
        <a-button @click="toggleFavorite" :class="{ favorited: isFavorited }">
          <StarOutlined :style="isFavorited ? { color: '#faad14' } : {}" />
          {{ isFavorited ? '已收藏' : '收藏' }}
        </a-button>
        <a-button @click="toggleLike" :class="{ liked: isLiked }">
          <LikeOutlined :style="isLiked ? { color: '#1890ff' } : {}" />
          {{ isLiked ? '已点赞' : '点赞' }} ({{ app.likes || 0 }})
        </a-button>
      </div>

      <a-divider>应用介绍</a-divider>

      <div class="app-description">
        <p>{{ app.description }}</p>
      </div>

      <a-divider>评论 ({{ comments.length }})</a-divider>

      <div class="comment-form">
        <a-rate v-model:value="newComment.rating" allow-half />
        <a-textarea
          v-model:value="newComment.content"
          placeholder="写下你的评论..."
          :rows="2"
        />
        <a-button type="primary" @click="submitComment" :loading="submitting">
          发布评论
        </a-button>
      </div>

      <div class="comments-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <a-avatar :src="comment.user_avatar" :size="32">
              {{ comment.user_name?.charAt(0) }}
            </a-avatar>
            <div class="comment-meta">
              <span class="username">{{ comment.user_name }}</span>
              <span class="time">{{ formatTime(comment.create_time) }}</span>
            </div>
            <a-rate :value="comment.rating" readonly allow-half size="small" />
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          
          <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <div class="reply-header">
                <span class="username">{{ reply.user_name }}</span>
                <span class="time">{{ formatTime(reply.create_time) }}</span>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
            </div>
          </div>
        </div>
        <a-empty v-if="comments.length === 0" description="暂无评论" />
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  UserOutlined,
  EyeOutlined,
  LikeOutlined,
  StarOutlined,
  PlayCircleOutlined
} from '@ant-design/icons-vue'
import {
  getMarketAppDetail,
  getAppComments,
  addAppComment,
  toggleAppFavorite,
  checkAppFavorite,
  toggleAppLike,
  getUserFavorites
} from '@/api'
import { useUserStore } from '@/stores/user'

interface AppData {
  id: number
  name: string
  description: string
  thumbnail?: string
  author_id: number
  author_name: string
  tags?: string[]
  version: string
  likes?: number
  views?: number
  is_open_source?: number
  content?: any
}

const props = defineProps<{
  appId?: number
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'launch', app: AppData): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const userStore = useUserStore()

const app = ref<AppData | null>(null)
const comments = ref<any[]>([])
const avgRating = ref<any>(null)
const isFavorited = ref(false)
const isLiked = ref(false)
const submitting = ref(false)

const newComment = ref({
  content: '',
  rating: 5
})

const handleCancel = () => {
  emit('update:visible', false)
}

const loadAppDetail = async () => {
  if (!props.appId) return
  
  try {
    const res: any = await getMarketAppDetail(props.appId)
    if (res.code === 200) {
      app.value = res.data
    }
  } catch (error) {
    message.error('加载应用详情失败')
  }
}

const loadComments = async () => {
  if (!props.appId) return
  
  try {
    const res: any = await getAppComments(props.appId)
    if (res.code === 200) {
      comments.value = res.data || []
      avgRating.value = res.stats
    }
  } catch (error) {
    console.error('加载评论失败', error)
  }
}

const loadFavoriteStatus = async () => {
  if (!props.appId || !userStore.currentUser?.id) return
  
  try {
    const res: any = await checkAppFavorite(props.appId, userStore.currentUser.id)
    if (res.code === 200) {
      isFavorited.value = res.data?.favorited || false
    }
  } catch (error) {
    console.error('检查收藏状态失败', error)
  }
}

const toggleFavorite = async () => {
  if (!props.appId || !userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }
  
  try {
    const res: any = await toggleAppFavorite(props.appId, userStore.currentUser.id)
    if (res.code === 200) {
      isFavorited.value = res.data?.favorited
      message.success(res.data?.message || '操作成功')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const toggleLike = async () => {
  if (!props.appId || !userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }
  
  try {
    const res: any = await toggleAppLike(props.appId, userStore.currentUser.id)
    if (res.code === 200) {
      isLiked.value = res.data?.liked
      if (app.value) {
        app.value.likes = res.data?.likes || 0
      }
      message.success(res.data?.liked ? '点赞成功' : '取消点赞')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const submitComment = async () => {
  if (!userStore.currentUser?.id) {
    message.warning('请先登录')
    return
  }
  
  if (!newComment.value.content.trim()) {
    message.warning('请输入评论内容')
    return
  }
  
  submitting.value = true
  
  try {
    const res: any = await addAppComment({
      appId: props.appId!,
      userId: userStore.currentUser.id,
      userName: userStore.currentUser.username,
      userAvatar: userStore.currentUser.avatar,
      content: newComment.value.content,
      rating: newComment.value.rating
    })
    
    if (res.code === 200) {
      message.success('评论成功')
      newComment.value.content = ''
      newComment.value.rating = 5
      loadComments()
    }
  } catch (error) {
    message.error('评论失败')
  } finally {
    submitting.value = false
  }
}

const launchApp = () => {
  if (app.value) {
    emit('launch', app.value)
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN')
}

watch(() => props.visible, (val) => {
  if (val && props.appId) {
    loadAppDetail()
    loadComments()
    loadFavoriteStatus()
  }
})

watch(() => props.appId, (newId) => {
  if (newId && props.visible) {
    loadAppDetail()
    loadComments()
    loadFavoriteStatus()
  }
})
</script>

<style scoped>
.app-detail {
  padding: 0;
}

.app-header {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.app-thumbnail {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.app-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-thumbnail {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-size: 48px;
  color: white;
}

.app-info {
  flex: 1;
}

.app-info h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.app-info .author {
  color: #666;
  margin-bottom: 4px;
}

.app-info .version {
  color: #999;
  font-size: 12px;
  margin-bottom: 8px;
}

.app-info .tags {
  margin-bottom: 8px;
}

.stats {
  display: flex;
  gap: 16px;
  color: #666;
}

.stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.app-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.app-actions .favorited,
.app-actions .liked {
  border-color: #faad14;
  color: #faad14;
}

.app-description {
  color: #333;
  line-height: 1.6;
}

.comment-form {
  margin-bottom: 20px;
}

.comment-form .ant-rate {
  margin-bottom: 8px;
}

.comment-form .ant-btn {
  margin-top: 8px;
}

.comments-list {
  max-height: 400px;
  overflow-y: auto;
}

.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-meta {
  flex: 1;
}

.username {
  font-weight: 500;
  margin-right: 8px;
}

.time {
  color: #999;
  font-size: 12px;
}

.comment-content {
  color: #333;
  line-height: 1.5;
}

.comment-replies {
  margin-left: 40px;
  margin-top: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

.reply-item {
  padding: 4px 0;
}

.reply-header {
  margin-bottom: 4px;
}

.reply-content {
  color: #666;
}
</style>
