FROM node:20-alpine AS build
WORKDIR /app
COPY ui-console/package*.json ./
RUN npm ci
COPY ui-console/ ./
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY ui-console/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
