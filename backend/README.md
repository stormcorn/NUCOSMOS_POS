# NUCOSMOS POS Backend

這個目錄是 POS 系統的 Spring Boot 後端，現階段已提供：

- Spring Boot 啟動入口
- 基本健康檢查 API
- 系統資訊 API
- 商品 API 範例
- CORS 與 API 回應格式基礎結構
- PostgreSQL + Flyway migration
- POS PIN + JWT 登入第一版

## 環境需求

- Java 17
- Docker Desktop

## 啟動方式

```bash
cd backend
./mvnw spring-boot:run
```

預設服務位址：

- `http://localhost:8081`

如果本機 `8081` 已被其他服務占用，可改用：

```bash
SERVER_PORT=8082 ./mvnw spring-boot:run
```

如果你也要使用這個 repo 預設的 PostgreSQL `5433`：

```bash
DB_PORT=5433 SERVER_PORT=8081 ./mvnw spring-boot:run
```

## 目前可用 API

- `GET /api/v1/health`
- `GET /api/v1/system/info`
- `POST /api/v1/auth/pin-login`
- `GET /api/v1/auth/me`
- `GET /api/v1/products`

## PostgreSQL 開發底座

如果你本機有 Docker，可以先用這個指令把 PostgreSQL 跑起來：

```bash
cd backend
docker compose --env-file .env.example up -d
```

這份設定預設使用 `5433`，避免和本機其他專案常見的 PostgreSQL `5432` 衝突。

目前這一版後端已接入 JPA、Flyway、PostgreSQL 設定，並提供 POS PIN + JWT 登入的第一版實作。

## 啟動順序建議

```bash
cd backend
docker compose --env-file .env.example up -d
./mvnw test
DB_PORT=5433 ./mvnw spring-boot:run
```

## 詳細文件

- [後端基礎建設文件](../docs/backend-foundation.md)
- [PIN + JWT 登入設計](../docs/pin-jwt-auth-design.md)
- [MacBook 接手工作指南](../docs/macbook-handoff.md)
