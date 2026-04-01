import { createRouter, createWebHistory } from "vue-router";

import { findFirstAccessiblePath } from "@/config/admin-navigation";
import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { pinia } from "@/stores/pinia";
import DefectiveInventoryView from "@/views/DefectiveInventoryView.vue";
import DevicesView from "@/views/DevicesView.vue";
import ImageOptimizationView from "@/views/ImageOptimizationView.vue";
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
import ReceiptFooterSettingsView from "@/views/ReceiptFooterSettingsView.vue";
import RegisterView from "@/views/RegisterView.vue";
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
      meta: { title: "Login", layout: "auth", requiresAuth: false },
    },
    {
      path: "/register",
      name: "register",
      component: RegisterView,
      meta: { title: "Register", layout: "auth", requiresAuth: false },
    },
    {
      path: "/",
      name: "dashboard",
      component: ReportsView,
      meta: {
        title: "Dashboard",
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
        title: "Products",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
      },
    },
    {
      path: "/product-categories",
      name: "product-categories",
      component: ProductCategoriesView,
      meta: {
        title: "Product Categories",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.PRODUCTS_VIEW],
      },
    },
    {
      path: "/inventory",
      name: "inventory",
      component: InventoryView,
      meta: {
        title: "Inventory",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/stocktakes",
      name: "inventory-stocktakes",
      component: InventoryStocktakesView,
      meta: {
        title: "Stocktakes",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/defective",
      name: "inventory-defective",
      component: DefectiveInventoryView,
      meta: {
        title: "Defective Inventory",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/materials",
      name: "inventory-materials",
      component: MaterialsInventoryView,
      meta: {
        title: "Materials",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/manufactured",
      name: "inventory-manufactured",
      component: ManufacturedInventoryView,
      meta: {
        title: "Manufactured Items",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/packaging",
      name: "inventory-packaging",
      component: PackagingInventoryView,
      meta: {
        title: "Packaging",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/inventory/replenishment",
      name: "inventory-replenishment",
      component: ReplenishmentView,
      meta: {
        title: "Replenishment",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.INVENTORY_VIEW],
      },
    },
    {
      path: "/suppliers",
      name: "suppliers",
      component: SuppliersView,
      meta: {
        title: "Suppliers",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.SUPPLIERS_VIEW],
      },
    },
    {
      path: "/procurement",
      name: "procurement",
      component: ProcurementView,
      meta: {
        title: "Procurement",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.PROCUREMENT_VIEW],
      },
    },
    {
      path: "/devices",
      name: "devices",
      component: DevicesView,
      meta: {
        title: "Devices",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.DEVICES_VIEW],
      },
    },
    {
      path: "/orders",
      name: "orders",
      component: OrdersView,
      meta: {
        title: "Orders",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.ORDERS_VIEW],
      },
    },
    {
      path: "/shifts",
      name: "shifts",
      component: ShiftsView,
      meta: {
        title: "Shifts",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.SHIFTS_VIEW],
      },
    },
    {
      path: "/access/users",
      name: "access-users",
      component: UsersAccessView,
      meta: {
        title: "Users",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.USERS_VIEW],
      },
    },
    {
      path: "/access/roles",
      name: "access-roles",
      component: RolePermissionsView,
      meta: {
        title: "Roles",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.ROLES_VIEW],
      },
    },
    {
      path: "/settings/receipt-footer",
      name: "settings-receipt-footer",
      component: ReceiptFooterSettingsView,
      meta: {
        title: "Receipt Footer",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
      },
    },
    {
      path: "/settings/image-optimization",
      name: "settings-image-optimization",
      component: ImageOptimizationView,
      meta: {
        title: "Image Optimization",
        requiresAuth: true,
        permissionKeys: [PERMISSIONS.SETTINGS_VIEW],
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

  if ((to.name === "login" || to.name === "register") && authStore.isAuthenticated) {
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
  const title = typeof to.meta.title === "string" ? to.meta.title : "Admin";
  document.title = `NUCOSMOS Admin | ${title}`;
});

export default router;
