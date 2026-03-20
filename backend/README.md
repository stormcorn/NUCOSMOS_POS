# NUCOSMOS POS Backend

這個目錄是 POS 系統的 Spring Boot 後端骨架，現階段先提供：

- Spring Boot 啟動入口
- 基本健康檢查 API
- 系統資訊 API
- 商品 API 範例
- CORS 與 API 回應格式基礎結構

## 環境需求

- Java 21
- Maven 3.9+

## 啟動方式

```bash
cd backend
mvn spring-boot:run
```

預設服務位址：

- `http://localhost:8080`

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

目前這一版後端已接入 JPA、Flyway、PostgreSQL 設定，並提供 POS PIN + JWT 登入的第一版實作。

## 下一步建議

- 接入 PostgreSQL 與 Spring Data JPA
- 導入 Flyway 管理資料庫 migration
- 建立認證授權與帳號模組
- 建立商品、訂單、門市、裝置等正式資料模型

## 詳細文件

- [後端基礎建設文件](../docs/backend-foundation.md)
- [PIN + JWT 登入設計](../docs/pin-jwt-auth-design.md)
