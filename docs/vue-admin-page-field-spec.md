# Vue 管理後台頁面清單與欄位規格

## 1. 文件目的

這份文件定義 Vue 管理後台第一階段頁面清單、區塊內容、主要欄位與互動需求，讓前端、後端與後續設計調整有一致基準。

## 2. 範圍

本文件以管理後台 MVP 為主，涵蓋：

- 登入頁
- 營運總覽
- 商品管理
- 裝置管理
- 訂單追蹤
- 報表摘要
- 班次資訊

## 3. 全域框架

共用版型檔案：

- [src/components/layout/AdminShell.vue](/c:/NUCOSMOS_POS/src/components/layout/AdminShell.vue)

全域共通元素：

- 左側導覽
- 頂部頁面標題
- 門市切換器
- 工作階段資訊
- 錯誤與權限提示區

建議全站共用欄位：

- 當前門市
- 當前角色
- 最後資料更新時間

## 4. 登入頁

建議路由：

- `/login`

### 4.1 畫面區塊

- 品牌標題
- 門市選擇
- 角色選擇
- PIN 輸入
- 登入按鈕
- 錯誤提示

### 4.2 欄位規格

- `storeCode`
  - 類型：select
  - 必填
  - 資料來源：門市列表
- `roleCode`
  - 類型：radio / segmented control
  - 必填
  - 建議值：`CASHIER`、`MANAGER`、`ADMIN`
- `pin`
  - 類型：password / pin input
  - 必填
  - 建議 4~6 碼
- `deviceCode`
  - 類型：hidden 或設定值
  - Web 後台可先不帶

### 4.3 互動規則

- 送出後進 loading
- 成功後導向首頁
- 失敗時顯示後端訊息

## 5. 營運總覽頁

目前檔案：

- [src/views/DashboardView.vue](/c:/NUCOSMOS_POS/src/views/DashboardView.vue)

建議路由：

- `/`

### 5.1 區塊清單

- KPI 卡片
- 品類占比
- 當日警示
- 當前班次摘要
- 快捷入口

### 5.2 KPI 卡片欄位

- 今日營收
- 有效訂單數
- 連線裝置數
- 待處理警示數

### 5.3 後續資料來源建議

- 今日營收：報表 API
- 有效訂單：訂單查詢 API
- 連線裝置：裝置 API
- 班次資訊：班次 API

## 6. 商品管理頁

目前檔案：

- [src/views/ProductsView.vue](/c:/NUCOSMOS_POS/src/views/ProductsView.vue)

建議路由：

- `/products`

### 6.1 列表區欄位

- `sku`
- `name`
- `categoryName`
- `price`
- `active`
- `imageUrl`

### 6.2 篩選條件

- 關鍵字
- 分類
- 狀態

### 6.3 表單欄位

- `categoryId`
  - 必填
- `sku`
  - 必填
  - 最多 50 字
- `name`
  - 必填
  - 最多 120 字
- `description`
  - 選填
  - 最多 500 字
- `imageUrl`
  - 選填
  - 建議填完整圖片網址
- `price`
  - 必填
  - 大於 0

### 6.4 操作按鈕

- 新增商品
- 編輯商品
- 停用商品
- 篩選重置

### 6.5 Empty State 建議

- 沒有商品時顯示 CTA
- 篩選後沒結果時顯示「調整查詢條件」

## 7. 裝置管理頁

目前檔案：

- [src/views/DevicesView.vue](/c:/NUCOSMOS_POS/src/views/DevicesView.vue)

建議路由：

- `/devices`

### 7.1 列表區欄位

- `deviceCode`
- `name`
- `storeCode`
- `platform`
- `status`
- `lastSeenAt`

### 7.2 篩選條件

- 門市
- 裝置狀態

### 7.3 狀態顯示建議

- `ACTIVE` / `online`：綠色
- `idle`：橘色
- `offline` / `inactive`：紅色

### 7.4 互動需求

- 支援手動刷新
- 顯示最後回報時間
- 顯示離線裝置提示

## 8. 訂單追蹤頁

目前檔案：

- [src/views/OrdersView.vue](/c:/NUCOSMOS_POS/src/views/OrdersView.vue)

建議路由：

- `/orders`

### 8.1 列表欄位

- `orderNumber` 或 `id`
- `orderedAt`
- `channel`
- `status`
- `paymentStatus`
- `totalAmount`
- `refundAmount`

### 8.2 篩選條件

- 訂單狀態
- 付款狀態
- 起訖時間
- 關鍵字

### 8.3 詳情抽屜欄位

- 訂單主檔
- 訂單品項明細
- 付款紀錄
- 退款紀錄
- 操作歷程

### 8.4 分頁規格

- 預設每頁 20 筆
- 前端顯示 1-based 頁碼
- 查詢條件應同步在 URL query

## 9. 報表摘要頁

建議路由：

- `/reports`

### 9.1 篩選條件

- 起始時間
- 結束時間
- 門市
- 快捷時間區間

### 9.2 顯示欄位

- `grossSalesAmount`
- `refundedAmount`
- `netSalesAmount`
- `cashSalesAmount`
- `cardSalesAmount`
- `orderCount`
- `voidedOrderCount`
- `averageOrderAmount`

### 9.3 視覺建議

- KPI 卡片
- 金額摘要
- 交易筆數摘要

## 10. 班次資訊頁

建議路由：

- `/shifts`

### 10.1 顯示欄位

- `status`
- `storeCode`
- `deviceCode`
- `openedByEmployeeCode`
- `closedByEmployeeCode`
- `openingCashAmount`
- `closingCashAmount`
- `expectedCashAmount`
- `cashSalesAmount`
- `cardSalesAmount`
- `refundedAmount`
- `netSalesAmount`
- `orderCount`
- `voidedOrderCount`
- `openedAt`
- `closedAt`

### 10.2 互動需求

- 顯示目前是否已開班
- 開班表單
- 關班表單
- 關班前結算摘要

## 11. 共用狀態設計

建議共用 store 切分如下：

- `authStore`
- `storeContextStore`
- `productStore`
- `deviceStore`
- `orderStore`
- `reportStore`

目前的 [src/stores/admin.ts](/c:/NUCOSMOS_POS/src/stores/admin.ts) 比較像暫時性的 mock store，之後建議逐步拆分。

## 12. 欄位命名策略

建議前端型別名稱盡量跟後端 response 對齊，例如：

- `storeCode`
- `deviceCode`
- `employeeCode`
- `categoryId`
- `paymentStatus`

這樣可以減少額外 mapping 成本。

## 13. 驗收重點

第一階段頁面驗收建議：

1. 能登入與還原工作階段
2. 能查詢商品列表
3. 能新增與修改商品
4. 能查看裝置清單
5. 能查詢訂單列表
6. 能查看銷售摘要

## 14. 結論

這份頁面規格的目的不是把畫面一次定死，而是讓 Vue 管理後台的頁面範圍、欄位集合與互動優先順序先穩定下來，讓開發能持續往前推。
