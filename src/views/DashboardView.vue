<script setup lang="ts">
import { computed, onMounted, watch } from "vue";

import StatCard from "@/components/dashboard/StatCard.vue";
import { useDeviceStore } from "@/stores/devices";
import { useOrderStore } from "@/stores/orders";
import { useProductStore } from "@/stores/products";
import { useReportStore } from "@/stores/reports";
import { useStoreContextStore } from "@/stores/store-context";
import { formatCurrency, formatPercent } from "@/utils/format";

const deviceStore = useDeviceStore();
const orderStore = useOrderStore();
const productStore = useProductStore();
const reportStore = useReportStore();
const storeContextStore = useStoreContextStore();

const dashboardStats = computed(() => {
  const summary = reportStore.salesSummary;

  return [
    {
      label: "今日淨營收",
      value: summary ? formatCurrency(summary.netSalesAmount) : "--",
      delta: summary ? `${summary.orderCount} 單完成` : "等待資料",
      tone: "mint" as const,
    },
    {
      label: "有效訂單",
      value: summary ? String(summary.orderCount) : String(orderStore.items.length),
      delta: summary ? `作廢 ${summary.voidedOrderCount} 單` : "來自訂單列表",
      tone: "amber" as const,
    },
    {
      label: "連線裝置",
      value: `${deviceStore.onlineCount} / ${deviceStore.devices.length || 0}`,
      delta: `${deviceStore.offlineCount} 台需巡檢`,
      tone: "sky" as const,
    },
    {
      label: "商品分類覆蓋",
      value: `${productStore.categories.length}`,
      delta: `${productStore.products.length} 項商品`,
      tone: "coral" as const,
    },
  ];
});

const categoryMix = computed(() => {
  if (productStore.categoryMix.length > 0) {
    return productStore.categoryMix;
  }

  return [{ label: "尚未載入", value: 0 }];
});

const paymentMix = computed(() => {
  const summary = reportStore.salesSummary;

  if (!summary || summary.netSalesAmount <= 0) {
    return [];
  }

  const cashShare = (summary.cashSalesAmount / summary.netSalesAmount) * 100;
  const cardShare = (summary.cardSalesAmount / summary.netSalesAmount) * 100;

  return [
    { label: "現金", value: formatPercent(cashShare) },
    { label: "刷卡", value: formatPercent(cardShare) },
    { label: "平均客單", value: formatCurrency(summary.averageOrderAmount) },
  ];
});

async function loadDashboard() {
  await Promise.all([
    reportStore.loadSalesSummary(),
    productStore.loadCatalog(),
    orderStore.loadOrders(),
    storeContextStore.loadStores(),
  ]);

  await deviceStore.loadDevices(storeContextStore.selectedStoreCode || undefined);
}

onMounted(() => {
  void loadDashboard();
});

watch(
  () => storeContextStore.selectedStoreCode,
  (storeCode) => {
    if (storeCode) {
      void deviceStore.loadDevices(storeCode);
    }
  },
);
</script>

<template>
  <section class="space-y-6">
    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <StatCard
        v-for="card in dashboardStats"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :delta="card.delta"
        :tone="card.tone"
      />
    </div>

    <div class="grid gap-6 xl:grid-cols-[1.25fr_0.95fr]">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Sales Composition</p>
            <h3 class="mt-2 text-xl font-semibold text-white">品類與營收節奏</h3>
          </div>
          <div class="rounded-full border border-white/10 px-3 py-1 text-xs text-slate-400">
            Live from backend
          </div>
        </div>

        <div class="mt-8 space-y-4">
          <div v-for="item in categoryMix" :key="item.label">
            <div class="mb-2 flex items-center justify-between text-sm">
              <span class="text-slate-300">{{ item.label }}</span>
              <span class="text-white">{{ item.value }}%</span>
            </div>
            <div class="h-3 rounded-full bg-white/6">
              <div
                class="h-3 rounded-full bg-gradient-to-r from-brand-aqua via-teal-300 to-brand-amber animate-pulse-line"
                :style="{ width: `${item.value}%` }"
              />
            </div>
          </div>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Action Board</p>
        <h3 class="mt-2 text-xl font-semibold text-white">今日優先處理</h3>

        <div class="mt-6 space-y-4">
          <div class="rounded-[1.5rem] border border-brand-coral/20 bg-brand-coral/10 p-4">
            <p class="text-sm font-semibold text-white">裝置巡檢</p>
            <p class="mt-2 text-sm text-slate-300">{{ deviceStore.offlineCount }} 台裝置目前不在 ACTIVE 狀態。</p>
          </div>
          <div class="rounded-[1.5rem] border border-amber-300/20 bg-amber-200/8 p-4">
            <p class="text-sm font-semibold text-white">商品結構</p>
            <p class="mt-2 text-sm text-slate-300">目前已載入 {{ productStore.products.length }} 項商品與 {{ productStore.categories.length }} 個分類。</p>
          </div>
          <div class="rounded-[1.5rem] border border-sky-300/20 bg-sky-300/8 p-4">
            <p class="text-sm font-semibold text-white">付款結構</p>
            <p class="mt-2 text-sm text-slate-300">
              <span v-if="paymentMix.length > 0">
                {{ paymentMix.map((item) => `${item.label} ${item.value}`).join(" ・ ") }}
              </span>
              <span v-else>報表資料載入後會顯示現金與刷卡占比。</span>
            </p>
          </div>
        </div>

        <p v-if="reportStore.errorMessage || productStore.errorMessage || deviceStore.errorMessage" class="mt-6 text-sm text-brand-coral">
          {{ reportStore.errorMessage || productStore.errorMessage || deviceStore.errorMessage }}
        </p>
      </article>
    </div>
  </section>
</template>
