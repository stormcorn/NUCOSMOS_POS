import { PERMISSIONS, type PermissionKey } from "@/constants/permissions";

export type AdminNavigationChild = {
  to: string;
  label: string;
  short: string;
  description: string;
  permissionKeys?: PermissionKey[];
};

export type AdminNavigationItem = {
  to: string;
  label: string;
  short: string;
  description: string;
  permissionKeys?: PermissionKey[];
  children?: AdminNavigationChild[];
};

export const adminNavigationItems: AdminNavigationItem[] = [
  {
    to: "/",
    label: "營運總覽",
    short: "OV",
    description: "檢視目前門市、核心營運指標與即時營運摘要。",
    permissionKeys: [PERMISSIONS.DASHBOARD_VIEW, PERMISSIONS.REPORTS_VIEW],
  },
  {
    to: "/products",
    label: "商品管理",
    short: "PD",
    description: "管理商品、售價、說明與商品端操作設定。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/product-categories",
    label: "商品分類",
    short: "PC",
    description: "維護 POS 商品分類排序與顯示狀態。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/inventory",
    label: "庫存管理",
    short: "IV",
    description: "追蹤可售庫存、原料庫存、瑕疵庫存與補貨作業。",
    permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
    children: [
      {
        to: "/inventory/stocktakes",
        label: "盤點管理",
        short: "ST",
        description: "建立與檢視目前門市的盤點作業。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/defective",
        label: "瑕疵庫存",
        short: "DF",
        description: "追蹤瑕疵或不可售庫存，並處理轉回與報廢。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/materials",
        label: "原料管理",
        short: "RM",
        description: "管理原料、入庫、成本與異動紀錄。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/manufactured",
        label: "製成品管理",
        short: "MF",
        description: "管理製成品、批次與相關庫存作業。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/packaging",
        label: "包裝管理",
        short: "PK",
        description: "管理包裝品項、入庫與異動紀錄。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/replenishment",
        label: "補貨建議",
        short: "RP",
        description: "依照補貨門檻檢視建議補貨項目。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    ],
  },
  {
    to: "/suppliers",
    label: "供應商管理",
    short: "SP",
    description: "維護供應商資料與採購來源資訊。",
    permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
  },
  {
    to: "/procurement",
    label: "採購管理",
    short: "PO",
    description: "建立與追蹤採購單與收貨狀態。",
    permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
  },
  {
    to: "/devices",
    label: "裝置管理",
    short: "DV",
    description: "檢視已註冊 POS 裝置與裝置層設定。",
    permissionKeys: [PERMISSIONS.DEVICES_VIEW],
  },
  {
    to: "/orders",
    label: "訂單管理",
    short: "OD",
    description: "檢視訂單、退款、付款狀態與歷史紀錄。",
    permissionKeys: [PERMISSIONS.ORDERS_VIEW],
  },
  {
    to: "/shifts",
    label: "班次管理",
    short: "SH",
    description: "檢視班次歷史、目前班次狀態與結班作業。",
    permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
  },
  {
    to: "/access/users",
    label: "帳號與權限",
    short: "AC",
    description: "管理員工帳號、角色指派與權限設定。",
    permissionKeys: [PERMISSIONS.USERS_VIEW, PERMISSIONS.ROLES_VIEW],
    children: [
      {
        to: "/access/users",
        label: "帳號管理",
        short: "US",
        description: "建立與管理員工帳號及門市指派。",
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
      {
        to: "/access/roles",
        label: "角色權限",
        short: "RL",
        description: "設定角色權限與權限組合。",
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
      },
    ],
  },
  {
    to: "/settings/receipt-footer",
    label: "系統設定",
    short: "SM",
    description: "管理收據、活動、場地與系統端營運設定。",
    permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
    children: [
      {
        to: "/settings/space-booking",
        label: "活動空間租借",
        short: "SB",
        description: "管理場地可預約時段、預約申請與封鎖時段。",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/receipt-footer",
        label: "收據備註",
        short: "RF",
        description: "編輯收據底部文字與收據備註範本。",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/redeem-prizes",
        label: "兌獎獎項",
        short: "RW",
        description: "維護抽獎獎項、機率與獎項數量。",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/image-optimization",
        label: "圖片優化",
        short: "IO",
        description: "檢視商品圖片並優化後台與 POS 使用素材。",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
    ],
  },
];

export function cloneAdminNavigationItems() {
  return adminNavigationItems.map((item) => ({
    ...item,
    permissionKeys: item.permissionKeys ? [...item.permissionKeys] : undefined,
    children: item.children?.map((child) => ({
      ...child,
      permissionKeys: child.permissionKeys ? [...child.permissionKeys] : undefined,
    })),
  }));
}

export function findNavigationEntry(path: string) {
  let bestMatch: { label: string; description: string; to: string } | null = null;

  for (const item of adminNavigationItems) {
    if (path === item.to || (item.to !== "/" && path.startsWith(`${item.to}/`))) {
      bestMatch = { label: item.label, description: item.description, to: item.to };
    }

    for (const child of item.children ?? []) {
      if (path === child.to || path.startsWith(`${child.to}/`)) {
        bestMatch = { label: child.label, description: child.description, to: child.to };
      }
    }
  }

  return bestMatch;
}

export function findFirstAccessiblePath(permissionKeys: readonly string[]) {
  for (const item of adminNavigationItems) {
    const visibleChildren = (item.children ?? []).filter(
      (child) =>
        !child.permissionKeys?.length ||
        child.permissionKeys.some((permissionKey) => permissionKeys.includes(permissionKey)),
    );

    if (visibleChildren.length > 0) {
      return visibleChildren[0].to;
    }

    if (
      !item.permissionKeys?.length ||
      item.permissionKeys.some((permissionKey) => permissionKeys.includes(permissionKey))
    ) {
      return item.to;
    }
  }

  return "/";
}
