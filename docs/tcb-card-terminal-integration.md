# 合庫刷卡機接入草案

## 1. 文件目的

這份文件用來整理 NUCOSMOS POS 後端接入合庫銀行信用卡刷卡機前，已完成的預留接口、尚待確認的銀行協定欄位，以及設備到位後的實作 checklist。

目前結論是：

- 後端支付主流程已可支援 `CASH` 與 `CARD`
- `CARD` 已拆成 `authorize -> capture -> void -> refund`
- 目前 `TCB_MOCK` 可用於開發與測試
- `TCB_BANK` 仍待對接實機協定與欄位

## 2. 目前後端已具備的預留接口

### 2.1 支付方式

- `CASH`
- `CARD`

### 2.2 卡片交易生命周期

- `AUTHORIZED`
- `CAPTURED`
- `VOIDED`
- `REFUNDED`
- `FAILED`

### 2.3 API 流程

- `POST /api/v1/orders/{orderId}/payments`
  - 目前只接受 `CASH`
- `POST /api/v1/orders/{orderId}/payments/authorize`
  - 建立卡片授權
- `POST /api/v1/orders/{orderId}/payments/{paymentId}/capture`
  - 對既有授權做請款
- `POST /api/v1/orders/{orderId}/refunds`
  - 針對原始 payment 做退款
- `POST /api/v1/orders/{orderId}/cancel`
  - 若存在未 capture 的卡片授權，會先執行 void

### 2.4 後端抽象層

目前已存在的 provider 與 service 契約：

- `CardTerminalProvider`
  - `TCB_MOCK`
  - `TCB_BANK`
- `CardTerminalService`
  - `authorize(...)`
  - `capture(...)`
  - `voidTransaction(...)`
  - `refund(...)`

## 3. Payment 紀錄已預留欄位

`payments` 已能記錄以下刷卡交易資訊：

- `payment_method`
- `status`
- `card_terminal_provider`
- `card_terminal_txn_id`
- `card_approval_code`
- `card_masked_pan`
- `card_batch_no`
- `card_rrn`
- `card_entry_mode`
- `card_transaction_status`
- `authorized_at`
- `captured_at`
- `voided_at`
- `refunded_at`

這些欄位足夠支撐第一版合庫接入，但銀行文件到位後仍要確認：

- `card_terminal_txn_id` 是否對應合庫的交易序號或端末機流水號
- `card_rrn` 是否就是銀行對帳用參考號
- `card_batch_no` 是否足以支援日結或批次結帳
- 是否還需要補 `merchant_id`、`terminal_id`、`invoice_no`、`retrieval_reference`

## 4. 建議的銀行協定欄位對照

在合庫正式文件到位前，建議先用以下 mapping 作為後端內部標準。

| 後端欄位 | 預期合庫欄位 | 說明 |
| --- | --- | --- |
| `card_terminal_provider` | provider code | 固定為 `TCB_BANK` |
| `card_terminal_txn_id` | transaction id | 每筆刷卡交易唯一識別 |
| `card_approval_code` | approval code | 授權碼 |
| `card_masked_pan` | masked pan | 遮罩卡號，例如 `**** **** **** 1234` |
| `card_batch_no` | batch no | 批次號 |
| `card_rrn` | rrn / reference no | 銀行對帳參考號 |
| `card_entry_mode` | entry mode | 晶片、感應、磁條等進卡方式 |
| `authorized_at` | auth time | 授權成功時間 |
| `captured_at` | capture time | 請款成功時間 |
| `voided_at` | void time | 取消授權或沖正時間 |
| `refunded_at` | refund time | 退款成功時間 |

## 5. 設備到位後的實作 checklist

### 5.1 先確認銀行與設備資訊

- 刷卡機型號
- 通訊方式
  - 本機 SDK
  - HTTP
  - TCP Socket
  - Serial / USB
- 作業系統限制
  - 只支援 Windows
  - 支援 Android
  - 需透過中介程式
- 測試環境與正式環境的連線差異
- 合庫提供的交易文件版本

### 5.2 先確認交易能力

- 是否支援單純授權 `authorize`
- 是否支援分離式請款 `capture`
- 是否支援取消授權 `void`
- 是否支援原卡退款 `refund`
- 是否支援部分退款
- 是否支援當日撤銷與跨日退款的不同流程

### 5.3 先確認錯誤與重試規則

- 使用者取消刷卡時的錯誤碼
- 端末機逾時時的錯誤碼
- 銀行主機未回應時的錯誤碼
- 重複請款是否會被拒絕或需做冪等防護
- 失敗後是否允許查詢原交易狀態

### 5.4 先確認對帳需求

- 財務對帳使用哪個欄位作為主鍵
- 是否需要保存完整簽單資訊
- 是否需要保存批次結帳結果
- 是否需要保存門市 merchant id / terminal id
- 是否需要下載 terminal 交易明細

## 6. 建議的實作順序

### Step 1. 補 provider 專屬 request / response model

建議新增：

- `TcbAuthorizeRequest`
- `TcbAuthorizeResponse`
- `TcbCaptureRequest`
- `TcbCaptureResponse`
- `TcbVoidRequest`
- `TcbVoidResponse`
- `TcbRefundRequest`
- `TcbRefundResponse`

### Step 2. 實作 `TCB_BANK` adapter

建議新增：

- `TcbBankCardTerminalService`

責任只處理：

- 呼叫合庫設備或中介程式
- 解析合庫回應
- 轉成內部 `CardTransactionResult`

不要讓 `OrderService` 直接理解銀行回應格式。

### Step 3. 補失敗碼與例外模型

建議新增一層 terminal error mapping，至少先區分：

- `USER_CANCELLED`
- `TERMINAL_TIMEOUT`
- `HOST_UNREACHABLE`
- `DECLINED`
- `DUPLICATE_TRANSACTION`
- `UNKNOWN_FAILURE`

### Step 4. 補裝置設定

若每間門市或每台 POS 綁不同刷卡機，後端之後應補：

- `merchant_id`
- `terminal_id`
- `store_id` 對應關係
- `device_id` 對應關係

### Step 5. 補交易查詢與對帳 API

至少要有：

- 依 `paymentId` 查 terminal 資訊
- 依 `card_terminal_txn_id` 查原交易
- 匯出對帳資料

## 7. 目前可視為已完成的部分

設備尚未到位前，以下項目可視為已經先準備完成：

- 訂單結帳主流程已支援卡片授權與請款分離
- 退款已綁定原始 payment
- 作廢未付款訂單時會自動 void 尚未 capture 的授權
- payment schema 已有第一版卡片交易欄位
- mock provider 可支援本地開發與 API 測試

## 8. 尚未完成但不必現在先做的部分

在拿到合庫正式文件前，先不要過度設計：

- 多支付品牌抽象層
- 其他第三方支付 provider
- 過早導入複雜的支付路由機制
- 提前做前端實機互動 UI 細節

先把合庫單一 provider 接好，比較務實。
