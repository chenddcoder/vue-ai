<template>
  <a-dropdown>
    <a-button type="text" class="theme-toggle-btn">
      <template #icon>
        <GlobalOutlined />
      </template>
    </a-button>
    <template #overlay>
      <a-menu>
        <a-menu-item-group title="主题">
          <a-menu-item key="light" @click="themeStore.setTheme('light')">
            <BulbOutlined /> 浅色模式
            <CheckOutlined v-if="themeStore.theme === 'light'" class="check-icon" />
          </a-menu-item>
          <a-menu-item key="dark" @click="themeStore.setTheme('dark')">
            <IconDark /> 深色模式
            <CheckOutlined v-if="themeStore.theme === 'dark'" class="check-icon" />
          </a-menu-item>
          <a-menu-item key="auto" @click="themeStore.setTheme('auto')">
            <DesktopOutlined /> 跟随系统
            <CheckOutlined v-if="themeStore.theme === 'auto'" class="check-icon" />
          </a-menu-item>
        </a-menu-item-group>
        <a-menu-divider />
        <a-menu-item-group title="语言">
          <a-menu-item key="zh-CN" @click="i18nStore.setLocale('zh-CN')">
            🇨🇳 简体中文
            <CheckOutlined v-if="i18nStore.locale === 'zh-CN'" class="check-icon" />
          </a-menu-item>
          <a-menu-item key="en-US" @click="i18nStore.setLocale('en-US')">
            🇺🇸 English
            <CheckOutlined v-if="i18nStore.locale === 'en-US'" class="check-icon" />
          </a-menu-item>
        </a-menu-item-group>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { BulbOutlined, DesktopOutlined, CheckOutlined, GlobalOutlined } from '@ant-design/icons-vue'
import { useThemeStore } from '@/stores/theme'
import { useI18nStore } from '@/stores/i18n'
import { h } from 'vue'

const themeStore = useThemeStore()
const i18nStore = useI18nStore()

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
