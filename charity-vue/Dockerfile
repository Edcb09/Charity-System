# ---- 构建阶段 ----
# 使用 Node.js 镜像来构建前端静态文件
FROM node:24 AS builder
WORKDIR /app
# 复制 package.json 和 package-lock.json
COPY package*.json ./
# 安装依赖
RUN npm install
# 复制所有源代码
COPY . .
# 执行构建，生成 dist 文件夹
RUN npm run build

# ---- 生产阶段 ----
# 使用 Nginx 镜像来托管构建好的静态文件
FROM nginx:alpine
# 将构建生成的 dist 文件夹复制到 Nginx 的默认目录
COPY --from=builder /app/dist /usr/share/nginx/html
# 复制自定义的 Nginx 配置文件（用于处理 Vue Router 的 history 模式和反向代理）
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80