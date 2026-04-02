# 會員與兌獎規則

## 目的

本文件用來確認目前 `nucosmos.io` 官網兌獎頁、收據兌獎流程、會員綁定、點數與優惠券回饋的正式規則。

目前這一版屬於第一階段 MVP，重點是：

- 客人可用收據上的 QR Code 或兌獎碼到官網查詢
- 客人可在官網完成會員綁定與兌換
- 系統會自動累積點數
- 系統會依固定門檻自動發券

## 公開入口

- 客戶公開頁路徑：`/redeem/{token}`
- 不可讓客戶看到 `/erp`
- 收據上應印出：
  - `redeemCode`
  - `redeemUrl`
  - QR Code

正式站預設公開網址基底：

- `https://nucosmos.io`

所以收據上的完整網址格式為：

- `https://nucosmos.io/redeem/{token}`

## 收據兌獎資料

每張訂單都會建立一筆兌獎資料：

- `receipt_redemptions`

主要欄位概念：

- `public_token`
  - 給 QR Code / 公開網址使用
  - 不可猜測
- `claim_code`
  - 給手動輸入查詢使用
- `claimed_at`
  - 是否已完成兌換
- `claimed_member_id`
  - 這張收據最後綁定到哪位會員

## 會員資料

官網兌獎頁目前會要求客人輸入：

- 姓名
- 手機號碼

會員資料存放於：

- `receipt_members`

目前規則：

- 以手機號碼作為會員唯一識別
- 若手機已存在，就更新會員姓名並沿用既有會員資料
- 若手機不存在，就建立新會員

手機號碼正規化規則：

- `09xxxxxxxx` 會轉成 `+8869xxxxxxxx`
- 純數字 `886...` 會補成 `+886...`
- 最終統一儲存為國際格式

## 兌換資格

一張收據可兌換的前提：

- 訂單狀態不是 `VOIDED`
- 付款狀態必須是下列其一：
  - `PAID`
  - `PARTIALLY_REFUNDED`
  - `REFUNDED`

不可兌換情況：

- 未付款
- 已作廢
- 已兌換過

## 兌換規則

目前規則：

- 每張收據只能成功兌換一次
- 成功兌換時，必須同時綁定會員資料
- 一旦 `claimed_at` 已有值，該收據不可再次兌換

## 點數規則

目前 MVP 固定規則如下：

- 每成功兌換 `1` 張收據，會員增加 `1` 點
- 每次成功兌換，同時增加：
  - `point_balance`
  - `total_claims`

目前沒有做：

- 點數扣回
- 點數過期
- 不同活動倍數點

## 優惠券規則

目前 MVP 固定規則如下：

- 當會員點數累積到每 `5` 點時，自動發一張優惠券
- 優惠券面額固定為：
  - `NT$20`

優惠券資料存放於：

- `receipt_coupons`

目前欄位概念：

- `coupon_code`
- `title`
- `discount_amount`
- `status`
- `issued_at`
- `source_redemption_id`

目前第一版發券規則：

- 第 `5`、`10`、`15`... 點時，各發一張
- 每次達門檻只發一張
- 券的來源會綁到觸發該次門檻的那一筆兌換紀錄

## 官網頁面顯示規則

官網兌獎頁目前應顯示：

- 訂單編號
- 門市
- 消費金額
- 品項數量
- 付款狀態
- 兌換狀態
- 兌獎碼
- 兌獎網址
- 會員姓名
- 會員手機
- 目前點數
- 累積兌換次數
- 本次回饋訊息
- 若有自動發券，顯示券碼與券內容

## 現階段限制

目前已完成：

- 收據兌獎入口
- 官網公開查詢
- 會員綁定
- 自動累積點數
- 固定門檻自動發券

目前尚未完成：

- 後台可調整點數門檻
- 後台可設定優惠券面額
- 優惠券核銷流程
- POS 端輸入會員後直接累點
- LINE / 簡訊 / Email 會員整合
- 完整會員中心

## 正式規則來源

目前規則的主要實作來源：

- [ReceiptRedemptionService.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/order/ReceiptRedemptionService.java)
- [PublicRedeemController.java](/c:/NUCOSMOS_POS/backend/src/main/java/com/nucosmos/pos/backend/order/PublicRedeemController.java)
- [index.html](/c:/NUCOSMOS_POS/backend/src/main/resources/static/redeem/index.html)
- [V37__create_receipt_members.sql](/c:/NUCOSMOS_POS/backend/src/main/resources/db/migration/V37__create_receipt_members.sql)
- [V38__add_receipt_member_rewards.sql](/c:/NUCOSMOS_POS/backend/src/main/resources/db/migration/V38__add_receipt_member_rewards.sql)
