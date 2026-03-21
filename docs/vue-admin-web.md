# Vue 管理後台前端文件

## 1. 文件目的

這份文件記錄目前 `NUCOSMOS_POS` 根目錄前端的現況。現在 Web 前端已經從 React 完整切換為 Vue 3，定位是 POS 系統的管理後台入口，而不是平板 POS 前台。

這份文件主要回答幾件事：

- 目前 Vue 後台用了哪些技術
- 現在有哪些頁面與假資料
- 實際檔案結構在哪裡
- 接下來怎麼串接 Spring Boot API

## 2. 目前定位

目前整體產品方向是：

- POS 平板前台：Flutter
- Web 管理後台：Vue 3
- 後端：Java Spring Boot
- 主資料庫：PostgreSQL

目前 repo 根目錄的前端，就是「Vue 管理後台」。

## 3. 技術選型

目前前端技術棧如下：

- Vue 3
- Vite 5
- TypeScript
- Vue Router 4
- Pinia
- Tailwind CSS

對應設定可以在這些檔案找到：

- [package.json](/c:/NUCOSMOS_POS/package.json)
- [vite.config.ts](/c:/NUCOSMOS_POS/vite.config.ts)
- [tsconfig.app.json](/c:/NUCOSMOS_POS/tsconfig.app.json)
- [tailwind.config.js](/c:/NUCOSMOS_POS/tailwind.config.js)

## 4. 目前前端結構

目前 `src/` 主要結構如下：

```text
src/
├─ App.vue
├─ main.ts
├─ env.d.ts
├─ router/
│  └─ index.ts
├─ stores/
│  └─ admin.ts
├─ components/
│  ├─ dashboard/
│  │  └─ StatCard.vue
│  └─ layout/
│     └─ AdminShell.vue
├─ views/
│  ├─ DashboardView.vue
│  ├─ ProductsView.vue
│  ├─ DevicesView.vue
│  └─ OrdersView.vue
└─ styles/
   └─ base.css
```

核心入口如下：

- [src/main.ts](/c:/NUCOSMOS_POS/src/main.ts)
- [src/App.vue](/c:/NUCOSMOS_POS/src/App.vue)
- [src/router/index.ts](/c:/NUCOSMOS_POS/src/router/index.ts)

## 5. 目前已完成頁面

### 5.1 營運總覽

頁面檔案：

- [src/views/DashboardView.vue](/c:/NUCOSMOS_POS/src/views/DashboardView.vue)

目前內容：

- 今日營收、有效訂單、連線裝置、待處理警示
- 品類銷售占比視覺化
- 今日巡檢與營運提醒卡片

### 5.2 商品管理

頁面檔案：

- [src/views/ProductsView.vue](/c:/NUCOSMOS_POS/src/views/ProductsView.vue)

目前內容：

- 商品清單表格
- SKU、分類、售價、狀態、庫存備註
- 新增商品按鈕骨架

### 5.3 裝置管理

頁面檔案：

- [src/views/DevicesView.vue](/c:/NUCOSMOS_POS/src/views/DevicesView.vue)

目前內容：

- POS 裝置狀態卡片
- 裝置 heartbeat 狀態展示
- 現場巡檢建議區塊

### 5.4 訂單追蹤

頁面檔案：

- [src/views/OrdersView.vue](/c:/NUCOSMOS_POS/src/views/OrdersView.vue)

目前內容：

- 近期訂單列表
- 付款與退款概況
- 後續串接 `OrderController` 的提示區塊

## 6. 路由設計

目前路由都定義在 [src/router/index.ts](/c:/NUCOSMOS_POS/src/router/index.ts)：

- `/`：營運總覽
- `/products`：商品管理
- `/devices`：裝置管理
- `/orders`：訂單追蹤

目前還沒有登入頁與權限守衛，這是下一階段要補的內容。

## 7. 狀態管理現況

目前狀態集中在 [src/stores/admin.ts](/c:/NUCOSMOS_POS/src/stores/admin.ts)。

這份 store 目前主要是前端假資料與畫面組裝資料，包含：

- 門市列表
- 目前選擇的門市
- 商品清單
- 裝置清單
- 近期訂單
- Dashboard 統計卡片資料

目前這些都還不是實際 API 資料，而是 mock data。

## 8. 樣式與版型

目前管理後台共用版型在：

- [src/components/layout/AdminShell.vue](/c:/NUCOSMOS_POS/src/components/layout/AdminShell.vue)

共用視覺基礎在：

- [src/styles/base.css](/c:/NUCOSMOS_POS/src/styles/base.css)

目前風格方向是：

- 深色營運控制台
- 青綠與暖橘作為主輔色
- 桌機優先，但保留基本響應式

## 9. 啟動方式

在 repo 根目錄執行：

```bash
npm install
npm run dev
```

預設網址：

- `http://localhost:5173`

建置指令：

```bash
npm run build
```

## 10. 與後端串接建議

目前後端已經有一批可支撐管理後台 MVP 的 API，下一步建議照這個順序串接：

1. JWT 登入
2. 商品管理
3. 裝置管理
4. 訂單查詢
5. 班次與報表

可優先對接的後端文件：

- [PIN + JWT 登入設計](/c:/NUCOSMOS_POS/docs/pin-jwt-auth-design.md)
- [後端基礎建設文件](/c:/NUCOSMOS_POS/docs/backend-foundation.md)

### 10.1 第一批建議建立的前端模組

- `src/api/http.ts`
- `src/api/auth.ts`
- `src/api/products.ts`
- `src/api/devices.ts`
- `src/api/orders.ts`
- `src/types/`
- `src/composables/`

### 10.2 第一批建議補的能力

- 登入頁
- JWT token 儲存策略
- Router auth guard
- API base URL 設定
- Loading / Error / Empty state
- 表單驗證
- 真實分頁與查詢條件

## 11. 目前已知限制

目前 Vue 後台已可啟動與建置，但還有這些限制：

- 尚未串任何真實後端 API
- 尚未實作登入頁
- 尚未做權限控管
- 尚未建立共用 API client
- 尚未做 CRUD 表單流程
- 尚未補測試

## 12. 建議下一步

如果接著要正式做 Vue 管理後台，建議依序施工：

1. 建立登入頁與 JWT 流程
2. 串接商品管理 API
3. 建立裝置管理查詢
4. 建立訂單列表與詳情頁
5. 補報表與班次頁面

## 13. 結論

目前 Vue 管理後台已經有：

- 可啟動的 Vue 3 專案骨架
- 清楚的頁面分區
- 共用版型與視覺方向
- 可作為後端串接起點的 store 與路由

它現在的定位比較像「管理後台第一版前端殼」，接下來的重點會是把 mock data 逐步替換成 Spring Boot API。
