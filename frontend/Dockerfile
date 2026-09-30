# Builder stage: install dependencies and build the static site
FROM node:22-alpine AS builder

WORKDIR /app

# Copy manifests first to leverage Docker cache
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Runtime stage: nginx serves the build and proxies API calls
FROM nginx:stable-alpine AS runner

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
