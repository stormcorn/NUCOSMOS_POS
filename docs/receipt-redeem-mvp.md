# 收據兌獎 MVP

## 目標

讓每張成功結帳的收據都能成為 `nucosmos.io` 的公開抽獎入口，並與會員點數、抵用券、後台獎項管理連動。

## 公開網址

- 預設公開網址：`https://nucosmos.io/redeem/{token}`
- 不能讓客戶看到 `/erp`

## 核心流程

1. POS 結帳完成後建立收據兌獎資料。
2. 收據列印兌獎碼、兌獎網址與 QR Code。
3. 客戶掃描 QR Code 進入公開兌獎頁。
4. 客戶輸入姓名與手機號碼後完成兌換。
5. 系統即時顯示：
   - 中獎或未中獎
   - 中獎獎項
   - 未中獎點數回饋
   - 是否發出 50 元抵用券

## 現行規則

- 每張收據只能兌換一次
- 中獎：
  - 顯示恭喜訊息
  - 顯示獎品
  - 扣減獎品剩餘數量
- 未中獎：
  - 顯示 `銘謝惠顧，再接再厲`
  - 獲得 `1` 點
- 每累積 `5` 點：
  - 自動發 `50 元抵用券`

## 公開頁固定顯示內容

- 抽獎結果
- 會員姓名與手機
- 目前點數與累積兌換次數
- 本次是否發券
- 各獎項機率與剩餘數量
- 活動提示：
  - `在店內五星好評、分享到任意社群、或分享 LINE 好友後出示給老闆看，可直接獲得 50 元抵用券。`

## 後台管理

後台「系統設定」新增抽獎設定頁，可管理：

- 獎項名稱
- 獎項說明
- 中獎機率
- 剩餘數量
- 啟用狀態
- 顯示順序

## 資料表

- `receipt_redemptions`
  - `public_token`
  - `claim_code`
  - `claimed_at`
  - `claimed_member_id`
  - `draw_outcome`
  - `awarded_points`
  - `prize_id`
- `receipt_members`
  - `display_name`
  - `phone_number`
  - `point_balance`
  - `total_claims`
- `receipt_coupons`
  - `coupon_code`
  - `title`
  - `discount_amount`
  - `status`
  - `issued_at`
  - `source_redemption_id`
- `receipt_prizes`
  - `name`
  - `description`
  - `probability_percent`
  - `remaining_quantity`
  - `active`
  - `display_order`

## 實作檔案

- [ReceiptRedemptionService.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/order/ReceiptRedemptionService.java)
- [PublicRedeemController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/order/PublicRedeemController.java)
- [ReceiptPrizeAdminController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/order/ReceiptPrizeAdminController.java)
- [index.html](/c:/NUCOSMOS_POS/backend/src/main/resources/static/redeem/index.html)
- [RedeemPrizeSettingsView.vue](/c:/NUCOSMOS_POS/src/views/RedeemPrizeSettingsView.vue)
- [V39__add_receipt_draw_rules.sql](/c:/NUCOSMOS_POS/backend/src/main/resources/db/migration/V39__add_receipt_draw_rules.sql)
- [member-redeem-rules.md](/c:/NUCOSMOS_POS/docs/member-redeem-rules.md)
