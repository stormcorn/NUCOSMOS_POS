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
    description: "集中查看銷售、庫存、成本與門市營運狀態。",
    permissionKeys: [PERMISSIONS.DASHBOARD_VIEW, PERMISSIONS.REPORTS_VIEW],
  },
  {
    to: "/products",
    label: "商品管理",
    short: "PD",
    description: "管理商品、售價、活動設定與配方成本。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/access/users",
    label: "帳號與權限",
    short: "AC",
    description: "管理後台使用者、角色與權限配置。",
    permissionKeys: [PERMISSIONS.USERS_VIEW, PERMISSIONS.ROLES_VIEW],
    children: [
      {
        to: "/access/users",
        label: "使用者管理",
        short: "US",
        description: "建立、停用與調整後台使用者帳號。",
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
      {
        to: "/access/roles",
        label: "角色權限",
        short: "RL",
        description: "設定角色可檢視與可操作的功能範圍。",
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
      },
    ],
  },
  {
    to: "/product-categories",
    label: "商品分類",
    short: "PC",
    description: "整理商品分類，支援後台與 POS 顯示排序。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/inventory",
    label: "庫存管理",
    short: "IV",
    description: "查看商品、原料、包裝與瑕疵庫存，以及異動與盤點資料。",
    permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
    children: [
      {
        to: "/inventory/stocktakes",
        label: "商品盤點",
        short: "ST",
        description: "建立盤點單、比對差異並完成過帳。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/defective",
        label: "瑕疵庫存",
        short: "DF",
        description: "集中查看瑕疵品、報廢與轉回可售狀態。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/materials",
        label: "原料管理",
        short: "RM",
        description: "管理原料庫存、單位換算、批號與耗用狀態。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/packaging",
        label: "包裝管理",
        short: "PK",
        description: "管理杯具、封膜、袋材等包裝品項與庫存。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/replenishment",
        label: "補貨建議",
        short: "RP",
        description: "依補貨門檻與現有庫存產生補貨建議。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    ],
  },
  {
    to: "/suppliers",
    label: "供應商管理",
    short: "SP",
    description: "管理供應商資料、聯絡方式與合作狀態。",
    permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
  },
  {
    to: "/procurement",
    label: "採購與進貨",
    short: "PO",
    description: "建立採購單、收貨入庫並追蹤進貨狀態。",
    permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
  },
  {
    to: "/devices",
    label: "裝置管理",
    short: "DV",
    description: "查看 POS 裝置狀態、綁定門市與連線情況。",
    permissionKeys: [PERMISSIONS.DEVICES_VIEW],
  },
  {
    to: "/orders",
    label: "訂單管理",
    short: "OD",
    description: "查詢訂單、退款、付款與成本回補結果。",
    permissionKeys: [PERMISSIONS.ORDERS_VIEW],
  },
  {
    to: "/shifts",
    label: "班次管理",
    short: "SH",
    description: "管理收銀班次、開班結班與金額統計。",
    permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
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
    const visibleChildren = (item.children ?? []).filter((child) =>
      !child.permissionKeys?.length || child.permissionKeys.some((permissionKey) => permissionKeys.includes(permissionKey)),
    );

    if (visibleChildren.length > 0) {
      return visibleChildren[0].to;
    }

    if (!item.permissionKeys?.length || item.permissionKeys.some((permissionKey) => permissionKeys.includes(permissionKey))) {
      return item.to;
    }
  }

  return "/";
}
