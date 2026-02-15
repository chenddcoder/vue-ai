<template>
  <div class="login-container">
    <div class="login-box">
      <div class="logo-section">
        <h1>Vue AI Platform</h1>
        <p>AI驱动的应用开发平台</p>
      </div>
      
      <a-card class="login-card">
        <a-tabs v-model:activeKey="activeTab" centered>
          <a-tab-pane key="login" tab="登录">
            <a-form
              :model="loginForm"
              :rules="rules"
              @finish="handleLogin"
              layout="vertical"
            >
              <a-form-item label="用户名" name="username">
                <a-input 
                  v-model:value="loginForm.username" 
                  placeholder="请输入用户名"
                  size="large"
                >
                  <template #prefix>
                    <UserOutlined />
                  </template>
                </a-input>
              </a-form-item>
              
              <a-form-item label="密码" name="password">
                <a-input-password 
                  v-model:value="loginForm.password" 
                  placeholder="请输入密码"
                  size="large"
                >
                  <template #prefix>
                    <LockOutlined />
                  </template>
                </a-input-password>
              </a-form-item>
              
              <a-form-item>
                <a-button 
                  type="primary" 
                  html-type="submit" 
                  size="large" 
                  block
                  :loading="loading"
                >
                  登录
                </a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
          
          <a-tab-pane key="register" tab="注册">
            <a-form
              :model="registerForm"
              :rules="registerRules"
              @finish="handleRegister"
              layout="vertical"
            >
              <a-form-item label="用户名" name="username">
                <a-input 
                  v-model:value="registerForm.username" 
                  placeholder="请输入用户名"
                  size="large"
                >
                  <template #prefix>
                    <UserOutlined />
                  </template>
                </a-input>
              </a-form-item>
              
              <a-form-item label="密码" name="password">
                <a-input-password 
                  v-model:value="registerForm.password" 
                  placeholder="请输入密码"
                  size="large"
                >
                  <template #prefix>
                    <LockOutlined />
                  </template>
                </a-input-password>
              </a-form-item>
              
              <a-form-item label="确认密码" name="confirmPassword">
                <a-input-password 
                  v-model:value="registerForm.confirmPassword" 
                  placeholder="请再次输入密码"
                  size="large"
                >
                  <template #prefix>
                    <LockOutlined />
                  </template>
                </a-input-password>
              </a-form-item>
              
              <a-form-item>
                <a-button 
                  type="primary" 
                  html-type="submit" 
                  size="large" 
                  block
                  :loading="loading"
                >
                  注册
                </a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
        
        <a-divider>或者</a-divider>
        
        <a-button 
          type="default" 
          size="large" 
          block
          @click="handleGuestLogin"
          :loading="guestLoading"
        >
          <template #icon><ExperimentOutlined /></template>
          游客体验
        </a-button>
        
        <div class="guest-tips">
          <InfoCircleOutlined />
          <span>游客模式下可以体验所有功能，但无法发布应用到市场</span>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, ExperimentOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { login, register } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const guestLoading = ref(false)

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

// 登录验证规则
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 注册验证规则
const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_: any, value: string) => {
        if (value !== registerForm.password) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

// 处理登录
const handleLogin = async (values: any) => {
  loading.value = true
  try {
    const res: any = await login({
      username: values.username,
      password: values.password
    })

    if (res.code === 200) {
      userStore.setToken(res.token)
      userStore.setUser(res.data)
      userStore.clearGuestMode()
      message.success('登录成功！')
      router.push('/project/new')
    } else {
      message.error(res.message || '登录失败')
    }
  } catch (error: any) {
    message.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

// 处理注册
const handleRegister = async (values: any) => {
  loading.value = true
  try {
    const res: any = await register({
      username: values.username,
      password: values.password
    })

    if (res.code === 200) {
      message.success('注册成功！已自动登录')
      userStore.setToken(res.token)
      userStore.setUser(res.data)
      userStore.clearGuestMode()
      router.push('/project/new')
    } else {
      message.error(res.message || '注册失败')
    }
  } catch (error: any) {
    message.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}

// 游客登录
const handleGuestLogin = async () => {
  guestLoading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    userStore.setGuestMode()
    message.success('欢迎体验！')
    router.push('/project/new')
  } finally {
    guestLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 480px;
}

.logo-section {
  text-align: center;
  margin-bottom: 32px;
  color: white;
}

.logo-section h1 {
  font-size: 2.5rem;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.logo-section p {
  font-size: 1rem;
  margin: 0;
  opacity: 0.9;
}

.login-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.guest-tips {
  margin-top: 16px;
  padding: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  font-size: 12px;
  color: #52c41a;
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.ant-card-body) {
  padding: 24px 32px;
}

/* 深色模式样式 */
.dark-theme .login-page {
  background: linear-gradient(135deg, #141414 0%, #1f1f1f 100%);
}
.dark-theme .logo-section h1 {
  color: rgba(255, 255, 255, 0.95);
}
.dark-theme .logo-section p {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .login-card {
  background: #1f1f1f;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
}
.dark-theme .guest-tips {
  background: rgba(82, 196, 26, 0.1);
  border-color: rgba(82, 196, 26, 0.3);
  color: #73d13d;
}
</style>
