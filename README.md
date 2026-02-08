# Vue AI Platform

一个 AI 驱动的应用开发平台，支持可视化编辑、代码生成和应用发布。

## 技术栈

### 前端
- **Vue 3** - 前端框架
- **TypeScript** - 类型安全
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Ant Design Vue 4** - UI 组件库
- **Monaco Editor** - 代码编辑器（VS Code 同款）
- **Vite** - 构建工具
- **Axios** - HTTP 请求

### 后端
- **Spring Boot 2.7** - Java 后端框架
- **SQLite** - 嵌入式数据库
- **JDBC** - 数据库访问
- **Hutool** - Java 工具库

---

## 当前版本: v0.5.0

### 已完成功能

#### 1. 用户认证系统 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户注册 | ✅ | 支持用户名密码注册 |
| 用户登录 | ✅ | 支持登录验证 |
| 游客模式 | ✅ | 无需登录即可体验所有功能 |
| 登录状态持久化 | ✅ | 刷新浏览器保持登录状态 |

**API 接口：**
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/user` - 获取用户信息

#### 2. AI 配置系统 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 多AI提供商支持 | ✅ | OpenAI、Claude、Azure、本地模型等 |
| 配置管理 | ✅ | 添加、编辑、删除、切换AI配置 |
| 配置测试 | ✅ | 测试AI配置是否可用 |
| 高级选项 | ✅ | 温度、最大Token等参数配置 |
| 配置持久化 | ✅ | 配置保存到数据库，支持多用户 |

**支持的AI提供商：**
- **OpenAI**: GPT-4、GPT-4 Turbo、GPT-3.5 Turbo
- **Anthropic Claude**: Claude 3 Opus、Sonnet、Haiku
- **Azure OpenAI**: 企业级OpenAI服务
- **本地模型**: Ollama、LM Studio等
- **自定义API**: 兼容OpenAI格式的任何API

**API 接口：**
- `GET /magic/ai/configs` - 获取用户AI配置列表
- `POST /magic/ai/configs` - 保存AI配置
- `DELETE /magic/ai/configs/{id}` - 删除AI配置
- `PUT /magic/ai/configs/{id}/active` - 设置活动配置
- `POST /magic/ai/configs/test` - 测试AI配置
- `GET /magic/ai/providers` - 获取支持的AI提供商
- `POST /magic/ai/generate` - AI代码生成

#### 3. 代码编辑器 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| Monaco Editor | ✅ | VS Code同款编辑器，语法高亮 |
| 文件管理 | ✅ | 文件树组件，文件类型图标 |
| 实时预览 | ✅ | 右侧实时预览 |
| 保存项目 | ✅ | 项目保存到数据库 |
| AI助手 | ✅ | 自然语言生成代码 |

**组件结构：**
- `MonacoEditor.vue` - 代码编辑器
- `FileTree.vue` - 文件树
- `Preview.vue` - 实时预览
- `AIAssistant.vue` - AI代码生成
- `AIConfigForm.vue` - AI配置表单

#### 4. 项目管理 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 创建项目 | ✅ | 新建项目 |
| 编辑项目 | ✅ | 编辑现有项目代码 |
| 保存项目 | ✅ | 保存项目内容 |
| 项目列表 | ✅ | 查看所有项目 |
| 项目删除 | ✅ | 删除项目 |
| 项目重命名 | ✅ | 重命名项目 |
| 多文件支持 | ✅ | 创建、删除、重命名多个文件 |
| 文件夹管理 | ✅ | 创建文件夹和组织结构 |

**API 接口：**
- `GET /api/project/list` - 获取项目列表
- `GET /api/project/{id}` - 获取项目详情
- `POST /api/project/save` - 保存/创建项目
- `DELETE /api/project/{id}` - 删除项目
- `PUT /api/project/{id}/rename` - 重命名项目

#### 5. 应用发布系统 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 发布到市场 | ✅ | 从编辑器发布到市场 |
| 开源设置 | ✅ | 支持设置开源或闭源（保护代码） |
| 自动保存 | ✅ | 发布前自动保存项目 |
| 管理已发布 | ✅ | 在"我的应用"中管理 |
| 下架应用 | ✅ | 从市场下架应用 |

#### 6. 应用市场 (全新改版) ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 应用图标网格 | ✅ | 类似Mac启动台的图标展示 |
| 点击启动 | ✅ | 点击图标直接启动应用 |
| 应用窗口管理 | ✅ | 支持多窗口同时运行 |
| 窗口拖拽 | ✅ | 自由拖拽窗口位置 |
| 窗口最小化 | ✅ | 最小化到Dock栏 |
| 窗口最大化/全屏 | ✅ | 全屏显示应用 |
| 窗口关闭 | ✅ | 关闭应用窗口 |
| Mac风格Dock栏 | ✅ | 底部显示运行中的应用 |
| ESC键支持 | ✅ | ESC键最小化当前窗口 |
| 搜索应用 | ✅ | 按名称/描述搜索 |
| 分类筛选 | ✅ | 按标签分类筛选 |

**新特性：**
- **启动台模式**: 类似 macOS 的应用启动台，全屏显示应用图标
- **窗口系统**: 完整的窗口生命周期管理
- **Dock栏**: 屏幕底部显示运行中的应用，支持快速切换
- **沉浸式体验**: 启动应用后只显示应用内容，隐藏平台界面

**组件结构：**
- `MarketLaunchpad.vue` - 应用市场页面（图标网格 + 窗口）
- `Launchpad.vue` - 全屏启动台组件
- 窗口控制按钮（红/黄/绿配色）
- 玻璃拟态Dock栏

**交互方式：**
- 点击应用图标 → 打开应用窗口
- 红色按钮 → 关闭应用
- 黄色按钮 → 最小化到Dock栏
- 绿色按钮 → 全屏/退出全屏
- Dock栏点击 → 显示/隐藏应用
- 拖拽窗口 → 移动窗口位置
- ESC键 → 最小化当前窗口

**API 接口：**
- `GET /api/market/apps` - 获取应用列表
- `GET /api/market/apps/{id}` - 获取应用详情
- `POST /api/market/apps/{id}/like` - 点赞
- `GET /api/market/my-apps` - 获取我发布的应用
- `POST /api/market/publish` - 发布应用
- `DELETE /api/market/apps/{id}` - 下架应用

#### 7. 项目保存优化 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 保存项目弹窗 | ✅ | 新建项目时弹窗输入名称 |
| 自动更新路由 | ✅ | 保存后自动更新项目ID和路由 |
| 预览路由优化 | ✅ | 使用内存历史记录防止哈希冲突 |
| 保存逻辑优化 | ✅ | 避免重复保存，提升性能 |

#### 8. 项目导出功能 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 项目导出 | ✅ | 支持将项目导出为ZIP压缩包 |
| 包含依赖 | ✅ | 自动生成package.json依赖文件 |
| 一键下载 | ✅ | 快速下载项目源码 |

#### 9. 智能模块代码生成 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 智能生成 | ✅ | 根据需求描述生成完整功能模块 |
| 多类型支持 | ✅ | 支持生成页面、组件、工具类等 |
| AI集成 | ✅ | 调用AI服务生成高质量代码 |
| 代码预览 | ✅ | 生成后可直接预览效果 |

---

## v0.5.0 更新内容

### 应用市场全新改版

#### 新增功能
1. **启动台模式**
   - 全屏应用启动台界面
   - 搜索和分类筛选
   - 毛玻璃背景效果

2. **窗口管理系统**
   - 独立窗口运行多个应用
   - 窗口拖拽移动
   - 窗口最小化/最大化/关闭
   - 窗口层级管理（点击置顶）

3. **Mac风格Dock栏**
   - 底部停靠栏显示运行中的应用
   - 悬停放大效果
   - 活动窗口指示点
   - 一键关闭所有应用

#### 技术改进
- 改进Vue组件加载逻辑，支持多文件组件
- 添加错误处理和加载状态
- 优化窗口位置和大小计算
- 增强路径解析算法

---

## 下一步迭代规划 (v0.6.0)

### 1. 社区功能增强 (P1)
- [ ] **应用评论**: 用户可以对发布的应用进行评论
- [ ] **应用评分**: 支持星级评分系统
- [ ] **应用收藏**: 用户收藏喜欢的应用到个人中心
- [ ] **用户关注**: 关注其他开发者

### 2. 应用详情增强 (P1)
- [ ] **应用截图**: 支持上传多张截图展示
- [ ] **应用视频**: 支持上传演示视频
- [ ] **使用说明**: 详细的使用文档和帮助
- [ ] **版本管理**: 应用版本历史记录

### 3. 用户体验优化 (P2)
- [ ] **消息通知**: 系统通知和消息中心
- [ ] **操作日志**: 用户操作历史记录
- [ ] **首屏优化**: 加载性能优化
- [ ] **深色模式**: 完整的深色主题支持

### 4. 编辑器增强 (P2)
- [ ] **代码补全**: 编辑器内智能代码补全
- [ ] **代码解释**: 选中代码进行AI解释
- [ ] **代码优化**: AI提供代码优化建议
- [ ] **错误检测**: 实时语法错误检测

### 5. 分享功能 (P2)
- [ ] **生成海报**: 生成应用分享海报
- [ ] **社交分享**: 一键分享到社交平台
- [ ] **嵌入代码**: 生成可嵌入网页的代码

---

## 后续规划 (v1.0.0)

### 高级功能
- [ ] **部署服务**: 一键部署应用到生产环境
- [ ] **团队协作**: 多人协作编辑项目
- [ ] **模板市场**: 官方和社区模板
- [ ] **API市场**: 第三方API集成
- [ ] **数据分析**: 应用使用数据分析

### 国际化
- [ ] **多语言支持**: 英文、日文等语言支持
- [ ] **本地化**: 适应不同地区用户

### 性能与安全
- [ ] **性能优化**: 大项目加载优化
- [ ] **安全加固**: 增强安全性和权限控制
- [ ] **CDN加速**: 静态资源加速

---

## 快速开始

### 前端启动

```bash
cd vue-ai-platform
pnpm install
pnpm dev
```

### 后端启动

```bash
cd vue-ai-server
mvn spring-boot:run
```

### 访问地址

- 前端：http://localhost:5173
- 后端 API：http://localhost:8081

---

## 数据库表结构

```sql
-- 用户表
CREATE TABLE magic_sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT DEFAULT 'user',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI配置表
CREATE TABLE magic_sys_ai_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    provider TEXT NOT NULL,
    api_key TEXT,
    base_url TEXT,
    model TEXT,
    temperature REAL DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2000,
    is_active INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 项目表
CREATE TABLE magic_sys_project (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    owner_id INTEGER,
    content TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 市场应用表
CREATE TABLE magic_sys_market_app (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER,
    name TEXT NOT NULL,
    description TEXT,
    tags TEXT,
    thumbnail TEXT,
    content TEXT,
    author_id INTEGER,
    author_name TEXT,
    author_avatar TEXT,
    likes INTEGER DEFAULT 0,
    views INTEGER DEFAULT 0,
    publish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status INTEGER DEFAULT 1,
    is_open_source INTEGER DEFAULT 1
);

-- 应用点赞表
CREATE TABLE magic_sys_market_app_like (
    app_id INTEGER,
    user_id INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (app_id, user_id)
);

-- 应用收藏表
CREATE TABLE magic_sys_market_app_favorite (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    app_id INTEGER NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 应用评论表
CREATE TABLE magic_sys_market_app_comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    rating INTEGER DEFAULT 5,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 项目结构

```
vue-ai/
├── vue-ai-platform/        # 前端应用
│   ├── src/
│   │   ├── api/            # API接口定义
│   │   ├── components/     # 公共组件
│   │   │   ├── editor/     # 编辑器组件
│   │   │   └── preview/    # 预览组件
│   │   ├── router/         # 路由配置
│   │   ├── stores/         # Pinia状态管理
│   │   ├── utils/          # 工具函数
│   │   ├── views/          # 页面组件
│   │   │   ├── EditorLayout.vue
│   │   │   ├── MarketLaunchpad.vue
│   │   │   ├── Launchpad.vue
│   │   │   ├── MyApps.vue
│   │   │   ├── AIConfig.vue
│   │   │   └── Login.vue
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
└── vue-ai-server/           # 后端应用
    └── src/main/java/
        └── com.vueai/server/
            ├── config/      # 配置类
            ├── controller/ # 控制器
            ├── dto/        # 数据传输对象
            ├── model/      # 数据模型
            └── service/    # 服务层
```

---

## 版本历史

### v0.5.0 - 应用市场全新改版
**重大更新**
- ✅ 全新应用市场界面，类似 macOS 启动台
- ✅ 应用图标网格展示，点击即可启动
- ✅ 完整的窗口管理系统（拖拽、最小化、最大化、关闭）
- ✅ Mac 风格 Dock 栏，支持窗口快速切换
- ✅ ESC 键快捷操作
- ✅ 改进的组件加载逻辑和错误处理

### v0.4.0 - 用户体验优化版本
- 新增用户头像上传功能（支持URL自定义头像）
- 实现实时自动保存（3秒无操作自动保存）
- 添加应用分享功能（一键复制分享链接）
- 浏览量统计（实时更新应用浏览次数）

### v0.3.0 - 智能代码生成与项目导出版本
- 新增保存项目弹窗并优化保存逻辑
- 添加项目导出功能（支持ZIP下载）
- 实现智能模块代码生成功能
- 为应用市场添加开源/闭源设置功能
- 添加通义千问AI支持
- 添加AI配置管理系统（多提供商支持）
- 完善项目管理和多文件编辑功能

### v0.2.0 - AI能力增强版本
- 用户认证系统（注册、登录、游客模式）
- AI配置系统（多AI提供商支持）
- 代码编辑器（Monaco Editor集成）
- 项目管理（多文件、文件夹管理）
- 应用发布系统
- 应用市场（浏览、搜索、使用模板）

### v0.1.0 - 核心编辑体验完善版本
- 项目删除功能
- 项目重命名功能
- 多文件支持（创建、删除、重命名）
- 文件夹管理（创建、重命名、删除）
- 文件树右键菜单优化

### v0.0.3 - AI配置功能完善版本
- 支持多种AI提供商配置
- AI配置管理页面
- 配置持久化存储

### v0.0.2 - 编辑器增强版本
- 文件类型图标显示
- 项目名称显示

### v0.0.1 - 初始版本
- 用户认证
- 代码编辑器
- 项目管理
- 应用发布
- 应用市场

---

## License

MIT
