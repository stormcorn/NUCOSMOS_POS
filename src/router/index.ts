import { createRouter, createWebHistory } from "vue-router";

import { findFirstAccessiblePath } from "@/config/admin-navigation";
import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { pinia } from "@/stores/pinia";
import DefectiveInventoryView from "@/views/DefectiveInventoryView.vue";
import DevicesView from "@/views/DevicesView.vue";
import InventoryStocktakesView from "@/views/InventoryStocktakesView.vue";
import InventoryView from "@/views/InventoryView.vue";
import LoginView from "@/views/LoginView.vue";
import ManufacturedInventoryView from "@/views/ManufacturedInventoryView.vue";
import MaterialsInventoryView from "@/views/MaterialsInventoryView.vue";
import OrdersView from "@/views/OrdersView.vue";
import PackagingInventoryView from "@/views/PackagingInventoryView.vue";
import ProcurementView from "@/views/ProcurementView.vue";
import ProductCategoriesView from "@/views/ProductCategoriesView.vue";
import ProductsView from "@/views/ProductsView.vue";
import ReportsView from "@/views/ReportsView.vue";
import ReplenishmentView from "@/views/ReplenishmentView.vue";
import RolePermissionsView from "@/views/RolePermissionsView.vue";
import ShiftsView from "@/views/ShiftsView.vue";
import SuppliersView from "@/views/SuppliersView.vue";
import UsersAccessView from "@/views/UsersAccessView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { title: "登入", layout: "auth", requiresAuth: false },
    },
    {
      path: "/",
      name: "dashboard",
      component: ReportsView,
      meta: {
        title: "營運總覽",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.DASHBOARD_VIEW, PERMISSIONS.REPORTS_VIEW],
      },
    },
    {
      path: "/reports",
      name: "reports",
      redirect: { name: "dashboard" },
      meta: { requiresAuth: true },
    },
    {
      path: "/products",
      name: "products",
      component: ProductsView,
      meta: {
        title: "商品管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
      },
    },
    {
      path: "/product-categories",
      name: "product-categories",
      component: ProductCategoriesView,
      meta: {
        title: "商品分類",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
      },
    },
    {
      path: "/inventory",
      name: "inventory",
      component: InventoryView,
      meta: {
        title: "庫存總覽",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/stocktakes",
      name: "inventory-stocktakes",
      component: InventoryStocktakesView,
      meta: {
        title: "盤點作業",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/defective",
      name: "inventory-defective",
      component: DefectiveInventoryView,
      meta: {
        title: "報廢與瑕疵",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/materials",
      name: "inventory-materials",
      component: MaterialsInventoryView,
      meta: {
        title: "原料管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/manufactured",
      name: "inventory-manufactured",
      component: ManufacturedInventoryView,
      meta: {
        title: "製成品管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/packaging",
      name: "inventory-packaging",
      component: PackagingInventoryView,
      meta: {
        title: "包裝管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/replenishment",
      name: "inventory-replenishment",
      component: ReplenishmentView,
      meta: {
        title: "補貨建議",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/suppliers",
      name: "suppliers",
      component: SuppliersView,
      meta: {
        title: "供應商管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
      },
    },
    {
      path: "/procurement",
      name: "procurement",
      component: ProcurementView,
      meta: {
        title: "採購進貨",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
      },
    },
    {
      path: "/devices",
      name: "devices",
      component: DevicesView,
      meta: {
        title: "裝置管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.DEVICES_VIEW],
      },
    },
    {
      path: "/orders",
      name: "orders",
      component: OrdersView,
      meta: {
        title: "訂單管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.ORDERS_VIEW],
      },
    },
    {
      path: "/shifts",
      name: "shifts",
      component: ShiftsView,
      meta: {
        title: "班次管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
      },
    },
    {
      path: "/access/users",
      name: "access-users",
      component: UsersAccessView,
      meta: {
        title: "帳號管理",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
    },
    {
      path: "/access/roles",
      name: "access-roles",
      component: RolePermissionsView,
      meta: {
        title: "角色權限",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
      },
    },
  ],
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia);
  await authStore.bootstrap();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }

  if (to.name === "login" && authStore.isAuthenticated) {
    return findFirstAccessiblePath(authStore.permissionKeys);
  }

  const requiredPermissionKeys = Array.isArray(to.meta.permissionKeys)
    ? to.meta.permissionKeys
    : [];
  if (
    requiredPermissionKeys.length > 0 &&
    !authStore.hasAnyPermission(requiredPermissionKeys)
  ) {
    return findFirstAccessiblePath(authStore.permissionKeys);
  }

  return true;
});

router.afterEach((to) => {
  const title = typeof to.meta.title === "string" ? to.meta.title : "管理後台";
  document.title = `NUCOSMOS Admin | ${title}`;
});

export default router;
