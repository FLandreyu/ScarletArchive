FROM node:22-alpine AS build
WORKDIR /build
COPY frontend/package*.json ./frontend/
RUN cd frontend && npm ci
COPY frontend/ ./frontend/
ARG SCARLET_CHARACTER_ART=on
ENV SCARLET_CHARACTER_ART=${SCARLET_CHARACTER_ART}
RUN cd frontend && npm run build
FROM nginx:alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/backend/src/main/resources/static/ /usr/share/nginx/html/
EXPOSE 80
