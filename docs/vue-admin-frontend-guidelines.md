# Vue 管理後台前端開發規範

## 1. 文件目的

這份文件定義 `NUCOSMOS_POS` Vue 管理後台的前端開發規範，目標是讓之後新增頁面、串 API、調整 UI 時能維持一致結構與可維護性。

## 2. 技術基準

目前前端基準如下：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Tailwind CSS

不建議再混入 React 或額外平行框架。

## 3. 目錄規範

建議使用以下結構：

```text
src/
├─ api/
├─ components/
├─ composables/
├─ layouts/
├─ router/
├─ stores/
├─ styles/
├─ types/
├─ utils/
└─ views/
```

目前已存在：

- `components/`
- `router/`
- `stores/`
- `styles/`
- `views/`

後續新模組應優先補齊 `api/`、`types/`、`composables/`。

## 4. 命名規範

### 4.1 檔名

- Vue component：`PascalCase.vue`
- view page：`PascalCaseView.vue`
- store：`camelCase.ts` 或 `kebab-case.ts`
- api module：`resourceName.ts`

### 4.2 型別

- Type / Interface 使用 `PascalCase`
- API DTO 建議以用途命名

例如：

- `LoginRequest`
- `LoginResponse`
- `ProductAdminItem`
- `PagedResponse<T>`

## 5. Component 原則

### 5.1 單一職責

每個 component 應儘量只解決一件事，例如：

- 版型容器
- KPI 卡片
- 商品表格
- 裝置狀態標籤

避免一個 component 同時包含：

- 資料請求
- 表格顯示
- 表單流程
- modal 行為

### 5.2 View 與 Component 分工

- `views/`：頁面容器、路由層級、資料整合
- `components/`：可重用 UI 區塊

## 6. Store 原則

Pinia store 建議只承擔：

- 工作階段狀態
- 目前門市上下文
- 可重用的列表狀態
- API 結果快取

避免 store 變成：

- 所有畫面的超大雜湊
- 純展示用局部 UI 狀態集中區

建議拆分：

- `useAuthStore`
- `useStoreContextStore`
- `useProductStore`
- `useDeviceStore`
- `useOrderStore`

## 7. API 封裝規範

### 7.1 不要在 View 直接寫 fetch 細節

建議：

- `api/*.ts` 專門負責 HTTP 請求
- `views/` 只負責調用與畫面流程

### 7.2 共用 HTTP Client

建議建立：

- `src/api/http.ts`

內容至少包含：

- `baseURL`
- JSON parse
- Authorization header 注入
- 401 / 403 基本處理
- 共用錯誤格式轉換

### 7.3 型別優先

每支 API 都應有 request / response 型別，例如：

```ts
export type PinLoginRequest = {
  storeCode: string;
  roleCode: string;
  pin: string;
  deviceCode?: string;
};
```

## 8. Router 規範

### 8.1 路由 meta

建議每條路由至少定義：

- `title`
- `requiresAuth`
- `roles`

例如：

```ts
meta: {
  title: "商品管理",
  requiresAuth: true,
  roles: ["MANAGER", "ADMIN"]
}
```

### 8.2 Guard

建議補上全域 router guard：

- 沒登入導去 `/login`
- token 失效清 session
- 無權限顯示 403 或導回首頁

## 9. UI 與樣式規範

### 9.1 共用設計語言

目前視覺方向已在：

- [src/styles/base.css](/c:/NUCOSMOS_POS/src/styles/base.css)
- [tailwind.config.js](/c:/NUCOSMOS_POS/tailwind.config.js)

規則：

- 盡量沿用現有色票
- 保持深色營運控制台方向
- 不要引入與現有語言衝突的整套 UI library

### 9.2 響應式原則

管理後台以桌機優先，但至少要保證：

- 1366px 桌機可完整使用
- 1024px 平板橫向可瀏覽
- 手機可基本檢視資訊

### 9.3 狀態呈現

每個資料型頁面都應有：

- loading
- empty
- error
- success

## 10. 表單規範

### 10.1 驗證位置

- 前端做基本驗證
- 後端做最終驗證

### 10.2 錯誤顯示

優先顯示：

- 欄位級錯誤
- 表單級錯誤
- API message

後端若回傳 `details` 陣列，前端應整理成可讀形式。

## 11. 狀態與資料同步

### 11.1 Query 與畫面同步

列表頁建議把以下狀態同步到 URL：

- page
- size
- keyword
- filter
- sort

### 11.2 避免重複請求

建議：

- 頁面初始化集中請求
- 篩選變更 debounce
- 建立 / 編輯成功後局部刷新

## 12. 錯誤處理規範

### 12.1 HTTP 狀態處理

- `400`：顯示驗證或業務錯誤
- `401`：登出並導回登入
- `403`：顯示無權限
- `404`：顯示資料不存在
- `500`：顯示通用錯誤與重試

### 12.2 使用者訊息

訊息要偏操作導向，例如：

- 「商品建立失敗，請確認售價是否大於 0」
- 「登入已失效，請重新登入」

避免直接把原始例外文字丟給使用者。

## 13. 型別與資料轉換

建議前端盡量保持與後端欄位一致，不要在一開始就做大量 rename。

必要轉換才做，例如：

- 日期字串轉 `Date`
- `BigDecimal` 類型金額轉顯示字串
- enum code 轉中文文案

## 14. 測試建議

目前前端還沒有測試基礎，但後續建議至少補：

- store 單元測試
- API module mock 測試
- 核心頁面 smoke test

如果短期先不補完整測試，也至少要保留：

- build 可通過
- 手動驗證清單

## 15. Git 與提交建議

建議提交時按功能切分：

- `feat(admin-auth): add login page and auth store`
- `feat(admin-products): connect product list api`
- `refactor(admin-layout): split shell and top bar`

避免把：

- 畫面
- API
- 文件
- 大量格式化

全部混成一個難以追蹤的 commit。

## 16. 開發優先順序

建議照這個順序開發：

1. 登入與 session 管理
2. 共用 HTTP client
3. 商品列表與商品表單
4. 裝置列表
5. 訂單查詢與詳情
6. 報表摘要

## 17. 結論

這份規範的核心目的，是讓 Vue 管理後台後續每次擴充都能維持：

- 清楚的模組邊界
- 穩定的 API 對接方式
- 一致的畫面與錯誤處理
- 可持續維護的專案結構
