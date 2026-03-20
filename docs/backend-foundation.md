# NUCOSMOS POS 後端基礎建設文件

## 1. 文件目的

這份文件記錄目前 NUCOSMOS POS 後端的第一階段實作結果，目標是把後端從單純 Spring Boot 骨架，提升為可持續擴充的基礎版。此版本已納入：

- Java Spring Boot 作為主框架
- PostgreSQL 作為正式主資料庫
- Spring Data JPA 作為資料存取層
- Flyway 作為資料庫 migration 工具
- H2 作為測試環境資料庫
- Docker Compose 作為本機 PostgreSQL 開發底座

## 2. 技術選型

### 2.1 後端框架

- Java 21
- Spring Boot 3.5
- Maven

### 2.2 資料層

- PostgreSQL：正式環境與本機開發的主資料庫
- Spring Data JPA：Entity 與 Repository 層
- Flyway：版本化 schema migration
- H2：測試用記憶體資料庫，採 PostgreSQL 相容模式

## 3. 目前資料夾結構

```text
backend/
├─ pom.xml
├─ compose.yaml
├─ .env.example
├─ README.md
└─ src/
   ├─ main/
   │  ├─ java/com/nucosmos/pos/backend/
   │  │  ├─ common/
   │  │  │  ├─ api/
   │  │  │  ├─ exception/
   │  │  │  └─ persistence/
   │  │  ├─ config/
   │  │  ├─ device/
   │  │  ├─ health/
   │  │  ├─ product/
   │  │  ├─ store/
   │  │  └─ system/
   │  └─ resources/
   │     ├─ application.yml
   │     └─ db/migration/
   └─ test/
      ├─ java/com/nucosmos/pos/backend/
      └─ resources/application.yml
```

## 4. 分層設計

### 4.1 Controller 層

負責接收 HTTP 請求、回傳 JSON，盡量不放商業邏輯。

目前包含：

- `HealthController`
- `SystemInfoController`
- `ProductController`

### 4.2 Service 層

負責封裝查詢與業務流程，讓 Controller 保持乾淨。目前已建立：

- `ProductQueryService`

### 4.3 Repository 層

負責資料庫存取，目前已建立：

- `ProductRepository`

### 4.4 Entity 層

目前已有以下核心 Entity：

- `BaseEntity`
- `StoreEntity`
- `DeviceEntity`
- `ProductCategoryEntity`
- `ProductEntity`

## 5. 資料庫設計現況

### 5.1 現有資料表

目前 Flyway migration 已建立以下資料表：

- `stores`
- `devices`
- `product_categories`
- `products`

### 5.2 設計目的

- `stores`：門市主檔
- `devices`：POS 裝置資料，綁定到門市
- `product_categories`：商品分類
- `products`：商品主檔

### 5.3 共通欄位策略

目前所有核心資料表都採用：

- `id`：UUID 主鍵
- `created_at`
- `updated_at`

JPA 端透過 `BaseEntity` 處理共通欄位，讓後續新增資料表時保持一致。

## 6. Migration 策略

### 6.1 Migration 檔案

目前已有兩支 migration：

- `V1__create_core_tables.sql`
- `V2__seed_initial_data.sql`

### 6.2 設計原則

- 所有 schema 變更都用 Flyway 管理
- 不依賴 Hibernate 自動建表
- `spring.jpa.hibernate.ddl-auto=validate`

這樣的好處是：

- schema 來源單一且可追蹤
- 團隊成員在不同電腦上能得到一致資料結構
- 便於未來部署到測試與正式環境

## 7. API 現況

### 7.1 系統 API

- `GET /api/v1/health`
- `GET /api/v1/system/info`

### 7.2 商品 API

- `GET /api/v1/products`

這支 API 已經不是回傳寫死資料，而是從 `products` 與 `product_categories` 資料表讀取資料後組裝回傳。

## 8. 設定檔說明

### 8.1 主設定

檔案：

- `backend/src/main/resources/application.yml`

目前包含：

- Spring Boot 應用名稱
- PostgreSQL datasource
- JPA 設定
- Flyway 設定
- Server port
- Actuator health/info
- CORS 設定

### 8.2 測試設定

檔案：

- `backend/src/test/resources/application.yml`

用途：

- 在測試時切換為 H2 記憶體資料庫
- 用 Flyway 建立測試 schema
- 驗證 Controller 與 JPA 整合是否正常

## 9. 本機開發方式

### 9.1 啟動 PostgreSQL

```bash
cd backend
docker compose --env-file .env.example up -d
```

### 9.2 啟動 Spring Boot

```bash
cd backend
mvn spring-boot:run
```

### 9.3 預設環境變數

可參考：

- `backend/.env.example`

主要欄位：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT`
- `FRONTEND_ORIGIN`

## 10. 測試策略

### 10.1 已有測試

- `NucosmosPosBackendApplicationTests`
- `ProductControllerTest`

### 10.2 目前覆蓋範圍

- Spring Context 能正常啟動
- Flyway migration 能在測試環境正確執行
- 商品 API 能從資料庫取回種子資料

## 11. 目前已知限制

- 尚未接入 Spring Security
- 尚未建立 `users`、`roles`、`orders`、`order_items` 等正式業務表
- 尚未導入 DTO 驗證請求的寫入 API
- 尚未處理多門市商品可售範圍與價格覆蓋
- 尚未實作同步與裝置心跳入庫

## 12. 下一步建議施工順序

### Phase A：身份與權限

- 建立 `users`、`roles`、`user_roles`
- 導入 Spring Security
- 建立登入與 JWT 機制

### Phase B：商品主檔完善

- 商品新增、修改、停用 API
- 分類管理 API
- 門市可售設定

### Phase C：訂單核心

- `orders`
- `order_items`
- `payments`
- `refunds`

### Phase D：POS 同步

- 裝置上行同步 API
- 下行商品同步 API
- 同步任務與錯誤紀錄

## 13. 與整體架構的關係

這份後端基礎版的角色是整個 POS 系統的中央協調層，負責：

- 管理主資料
- 接收 POS 交易資料
- 提供後台管理 API
- 承載未來的認證、報表、同步、審計能力

因此在架構上，後端應始終維持「商業規則中心」的角色，而不是只當資料轉發器。
