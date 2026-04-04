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
    label: "Operations Overview",
    short: "OV",
    description: "Review the current store, key health indicators, and business snapshots.",
    permissionKeys: [PERMISSIONS.DASHBOARD_VIEW, PERMISSIONS.REPORTS_VIEW],
  },
  {
    to: "/products",
    label: "Products",
    short: "PD",
    description: "Manage products, selling price, descriptions, and product-side operations.",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/product-categories",
    label: "Categories",
    short: "PC",
    description: "Maintain POS product category ordering and visibility.",
    permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
  },
  {
    to: "/inventory",
    label: "Inventory",
    short: "IV",
    description: "Track sellable stock, material stock, defective stock, and replenishment actions.",
    permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
    children: [
      {
        to: "/inventory/stocktakes",
        label: "Stocktakes",
        short: "ST",
        description: "Create and review cycle counts for current store inventory.",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/defective",
        label: "Defective",
        short: "DF",
        description: "Track damaged or unsellable inventory and handle restore or scrap actions.",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/materials",
        label: "Materials",
        short: "RM",
        description: "Manage raw materials, receiving, cost, and movement history.",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/manufactured",
        label: "Manufactured",
        short: "MF",
        description: "Manage manufactured items, lots, and related inventory operations.",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/packaging",
        label: "Packaging",
        short: "PK",
        description: "Manage packaging items, receiving, and movement records.",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
      {
        to: "/inventory/replenishment",
        label: "Replenishment",
        short: "RP",
        description: "Review suggested replenishment actions based on stock thresholds.",
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    ],
  },
  {
    to: "/suppliers",
    label: "Suppliers",
    short: "SP",
    description: "Maintain supplier records and sourcing information.",
    permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
  },
  {
    to: "/procurement",
    label: "Procurement",
    short: "PO",
    description: "Create and track purchase orders and receiving status.",
    permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
  },
  {
    to: "/devices",
    label: "Devices",
    short: "DV",
    description: "Monitor registered POS devices and device-level configuration.",
    permissionKeys: [PERMISSIONS.DEVICES_VIEW],
  },
  {
    to: "/orders",
    label: "Orders",
    short: "OD",
    description: "Inspect orders, refunds, payment status, and order history.",
    permissionKeys: [PERMISSIONS.ORDERS_VIEW],
  },
  {
    to: "/shifts",
    label: "Shifts",
    short: "SH",
    description: "Review shift history, active shift state, and closeout activity.",
    permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
  },
  {
    to: "/access/users",
    label: "Access Control",
    short: "AC",
    description: "Manage staff accounts, role assignments, and access policies.",
    permissionKeys: [PERMISSIONS.USERS_VIEW, PERMISSIONS.ROLES_VIEW],
    children: [
      {
        to: "/access/users",
        label: "Users",
        short: "US",
        description: "Create and manage staff accounts and their store assignments.",
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
      {
        to: "/access/roles",
        label: "Roles",
        short: "RL",
        description: "Configure role permissions and permission bundles.",
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
      },
    ],
  },
  {
    to: "/settings/receipt-footer",
    label: "Settings",
    short: "SM",
    description: "Manage receipt, promotion, venue, and system-side operating settings.",
    permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
    children: [
      {
        to: "/settings/space-booking",
        label: "Space Booking",
        short: "SB",
        description: "Manage venue availability, booking requests, and blockout periods.",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/receipt-footer",
        label: "Receipt Footer",
        short: "RF",
        description: "Edit the receipt footer text and footer template presets.",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/redeem-prizes",
        label: "Redeem Prizes",
        short: "RW",
        description: "Maintain prize definitions, probabilities, and draw inventory.",
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
      {
        to: "/settings/image-optimization",
        label: "Image Optimization",
        short: "IO",
        description: "Review product images and optimize assets for admin and POS usage.",
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
