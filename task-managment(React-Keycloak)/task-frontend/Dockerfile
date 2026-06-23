# ============================================================
# Stage 1 — BUILD
# Uses Node 20 to install deps and compile the React + Vite app.
# The output is a static /dist folder.
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first — Docker caches this layer.
# npm ci only re-runs when package-lock.json changes.
COPY package*.json ./
RUN npm ci

# Vite embeds VITE_* environment variables at BUILD TIME.
# They are inlined into the compiled JS bundle.
# ARG values come from docker-compose build.args section.
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ARG VITE_KEYCLOAK_REALM=internship-task-realm
ARG VITE_KEYCLOAK_CLIENT_ID=task-frontend
ARG VITE_API_BASE_URL=http://localhost:8080

# Expose ARGs as ENV so Vite picks them up during build
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy source and build
COPY . .
RUN npm run build
# Output is in /app/dist

# ============================================================
# Stage 2 — RUNTIME
# Uses Nginx Alpine to serve the static files.
# The entire Node environment, source code, and node_modules
# are discarded — final image is ~25MB.
# ============================================================
FROM nginx:alpine AS runtime

# Copy compiled React app from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy our Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

# Start Nginx in foreground (Docker requires this)
CMD ["nginx", "-g", "daemon off;"]
