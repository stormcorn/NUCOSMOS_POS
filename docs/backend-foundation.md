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

- Java 17
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
- `AuthController`
- `OrderController`

### 4.2 Service 層

負責封裝查詢與業務流程，讓 Controller 保持乾淨。目前已建立：

- `ProductQueryService`
- `PinAuthService`
- `OrderService`

### 4.3 Repository 層

負責資料庫存取，目前已建立：

- `ProductRepository`
- `UserRepository`
- `StoreRepository`
- `DeviceRepository`
- `OrderRepository`
- `PaymentRepository`
- `RefundRepository`

### 4.4 Entity 層

目前已有以下核心 Entity：

- `BaseEntity`
- `StoreEntity`
- `DeviceEntity`
- `ProductCategoryEntity`
- `ProductEntity`
- `UserEntity`
- `RoleEntity`
- `OrderEntity`
- `OrderItemEntity`
- `PaymentEntity`
- `RefundEntity`

## 5. 資料庫設計現況

### 5.1 現有資料表

目前 Flyway migration 已建立以下資料表：

- `stores`
- `devices`
- `product_categories`
- `products`
- `roles`
- `users`
- `user_roles`
- `store_staff_assignments`
- `orders`
- `order_items`
- `payments`
- `refunds`

### 5.2 設計目的

- `stores`：門市主檔
- `devices`：POS 裝置資料，綁定到門市
- `product_categories`：商品分類
- `products`：商品主檔
- `roles` / `users` / `user_roles`：員工與角色權限
- `store_staff_assignments`：員工與門市綁定
- `orders`：訂單主檔，含付款狀態、已付金額、退款金額
- `order_items`：訂單商品明細與價格快照
- `payments`：付款紀錄
- `refunds`：退款紀錄，綁定原始 payment 與退款方式

### 5.3 共通欄位策略

目前所有核心資料表都採用：

- `id`：UUID 主鍵
- `created_at`
- `updated_at`

JPA 端透過 `BaseEntity` 處理共通欄位，讓後續新增資料表時保持一致。

## 6. Migration 策略

### 6.1 Migration 檔案

目前已有十一支 migration：

- `V1__create_core_tables.sql`
- `V2__seed_initial_data.sql`
- `V3__create_auth_tables.sql`
- `V4__seed_auth_data.sql`
- `V5__create_order_tables.sql`
- `V6__create_payments_table.sql`
- `V7__create_refunds_table.sql`
- `V8__link_refunds_to_payments.sql`
- `V9__add_card_terminal_fields_to_payments.sql`
- `V10__add_card_transaction_lifecycle_to_payments.sql`
- `V11__create_shifts_table.sql`

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
- `POST /api/v1/devices/heartbeat`
- `GET /api/v1/sync/bootstrap`
- `GET /api/v1/sync/catalog`
- `GET /api/v1/reports/sales-summary`
- `GET /api/v1/shifts/current`
- `POST /api/v1/shifts/open`
- `POST /api/v1/shifts/{shiftId}/close`

### 7.2 商品 API

- `GET /api/v1/products`
- `GET /api/v1/admin/product-categories`
- `GET /api/v1/admin/products`
- `POST /api/v1/admin/products`
- `PUT /api/v1/admin/products/{productId}`
- `POST /api/v1/admin/products/{productId}/deactivate`
- `GET /api/v1/admin/stores`
- `GET /api/v1/admin/devices`

這支 API 已經不是回傳寫死資料，而是從 `products` 與 `product_categories` 資料表讀取資料後組裝回傳。

管理端商品 API 目前提供：

- 商品分類列表
- 商品建立
- 商品修改
- 商品停用

管理端目前限制為 `MANAGER` / `ADMIN` 可使用。

管理端門市/裝置 API 目前提供：

- 門市列表
- 裝置列表
- POS 裝置 heartbeat 更新 `lastSeenAt`

同步 API 第一版目前提供：

- `GET /api/v1/sync/bootstrap`
  - POS 開機或登入後取得當前 device 可用狀態、分類與商品主資料
- `GET /api/v1/sync/catalog?since=...`
  - 依時間增量抓取分類與商品更新

報表與交班 API 第一版目前提供：

- `GET /api/v1/reports/sales-summary`
  - 依 `from` / `to` 回傳門市營收摘要
- `GET /api/v1/shifts/current`
  - 查目前裝置的開班狀態
- `POST /api/v1/shifts/open`
  - 以登入中的裝置與店員開班
- `POST /api/v1/shifts/{shiftId}/close`
  - 關班並回填現金/卡片/退款/淨額統計

### 7.3 Auth API

- `POST /api/v1/auth/pin-login`
- `GET /api/v1/auth/me`

### 7.4 訂單與結帳 API

- `GET /api/v1/orders`
- `POST /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `POST /api/v1/orders/{orderId}/payments`
- `POST /api/v1/orders/{orderId}/payments/authorize`
- `POST /api/v1/orders/{orderId}/payments/{paymentId}/capture`
- `POST /api/v1/orders/{orderId}/refunds`
- `POST /api/v1/orders/{orderId}/cancel`

`GET /api/v1/orders` 目前支援：

- `status`
- `paymentStatus`
- `from` / `to`（ISO-8601 日期時間）
- `page`
- `size`
- `sortBy`：`orderedAt`、`closedAt`、`totalAmount`、`paidAmount`
- `sortDirection`：`asc`、`desc`

支付方式目前僅支援：

- `CASH`
- `CARD`

其中：

- `POST /payments` 目前只接受 `CASH`
- `CARD` 必須先走授權，再走 capture
- `CARD` 目前先接 mock terminal，作為日後串接合庫銀行刷卡機的介面預留

Card terminal provider 目前定義為：

- `TCB_MOCK`
- `TCB_BANK`

卡片交易生命週期目前已落在 payment 紀錄上：

- `AUTHORIZED`
- `CAPTURED`
- `VOIDED`
- `REFUNDED`

退款方式目前僅支援：

- `CASH`
- `CARD_REVERSAL`
- `CARD_REFUND`

### 7.5 `curl` 範例

先登入並取 token：

```bash
TOKEN=$(curl -sS -X POST http://localhost:8081/api/v1/auth/pin-login \
  -H "Content-Type: application/json" \
  -d '{
    "storeCode": "TW001",
    "roleCode": "CASHIER",
    "pin": "1234",
    "deviceCode": "POS-TABLET-001"
  }' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
```

建立訂單：

```bash
curl -sS -X POST http://localhost:8081/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "note": "Lunch rush order",
    "items": [
      {
        "productId": "44444444-4444-4444-4444-444444444441",
        "quantity": 2,
        "note": "Less ice"
      },
      {
        "productId": "44444444-4444-4444-4444-444444444443",
        "quantity": 1
      }
    ]
  }'
```

列出目前門市的訂單：

```bash
curl -sS "http://localhost:8081/api/v1/orders?paymentStatus=PAID&page=0&size=20&sortBy=orderedAt&sortDirection=desc" \
  -H "Authorization: Bearer $TOKEN"
```

依日期區間查詢並用金額排序：

```bash
curl -sS "http://localhost:8081/api/v1/orders?from=2026-03-01T00:00:00Z&to=2026-03-31T23:59:59Z&page=0&size=20&sortBy=totalAmount&sortDirection=asc" \
  -H "Authorization: Bearer $TOKEN"
```

現金結帳：

```bash
curl -sS -X POST http://localhost:8081/api/v1/orders/<order-id>/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CASH",
    "amount": 22.25,
    "amountReceived": 22.25,
    "note": "Customer paid by cash"
  }'
```

信用卡授權（合庫 mock terminal）：

```bash
curl -sS -X POST http://localhost:8081/api/v1/orders/<order-id>/payments/authorize \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 22.25,
    "note": "Customer card authorization"
  }'
```

信用卡請款：

```bash
curl -sS -X POST http://localhost:8081/api/v1/orders/<order-id>/payments/<payment-id>/capture \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "note": "Capture authorized card payment"
  }'
```

部分退款：

```bash
curl -sS -X POST http://localhost:8081/api/v1/orders/<order-id>/refunds \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "<payment-id>",
    "refundMethod": "CARD_REFUND",
    "amount": 10.00,
    "reason": "Customer changed mind"
  }'
```

作廢未付款訂單：

```bash
curl -sS -X POST http://localhost:8081/api/v1/orders/<order-id>/cancel \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Cashier input mistake"
  }'
```

若訂單上存在尚未 capture 的信用卡授權，作廢時系統會先對 terminal 執行 void，再把訂單標成 `VOIDED`。

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
./mvnw spring-boot:run
```

如果本機 `5432` 或 `8081` 已被其他服務占用，可改用：

```bash
cd backend
docker compose --env-file .env.example up -d
DB_PORT=5433 SERVER_PORT=8081 ./mvnw spring-boot:run
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
- `AuthControllerTest`
- `OrderControllerTest`

### 10.2 目前覆蓋範圍

- Spring Context 能正常啟動
- Flyway migration 能在測試環境正確執行
- 商品 API 能從資料庫取回種子資料
- 商品管理 API 能建立、修改、停用商品
- PIN + JWT 登入流程能運作
- 建單、付款、退款、作廢流程能運作
- 訂單列表分頁、排序、日期區間查詢能運作
- `CASH/CARD` 支付方式與 card terminal mock 欄位能運作
- 合庫刷卡機契約層（authorize/capture/void/refund）已建立，現階段由 mock provider 實作
- 基礎營收報表與交班開關班流程能運作

## 11. 目前已知限制

- 尚未接上合庫銀行實際刷卡機協定
- 尚未處理多門市商品可售範圍與價格覆蓋
- 尚未實作同步與裝置心跳入庫
- 尚未實作班次明細、交班差異審核與日結彙總

合庫刷卡機接入前的 checklist 與欄位對照草案，整理在 [tcb-card-terminal-integration.md](/Users/stormcorn/NUCOSMOS_POS/docs/tcb-card-terminal-integration.md)。

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

- 退款與作廢的審計欄位完善
- 退款綁定原始 payment 與退款方式
- 發票/結帳單據資訊

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
