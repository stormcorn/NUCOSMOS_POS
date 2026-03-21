# Vue 管理後台 API 對接規格

## 1. 文件目的

這份文件定義 Vue 管理後台與 Spring Boot 後端之間的 API 對接方式，目標是讓前端可以在不反覆猜測欄位與流程的情況下直接施工。

目前以管理後台第一階段最需要的功能為主：

- JWT 登入與工作階段
- 商品管理
- 門市與裝置管理
- 訂單查詢
- 班次與報表

## 2. 基本原則

### 2.1 Base URL

本地開發預設：

- 前端：`http://localhost:5173`
- 後端：`http://localhost:8081`

建議前端 `.env` 先使用：

```env
VITE_API_BASE_URL=http://localhost:8081
```

### 2.2 Authorization

除了公開端點以外，其餘 API 都應帶上：

```http
Authorization: Bearer <accessToken>
```

### 2.3 成功回應格式

後端目前統一成功回應格式為：

```json
{
  "success": true,
  "data": {}
}
```

對應後端實作：

- [ApiResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/common/api/ApiResponse.java)

### 2.4 錯誤回應格式

後端目前統一錯誤回應格式為：

```json
{
  "success": false,
  "message": "Validation failed",
  "details": [
    "sku: must not be blank"
  ],
  "timestamp": "2026-03-21T08:30:00Z"
}
```

對應後端實作：

- [ErrorResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/common/api/ErrorResponse.java)
- [GlobalExceptionHandler.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/common/exception/GlobalExceptionHandler.java)

## 3. 前端建議 API 模組

建議先建立以下模組：

- `src/api/http.ts`
- `src/api/auth.ts`
- `src/api/products.ts`
- `src/api/devices.ts`
- `src/api/orders.ts`
- `src/api/reports.ts`
- `src/api/shifts.ts`

建議共用型別：

- `src/types/api.ts`
- `src/types/auth.ts`
- `src/types/product.ts`
- `src/types/device.ts`
- `src/types/order.ts`
- `src/types/report.ts`

## 4. Auth API

對應後端：

- [AuthController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/auth/AuthController.java)

### 4.1 PIN 登入

`POST /api/v1/auth/pin-login`

用途：

- 管理員或店員用 PIN 取得工作階段 JWT

Request:

```json
{
  "storeCode": "TW001",
  "roleCode": "MANAGER",
  "pin": "9999",
  "deviceCode": "POS-TABLET-001"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "<jwt>",
    "expiresAt": "2026-03-21T20:00:00+08:00",
    "deviceCode": "POS-TABLET-001",
    "store": {
      "code": "TW001",
      "name": "Taipei Main Store"
    },
    "staff": {
      "employeeCode": "EMP-MANAGER-001",
      "displayName": "Store Manager",
      "activeRole": "MANAGER",
      "roleCodes": ["MANAGER"]
    }
  }
}
```

前端建議處理：

- 成功後儲存 `accessToken`
- 同時保留 `expiresAt`
- 導頁到 `/`
- 若失敗則顯示後端 `message`

### 4.2 取得目前工作階段

`GET /api/v1/auth/me`

用途：

- 前端重整後恢復登入狀態
- 顯示目前登入者、門市、角色

Response:

```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "employeeCode": "EMP-MANAGER-001",
    "displayName": "Store Manager",
    "storeCode": "TW001",
    "activeRole": "MANAGER",
    "roleCodes": ["MANAGER"],
    "deviceCode": "POS-TABLET-001"
  }
}
```

## 5. 商品管理 API

對應後端：

- [ProductAdminController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/product/ProductAdminController.java)
- [ProductUpsertRequest.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/product/ProductUpsertRequest.java)
- [ProductAdminResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/product/ProductAdminResponse.java)

### 5.1 取得商品分類

`GET /api/v1/admin/product-categories`

用途：

- 商品新增 / 編輯表單的分類下拉選單

前端建議：

- 進入商品頁時先 preload
- 可快取於 store

### 5.2 取得商品列表

`GET /api/v1/admin/products?active=true`

Query:

- `active`：可選，`true` / `false`

用途：

- 商品列表頁
- 上架中 / 停用商品切換

Response data 單筆欄位：

- `id`
- `categoryId`
- `categoryCode`
- `categoryName`
- `sku`
- `name`
- `description`
- `imageUrl`
- `price`
- `active`

### 5.3 建立商品

`POST /api/v1/admin/products`

Request:

```json
{
  "categoryId": "uuid",
  "sku": "DRK-005",
  "name": "House Milk Tea",
  "description": "Classic milk tea",
  "imageUrl": "https://example.com/house-milk-tea.jpg",
  "price": 6.5
}
```

前端驗證建議：

- `categoryId` 必填
- `sku` 必填，最多 50 字
- `name` 必填，最多 120 字
- `description` 最多 500 字
- `imageUrl` 選填，最多 500 字
- `price` 必填且大於 0

### 5.4 修改商品

`PUT /api/v1/admin/products/{productId}`

用途：

- 商品編輯

Request 格式與建立商品相同。

### 5.5 停用商品

`POST /api/v1/admin/products/{productId}/deactivate`

用途：

- 從管理後台做商品下架

前端建議：

- 先跳確認視窗
- 成功後 refresh 當前列表

## 6. 門市與裝置 API

對應後端：

- [StoreDeviceAdminController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/device/StoreDeviceAdminController.java)
- [StoreSummaryResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/store/StoreSummaryResponse.java)
- [DeviceAdminResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/device/DeviceAdminResponse.java)

### 6.1 取得門市列表

`GET /api/v1/admin/stores`

用途：

- 裝置頁、報表頁、全站門市切換器

欄位：

- `id`
- `code`
- `name`
- `timezone`
- `currencyCode`
- `status`

### 6.2 取得裝置列表

`GET /api/v1/admin/devices`

Query:

- `storeCode`：可選
- `status`：可選

用途：

- 裝置管理列表
- 依門市篩選
- 依在線狀態篩選

欄位：

- `id`
- `storeId`
- `storeCode`
- `deviceCode`
- `name`
- `platform`
- `status`
- `lastSeenAt`

## 7. 訂單 API

對應後端：

- [OrderController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/order/OrderController.java)
- [PagedResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/common/api/PagedResponse.java)

### 7.1 取得訂單列表

`GET /api/v1/orders`

Query:

- `status`
- `paymentStatus`
- `from`
- `to`
- `page`
- `size`
- `sortBy`
- `sortDirection`

預設：

- `page=0`
- `size=20`
- `sortBy=orderedAt`
- `sortDirection=desc`

Response:

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 120,
    "totalPages": 6,
    "hasNext": true
  }
}
```

前端建議：

- 頁碼 UI 顯示時轉成 1-based
- Query string 與列表狀態同步
- 日期查詢一律使用 ISO-8601

### 7.2 取得訂單詳情

`GET /api/v1/orders/{orderId}`

用途：

- 訂單詳情抽屜
- 訂單詳情頁

### 7.3 其他訂單操作

目前後端也已提供：

- `POST /api/v1/orders`
- `POST /api/v1/orders/{orderId}/payments`
- `POST /api/v1/orders/{orderId}/payments/authorize`
- `POST /api/v1/orders/{orderId}/payments/{paymentId}/capture`
- `POST /api/v1/orders/{orderId}/refunds`
- `POST /api/v1/orders/{orderId}/cancel`

管理後台第一階段建議先做查詢與詳情，不急著把建立訂單流程放進 Web 後台。

## 8. 報表 API

對應後端：

- [ReportController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/report/ReportController.java)
- [SalesSummaryResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/report/SalesSummaryResponse.java)

### 8.1 銷售摘要

`GET /api/v1/reports/sales-summary?from=...&to=...`

權限：

- `MANAGER`
- `ADMIN`

欄位：

- `storeCode`
- `from`
- `to`
- `orderCount`
- `voidedOrderCount`
- `grossSalesAmount`
- `refundedAmount`
- `netSalesAmount`
- `cashSalesAmount`
- `cardSalesAmount`
- `averageOrderAmount`

前端建議：

- 進頁預設今天起訖
- 支援快捷條件：今天、昨天、本週、近 7 天

## 9. 班次 API

對應後端：

- [ShiftController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/shift/ShiftController.java)
- [ShiftResponse.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/shift/ShiftResponse.java)

### 9.1 目前班次

`GET /api/v1/shifts/current`

用途：

- 顯示目前開班狀態
- Dashboard 顯示值班資訊

### 9.2 開班

`POST /api/v1/shifts/open`

用途：

- 建立班次

### 9.3 關班

`POST /api/v1/shifts/{shiftId}/close`

用途：

- 關班結算

前端建議：

- 關班表單要標示現金、卡片、退款、淨額摘要
- 關班完成後刷新 dashboard 與報表

## 10. 前端共用對接策略

### 10.1 Token 儲存

管理後台建議：

- 優先存在 memory + `localStorage`
- 初始化時呼叫 `/api/v1/auth/me`
- token 無效時清空並導回登入頁

### 10.2 401 / 403 行為

- `401`：清 token，導回登入頁
- `403`：留在當頁，顯示無權限提示

### 10.3 Loading / Empty / Error

所有列表型頁面至少提供：

- 初次 loading skeleton
- 空資料提示
- 可重試錯誤區塊

## 11. 建議實作順序

1. `auth.ts` + 登入頁
2. `http.ts` + token interceptor
3. `products.ts` + 商品列表 / 新增 / 編輯
4. `devices.ts` + 門市 / 裝置列表
5. `orders.ts` + 訂單查詢
6. `reports.ts` + 銷售摘要

## 12. 結論

目前後端已經足夠支撐 Vue 管理後台第一階段施工。前端應先把登入、商品、裝置、訂單查詢這四塊打通，再逐步把 Dashboard 的 mock data 換成真實 API。
