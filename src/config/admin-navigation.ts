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
    description: "集中查看營收、裝置在線、毛利、庫存與今日重點。",
    permissionKeys: [PERMISSIONS.DASHBOARD_VIEW, PERMISSIONS.REPORTS_VIEW],
  },
  {
    to: "/products",
    label: "商品管理",
    short: "PD",
    description: "維護商品、售價、圖片、配方與客製化選項。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/product-categories",
    label: "商品分類",
    short: "PC",
    description: "整理商品分類與前台顯示順序。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/inventory",
    label: "庫存管理",
    short: "IV",
    description: "查看商品、原料、製成品與包裝的庫存與異動。",
    permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
    children: [
      {
        to: "/inventory/stocktakes",
        label: "盤點作業",
        short: "ST",
        description: "建立與處理商品盤點單。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/defective",
        label: "瑕疵庫存",
        short: "DF",
        description: "管理瑕疵、報廢與待處理庫存。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/materials",
        label: "原料管理",
        short: "RM",
        description: "查看原料庫存、補貨門檻與異動紀錄。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/manufactured",
        label: "製成品管理",
        short: "MF",
        description: "管理中央製成品、轉入與領用資料。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/packaging",
        label: "包裝管理",
        short: "PK",
        description: "維護包材庫存、單位與門檻設定。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/replenishment",
        label: "補貨建議",
        short: "RP",
        description: "依商品、原料與包裝庫存狀態提供補貨建議。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    ],
  },
  {
    to: "/suppliers",
    label: "供應商管理",
    short: "SP",
    description: "維護供應商基本資料與聯絡方式。",
    permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
  },
  {
    to: "/procurement",
    label: "採購進貨",
    short: "PO",
    description: "建立採購單、進貨單並追蹤到貨狀態。",
    permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
  },
  {
    to: "/devices",
    label: "裝置管理",
    short: "DV",
    description: "查看 POS 裝置在線狀態與最近 heartbeat。",
    permissionKeys: [PERMISSIONS.DEVICES_VIEW],
  },
  {
    to: "/orders",
    label: "訂單管理",
    short: "OD",
    description: "查詢訂單、退款與訂單明細。",
    permissionKeys: [PERMISSIONS.ORDERS_VIEW],
  },
  {
    to: "/shifts",
    label: "班次管理",
    short: "SH",
    description: "處理開班、關班與班次結算。",
    permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
  },
  {
    to: "/access/users",
    label: "帳號與權限",
    short: "AC",
    description: "管理員工帳號、角色與可操作門市。",
    permissionKeys: [PERMISSIONS.USERS_VIEW, PERMISSIONS.ROLES_VIEW],
    children: [
      {
        to: "/access/users",
        label: "帳號管理",
        short: "US",
        description: "建立、停用或調整員工帳號。",
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
      {
        to: "/access/roles",
        label: "角色權限",
        short: "RL",
        description: "設定角色可查看、可編輯與可操作權限。",
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
      },
    ],
  },
  {
    to: "/settings/receipt-footer",
    label: "系統管理",
    short: "SM",
    description: "維護收據內容、圖片優化與其他系統設定。",
    permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
    children: [
      {
        to: "/settings/receipt-footer",
        label: "收據內容",
        short: "RF",
        description: "編輯門市收據下方的自訂固定文字，供後台與 POS 共用。",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/image-optimization",
        label: "圖片優化",
        short: "IO",
        description: "掃描並優化既有圖片，降低 POS 與後台載入負擔。",
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
