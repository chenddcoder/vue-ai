<template>
  <a-dropdown>
    <a-button type="text" class="theme-toggle-btn">
      <template #icon>
        <BulbOutlined v-if="themeStore.theme === 'light'" />
        <IconDark v-else-if="themeStore.theme === 'dark'" />
        <DesktopOutlined v-else />
      </template>
    </a-button>
    <template #overlay>
      <a-menu @click="handleThemeChange">
        <a-menu-item key="light">
          <template #icon><BulbOutlined /></template>
          浅色模式
          <CheckOutlined v-if="themeStore.theme === 'light'" class="check-icon" />
        </a-menu-item>
        <a-menu-item key="dark">
          <template #icon><IconDark /></template>
          深色模式
          <CheckOutlined v-if="themeStore.theme === 'dark'" class="check-icon" />
        </a-menu-item>
        <a-menu-item key="auto">
          <template #icon><DesktopOutlined /></template>
          跟随系统
          <CheckOutlined v-if="themeStore.theme === 'auto'" class="check-icon" />
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { BulbOutlined, DesktopOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { useThemeStore } from '@/stores/theme'
import { h } from 'vue'

const themeStore = useThemeStore()

// 深色模式图标
const IconDark = () => h('svg', {
  viewBox: '64 64 896 896',
  focusable: 'false',
  'data-icon': 'moon',
  width: '1em',
  height: '1em',
  fill: 'currentColor',
  'aria-hidden': 'true'
}, [
  h('path', {
    d: 'M489.5 106.4c-17.2 4.6-33.1 12.6-46.8 23.7-17.7 14.4-30.5 33.2-36.9 54.4-4.6 16-4.6 34.2 0 50.2 6.4 21.2 19.2 40 36.9 54.4 17.7 14.4 39.5 23.6 62.9 26.3 6.8.8 21.8.8 28.6 0 23.4-2.7 45.2-11.9 62.9-26.3 17.7-14.4 30.5-33.2 36.9-54.4 4.6-16 4.6-34.2 0-50.2-6.4-21.2-19.2-40-36.9-54.4-13.7-11.1-29.6-19.1-46.8-23.7-12.6-3.4-35-3.4-47.7.1zM512 288c35.3 0 64 28.7 64 64s-28.7 64-64 64-64-28.7-64-64 28.7-64 64-64z'
  })
])

const handleThemeChange = ({ key }: { key: string }) => {
  themeStore.setTheme(key as 'light' | 'dark' | 'auto')
}
</script>

<style scoped>
.theme-toggle-btn {
  color: white;
}

.check-icon {
  margin-left: 8px;
  color: #1890ff;
}
</style>
