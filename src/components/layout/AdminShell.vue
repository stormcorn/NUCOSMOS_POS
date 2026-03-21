<script setup lang="ts">
import { computed, onMounted, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";

import { useAuthStore } from "@/stores/auth";
import { useStoreContextStore } from "@/stores/store-context";

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const storeContextStore = useStoreContextStore();

const navigationItems = [
  { to: "/", label: "總覽儀表板", short: "OV" },
  { to: "/product-categories", label: "商品分類", short: "PC" },
  { to: "/inventory", label: "庫存管理", short: "IV" },
  { to: "/products", label: "商品管理", short: "PD" },
  { to: "/devices", label: "裝置管理", short: "DV" },
  { to: "/orders", label: "訂單查詢", short: "OD" },
  { to: "/reports", label: "銷售報表", short: "RP" },
  { to: "/shifts", label: "班次管理", short: "SH" },
];

const pageSubtitle = computed(() => {
  switch (route.path) {
    case "/product-categories":
      return "管理商品分類代碼、排序與啟用狀態。";
    case "/inventory":
      return "管理庫存台帳、進銷存異動與補貨門檻。";
    case "/products":
      return "維護商品資料、價格、圖片與分類。";
    case "/devices":
      return "查看門市裝置狀態與最近在線時間。";
    case "/orders":
      return "查詢訂單清單、付款與退款紀錄。";
    case "/reports":
      return "彙整營收、訂單與銷售趨勢。";
    case "/shifts":
      return "管理開班、結班與現金交接紀錄。";
    default:
      return "集中管理門市營運資料與管理後台操作。";
  }
});

async function ensureStores() {
  if (authStore.isAuthenticated && storeContextStore.stores.length === 0 && !storeContextStore.loading) {
    await storeContextStore.loadStores();
  }

  storeContextStore.syncSelectedStore();
}

async function handleLogout() {
  authStore.signOut();
  await router.push({ name: "login" });
}

onMounted(() => {
  void ensureStores();
});

watch(
  () => authStore.isAuthenticated,
  () => {
    void ensureStores();
  },
);
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <div class="pointer-events-none fixed inset-0 overflow-hidden">
      <div class="absolute inset-x-0 top-0 h-72 bg-[radial-gradient(circle_at_top,_rgba(81,245,214,0.18),_transparent_58%)]" />
      <div class="absolute right-[-12rem] top-40 h-80 w-80 rounded-full bg-[radial-gradient(circle,_rgba(255,181,94,0.22),_transparent_62%)] blur-3xl" />
    </div>

    <div class="relative mx-auto grid min-h-screen max-w-[1600px] lg:grid-cols-[280px_minmax(0,1fr)]">
      <aside class="border-b border-white/8 bg-white/5 px-5 py-6 backdrop-blur-xl lg:border-b-0 lg:border-r lg:px-6">
        <div class="rounded-[2rem] border border-white/10 bg-brand-panel/80 p-5 shadow-soft shadow-black/30">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs uppercase tracking-[0.35em] text-brand-aqua/70">NUCOSMOS</p>
              <h1 class="mt-2 text-2xl font-semibold text-white">Admin Bridge</h1>
            </div>
            <div class="rounded-2xl border border-brand-aqua/20 bg-brand-aqua/10 px-3 py-2 text-sm font-medium text-brand-aqua">
              Live
            </div>
          </div>

          <div class="mt-8 space-y-4">
            <label class="block text-xs uppercase tracking-[0.32em] text-slate-400">Current Store</label>
            <select
              :value="storeContextStore.selectedStoreCode"
              class="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-sm text-slate-100 outline-none transition focus:border-brand-aqua/50 focus:ring-2 focus:ring-brand-aqua/20"
              @change="storeContextStore.setSelectedStoreCode(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="store in storeContextStore.stores" :key="store.code" :value="store.code">
                {{ store.name }}
              </option>
            </select>
          </div>

          <nav class="mt-8 space-y-2">
            <RouterLink
              v-for="item in navigationItems"
              :key="item.to"
              :to="item.to"
              class="group flex items-center gap-3 rounded-2xl border px-3 py-3 transition"
              :class="route.path === item.to
                ? 'border-brand-aqua/30 bg-brand-aqua/12 text-white shadow-glow'
                : 'border-white/6 bg-white/0 text-slate-300 hover:border-white/14 hover:bg-white/6 hover:text-white'"
            >
              <span
                class="flex h-11 w-11 items-center justify-center rounded-2xl text-xs font-semibold tracking-[0.2em]"
                :class="route.path === item.to ? 'bg-brand-aqua text-slate-950' : 'bg-slate-900 text-brand-aqua'"
              >
                {{ item.short }}
              </span>
              <span class="text-sm font-medium">{{ item.label }}</span>
            </RouterLink>
          </nav>

          <div class="mt-10 rounded-[1.5rem] border border-amber-300/15 bg-gradient-to-br from-amber-200/10 to-transparent p-4">
            <p class="text-xs uppercase tracking-[0.3em] text-amber-200/70">Session Pulse</p>
            <p class="mt-3 text-lg font-semibold text-white">{{ authStore.activeRole || "未登入" }}</p>
            <p class="mt-2 text-sm text-slate-400">
              {{ storeContextStore.selectedStore?.name || authStore.currentStoreCode || "未選擇門市" }}
              正在使用管理後台
            </p>
          </div>
        </div>
      </aside>

      <main class="px-5 py-6 lg:px-8">
        <header class="mb-6 flex flex-col gap-4 rounded-[2rem] border border-white/10 bg-white/5 px-5 py-5 backdrop-blur-md lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.35em] text-brand-aqua/70">Operations Console</p>
            <h2 class="mt-2 text-3xl font-semibold text-white">{{ route.meta.title || "總覽儀表板" }}</h2>
            <p class="mt-2 max-w-2xl text-sm text-slate-400">{{ pageSubtitle }}</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 px-4 py-3">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Store Focus</p>
              <p class="mt-2 text-base font-semibold text-white">{{ storeContextStore.selectedStore?.name || authStore.currentStoreCode || "未選擇門市" }}</p>
              <p class="text-sm text-slate-400">{{ storeContextStore.selectedStore?.code || authStore.currentStoreCode || "-" }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 px-4 py-3">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Signed In</p>
              <p class="mt-2 text-base font-semibold text-white">{{ authStore.userDisplayName }}</p>
              <p class="text-sm text-slate-400">{{ authStore.session?.employeeCode || "未登入" }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 px-4 py-3 sm:col-span-2 xl:col-span-1">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Mode</p>
              <div class="mt-2 flex items-center justify-between gap-3">
                <div>
                  <p class="text-base font-semibold text-white">{{ authStore.activeRole || "Guest" }}</p>
                  <p class="text-sm text-slate-400">JWT session active</p>
                </div>
                <button
                  v-if="authStore.isAuthenticated"
                  class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-brand-aqua/30 hover:text-white"
                  @click="handleLogout"
                >
                  Logout
                </button>
              </div>
            </div>
          </div>
        </header>

        <slot />
      </main>
    </div>
  </div>
</template>
