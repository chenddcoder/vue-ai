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

// 深色模式图标 - 使用更清晰的SVG
const IconDark = () => h('svg', {
  viewBox: '0 0 24 24',
  focusable: 'false',
  'data-icon': 'moon',
  width: '1em',
  height: '1em',
  fill: 'currentColor',
  'aria-hidden': 'true'
}, [
  h('path', {
    d: 'M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z'
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

/* 深色模式下的下拉菜单样式 */
:deep(.dark-theme .ant-dropdown-menu) {
  background: #1f1f1f !important;
  border: 1px solid #434343;
}

:deep(.dark-theme .ant-dropdown-menu-item) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
}

:deep(.dark-theme .ant-dropdown-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
}

:deep(.dark-theme .ant-dropdown-menu-item svg),
:deep(.dark-theme .ant-dropdown-menu-item .anticon) {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.dark-theme .ant-dropdown-menu-item-divider) {
  background: #434343;
}
</style>
