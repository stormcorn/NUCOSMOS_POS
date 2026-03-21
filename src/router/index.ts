import { createRouter, createWebHistory } from "vue-router";

import { useAuthStore } from "@/stores/auth";
import { pinia } from "@/stores/pinia";
import DashboardView from "@/views/DashboardView.vue";
import DevicesView from "@/views/DevicesView.vue";
import InventoryView from "@/views/InventoryView.vue";
import LoginView from "@/views/LoginView.vue";
import OrdersView from "@/views/OrdersView.vue";
import ProductCategoriesView from "@/views/ProductCategoriesView.vue";
import ProductsView from "@/views/ProductsView.vue";
import ReportsView from "@/views/ReportsView.vue";
import ShiftsView from "@/views/ShiftsView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { title: "管理員登入", layout: "auth", requiresAuth: false },
    },
    {
      path: "/",
      name: "dashboard",
      component: DashboardView,
      meta: { title: "總覽儀表板", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
    },
    {
      path: "/product-categories",
      name: "product-categories",
      component: ProductCategoriesView,
      meta: { title: "商品分類管理", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
    },
    {
      path: "/inventory",
      name: "inventory",
      component: InventoryView,
      meta: { title: "庫存管理", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
    },
    {
      path: "/products",
      name: "products",
      component: ProductsView,
      meta: { title: "商品管理", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
    },
    {
      path: "/devices",
      name: "devices",
      component: DevicesView,
      meta: { title: "裝置管理", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
    },
    {
      path: "/orders",
      name: "orders",
      component: OrdersView,
      meta: { title: "訂單查詢", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
    },
    {
      path: "/reports",
      name: "reports",
      component: ReportsView,
      meta: { title: "銷售報表", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
    },
    {
      path: "/shifts",
      name: "shifts",
      component: ShiftsView,
      meta: { title: "班次管理", requiresAuth: true, roles: ["MANAGER", "ADMIN"] },
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
    return { name: "dashboard" };
  }

  const roles = Array.isArray(to.meta.roles) ? to.meta.roles : [];
  if (roles.length > 0 && authStore.session && !roles.includes(authStore.session.activeRole)) {
    return { name: "dashboard" };
  }

  return true;
});

router.afterEach((to) => {
  const title = typeof to.meta.title === "string" ? to.meta.title : "管理後台";
  document.title = `NUCOSMOS Admin | ${title}`;
});

export default router;
