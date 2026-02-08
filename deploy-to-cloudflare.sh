#!/bin/bash

# Cloudflare Pages 部署脚本
# 前提：
# 1. 已安装 Node.js 和 pnpm
# 2. 已拥有 Cloudflare 账号
# 3. 后端服务已部署并有公开可访问的 HTTPS 地址

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== 开始部署 Vue AI Platform 到 Cloudflare Pages ===${NC}"

# 1. 检查后端 API 地址配置
echo -e "${YELLOW}检查 API 配置...${NC}"
cd vue-ai-platform

if [ ! -f .env.production ]; then
    echo -e "${YELLOW}警告: 未找到 .env.production 文件${NC}"
    echo "正在创建 .env.production 模板..."
    echo "VITE_API_BASE_URL=https://vue-ai.chenddcoder.cn" > .env.production
    echo -e "${RED}请编辑 vue-ai-platform/.env.production 文件，填入你实际的后端 API 地址！${NC}"
    echo -e "${RED}例如: VITE_API_BASE_URL=https://api.example.com${NC}"
    read -p "修改完成后按回车继续..."
else
    echo -e "${GREEN}发现 .env.production 文件${NC}"
    grep "VITE_API_BASE_URL" .env.production
    echo "确认上述 API 地址正确吗？(y/n)"
    read -r confirm
    if [ "$confirm" != "y" ]; then
        echo "请修改 .env.production 后重新运行脚本"
        exit 1
    fi
fi

# 2. 安装依赖
echo -e "${GREEN}正在安装依赖...${NC}"
pnpm install

# 3. 构建项目
echo -e "${GREEN}正在构建项目...${NC}"
pnpm build

if [ $? -ne 0 ]; then
    echo -e "${RED}构建失败！请检查错误信息。${NC}"
    exit 1
fi

# 4. 部署到 Cloudflare
echo -e "${GREEN}准备部署到 Cloudflare Pages...${NC}"
echo "如果你是第一次运行，可能需要浏览器登录 Cloudflare 授权。"

# 检查是否安装了 wrangler，如果没有则使用 npx
WRANGLER_CMD="npx wrangler"


# 交互式部署
echo "正在启动部署..."
$WRANGLER_CMD pages deploy dist --project-name=vue-ai-platform

if [ $? -eq 0 ]; then
    echo -e "${GREEN}=== 部署成功！ ===${NC}"
    echo -e "请确保你的后端服务配置了允许 Cloudflare 域名的 CORS (跨域资源共享)。"
else
    echo -e "${RED}部署失败。${NC}"
fi
