<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";

import { fetchNavigationPreference, saveNavigationPreference } from "@/api/auth";
import {
  cloneAdminNavigationItems,
  findNavigationEntry,
  type AdminNavigationChild,
  type AdminNavigationItem,
} from "@/config/admin-navigation";
import { useAuthStore } from "@/stores/auth";
import { useStoreContextStore } from "@/stores/store-context";

const LOCAL_STORAGE_KEY = "nucosmos-admin-navigation-order";

type PersistedNavigationOrder = {
  rootOrder: string[];
  childOrders: Record<string, string[]>;
};

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const storeContextStore = useStoreContextStore();

const navigationItems = ref<AdminNavigationItem[]>(cloneAdminNavigationItems());
const draggingRootTo = ref<string | null>(null);
const draggingChild = ref<{ parentTo: string; childTo: string } | null>(null);
const isNavOpen = ref(false);

const visibleNavigationItems = computed(() =>
  navigationItems.value
    .map((item) => {
      const visibleChildren = (item.children ?? []).filter((child) => authStore.hasAnyPermission(child.permissionKeys ?? []));
      const itemVisible = authStore.hasAnyPermission(item.permissionKeys ?? []);

      if (!itemVisible && visibleChildren.length === 0) {
        return null;
      }

      return {
        ...item,
        children: visibleChildren.length > 0 ? visibleChildren : undefined,
      };
    })
    .filter(Boolean) as AdminNavigationItem[],
);

const pageSubtitle = computed(() => {
  return findNavigationEntry(route.path)?.description ?? "查看目前頁面的摘要、狀態與最近操作。";
});

function hasAnySavedOrder(order: PersistedNavigationOrder) {
  return order.rootOrder.length > 0 || Object.values(order.childOrders).some((children) => children.length > 0);
}

function isRouteActive(target: string) {
  return route.path === target || route.path.startsWith(`${target}/`);
}

function closeNavigation() {
  isNavOpen.value = false;
}

function reorderByKey<T>(list: T[], fromKey: string, toKey: string, getKey: (item: T) => string) {
  const next = [...list];
  const fromIndex = next.findIndex((item) => getKey(item) === fromKey);
  const toIndex = next.findIndex((item) => getKey(item) === toKey);

  if (fromIndex < 0 || toIndex < 0 || fromIndex === toIndex) {
    return next;
  }

  const [movedItem] = next.splice(fromIndex, 1);
  next.splice(toIndex, 0, movedItem);
  return next;
}

function applyPersistedOrder(persisted: PersistedNavigationOrder | null | undefined) {
  const baseItems = cloneAdminNavigationItems();
  if (!persisted) {
    navigationItems.value = baseItems;
    return;
  }

  const orderedRoots = [
    ...(persisted.rootOrder
      .map((to) => baseItems.find((item) => item.to === to))
      .filter(Boolean) as AdminNavigationItem[]),
    ...baseItems.filter((item) => !persisted.rootOrder.includes(item.to)),
  ];

  navigationItems.value = orderedRoots.map((item) => {
    const childOrder = persisted.childOrders[item.to] ?? [];
    if (!item.children?.length) {
      return item;
    }

    const orderedChildren = [
      ...(childOrder
        .map((to) => item.children?.find((child) => child.to === to))
        .filter(Boolean) as AdminNavigationChild[]),
      ...item.children.filter((child) => !childOrder.includes(child.to)),
    ];

    return {
      ...item,
      children: orderedChildren,
    };
  });
}

function buildPersistedOrder(): PersistedNavigationOrder {
  return {
    rootOrder: navigationItems.value.map((item) => item.to),
    childOrders: Object.fromEntries(
      navigationItems.value
        .filter((item) => item.children?.length)
        .map((item) => [item.to, item.children?.map((child) => child.to) ?? []]),
    ),
  };
}

function loadPersistedOrder() {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(LOCAL_STORAGE_KEY);
  if (!raw) {
    applyPersistedOrder(null);
    return null;
  }

  try {
    const persisted = JSON.parse(raw) as PersistedNavigationOrder;
    applyPersistedOrder(persisted);
    return persisted;
  } catch {
    applyPersistedOrder(null);
    return null;
  }
}

async function persistOrder() {
  const payload = buildPersistedOrder();

  if (typeof window !== "undefined") {
    window.localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(payload));
  }

  if (!authStore.isAuthenticated) {
    return;
  }

  try {
    await saveNavigationPreference(payload);
  } catch {
    // Keep local order even if sync fails; next successful login will retry.
  }
}

async function syncNavigationPreference() {
  if (!authStore.isAuthenticated) {
    return;
  }

  const localOrder = loadPersistedOrder();

  try {
    const remotePreference = await fetchNavigationPreference();
    const remoteOrder: PersistedNavigationOrder = {
      rootOrder: remotePreference.rootOrder ?? [],
      childOrders: remotePreference.childOrders ?? {},
    };

    if (hasAnySavedOrder(remoteOrder)) {
      applyPersistedOrder(remoteOrder);
      if (typeof window !== "undefined") {
        window.localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(remoteOrder));
      }
      return;
    }

    if (localOrder && hasAnySavedOrder(localOrder)) {
      await saveNavigationPreference(localOrder);
    }
  } catch {
    if (!localOrder) {
      applyPersistedOrder(null);
    }
  }
}

async function resetNavigationOrder() {
  applyPersistedOrder(null);
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(LOCAL_STORAGE_KEY);
  }

  if (authStore.isAuthenticated) {
    try {
      await saveNavigationPreference({ rootOrder: [], childOrders: {} });
    } catch {
      // Ignore reset sync errors; UI already reverted locally.
    }
  }
}

function handleRootDragStart(itemTo: string) {
  draggingRootTo.value = itemTo;
}

function handleRootDrop(targetTo: string) {
  if (!draggingRootTo.value || draggingRootTo.value === targetTo) {
    draggingRootTo.value = null;
    return;
  }

  navigationItems.value = reorderByKey(navigationItems.value, draggingRootTo.value, targetTo, (item) => item.to);
  draggingRootTo.value = null;
  void persistOrder();
}

function handleChildDragStart(parentTo: string, childTo: string) {
  draggingChild.value = { parentTo, childTo };
}

function handleChildDrop(parentTo: string, targetChildTo: string) {
  if (!draggingChild.value || draggingChild.value.parentTo !== parentTo || draggingChild.value.childTo === targetChildTo) {
    draggingChild.value = null;
    return;
  }

  navigationItems.value = navigationItems.value.map((item) => {
    if (item.to !== parentTo || !item.children) {
      return item;
    }

    return {
      ...item,
      children: reorderByKey(item.children, draggingChild.value!.childTo, targetChildTo, (child) => child.to),
    };
  });

  draggingChild.value = null;
  void persistOrder();
}

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
  loadPersistedOrder();
  void syncNavigationPreference();
  void ensureStores();
});

watch(
  () => authStore.isAuthenticated,
  () => {
    void syncNavigationPreference();
    void ensureStores();
  },
);

watch(
  () => route.fullPath,
  () => {
    closeNavigation();
  },
);
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <div class="pointer-events-none fixed inset-0 overflow-hidden">
      <div class="absolute inset-x-0 top-0 h-72 bg-[radial-gradient(circle_at_top,_rgba(81,245,214,0.18),_transparent_58%)]" />
      <div class="absolute right-[-12rem] top-40 h-80 w-80 rounded-full bg-[radial-gradient(circle,_rgba(255,181,94,0.22),_transparent_62%)] blur-3xl" />
    </div>

    <div v-if="isNavOpen" class="fixed inset-0 z-40 bg-slate-950/70 backdrop-blur-sm xl:hidden" @click="closeNavigation" />

    <div class="relative mx-auto min-h-screen max-w-[1600px] xl:grid xl:grid-cols-[280px_minmax(0,1fr)]">
      <aside
        class="fixed inset-y-0 left-0 z-50 w-[min(86vw,320px)] overflow-y-auto border-r border-white/10 bg-white/5 px-4 py-5 backdrop-blur-xl transition-transform duration-300 xl:static xl:w-auto xl:translate-x-0 xl:px-6 xl:py-6"
        :class="isNavOpen ? 'translate-x-0' : '-translate-x-full xl:translate-x-0'"
      >
        <div class="rounded-[2rem] border border-white/10 bg-brand-panel/80 p-5 shadow-soft shadow-black/30">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-xs uppercase tracking-[0.35em] text-brand-aqua/70">NUCOSMOS</p>
              <h1 class="mt-2 text-2xl font-semibold text-white">Admin Bridge</h1>
            </div>
            <div class="flex items-center gap-2">
              <div class="rounded-2xl border border-brand-aqua/20 bg-brand-aqua/10 px-3 py-2 text-sm font-medium text-brand-aqua">
                Live
              </div>
              <button
                class="rounded-2xl border border-white/10 px-3 py-2 text-xs text-slate-200 xl:hidden"
                type="button"
                @click="closeNavigation"
              >
                關閉
              </button>
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

          <div class="mt-6 flex items-center justify-between gap-3 rounded-2xl border border-white/8 bg-slate-950/35 px-4 py-3 text-xs text-slate-400">
            <span>可直接拖曳主選單與子選單排序。</span>
            <button class="rounded-xl border border-white/10 px-3 py-2 text-[11px] text-slate-200 transition hover:border-brand-aqua/30 hover:text-white" @click="resetNavigationOrder">
              恢復預設
            </button>
          </div>

          <nav class="mt-8 space-y-2">
            <div
              v-for="item in visibleNavigationItems"
              :key="item.to"
              class="space-y-2"
              draggable="true"
              @dragstart="handleRootDragStart(item.to)"
              @dragend="draggingRootTo = null"
              @dragover.prevent
              @drop.prevent="handleRootDrop(item.to)"
            >
              <RouterLink
                :to="item.to"
                class="group flex items-center gap-3 rounded-2xl border px-3 py-3 transition"
                :class="isRouteActive(item.to)
                  ? 'border-brand-aqua/30 bg-brand-aqua/12 text-white shadow-glow'
                  : 'border-white/6 bg-white/0 text-slate-300 hover:border-white/14 hover:bg-white/6 hover:text-white'"
              >
                <span
                  class="flex h-11 w-11 items-center justify-center rounded-2xl text-xs font-semibold tracking-[0.2em]"
                  :class="isRouteActive(item.to) ? 'bg-brand-aqua text-slate-950' : 'bg-slate-900 text-brand-aqua'"
                >
                  {{ item.short }}
                </span>
                <span class="min-w-0 flex-1 text-sm font-medium">{{ item.label }}</span>
                <span class="text-xs text-slate-500">拖曳</span>
              </RouterLink>

              <div v-if="item.children" class="space-y-2 pl-5">
                <div
                  v-for="child in item.children"
                  :key="child.to"
                  draggable="true"
                  @dragstart="handleChildDragStart(item.to, child.to)"
                  @dragend="draggingChild = null"
                  @dragover.stop.prevent
                  @drop.stop.prevent="handleChildDrop(item.to, child.to)"
                >
                  <RouterLink
                    :to="child.to"
                    class="group flex items-center gap-3 rounded-2xl border px-3 py-2.5 transition"
                    :class="route.path === child.to
                      ? 'border-amber-300/30 bg-amber-200/12 text-white'
                      : 'border-white/6 bg-white/0 text-slate-400 hover:border-white/14 hover:bg-white/6 hover:text-white'"
                  >
                    <span
                      class="flex h-9 w-9 items-center justify-center rounded-2xl text-[11px] font-semibold tracking-[0.18em]"
                      :class="route.path === child.to ? 'bg-amber-300 text-slate-950' : 'bg-slate-900 text-amber-200'"
                    >
                      {{ child.short }}
                    </span>
                    <span class="min-w-0 flex-1 text-sm font-medium">{{ child.label }}</span>
                    <span class="text-[11px] text-slate-500">拖曳</span>
                  </RouterLink>
                </div>
              </div>
            </div>
          </nav>

          <div class="mt-10 rounded-[1.5rem] border border-amber-300/15 bg-gradient-to-br from-amber-200/10 to-transparent p-4">
            <p class="text-xs uppercase tracking-[0.3em] text-amber-200/70">Session Pulse</p>
            <p class="mt-3 text-lg font-semibold text-white">{{ authStore.activeRole || "尚未登入" }}</p>
            <p class="mt-2 text-sm text-slate-400">
              {{ storeContextStore.selectedStore?.name || authStore.currentStoreCode || "尚未選擇門市" }}
              的即時工作階段資訊。
            </p>
          </div>
        </div>
      </aside>

      <main class="min-w-0 px-4 py-4 sm:px-5 sm:py-5 lg:px-6 xl:px-8 xl:py-6">
        <header class="mb-6 flex flex-col gap-4 rounded-[2rem] border border-white/10 bg-white/5 px-4 py-4 backdrop-blur-md md:px-5 md:py-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <button
              class="mb-4 inline-flex items-center gap-2 rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-slate-100 xl:hidden"
              type="button"
              @click="isNavOpen = true"
            >
              <span class="text-lg leading-none">≡</span>
              <span>選單</span>
            </button>
            <p class="text-xs uppercase tracking-[0.35em] text-brand-aqua/70">Operations Console</p>
            <h2 class="mt-2 text-2xl font-semibold text-white md:text-3xl">{{ route.meta.title || "管理後台" }}</h2>
            <p class="mt-2 max-w-2xl text-sm text-slate-400">{{ pageSubtitle }}</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2 2xl:grid-cols-3">
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 px-4 py-3">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Store Focus</p>
              <p class="mt-2 text-base font-semibold text-white">
                {{ storeContextStore.selectedStore?.name || authStore.currentStoreCode || "尚未選擇門市" }}
              </p>
              <p class="text-sm text-slate-400">{{ storeContextStore.selectedStore?.code || authStore.currentStoreCode || "-" }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 px-4 py-3">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Signed In</p>
              <p class="mt-2 text-base font-semibold text-white">{{ authStore.userDisplayName }}</p>
              <p class="text-sm text-slate-400">{{ authStore.session?.employeeCode || "尚未登入帳號" }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 px-4 py-3 sm:col-span-2 2xl:col-span-1">
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

        <div class="min-w-0">
          <slot />
        </div>
      </main>
    </div>
  </div>
</template>
