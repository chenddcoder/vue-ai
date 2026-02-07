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

---

## 已完成功能

### 1. 用户认证系统

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户注册 | ✅ | 支持用户名密码注册 |
| 用户登录 | ✅ | 支持登录验证 |
| 游客模式 | ✅ | 无需登录即可体验所有功能 |
| 登录状态持久化 | ✅ |刷新浏览器保持登录状态 |

**API 接口：**
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/user` - 获取用户信息

**AI配置相关API：**
- `GET /magic/ai/configs` - 获取用户AI配置列表
- `POST /magic/ai/configs` - 保存AI配置
- `DELETE /magic/ai/configs/{id}` - 删除AI配置
- `PUT /magic/ai/configs/{id}/active` - 设置活动配置
- `POST /magic/ai/configs/test` - 测试AI配置
- `GET /magic/ai/providers` - 获取支持的AI提供商
- `POST /magic/ai/generate` - AI代码生成（支持配置化）

### 2. 代码编辑器

| 功能 | 状态 | 说明 |
|------|------|------|
| Monaco Editor | ✅ | VS Code 同款编辑器，支持语法高亮 |
| 文件管理 | ✅ | 文件树组件，支持切换文件，带文件类型图标 |
| 实时预览 | ✅ | 右侧实时预览效果 |
| 保存项目 | ✅ | 将项目保存到数据库 |
| 项目名称显示 | ✅ | 编辑器顶部显示当前项目名称 |
| AI 助手 | ✅ | 根据自然语言生成代码，支持多种AI模型配置 |

**组件结构：**
- `MonacoEditor.vue` - 代码编辑器
- `FileTree.vue` - 文件树，支持文件类型图标显示
- `FileIcons.ts` - 文件类型图标定义
- `Preview.vue` - 实时预览
- `AIAssistant.vue` - AI 代码生成，支持多种AI模型配置
- `AIConfigForm.vue` - AI配置表单组件

### 2.1 AI配置系统

| 功能 | 状态 | 说明 |
|------|------|------|
| 多AI提供商支持 | ✅ | 支持OpenAI、Claude、Azure OpenAI、本地模型等 |
| 配置管理 | ✅ | 添加、编辑、删除、切换AI配置 |
| 配置测试 | ✅ | 测试AI配置是否可用 |
| 高级选项 | ✅ | 支持温度、最大Token等参数配置 |
| 配置持久化 | ✅ | 配置保存到数据库，支持多用户 |

**支持的AI提供商：**
- **OpenAI**: GPT-4、GPT-4 Turbo、GPT-3.5 Turbo
- **Anthropic Claude**: Claude 3 Opus、Sonnet、Haiku
- **Azure OpenAI**: 企业级OpenAI服务
- **本地模型**: Ollama、LM Studio等
- **自定义API**: 兼容OpenAI格式的任何API

**数据表：**
- `magic_sys_ai_config` - AI配置表

### 3. 项目管理

| 功能 | 状态 | 说明 |
|------|------|------|
| 创建项目 | ✅ | 新建项目 |
| 编辑项目 | ✅ | 编辑现有项目代码 |
| 保存项目 | ✅ | 保存项目内容 |
| 项目列表 | ✅ | 查看所有项目 |

**API 接口：**
- `GET /api/project/list` - 获取项目列表
- `GET /api/project/{id}` - 获取项目详情
- `POST /api/project/save` - 保存/创建项目

### 4. 应用发布系统

| 功能 | 状态 | 说明 |
|------|------|------|
| 发布应用到市场 | ✅ | 从编辑器发布到市场 |
| 自动保存再发布 | ✅ | 发布前自动保存项目 |
| 管理已发布应用 | ✅ | 在"我的应用"中管理 |
| 下架应用 | ✅ | 从市场下架应用 |

**API 接口：**
- `POST /api/market/publish` - 发布应用
- `DELETE /api/market/apps/{id}` - 下架应用

**数据表：**
- `magic_sys_market_app` - 市场应用表
- `magic_sys_market_app_like` - 应用点赞表

### 5. 应用市场

| 功能 | 状态 | 说明 |
|------|------|------|
| 浏览应用 | ✅ | 查看所有已发布应用 |
| 搜索应用 | ✅ | 按名称/描述搜索 |
| 分类筛选 | ✅ | 按标签分类筛选 |
| 应用详情 | ✅ | 查看应用详细信息 |
| 应用预览 | ✅ | 在详情页预览应用 |
| 点赞应用 | ✅ | 点赞/取消点赞 |
| 使用模板 | ✅ | 将应用导入为新项目 |

**API 接口：**
- `GET /api/market/apps` - 获取应用列表（支持搜索、筛选、分页）
- `GET /api/market/apps/{id}` - 获取应用详情
- `POST /api/market/apps/{id}/like` - 点赞/取消点赞
- `GET /api/market/my-apps` - 获取我发布的应用

### 6. 用户界面

| 功能 | 状态 | 说明 |
|------|------|------|
| 登录页面 | ✅ | 登录/注册/游客体验 |
| 编辑器页面 | ✅ | 主编辑界面 |
| 应用市场 | ✅ | 浏览发布应用 |
| 应用详情 | ✅ | 查看/预览应用 |
| 我的应用 | ✅ | 管理项目和已发布应用 |

---

## 待完善功能

### 高优先级

| 功能 | 描述 | 复杂度 |
|------|------|--------|
| 项目删除 | 在"我的项目"中删除项目 | 低 |
| 项目重命名 | 支持修改项目名称 | 低 |
| 多文件支持 | 创建/删除/重命名多个文件 | 中 |
| 文件夹管理 | 支持创建文件夹和组织结构 | 中 |
| 实时保存 | 自动保存，无需手动点击 | 低 |

### 中优先级

| 功能 | 描述 | 复杂度 |
|------|------|--------|
| 用户头像 | 上传和显示头像 | 低 |
| 应用评论 | 对应用进行评论 | 中 |
| 应用评分 | 对应用进行星级评分 | 低 |
| 分享功能 | 生成分享链接 | 低 |
| 浏览量统计 | 实时更新浏览次数 | 低 |

### 低优先级

| 功能 | 描述 | 复杂度 |
|------|------|--------|
| 社交登录 | 微信/GitHub 登录 | 高 |
| 消息通知 | 系统通知和消息 | 中 |
| 应用收藏 | 收藏喜欢的应用 | 低 |
| 用户关注 | 关注其他开发者 | 中 |
| 活动日志 | 记录用户操作历史 | 低 |

### 技术优化

| 功能 | 描述 | 复杂度 |
|------|------|--------|
| 代码分割 | 优化首屏加载速度 | 中 |
| 单元测试 | 添加核心功能测试 | 中 |
| API Mock | 前后端分离开发 | 低 |
| 错误边界 | 全局错误处理 | 低 |
| 性能优化 | 减少包体积和加载时间 | 中 |

---

## 项目结构

```
vue-ai-platform/
├── src/
│   ├── api/                    # API 接口定义
│   │   └── index.ts
│   ├── components/             # 公共组件
│   │   ├── FileTree.vue
│   │   ├── editor/
│   │   │   ├── AIAssistant.vue
│   │   │   └── MonacoEditor.vue
│   │   └── preview/
│   │       └── Preview.vue
│   ├── router/                 # 路由配置
│   │   └── index.ts
│   ├── stores/                 # Pinia 状态管理
│   │   ├── project.ts
│   │   └── user.ts
│   ├── utils/                  # 工具函数
│   │   └── request.ts
│   ├── views/                  # 页面组件
│   │   ├── Login.vue
│   │   ├── EditorLayout.vue
│   │   ├── Market.vue
│   │   ├── MarketAppDetail.vue
│   │   └── MyApps.vue
│   ├── App.vue
│   ├── main.ts
│   └── style.css
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

---

## 快速开始

### 前端启动

```bash
cd vue-ai-platform
npm install
npm run dev
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
    status INTEGER DEFAULT 1
);

-- 应用点赞表
CREATE TABLE magic_sys_market_app_like (
    app_id INTEGER,
    user_id INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (app_id, user_id)
);
```

---

## 版本历史

- **v0.0.3** - AI配置功能完善版本
  - ✨ 支持多种AI提供商配置（OpenAI、Claude、Azure等）
  - ✨ AI配置管理页面，支持添加、编辑、删除、测试配置
  - ✨ AI助手界面优化，支持高级参数配置
  - ✨ 配置持久化存储，支持多用户配置管理
  - ✨ 新增AI配置入口到导航菜单和用户菜单

- **v0.0.2** - 编辑器增强版本
  - ✨ 文件树增加文件类型图标显示
  - ✨ 编辑器顶部显示当前项目名称
  - 🔧 优化文件树组件交互体验

- **v0.0.1** - 初始版本
  - 用户认证（登录/注册/游客）
  - 代码编辑器
  - 项目管理
  - 应用发布
  - 应用市场

---

## License

MIT
