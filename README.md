# NUCOSMOS POS

產品規劃方向是 Flutter POS 與 Vue 管理後台。

目前這個 repo 現階段包含：

- React + Vite 的 POS 介面原型
- POS 系統實用架構 PRD
- Spring Boot 後端
- PostgreSQL + Flyway migration
- POS PIN + JWT 登入第一版

## 開發

```bash
npm install
npm run dev
```

## 後端

```bash
cd backend
./mvnw spring-boot:run
```

如果本機 `5432` 或 `8080` 已被其他專案占用，可改用：

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
- [後端基礎建設文件](./docs/backend-foundation.md)
- [PIN + JWT 登入設計](./docs/pin-jwt-auth-design.md)
- [MacBook 接手工作指南](./docs/macbook-handoff.md)
