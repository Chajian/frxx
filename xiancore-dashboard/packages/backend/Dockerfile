# Backend Dockerfile for XianCore Dashboard
FROM node:20-slim AS base

# Install pnpm
RUN corepack enable && corepack prepare pnpm@9.15.1 --activate

# Build stage
FROM base AS builder

WORKDIR /app

# Copy workspace files
COPY pnpm-workspace.yaml pnpm-lock.yaml package.json ./
COPY packages/backend/package.json ./packages/backend/

# Install dependencies
RUN pnpm install --frozen-lockfile

# Copy backend source code
COPY packages/backend ./packages/backend

# Generate Prisma Client
WORKDIR /app/packages/backend
RUN pnpm prisma:generate

# Build the application
RUN pnpm build

# Production stage
FROM base AS runner

WORKDIR /app

# Copy workspace files
COPY pnpm-workspace.yaml pnpm-lock.yaml package.json ./
COPY packages/backend/package.json ./packages/backend/

# Install all dependencies (including prisma CLI for generation)
RUN pnpm install --frozen-lockfile

# Copy built files and Prisma schema
COPY --from=builder /app/packages/backend/dist ./packages/backend/dist
COPY --from=builder /app/packages/backend/prisma ./packages/backend/prisma

# Set working directory to backend
WORKDIR /app/packages/backend

# Generate Prisma Client
RUN pnpm prisma:generate

# Remove dev dependencies to reduce image size
RUN pnpm prune --prod

# Create non-root user (Debian syntax)
RUN groupadd -r -g 1001 nodejs && \
    useradd -r -u 1001 -g nodejs nodejs && \
    mkdir -p /home/nodejs && \
    chown -R nodejs:nodejs /app /home/nodejs

USER nodejs

# Expose port
EXPOSE 8400

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD node -e "require('http').get('http://localhost:8400/api/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})" || exit 1

# Start the application directly with node (avoid pnpm permission issues)
CMD ["node", "dist/index.js"]
