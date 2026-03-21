# NUCOSMOS POS

產品規劃方向是 Flutter POS 與 Vue 管理後台。

目前這個 repo 現階段包含：

- Vue 3 + Vite 的管理後台前端
- POS 系統實用架構 PRD
- Spring Boot 後端
- PostgreSQL + Flyway migration
- POS PIN + JWT 登入第一版

## 開發

```bash
npm install
npm run dev
```

前端預設位置：

- `http://localhost:5173`

前端 API Base URL 可透過 [.env.example](./.env.example) 設定：

- `VITE_API_BASE_URL=http://localhost:8081`

## 後端

```bash
cd backend
./mvnw spring-boot:run
```

目前預設後端服務使用 `8081`。如果本機 `5432` 或 `8081` 已被其他專案占用，可改用：

```bash
cd backend
docker compose --env-file .env.example up -d
DB_PORT=5433 SERVER_PORT=8081 ./mvnw spring-boot:run
```

## 建置

```bash
npm run build
```

## 文件

- [POS 系統實用架構 PRD](./docs/pos-system-prd.md)
- [Vue 管理後台前端文件](./docs/vue-admin-web.md)
- [Vue 管理後台 API 對接規格](./docs/vue-admin-api-spec.md)
- [Vue 管理後台頁面清單與欄位規格](./docs/vue-admin-page-field-spec.md)
- [Vue 管理後台前端開發規範](./docs/vue-admin-frontend-guidelines.md)
- [後端基礎建設文件](./docs/backend-foundation.md)
- [PIN + JWT 登入設計](./docs/pin-jwt-auth-design.md)
- [MacBook 接手工作指南](./docs/macbook-handoff.md)
