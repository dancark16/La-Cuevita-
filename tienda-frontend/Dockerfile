# Se construye desde la raíz del repositorio:  docker build -f tienda-frontend/Dockerfile .

# ---- Compilación de Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY tienda-frontend/package.json tienda-frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY tienda-frontend/ ./
RUN npx ng build --configuration production

# ---- Nginx: sirve la tienda y reenvía /api al backend
FROM nginx:1.27-alpine
RUN rm -f /etc/nginx/conf.d/default.conf
COPY deploy/nginx/site /etc/nginx/site
COPY deploy/nginx/snippets /etc/nginx/snippets
COPY deploy/nginx/conf.d /etc/nginx/conf.d
COPY deploy/nginx/40-select-config.sh /docker-entrypoint.d/40-select-config.sh
RUN chmod +x /docker-entrypoint.d/40-select-config.sh
COPY --from=build /app/dist/tienda-frontend/browser /usr/share/nginx/html
EXPOSE 80 443
