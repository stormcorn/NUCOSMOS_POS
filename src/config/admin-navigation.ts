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
    description: "查看營收、裝置與門市營運摘要。",
    permissionKeys: [PERMISSIONS.DASHBOARD_VIEW, PERMISSIONS.REPORTS_VIEW],
  },
  {
    to: "/products",
    label: "商品管理",
    short: "PD",
    description: "維護商品資料、售價、配方與活動設定。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/product-categories",
    label: "商品分類",
    short: "PC",
    description: "整理 POS 商品分類與顯示順序。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/inventory",
    label: "庫存管理",
    short: "IV",
    description: "查看商品、原料、製成品與包裝庫存。",
    permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
    children: [
      {
        to: "/inventory/stocktakes",
        label: "盤點作業",
        short: "ST",
        description: "建立商品盤點單並調整帳務差異。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/defective",
        label: "瑕疵庫存",
        short: "DF",
        description: "管理商品瑕疵庫存與報廢處理。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/materials",
        label: "原料管理",
        short: "RM",
        description: "維護原料庫存、成本與異動記錄。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/manufactured",
        label: "製成品管理",
        short: "MF",
        description: "查看製成品在手量與成本資訊。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/packaging",
        label: "包裝管理",
        short: "PK",
        description: "維護包材庫存、規格與採購成本。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/replenishment",
        label: "補貨建議",
        short: "RP",
        description: "根據補貨線提供商品與原料補貨提醒。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    ],
  },
  {
    to: "/suppliers",
    label: "供應商管理",
    short: "SP",
    description: "維護供應商資料與聯絡資訊。",
    permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
  },
  {
    to: "/procurement",
    label: "採購進貨",
    short: "PO",
    description: "管理採購單、進貨與成本來源。",
    permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
  },
  {
    to: "/devices",
    label: "裝置管理",
    short: "DV",
    description: "查看 POS 裝置狀態、心跳與門市綁定。",
    permissionKeys: [PERMISSIONS.DEVICES_VIEW],
  },
  {
    to: "/orders",
    label: "訂單管理",
    short: "OD",
    description: "查詢訂單、付款狀態與退款紀錄。",
    permissionKeys: [PERMISSIONS.ORDERS_VIEW],
  },
  {
    to: "/shifts",
    label: "班次管理",
    short: "SH",
    description: "管理開班、關班與交班資訊。",
    permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
  },
  {
    to: "/access/users",
    label: "帳號與權限",
    short: "AC",
    description: "管理使用者、角色與權限矩陣。",
    permissionKeys: [PERMISSIONS.USERS_VIEW, PERMISSIONS.ROLES_VIEW],
    children: [
      {
        to: "/access/users",
        label: "帳號管理",
        short: "US",
        description: "建立與維護員工帳號、手機與角色。",
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
      {
        to: "/access/roles",
        label: "角色權限",
        short: "RL",
        description: "設定角色可查看與可操作的功能權限。",
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
      },
    ],
  },
  {
    to: "/settings/receipt-footer",
    label: "系統設定",
    short: "SM",
    description: "管理收據、兌獎與圖片優化等系統設定。",
    permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
    children: [
      {
        to: "/settings/receipt-footer",
        label: "收據內容",
        short: "RF",
        description: "維護收據底部多行內容與預設模板。",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/redeem-prizes",
        label: "抽獎設定",
        short: "RW",
        description: "管理抽獎獎項、中獎機率與剩餘數量。",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/image-optimization",
        label: "圖片優化",
        short: "IO",
        description: "批次壓縮商品與供應資料圖片，減少 POS 載入負擔。",
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
