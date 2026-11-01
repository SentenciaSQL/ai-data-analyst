FROM node:22.23.4-bookworm-slim AS build
WORKDIR /src
RUN --mount=type=bind,source=package.json,target=package.json \
    --mount=type=bind,source=package-lock.json,target=package-lock.json \
    --mount=type=cache,target=/root/.npm \
    npm ci
COPY . .
ARG PRIMENG_LICENSE=
RUN node -e 'const fs=require("fs"); const key=process.env.PRIMENG_LICENSE||""; if (key) fs.writeFileSync("src/app/primeng-license.ts", "export const PRIMENG_LICENSE = "+JSON.stringify(key)+";\n");'
RUN npm run build

FROM nginx:1.28.3-alpine AS runtime
COPY nginx.conf /etc/nginx/nginx.conf
COPY --link --from=build /src/dist/frontend/browser /usr/share/nginx/html
RUN mkdir -p /tmp/client_temp /tmp/proxy_temp /tmp/fastcgi_temp /tmp/uwsgi_temp /tmp/scgi_temp \
    && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx /tmp
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
