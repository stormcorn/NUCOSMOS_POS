export type ReceiptFooterTemplate = {
  id: string;
  name: string;
  description: string;
  text: string;
};

export const receiptFooterTemplates: ReceiptFooterTemplate[] = [
  {
    id: "basic-thanks",
    name: "感謝光臨",
    description: "適合一般門市收據，保留基本致謝與營業提醒。",
    text: ["感謝您的光臨", "門市營業時間 10:00 - 22:00", "如需協助請洽現場人員"].join("\n"),
  },
  {
    id: "member-reminder",
    name: "會員提醒",
    description: "適合推廣會員與集點活動的收據內容。",
    text: ["感謝您的支持", "加入會員可享活動通知與點數累積", "最新優惠請洽櫃台或官方社群"].join("\n"),
  },
  {
    id: "support-contact",
    name: "售後聯絡",
    description: "適合需要加註客服與售後說明的店家。",
    text: ["若商品有任何問題", "請於當日憑單據洽門市協助", "我們會盡快為您處理"].join("\n"),
  },
];
