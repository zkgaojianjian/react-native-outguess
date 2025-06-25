# 发布指南

## 📦 发布到 NPM

### 1. 准备发布

确保所有代码已经完成并测试通过：

```bash
# 运行测试
npm test

# 类型检查
npm run typecheck

# 代码检查
npm run lint

# 构建库
npm run build
```

### 2. 版本管理

使用语义化版本控制：

```bash
# 补丁版本 (1.0.0 -> 1.0.1)
npm version patch

# 次要版本 (1.0.0 -> 1.1.0)
npm version minor

# 主要版本 (1.0.0 -> 2.0.0)
npm version major
```

### 3. 发布到 NPM

```bash
# 登录 NPM (首次)
npm login

# 发布
npm publish

# 发布 beta 版本
npm publish --tag beta
```

### 4. 发布检查清单

- [ ] 更新 README.md
- [ ] 更新 CHANGELOG.md
- [ ] 确保所有测试通过
- [ ] 检查 package.json 中的版本号
- [ ] 确保 .npmignore 正确配置
- [ ] 验证构建产物

## 🏷️ Git 标签管理

```bash
# 创建标签
git tag v1.0.0

# 推送标签
git push origin v1.0.0

# 推送所有标签
git push origin --tags
```

## 📋 发布后验证

```bash
# 安装发布的包进行验证
npm install react-native-outguess@latest

# 检查包内容
npm pack --dry-run
```

## 🔄 自动化发布 (GitHub Actions)

创建 `.github/workflows/publish.yml`:

```yaml
name: Publish to NPM

on:
  release:
    types: [published]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
          registry-url: 'https://registry.npmjs.org'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run tests
        run: npm test
      
      - name: Build
        run: npm run build
      
      - name: Publish
        run: npm publish
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

## 📊 发布统计

发布后可以在以下地方查看统计：

- [NPM 包页面](https://www.npmjs.com/package/react-native-outguess)
- [NPM 下载统计](https://npm-stat.com/charts.html?package=react-native-outguess)
- GitHub Releases 页面

## 🚀 推广

- 在 React Native 社区分享
- 写技术博客介绍使用方法
- 在相关论坛和社交媒体宣传
- 提交到 awesome-react-native 列表