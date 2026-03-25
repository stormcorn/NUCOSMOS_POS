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
    description: "查看營收、訂單、熱門商品與關鍵營運指標。",
    permissionKeys: [PERMISSIONS.DASHBOARD_VIEW, PERMISSIONS.REPORTS_VIEW],
  },
  {
    to: "/products",
    label: "商品管理",
    short: "PD",
    description: "維護商品、售價、客製化選項與上架狀態。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/product-categories",
    label: "商品分類",
    short: "PC",
    description: "管理前台分類與商品歸屬。",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/inventory",
    label: "庫存管理",
    short: "IV",
    description: "查看成品、原料、製成品、包裝與盤點資訊。",
    permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
    children: [
      {
        to: "/inventory/stocktakes",
        label: "盤點作業",
        short: "ST",
        description: "進行盤點與調整實際庫存。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/defective",
        label: "報廢與瑕疵",
        short: "DF",
        description: "追蹤報廢、損壞與瑕疵品數量。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/materials",
        label: "原料管理",
        short: "RM",
        description: "管理原料庫存、單位、補貨點與異動。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/manufactured",
        label: "製成品管理",
        short: "MF",
        description: "管理半成品與製成品庫存、批次與異動。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/packaging",
        label: "包裝管理",
        short: "PK",
        description: "管理杯子、瓶器、紙盒與其他包材。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/replenishment",
        label: "補貨建議",
        short: "RP",
        description: "彙整原料、製成品與包裝的補貨建議。",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    ],
  },
  {
    to: "/suppliers",
    label: "供應商管理",
    short: "SP",
    description: "維護供應商基本資料與聯絡資訊。",
    permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
  },
  {
    to: "/procurement",
    label: "採購進貨",
    short: "PO",
    description: "建立採購單並接收原料、製成品與包裝。",
    permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
  },
  {
    to: "/devices",
    label: "裝置管理",
    short: "DV",
    description: "管理 POS 平板、列印機與裝置授權。",
    permissionKeys: [PERMISSIONS.DEVICES_VIEW],
  },
  {
    to: "/orders",
    label: "訂單管理",
    short: "OD",
    description: "查看訂單、付款狀態與退款紀錄。",
    permissionKeys: [PERMISSIONS.ORDERS_VIEW],
  },
  {
    to: "/shifts",
    label: "班次管理",
    short: "SH",
    description: "管理交班、結帳與班次狀態。",
    permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
  },
  {
    to: "/access/users",
    label: "帳號與權限",
    short: "AC",
    description: "管理人員帳號、角色與後台權限。",
    permissionKeys: [PERMISSIONS.USERS_VIEW, PERMISSIONS.ROLES_VIEW],
    children: [
      {
        to: "/access/users",
        label: "帳號管理",
        short: "US",
        description: "建立與維護使用者帳號。",
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
      {
        to: "/access/roles",
        label: "角色權限",
        short: "RL",
        description: "設定角色可用功能與權限範圍。",
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
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
