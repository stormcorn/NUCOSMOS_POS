# 收據兌獎 MVP

## 目標

讓客戶在 `nucosmos.io` 上透過收據 QR Code 參加抽獎，並建立會員、點數、抵用券的最小可用流程。

## 公開流程

1. POS 結帳完成後，收據印出：
   - 兌獎碼
   - 兌獎網址
   - QR Code
2. 客戶掃描收據 QR Code，開啟 `https://nucosmos.io/redeem/{token}`
3. 客戶先完成手機簡訊登入
4. 登入成功後，點擊參加抽獎
5. 系統顯示中獎與否與本次回饋

## 已完成範圍

- 收據自動建立 `redeemCode`、`redeemUrl`
- 公開兌獎頁 `https://nucosmos.io/redeem/{token}`
- 公開查詢 API：
  - `GET /api/v1/public/redeem/{token}`
  - `GET /api/v1/public/redeem/search?code=...`
- 公開會員 API：
  - `GET /api/v1/public/member/firebase-config`
  - `POST /api/v1/public/member/login/sms`
  - `GET /api/v1/public/member/session`
  - `POST /api/v1/public/member/logout`
- 公開兌獎 API：
  - `POST /api/v1/public/redeem/{token}/claim`
- 手機簡訊登入沿用 Firebase Phone Auth
- 公開會員 session 透過 HttpOnly cookie 維持，預設有效期 90 天
- 抽獎結果與會員點數回饋顯示在同一頁
- 管理後台可設定獎項、機率、剩餘數量

## 抽獎與點數規則

- 中獎：
  - 顯示中獎獎品
  - 不額外給點
- 未中獎：
  - 顯示「銘謝惠顧，再接再厲」
  - 獲得 1 點
- 每滿 5 點：
  - 自動發一張 50 元抵用券

## 管理後台需求

- 後台可管理：
  - 獎項名稱
  - 獎項說明
  - 中獎機率
  - 剩餘數量
  - 啟用狀態
  - 顯示排序

## 後續可擴充

- 會員中心
- 優惠券查詢與核銷
- 點數規則可後台設定
- 分享任務的實際驗證流程
- 會員登入後自動帶出歷史兌獎紀錄
