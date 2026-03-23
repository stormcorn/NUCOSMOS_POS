# NUCOSMOS POS

這個 repo 是 `NUCOSMOS POS` 的開發主專案，目前包含：

- Flutter POS 前台的產品規劃文件
- Vue 3 管理後台
- Java 17 Spring Boot 後端
- PostgreSQL + Flyway

## 本機開發

前端：

```bash
npm install
npm run dev
```

預設網址：

- `http://localhost:5173`

前端 API base URL 可在 [.env.example](./.env.example) 設定：

- `VITE_API_BASE_URL=http://localhost:8081`

後端：

```bash
cd backend
./mvnw spring-boot:run
```

若要用 Docker 啟 PostgreSQL：

```bash
cd backend
docker compose --env-file .env.example up -d
DB_PORT=5433 SERVER_PORT=8081 ./mvnw spring-boot:run
```

## 建置

```bash
npm run build
```

## 文件入口

- [管理後台與進銷存總 Roadmap](./docs/admin-system-roadmap.md)
- [POS 系統 PRD](./docs/pos-system-prd.md)
- [Vue 管理後台總覽](./docs/vue-admin-web.md)
- [Vue 管理後台 API 規格](./docs/vue-admin-api-spec.md)
- [Vue 管理後台頁面與欄位規格](./docs/vue-admin-page-field-spec.md)
- [Vue 管理後台前端開發規範](./docs/vue-admin-frontend-guidelines.md)
- [後端基礎架構文件](./docs/backend-foundation.md)
- [PIN + JWT 登入設計](./docs/pin-jwt-auth-design.md)
- [MacBook 交接文件](./docs/macbook-handoff.md)
- [Docker Desktop 換 Mac 開發流程](./docs/mac-docker-desktop-flow.md)
- [服務重啟與環境變數驗證規範](./docs/service-restart-and-env-validation.md)
