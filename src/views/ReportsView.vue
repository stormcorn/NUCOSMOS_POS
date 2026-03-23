<script setup lang="ts">
import { computed, onMounted, reactive, watch } from "vue";

import DonutChart from "@/components/dashboard/DonutChart.vue";
import HorizontalBarChart from "@/components/dashboard/HorizontalBarChart.vue";
import LineChart from "@/components/dashboard/LineChart.vue";
import StatCard from "@/components/dashboard/StatCard.vue";
import { useDeviceStore } from "@/stores/devices";
import { useProductStore } from "@/stores/products";
import { useReportStore } from "@/stores/reports";
import { useStoreContextStore } from "@/stores/store-context";
import { formatCurrency, formatDateTime, formatPercent } from "@/utils/format";

const deviceStore = useDeviceStore();
const productStore = useProductStore();
const reportStore = useReportStore();
const storeContextStore = useStoreContextStore();

function todayRange() {
  const now = new Date();
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  const end = new Date(now);
  end.setHours(23, 59, 59, 999);

  return {
    from: start.toISOString().slice(0, 16),
    to: end.toISOString().slice(0, 16),
  };
}

const quickRange = todayRange();
const filters = reactive({
  from: quickRange.from,
  to: quickRange.to,
});

const overviewCards = computed(() => {
  const summary = reportStore.salesSummary;
  const inventorySummary = reportStore.inventoryAnalytics?.summary;

  return [
    {
      label: "今日淨營收",
      value: summary ? formatCurrency(summary.netSalesAmount) : "--",
      delta: summary ? `${summary.orderCount} 筆訂單` : "等待資料",
      tone: "mint" as const,
    },
    {
      label: "平均客單價",
      value: summary ? formatCurrency(summary.averageOrderAmount) : "--",
      delta: summary ? `作廢 ${summary.voidedOrderCount} 筆` : "等待資料",
      tone: "amber" as const,
    },
    {
      label: "裝置在線",
      value: `${deviceStore.onlineCount} / ${deviceStore.devices.length || 0}`,
      delta: `${deviceStore.offlineCount} 台離線`,
      tone: "sky" as const,
    },
    {
      label: "低庫存總數",
      value: inventorySummary
        ? String(
            inventorySummary.productLowStockCount
              + inventorySummary.materialLowStockCount
              + inventorySummary.packagingLowStockCount,
          )
        : "--",
      delta: inventorySummary ? "商品、原料、包材合計" : "等待資料",
      tone: "coral" as const,
    },
  ];
});

const categoryMix = computed(() => productStore.categoryMix.slice());

const paymentMix = computed(() => {
  const summary = reportStore.salesSummary;

  if (!summary || summary.netSalesAmount <= 0) {
    return [];
  }

  return [
    {
      label: "現金",
      value: Number(summary.cashSalesAmount.toFixed(2)),
      color: "#22d3ee",
    },
    {
      label: "刷卡",
      value: Number(summary.cardSalesAmount.toFixed(2)),
      color: "#f59e0b",
    },
  ];
});

const salesTrendChartPoints = computed(() =>
  (reportStore.salesTrend?.points ?? []).map((point) => ({
    label: point.bucketLabel,
    value: point.netSalesAmount,
    displayValue: formatCurrency(point.netSalesAmount),
  })),
);

const inventoryBreakdown = computed(() => {
  const summary = reportStore.inventoryAnalytics?.summary;
  if (!summary) {
    return [];
  }

  return [
    {
      label: "可售商品庫存",
      value: summary.totalSellableQuantity,
      displayValue: `${summary.totalSellableQuantity}`,
      hint: `商品低庫存 ${summary.productLowStockCount} 項`,
      color: "linear-gradient(90deg, rgba(34,211,238,0.95), rgba(45,212,191,0.9))",
    },
    {
      label: "瑕疵商品庫存",
      value: summary.totalDefectiveQuantity,
      displayValue: `${summary.totalDefectiveQuantity}`,
      hint: "待報廢或待處理",
      color: "linear-gradient(90deg, rgba(251,113,133,0.95), rgba(249,115,22,0.9))",
    },
    {
      label: "原料庫存總量",
      value: summary.totalMaterialQuantity,
      displayValue: `${summary.totalMaterialQuantity}`,
      hint: `原料低庫存 ${summary.materialLowStockCount} 項`,
      color: "linear-gradient(90deg, rgba(129,140,248,0.95), rgba(59,130,246,0.9))",
    },
    {
      label: "包裝庫存總量",
      value: summary.totalPackagingQuantity,
      displayValue: `${summary.totalPackagingQuantity}`,
      hint: `包裝低庫存 ${summary.packagingLowStockCount} 項`,
      color: "linear-gradient(90deg, rgba(250,204,21,0.95), rgba(249,115,22,0.85))",
    },
  ];
});

function movementLabel(type: string) {
  return (
    {
      PURCHASE_IN: "進貨入庫",
      SALE_OUT: "銷貨出庫",
      REFUND_IN: "客退回庫",
      REFUND_DEFECT: "客退瑕疵",
      ADJUSTMENT_IN: "盤點調增",
      ADJUSTMENT_OUT: "盤點調減",
      DAMAGE_OUT: "門市損耗",
      SCRAP_OUT: "報廢出庫",
      SAMPLE_OUT: "試吃試飲",
      PRODUCTION_CONSUME: "生產耗用",
      CONSUME_OUT: "耗用出庫",
      RETURN_IN: "回庫入庫",
    }[type] ?? type
  );
}

const topMovementChartItems = computed(() => {
  const items = [
    ...(reportStore.inventoryAnalytics?.productMovementTotals ?? []),
    ...(reportStore.inventoryAnalytics?.materialMovementTotals ?? []),
    ...(reportStore.inventoryAnalytics?.packagingMovementTotals ?? []),
  ];

  return items
    .slice()
    .sort((left, right) => right.totalQuantity - left.totalQuantity)
    .slice(0, 6)
    .map((item) => ({
      label: `${item.scope} ${movementLabel(item.movementType)}`,
      value: item.totalQuantity,
      displayValue: `${item.totalQuantity}`,
      hint: `筆數 ${item.entryCount} / 淨變化 ${item.netDelta}`,
    }));
});

const salesCards = computed(() => {
  const summary = reportStore.salesSummary;
  if (!summary) {
    return [];
  }

  return [
    { label: "毛營收", value: formatCurrency(summary.grossSalesAmount) },
    { label: "退款金額", value: formatCurrency(summary.refundedAmount) },
    { label: "淨營收", value: formatCurrency(summary.netSalesAmount) },
    { label: "平均客單價", value: formatCurrency(summary.averageOrderAmount) },
  ];
});

const profitCards = computed(() => {
  const summary = reportStore.salesSummary;
  if (!summary) {
    return [];
  }

  return [
    { label: "銷貨成本", value: formatCurrency(summary.cogsAmount) },
    { label: "退款成本", value: formatCurrency(summary.refundedCogsAmount) },
    { label: "淨成本", value: formatCurrency(summary.netCogsAmount) },
    { label: "毛利", value: formatCurrency(summary.grossProfitAmount) },
    { label: "毛利率", value: formatPercent(summary.grossMarginRate) },
  ];
});

const inventoryCards = computed(() => {
  const summary = reportStore.inventoryAnalytics?.summary;
  if (!summary) {
    return [];
  }

  return [
    { label: "商品 SKU", value: `${summary.productSkuCount}` },
    { label: "商品低庫存", value: `${summary.productLowStockCount}` },
    { label: "可售庫存總量", value: `${summary.totalSellableQuantity}` },
    { label: "瑕疵庫存總量", value: `${summary.totalDefectiveQuantity}` },
    { label: "原料 SKU", value: `${summary.materialSkuCount}` },
    { label: "原料低庫存", value: `${summary.materialLowStockCount}` },
    { label: "原料總量", value: `${summary.totalMaterialQuantity}` },
    { label: "包裝 SKU", value: `${summary.packagingSkuCount}` },
    { label: "包裝低庫存", value: `${summary.packagingLowStockCount}` },
    { label: "包裝總量", value: `${summary.totalPackagingQuantity}` },
  ];
});

const actionItems = computed(() => {
  const inventorySummary = reportStore.inventoryAnalytics?.summary;

  return [
    {
      title: "裝置監控",
      content:
        deviceStore.offlineCount > 0
          ? `${deviceStore.offlineCount} 台裝置離線，建議優先檢查門市網路與平板狀態。`
          : "所有 POS 裝置目前都在線。",
      tone: "border-brand-coral/20 bg-brand-coral/10",
    },
    {
      title: "商品結構",
      content: `目前共有 ${productStore.products.length} 個商品，分佈在 ${productStore.categories.length} 個分類。`,
      tone: "border-amber-300/20 bg-amber-200/8",
    },
    {
      title: "收款結構",
      content:
        paymentMix.value.length > 0
          ? paymentMix.value
              .map((item) => {
                const total = paymentMix.value.reduce((sum, entry) => sum + entry.value, 0);
                const percent = total > 0 ? Math.round((item.value / total) * 100) : 0;
                return `${item.label} ${percent}%`;
              })
              .join(" / ")
          : "目前篩選區間內尚無銷售資料。",
      tone: "border-sky-300/20 bg-sky-300/8",
    },
    {
      title: "庫存提醒",
      content: inventorySummary
        ? `低庫存共 ${inventorySummary.productLowStockCount + inventorySummary.materialLowStockCount + inventorySummary.packagingLowStockCount} 項，請留意補貨建議。`
        : "尚未取得庫存分析資料。",
      tone: "border-brand-aqua/20 bg-brand-aqua/10",
    },
  ];
});

async function refresh() {
  const from = new Date(filters.from).toISOString();
  const to = new Date(filters.to).toISOString();

  await Promise.all([
    reportStore.loadAllReports(from, to),
    productStore.loadCatalog(),
    storeContextStore.loadStores(),
  ]);

  await deviceStore.loadDevices(storeContextStore.selectedStoreCode || undefined);
}

onMounted(() => {
  void refresh();
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
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Operations Overview</p>
          <h2 class="mt-2 text-2xl font-semibold text-white">營運總覽</h2>
          <p class="mt-2 max-w-3xl text-sm text-slate-400">
            這裡整合首頁摘要與分析報表，先看今日營運，再往下查看營收趨勢、收款結構、商品分佈、毛利、庫存與效期風險。
          </p>
        </div>

        <div class="flex flex-wrap gap-3">
          <label class="block">
            <span class="mb-2 block text-xs uppercase tracking-[0.24em] text-slate-500">From</span>
            <input
              v-model="filters.from"
              type="datetime-local"
              class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
            />
          </label>
          <label class="block">
            <span class="mb-2 block text-xs uppercase tracking-[0.24em] text-slate-500">To</span>
            <input
              v-model="filters.to"
              type="datetime-local"
              class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
            />
          </label>
          <button
            class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110"
            @click="refresh"
          >
            重新整理報表
          </button>
        </div>
      </div>

      <p
        v-if="reportStore.errorMessage || productStore.errorMessage || deviceStore.errorMessage || storeContextStore.errorMessage"
        class="mt-4 text-sm text-brand-coral"
      >
        {{ reportStore.errorMessage || productStore.errorMessage || deviceStore.errorMessage || storeContextStore.errorMessage }}
      </p>
    </article>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <StatCard
        v-for="card in overviewCards"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :delta="card.delta"
        :tone="card.tone"
      />
    </div>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="mb-5 flex items-center justify-between gap-4">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Sales Trend</p>
          <h3 class="mt-2 text-xl font-semibold text-white">營收趨勢圖</h3>
        </div>
        <div class="rounded-full border border-white/10 px-3 py-1 text-xs text-slate-400">
          {{ reportStore.salesTrend?.granularity ?? "DAY" }}
        </div>
      </div>
      <LineChart
        :points="salesTrendChartPoints"
        stroke-color="#22d3ee"
        fill-color="rgba(34,211,238,0.28)"
        empty-text="目前沒有營收趨勢資料。"
      />
    </article>

    <div class="grid gap-6 xl:grid-cols-[1.05fr_1fr_0.95fr]">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Payment Mix</p>
          <h3 class="mt-2 text-xl font-semibold text-white">收款結構圖</h3>
        </div>
        <DonutChart
          :segments="paymentMix"
          center-title="淨營收"
          :center-value="reportStore.salesSummary ? formatCurrency(reportStore.salesSummary.netSalesAmount) : '--'"
        />
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Category Mix</p>
          <h3 class="mt-2 text-xl font-semibold text-white">商品分類分佈</h3>
        </div>
        <HorizontalBarChart
          :items="categoryMix.map((item, index) => ({
            label: item.label,
            value: item.value,
            displayValue: `${item.value}%`,
            hint: '依商品數量占比',
            color: [
              'linear-gradient(90deg, rgba(34,211,238,0.95), rgba(45,212,191,0.9))',
              'linear-gradient(90deg, rgba(250,204,21,0.95), rgba(249,115,22,0.85))',
              'linear-gradient(90deg, rgba(129,140,248,0.95), rgba(59,130,246,0.9))',
              'linear-gradient(90deg, rgba(251,113,133,0.95), rgba(249,115,22,0.9))',
            ][index % 4],
          }))"
          :max="100"
          empty-text="目前沒有商品分類分佈資料。"
        />
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Action Board</p>
        <h3 class="mt-2 text-xl font-semibold text-white">營運提醒</h3>

        <div class="mt-6 space-y-4">
          <div
            v-for="item in actionItems"
            :key="item.title"
            class="rounded-[1.5rem] border p-4"
            :class="item.tone"
          >
            <p class="text-sm font-semibold text-white">{{ item.title }}</p>
            <p class="mt-2 text-sm text-slate-300">{{ item.content }}</p>
          </div>
        </div>
      </article>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Inventory Balance</p>
          <h3 class="mt-2 text-xl font-semibold text-white">庫存結構圖</h3>
        </div>
        <HorizontalBarChart
          :items="inventoryBreakdown"
          :max="Math.max(...inventoryBreakdown.map((item) => item.value), 0)"
          empty-text="目前沒有庫存結構資料。"
        />
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Movement Pulse</p>
          <h3 class="mt-2 text-xl font-semibold text-white">主要異動分佈</h3>
        </div>
        <HorizontalBarChart
          :items="topMovementChartItems"
          :max="Math.max(...topMovementChartItems.map((item) => item.value), 0)"
          empty-text="目前沒有可顯示的異動資料。"
        />
      </article>
    </div>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article
        v-for="card in salesCards"
        :key="card.label"
        class="rounded-[1.75rem] border border-white/10 bg-slate-950/55 p-5 shadow-soft shadow-black/20"
      >
        <p class="text-xs uppercase tracking-[0.28em] text-slate-500">{{ card.label }}</p>
        <p class="mt-4 text-3xl font-semibold text-white">{{ card.value }}</p>
      </article>
      <article
        v-if="salesCards.length === 0"
        class="rounded-[1.75rem] border border-white/10 bg-slate-950/55 p-5 text-sm text-slate-400 md:col-span-2 xl:col-span-4"
      >
        {{ reportStore.loading ? "報表載入中..." : "目前沒有可顯示的銷售資料。" }}
      </article>
    </div>

    <article
      v-if="reportStore.salesSummary"
      class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20"
    >
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Profitability</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">毛利分析</h3>

      <div class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div v-for="card in profitCards" :key="card.label" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">{{ card.label }}</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ card.value }}</p>
        </div>
      </div>

      <div class="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">現金營收</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(reportStore.salesSummary.cashSalesAmount) }}</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">刷卡營收</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(reportStore.salesSummary.cardSalesAmount) }}</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">訂單數</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ reportStore.salesSummary.orderCount }}</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">作廢訂單</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ reportStore.salesSummary.voidedOrderCount }}</p>
        </div>
      </div>

      <div class="mt-4 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
        統計區間：{{ formatDateTime(reportStore.salesSummary.from) }} 至 {{ formatDateTime(reportStore.salesSummary.to) }}
      </div>
    </article>

    <article
      v-if="inventoryCards.length > 0"
      class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20"
    >
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Inventory KPI</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">庫存 KPI</h3>

      <div class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div v-for="card in inventoryCards" :key="card.label" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">{{ card.label }}</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ card.value }}</p>
        </div>
      </div>
    </article>

    <div class="grid gap-6 xl:grid-cols-3">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Low Stock</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">商品低庫存</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.lowStockProducts ?? []"
            :key="`product-${item.sku}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <p class="font-medium text-white">{{ item.name }}</p>
            <p class="mt-1 text-xs text-slate-400">{{ item.sku }} / {{ item.secondaryLabel || "未提供分類" }}</p>
            <p class="mt-2 text-sm text-slate-300">目前 {{ item.quantityOnHand }} {{ item.unit }}，補貨門檻 {{ item.reorderLevel }} {{ item.unit }}</p>
          </div>
          <p v-if="(reportStore.inventoryAnalytics?.lowStockProducts.length ?? 0) === 0" class="text-sm text-slate-400">目前沒有商品低庫存項目。</p>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Low Stock</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">原料低庫存</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.lowStockMaterials ?? []"
            :key="`material-${item.sku}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <p class="font-medium text-white">{{ item.name }}</p>
            <p class="mt-1 text-xs text-slate-400">{{ item.sku }}</p>
            <p class="mt-2 text-sm text-slate-300">目前 {{ item.quantityOnHand }} {{ item.unit }}，補貨門檻 {{ item.reorderLevel }} {{ item.unit }}</p>
          </div>
          <p v-if="(reportStore.inventoryAnalytics?.lowStockMaterials.length ?? 0) === 0" class="text-sm text-slate-400">目前沒有原料低庫存項目。</p>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Low Stock</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">包裝低庫存</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.lowStockPackaging ?? []"
            :key="`packaging-${item.sku}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <p class="font-medium text-white">{{ item.name }}</p>
            <p class="mt-1 text-xs text-slate-400">{{ item.sku }}</p>
            <p class="mt-2 text-sm text-slate-300">目前 {{ item.quantityOnHand }} {{ item.unit }}，補貨門檻 {{ item.reorderLevel }} {{ item.unit }}</p>
          </div>
          <p v-if="(reportStore.inventoryAnalytics?.lowStockPackaging.length ?? 0) === 0" class="text-sm text-slate-400">目前沒有包裝低庫存項目。</p>
        </div>
      </article>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Consumption</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">原料耗用</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.materialConsumption ?? []"
            :key="`material-consume-${item.sku}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-white">{{ item.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ item.sku }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold text-white">{{ item.consumedQuantity }} {{ item.unit }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ formatCurrency(item.consumedCost) }}</p>
              </div>
            </div>
          </div>
          <p v-if="(reportStore.inventoryAnalytics?.materialConsumption.length ?? 0) === 0" class="text-sm text-slate-400">目前沒有原料耗用資料。</p>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Consumption</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">包裝耗用</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.packagingConsumption ?? []"
            :key="`packaging-consume-${item.sku}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-white">{{ item.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ item.sku }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold text-white">{{ item.consumedQuantity }} {{ item.unit }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ formatCurrency(item.consumedCost) }}</p>
              </div>
            </div>
          </div>
          <p v-if="(reportStore.inventoryAnalytics?.packagingConsumption.length ?? 0) === 0" class="text-sm text-slate-400">目前沒有包裝耗用資料。</p>
        </div>
      </article>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Defective & Waste</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">瑕疵與報廢</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.defectiveAndWaste ?? []"
            :key="`${item.sku}-${item.movementType}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-white">{{ item.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ item.sku }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold text-brand-coral">{{ item.affectedQuantity }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ movementLabel(item.movementType) }}</p>
              </div>
            </div>
          </div>
          <p v-if="(reportStore.inventoryAnalytics?.defectiveAndWaste.length ?? 0) === 0" class="text-sm text-slate-400">目前沒有瑕疵或報廢資料。</p>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Expiring Lots</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">即將到期批號</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="lot in [
              ...(reportStore.inventoryAnalytics?.expiringMaterialLots ?? []),
              ...(reportStore.inventoryAnalytics?.expiringPackagingLots ?? []),
            ]"
            :key="`${lot.scope}-${lot.sku}-${lot.batchCode}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-white">{{ lot.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ lot.sku }} / 批號 {{ lot.batchCode || "未指定" }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold text-white">{{ lot.remainingQuantity }} {{ lot.unit }}</p>
                <p class="mt-1 text-xs text-brand-amber">{{ lot.daysUntilExpiry }} 天後到期</p>
              </div>
            </div>
            <p class="mt-3 text-xs text-slate-400">到期時間：{{ formatDateTime(lot.expiryDate) }}</p>
          </div>
          <p
            v-if="
              ((reportStore.inventoryAnalytics?.expiringMaterialLots.length ?? 0)
                + (reportStore.inventoryAnalytics?.expiringPackagingLots.length ?? 0)) === 0
            "
            class="text-sm text-slate-400"
          >
            目前沒有即將到期的批號。
          </p>
        </div>
      </article>
    </div>
  </section>
</template>
