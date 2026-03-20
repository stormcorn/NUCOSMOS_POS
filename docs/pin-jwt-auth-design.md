# POS PIN + JWT 登入設計

## 1. 設計目標

本文件定義 NUCOSMOS POS 前台的快速登入方案，目標是在安卓平板 POS 上提供：

- 店員以 PIN 快速登入
- 角色導向的工作階段
- 後端可驗證、可審計、可擴充的登入流程
- 與 Vue 後台、Flutter POS 共用同一套 JWT 驗證邏輯

## 2. 為什麼不是只做前端 PIN 畫面

POS 的 PIN 介面只是登入入口，真正的權限判斷不能放在前端。前端選的角色只代表「登入意圖」，最終仍必須由後端驗證：

- 這個 PIN 對應哪個員工
- 這個員工是否屬於該門市
- 這個員工是否具有選取的角色
- 這台裝置是否可用

## 3. 登入流程

```text
POS 平板
  -> 選門市
  -> 選角色
  -> 輸入 PIN
  -> 呼叫 /api/v1/auth/pin-login

後端
  -> 驗證門市
  -> 驗證角色
  -> 若有 deviceCode，驗證裝置
  -> 在該門市 + 該角色可登入的員工中比對 PIN hash
  -> 成功後簽發 JWT
  -> 回傳 staff / store / activeRole / expiresAt / accessToken
```

## 4. JWT 內含資訊

目前 JWT 內含以下資料：

- `sub`：userId
- `employeeCode`
- `displayName`
- `storeCode`
- `roleCodes`
- `activeRole`
- `deviceCode`

## 5. 資料表設計

目前此流程使用以下資料表：

- `roles`
- `users`
- `user_roles`
- `store_staff_assignments`

## 6. API 設計

### 6.1 PIN 登入

`POST /api/v1/auth/pin-login`

Request:

```json
{
  "storeCode": "TW001",
  "roleCode": "CASHIER",
  "pin": "1234",
  "deviceCode": "POS-TABLET-001"
}
```

### 6.2 目前登入者

`GET /api/v1/auth/me`

用途：

- 前端重整後確認目前 token 對應的工作階段
- 顯示目前員工、門市、角色資訊

## 7. PIN 安全策略

- PIN 不明文存資料庫
- 目前使用 `BCrypt` hash
- 後端以 `PasswordEncoder.matches()` 驗證
- 錯誤訊息統一為模糊訊息，避免帳號枚舉

## 8. 角色模型

目前範例角色：

- `CASHIER`
- `MANAGER`
- `ADMIN`

設計原則：

- 使用者可擁有多個角色
- 前端登入時可指定本次要用哪個角色
- 後端會確認該角色是否真的屬於此使用者
- JWT 中同時保留 `roleCodes` 與 `activeRole`

## 9. 裝置驗證

`deviceCode` 在這一版是可選的，但如果前端帶了，後端就會驗證：

- 裝置是否存在
- 是否屬於指定門市
- 是否為 `ACTIVE`

## 10. 目前受保護 API

目前系統已改成：

- `/api/v1/auth/**`：公開
- `/api/v1/health`：公開
- `/api/v1/system/info`：公開
- `/api/v1/products`：需帶 JWT

## 11. 測試資料

目前 migration 內已放入測試用帳號：

- `EMP-CASHIER-001`
  - Role: `CASHIER`
  - PIN: `1234`
- `EMP-MANAGER-001`
  - Role: `MANAGER`
  - PIN: `9999`
- `EMP-SUPERVISOR-001`
  - Role: `CASHIER`, `MANAGER`
  - PIN: `5678`

## 12. 後續建議

- Refresh Token
- Logout / Token revocation
- PIN 失敗次數限制
- Manager Override API
- 離線 PIN 快取策略
