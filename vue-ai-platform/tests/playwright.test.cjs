const { test, expect } = require('@playwright/test');

const BASE_URL = 'http://localhost:5173';

// 等待开发服务器启动
test.beforeEach(async ({ page }) => {
  await page.goto(BASE_URL);
});

test.describe('Vue AI Platform - 登录/注册功能测试', () => {
  test('登录页面应该正常加载', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    
    // 检查页面标题
    await expect(page.locator('h1')).toContainText('Vue AI Platform');
    
    // 检查登录表单元素
    await expect(page.locator('text=登录')).toBeVisible();
    await expect(page.locator('text=注册')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible();
    
    // 检查游客体验按钮
    await expect(page.locator('text=游客体验')).toBeVisible();
    
    console.log('✅ 登录页面加载正常');
  });

  test('注册功能应该可以切换', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    
    // 点击注册标签
    await page.click('text=注册');
    
    // 检查注册表单元素
    await expect(page.locator('input[placeholder="请再次输入密码"]')).toBeVisible();
    
    // 检查登录按钮是否可见
    await expect(page.locator('button:has-text("注册")')).toBeVisible();
    
    console.log('✅ 注册功能切换正常');
  });

  test('游客体验功能应该可以进入编辑器', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    
    // 点击游客体验按钮
    await page.click('button:has-text("游客体验")');
    
    // 等待跳转到编辑器页面
    await page.waitForURL('**/project/new');
    
    // 检查编辑器元素
    await expect(page.locator('text=Vue AI Platform')).toBeVisible();
    await expect(page.locator('.ant-layout-header')).toBeVisible();
    
    // 检查游客标识
    await expect(page.locator('.guest-badge')).toBeVisible();
    
    console.log('✅ 游客体验功能正常');
  });
});

test.describe('Vue AI Platform - 编辑器功能测试', () => {
  test('编辑器页面应该包含所有主要组件', async ({ page }) => {
    // 先以游客身份登录
    await page.goto(`${BASE_URL}/login`);
    await page.click('button:has-text("游客体验")');
    await page.waitForURL('**/project/new');
    
    // 检查文件树
    await expect(page.locator('.ant-layout-sider')).toBeVisible();
    
    // 检查编辑器区域
    await expect(page.locator('.editor-pane')).toBeVisible();
    
    // 检查预览区域
    await expect(page.locator('.preview-pane')).toBeVisible();
    
    // 检查 AI 助手区域
    await expect(page.locator('.ai-pane')).toBeVisible();
    
    // 检查保存按钮
    await expect(page.locator('button:has-text("保存")')).toBeVisible();
    
    console.log('✅ 编辑器页面组件完整');
  });

  test('游客模式下发布按钮应该被禁用', async ({ page }) => {
    // 以游客身份登录
    await page.goto(`${BASE_URL}/login`);
    await page.click('button:has-text("游客体验")');
    await page.waitForURL('**/project/new');
    
    // 检查发布按钮（应该禁用）
    const publishButton = page.locator('button:has-text("发布")');
    await expect(publishButton).toBeVisible();
    await expect(publishButton).toBeDisabled();
    
    console.log('✅ 游客模式下发布按钮已禁用');
  });

  test('顶部导航应该包含应用市场入口', async ({ page }) => {
    // 以游客身份登录
    await page.goto(`${BASE_URL}/login`);
    await page.click('button:has-text("游客体验")');
    await page.waitForURL('**/project/new');
    
    // 检查导航菜单
    await expect(page.locator('text=编辑器')).toBeVisible();
    await expect(page.locator('text=应用市场')).toBeVisible();
    
    console.log('✅ 导航菜单正常');
  });
});

test.describe('Vue AI Platform - 应用市场测试', () => {
  test('应用市场页面应该正常加载', async ({ page }) => {
    await page.goto(`${BASE_URL}/market`);
    
    // 检查页面标题
    await expect(page.locator('h1:has-text("应用市场")')).toBeVisible();
    
    // 检查搜索框
    await expect(page.locator('input[placeholder="搜索应用..."]')).toBeVisible();
    
    // 检查分类筛选
    await expect(page.locator('text=分类')).toBeVisible();
    await expect(page.locator('text=全部')).toBeVisible();
    
    // 检查应用卡片
    await expect(page.locator('.app-card').first()).toBeVisible();
    
    console.log('✅ 应用市场页面加载正常');
  });

  test('应用卡片应该显示正确信息', async ({ page }) => {
    await page.goto(`${BASE_URL}/market`);
    
    // 获取第一个应用卡片
    const firstCard = page.locator('.app-card').first();
    
    // 检查卡片元素
    await expect(firstCard.locator('.ant-card-meta-title')).toBeVisible();
    await expect(firstCard.locator('.app-desc')).toBeVisible();
    await expect(firstCard.locator('.app-meta')).toBeVisible();
    
    console.log('✅ 应用卡片信息完整');
  });

  test('点击应用卡片应该跳转到详情页', async ({ page }) => {
    await page.goto(`${BASE_URL}/market`);
    
    // 点击第一个应用卡片
    await page.locator('.app-card').first().click();
    
    // 等待跳转到详情页
    await page.waitForURL('**/market/app/**');
    
    // 检查详情页元素
    await expect(page.locator('text=返回市场')).toBeVisible();
    await expect(page.locator('text=应用介绍')).toBeVisible();
    await expect(page.locator('text=作者')).toBeVisible();
    
    console.log('✅ 应用详情页跳转正常');
  });
});

test.describe('Vue AI Platform - 登录后功能测试', () => {
  test('登录后应该能正常使用发布功能', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    
    // 切换到登录标签
    await page.click('text=登录');
    
    // 填写登录表单
    await page.fill('input[placeholder="请输入用户名"]', 'testuser');
    await page.fill('input[placeholder="请输入密码"]', 'testpass123');
    
    // 点击登录按钮
    await page.click('button:has-text("登录")');
    
    // 等待跳转到编辑器
    await page.waitForURL('**/project/new', { timeout: 5000 });
    
    // 检查发布按钮是否可用（不再是禁用状态）
    const publishButton = page.locator('button:has-text("发布")');
    await expect(publishButton).toBeVisible();
    await expect(publishButton).toBeEnabled();
    
    console.log('✅ 登录后发布功能可用');
  });

  test('登录后应该能看到"我的应用"菜单', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    
    // 登录
    await page.click('text=登录');
    await page.fill('input[placeholder="请输入用户名"]', 'testuser');
    await page.fill('input[placeholder="请输入密码"]', 'testpass123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('**/project/new', { timeout: 5000 });
    
    // 检查"我的应用"菜单是否可见
    await expect(page.locator('text=我的应用')).toBeVisible();
    
    // 点击进入我的应用页面
    await page.click('text=我的应用');
    await page.waitForURL('**/my-apps');
    
    // 检查我的应用页面元素
    await expect(page.locator('h1:has-text("我的应用")')).toBeVisible();
    await expect(page.locator('text=我的项目')).toBeVisible();
    await expect(page.locator('text=已发布应用')).toBeVisible();
    
    console.log('✅ 我的应用页面正常');
  });
});

test.describe('Vue AI Platform - 导航功能测试', () => {
  test('应该能在不同页面间正常导航', async ({ page }) => {
    // 从登录页开始
    await page.goto(`${BASE_URL}/login`);
    
    // 点击游客体验进入编辑器
    await page.click('button:has-text("游客体验")');
    await page.waitForURL('**/project/new');
    
    // 点击应用市场
    await page.click('text=应用市场');
    await page.waitForURL('**/market');
    await expect(page.locator('h1:has-text("应用市场")')).toBeVisible();
    
    // 点击返回编辑器
    await page.click('text=编辑器');
    await page.waitForURL('**/project/new');
    await expect(page.locator('.editor-pane')).toBeVisible();
    
    console.log('✅ 页面导航功能正常');
  });
});

// 截图测试
test.describe('Vue AI Platform - 截图测试', () => {
  test('登录页面截图', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'test-results/login-page.png', fullPage: true });
    console.log('✅ 登录页面截图已保存');
  });

  test('编辑器页面截图', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.click('button:has-text("游客体验")');
    await page.waitForURL('**/project/new');
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'test-results/editor-page.png', fullPage: true });
    console.log('✅ 编辑器页面截图已保存');
  });

  test('应用市场截图', async ({ page }) => {
    await page.goto(`${BASE_URL}/market`);
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'test-results/market-page.png', fullPage: true });
    console.log('✅ 应用市场页面截图已保存');
  });
});
