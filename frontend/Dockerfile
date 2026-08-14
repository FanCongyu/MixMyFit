FROM node:22-alpine AS build
WORKDIR /workspace
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
ENV PORT=80
ENV BACKEND_ORIGIN=http://backend:8080
ENV NGINX_ENVSUBST_FILTER="^(PORT|BACKEND_ORIGIN)$"
RUN mkdir -p /etc/nginx/templates && printf 'server {\n  listen ${PORT};\n  server_name _;\n\n  location /api/ {\n    proxy_pass ${BACKEND_ORIGIN}/api/;\n    proxy_set_header Host $host;\n    proxy_set_header X-Real-IP $remote_addr;\n    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n    proxy_set_header X-Forwarded-Proto $scheme;\n  }\n\n  location / {\n    root /usr/share/nginx/html;\n    try_files $uri $uri/ /index.html;\n  }\n}\n' > /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
