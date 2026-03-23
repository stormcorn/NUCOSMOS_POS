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
  const sales = reportStore.salesSummary;
  const summary = reportStore.inventoryAnalytics?.summary;

  return [
    {
      label: "淨營收",
      value: sales ? formatCurrency(sales.netSalesAmount) : "--",
      delta: sales ? `${sales.orderCount} 筆訂單` : "等待資料",
      tone: "mint" as const,
    },
    {
      label: "平均客單",
      value: sales ? formatCurrency(sales.averageOrderAmount) : "--",
      delta: sales ? `${sales.voidedOrderCount} 筆作廢` : "等待資料",
      tone: "amber" as const,
    },
    {
      label: "在線裝置",
      value: `${deviceStore.onlineCount} / ${deviceStore.devices.length || 0}`,
      delta: `${deviceStore.offlineCount} 台離線`,
      tone: "sky" as const,
    },
    {
      label: "低庫存項目",
      value: summary
        ? String(
            summary.productLowStockCount +
              summary.materialLowStockCount +
              summary.packagingLowStockCount,
          )
        : "--",
      delta: summary ? "商品 / 原料 / 包裝" : "等待資料",
      tone: "coral" as const,
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

const paymentMix = computed(() => {
  const sales = reportStore.salesSummary;
  if (!sales || sales.netSalesAmount <= 0) {
    return [];
  }

  return [
    {
      label: "現金",
      value: Number(sales.cashSalesAmount.toFixed(2)),
      color: "#22d3ee",
    },
    {
      label: "刷卡",
      value: Number(sales.cardSalesAmount.toFixed(2)),
      color: "#f59e0b",
    },
  ];
});

const categoryMixChart = computed(() =>
  productStore.categoryMix.map((item, index) => ({
    label: item.label,
    value: item.value,
    displayValue: `${item.value}%`,
    hint: "依商品數量占比",
    color: [
      "linear-gradient(90deg, rgba(34,211,238,0.95), rgba(45,212,191,0.9))",
      "linear-gradient(90deg, rgba(250,204,21,0.95), rgba(249,115,22,0.85))",
      "linear-gradient(90deg, rgba(129,140,248,0.95), rgba(59,130,246,0.9))",
      "linear-gradient(90deg, rgba(251,113,133,0.95), rgba(249,115,22,0.9))",
    ][index % 4],
  })),
);

const inventoryBreakdown = computed(() => {
  const summary = reportStore.inventoryAnalytics?.summary;
  if (!summary) {
    return [];
  }

  return [
    {
      label: "商品可售庫存",
      value: summary.totalSellableQuantity,
      displayValue: `${summary.totalSellableQuantity}`,
      hint: `低庫存 ${summary.productLowStockCount} 項`,
      color: "linear-gradient(90deg, rgba(34,211,238,0.95), rgba(45,212,191,0.9))",
    },
    {
      label: "商品瑕疵庫存",
      value: summary.totalDefectiveQuantity,
      displayValue: `${summary.totalDefectiveQuantity}`,
      hint: "待報廢或待處理",
      color: "linear-gradient(90deg, rgba(251,113,133,0.95), rgba(249,115,22,0.9))",
    },
    {
      label: "原料在手量",
      value: summary.totalMaterialQuantity,
      displayValue: `${summary.totalMaterialQuantity}`,
      hint: `低庫存 ${summary.materialLowStockCount} 項`,
      color: "linear-gradient(90deg, rgba(129,140,248,0.95), rgba(59,130,246,0.9))",
    },
    {
      label: "包裝在手量",
      value: summary.totalPackagingQuantity,
      displayValue: `${summary.totalPackagingQuantity}`,
      hint: `低庫存 ${summary.packagingLowStockCount} 項`,
      color: "linear-gradient(90deg, rgba(250,204,21,0.95), rgba(249,115,22,0.85))",
    },
  ];
});

const profitabilitySummaryCards = computed(() => {
  const summary = reportStore.profitabilityAnalysis?.costTransferSummary;
  if (!summary) {
    return [];
  }

  return [
    {
      label: "實際淨成本",
      value: formatCurrency(summary.realizedNetCogsAmount),
      helper: "依訂單實際結轉",
    },
    {
      label: "標準淨成本",
      value: formatCurrency(summary.standardNetCogsAmount),
      helper: "依目前配方與最新單位成本",
    },
    {
      label: "成本差異",
      value: formatCurrency(summary.cogsVarianceAmount),
      helper: formatPercent(summary.cogsVarianceRate),
    },
    {
      label: "實際毛利",
      value: formatCurrency(summary.realizedGrossProfitAmount),
      helper: formatPercent(summary.realizedGrossMarginRate),
    },
    {
      label: "標準毛利",
      value: formatCurrency(summary.standardGrossProfitAmount),
      helper: formatPercent(summary.standardGrossMarginRate),
    },
  ];
});

const topProductProfitChart = computed(() =>
  (reportStore.profitabilityAnalysis?.topProductsByGrossProfit ?? []).map((item, index) => ({
    label: item.name,
    value: item.realizedGrossProfitAmount,
    displayValue: formatCurrency(item.realizedGrossProfitAmount),
    hint: `${item.netQuantity} 份 / ${item.categoryName}`,
    color: [
      "linear-gradient(90deg, rgba(34,211,238,0.95), rgba(45,212,191,0.9))",
      "linear-gradient(90deg, rgba(129,140,248,0.95), rgba(59,130,246,0.9))",
      "linear-gradient(90deg, rgba(250,204,21,0.95), rgba(249,115,22,0.85))",
      "linear-gradient(90deg, rgba(251,113,133,0.95), rgba(249,115,22,0.9))",
    ][index % 4],
  })),
);

const lowMarginProductChart = computed(() =>
  (reportStore.profitabilityAnalysis?.lowestProductsByMargin ?? []).map((item, index) => ({
    label: item.name,
    value: Math.max(item.realizedGrossMarginRate, 0),
    displayValue: formatPercent(item.realizedGrossMarginRate),
    hint: `成本差異 ${formatCurrency(item.cogsVarianceAmount)}`,
    color: [
      "linear-gradient(90deg, rgba(251,113,133,0.95), rgba(249,115,22,0.9))",
      "linear-gradient(90deg, rgba(245,158,11,0.95), rgba(251,191,36,0.9))",
      "linear-gradient(90deg, rgba(129,140,248,0.95), rgba(59,130,246,0.9))",
    ][index % 3],
  })),
);

const categoryProfitChart = computed(() =>
  (reportStore.profitabilityAnalysis?.categoryProfitability ?? []).map((item, index) => ({
    label: item.categoryName,
    value: item.realizedGrossProfitAmount,
    displayValue: formatCurrency(item.realizedGrossProfitAmount),
    hint: `毛利率 ${formatPercent(item.realizedGrossMarginRate)}`,
    color: [
      "linear-gradient(90deg, rgba(34,211,238,0.95), rgba(45,212,191,0.9))",
      "linear-gradient(90deg, rgba(250,204,21,0.95), rgba(249,115,22,0.85))",
      "linear-gradient(90deg, rgba(129,140,248,0.95), rgba(59,130,246,0.9))",
      "linear-gradient(90deg, rgba(251,113,133,0.95), rgba(249,115,22,0.9))",
    ][index % 4],
  })),
);

const actionItems = computed(() => {
  const summary = reportStore.inventoryAnalytics?.summary;
  const costTransfer = reportStore.profitabilityAnalysis?.costTransferSummary;

  return [
    {
      title: "裝置狀態",
      content:
        deviceStore.offlineCount > 0
          ? `${deviceStore.offlineCount} 台 POS 裝置離線，建議先檢查網路與同步狀態。`
          : "目前所有 POS 裝置都在線。",
      tone: "border-brand-coral/20 bg-brand-coral/10",
    },
    {
      title: "成本結轉觀察",
      content: costTransfer
        ? `實際淨成本 ${formatCurrency(costTransfer.realizedNetCogsAmount)}，與標準淨成本差異 ${formatCurrency(costTransfer.cogsVarianceAmount)}。`
        : "尚未取得成本結轉資料。",
      tone: "border-brand-aqua/20 bg-brand-aqua/10",
    },
    {
      title: "庫存提醒",
      content: summary
        ? `商品 ${summary.productLowStockCount} 項、原料 ${summary.materialLowStockCount} 項、包裝 ${summary.packagingLowStockCount} 項低於補貨線。`
        : "尚未取得庫存分析資料。",
      tone: "border-amber-300/20 bg-amber-200/8",
    },
  ];
});

const expiringLots = computed(() => [
  ...(reportStore.inventoryAnalytics?.expiringMaterialLots ?? []),
  ...(reportStore.inventoryAnalytics?.expiringPackagingLots ?? []),
]);

function movementLabel(type: string) {
  return (
    {
      PURCHASE_IN: "進貨入庫",
      SALE_OUT: "銷貨出庫",
      REFUND_IN: "客退回庫",
      REFUND_DEFECT: "客退瑕疵",
      ADJUSTMENT_IN: "盤點調增",
      ADJUSTMENT_OUT: "盤點調減",
      DAMAGE_OUT: "損耗出庫",
      SCRAP_OUT: "報廢出庫",
      SAMPLE_OUT: "試吃試飲",
      PRODUCTION_CONSUME: "製作耗用",
      CONSUME_OUT: "一般耗用",
      RETURN_IN: "退回入庫",
    }[type] ?? type
  );
}

function varianceTone(value: number) {
  if (value > 0) {
    return "text-brand-coral";
  }
  if (value < 0) {
    return "text-brand-aqua";
  }
  return "text-slate-300";
}

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
            集中查看營收、成本結轉、毛利、庫存與到期風險，讓店務管理與決策能用同一頁完成。
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
            重新整理
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

    <div class="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5 flex items-center justify-between gap-4">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Sales Trend</p>
            <h3 class="mt-2 text-xl font-semibold text-white">營收趨勢</h3>
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

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Action Board</p>
        <h3 class="mt-2 text-xl font-semibold text-white">重點提醒</h3>
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

    <div class="grid gap-6 xl:grid-cols-3">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Payment Mix</p>
          <h3 class="mt-2 text-xl font-semibold text-white">收款結構</h3>
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
          <h3 class="mt-2 text-xl font-semibold text-white">商品分類占比</h3>
        </div>
        <HorizontalBarChart
          :items="categoryMixChart"
          :max="100"
          empty-text="目前沒有商品分類資料。"
        />
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Inventory Balance</p>
          <h3 class="mt-2 text-xl font-semibold text-white">庫存結構</h3>
        </div>
        <HorizontalBarChart
          :items="inventoryBreakdown"
          :max="Math.max(...inventoryBreakdown.map((item) => item.value), 0)"
          empty-text="目前沒有庫存結構資料。"
        />
      </article>
    </div>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Cost Transfer</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">成本結轉與毛利分析</h3>
          <p class="mt-2 text-sm text-slate-400">
            比較訂單實際結轉成本與標準配方成本，快速看出毛利落差與成本偏移來源。
          </p>
        </div>
        <div
          v-if="reportStore.profitabilityAnalysis"
          class="rounded-2xl border border-white/10 bg-white/4 px-4 py-3 text-sm text-slate-300"
        >
          區間：{{ formatDateTime(reportStore.profitabilityAnalysis.from) }} 至
          {{ formatDateTime(reportStore.profitabilityAnalysis.to) }}
        </div>
      </div>

      <div class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div
          v-for="card in profitabilitySummaryCards"
          :key="card.label"
          class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4"
        >
          <p class="text-sm text-slate-400">{{ card.label }}</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ card.value }}</p>
          <p class="mt-2 text-xs text-slate-500">{{ card.helper }}</p>
        </div>
        <div
          v-if="profitabilitySummaryCards.length === 0"
          class="rounded-[1.5rem] border border-dashed border-white/10 p-6 text-sm text-slate-400 md:col-span-2 xl:col-span-5"
        >
          目前沒有成本結轉分析資料。
        </div>
      </div>
    </article>

    <div class="grid gap-6 xl:grid-cols-3">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Top Products</p>
          <h3 class="mt-2 text-xl font-semibold text-white">高毛利商品</h3>
        </div>
        <HorizontalBarChart
          :items="topProductProfitChart"
          :max="Math.max(...topProductProfitChart.map((item) => item.value), 0)"
          empty-text="目前沒有商品毛利資料。"
        />
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Low Margin</p>
          <h3 class="mt-2 text-xl font-semibold text-white">低毛利商品</h3>
        </div>
        <HorizontalBarChart
          :items="lowMarginProductChart"
          :max="100"
          empty-text="目前沒有低毛利商品資料。"
        />
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Category Margin</p>
          <h3 class="mt-2 text-xl font-semibold text-white">分類毛利</h3>
        </div>
        <HorizontalBarChart
          :items="categoryProfitChart"
          :max="Math.max(...categoryProfitChart.map((item) => item.value), 0)"
          empty-text="目前沒有分類毛利資料。"
        />
      </article>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Products</p>
          <h3 class="mt-2 text-xl font-semibold text-white">商品毛利明細</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full text-sm">
            <thead class="text-left text-slate-500">
              <tr>
                <th class="pb-3 pr-4">商品</th>
                <th class="pb-3 pr-4">淨銷量</th>
                <th class="pb-3 pr-4">淨營收</th>
                <th class="pb-3 pr-4">實際毛利</th>
                <th class="pb-3 pr-4">毛利率</th>
                <th class="pb-3">成本差異</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-white/8 text-slate-200">
              <tr
                v-for="item in reportStore.profitabilityAnalysis?.topProductsByGrossProfit ?? []"
                :key="item.productId"
              >
                <td class="py-3 pr-4">
                  <p class="font-medium text-white">{{ item.name }}</p>
                  <p class="mt-1 text-xs text-slate-500">{{ item.sku }} / {{ item.categoryName }}</p>
                </td>
                <td class="py-3 pr-4">{{ item.netQuantity }}</td>
                <td class="py-3 pr-4">{{ formatCurrency(item.netSalesAmount) }}</td>
                <td class="py-3 pr-4">{{ formatCurrency(item.realizedGrossProfitAmount) }}</td>
                <td class="py-3 pr-4">{{ formatPercent(item.realizedGrossMarginRate) }}</td>
                <td class="py-3" :class="varianceTone(item.cogsVarianceAmount)">
                  {{ formatCurrency(item.cogsVarianceAmount) }}
                </td>
              </tr>
              <tr v-if="(reportStore.profitabilityAnalysis?.topProductsByGrossProfit.length ?? 0) === 0">
                <td colspan="6" class="py-6 text-center text-slate-400">目前沒有商品毛利資料。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="mb-5">
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Orders</p>
          <h3 class="mt-2 text-xl font-semibold text-white">訂單毛利明細</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full text-sm">
            <thead class="text-left text-slate-500">
              <tr>
                <th class="pb-3 pr-4">訂單</th>
                <th class="pb-3 pr-4">品項數</th>
                <th class="pb-3 pr-4">淨營收</th>
                <th class="pb-3 pr-4">實際毛利</th>
                <th class="pb-3 pr-4">毛利率</th>
                <th class="pb-3">成本差異</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-white/8 text-slate-200">
              <tr
                v-for="item in reportStore.profitabilityAnalysis?.topOrdersByGrossProfit ?? []"
                :key="item.orderId"
              >
                <td class="py-3 pr-4">
                  <p class="font-medium text-white">{{ item.orderNumber }}</p>
                  <p class="mt-1 text-xs text-slate-500">{{ formatDateTime(item.orderedAt) }}</p>
                </td>
                <td class="py-3 pr-4">{{ item.itemCount }}</td>
                <td class="py-3 pr-4">{{ formatCurrency(item.netSalesAmount) }}</td>
                <td class="py-3 pr-4">{{ formatCurrency(item.realizedGrossProfitAmount) }}</td>
                <td class="py-3 pr-4">{{ formatPercent(item.realizedGrossMarginRate) }}</td>
                <td class="py-3" :class="varianceTone(item.cogsVarianceAmount)">
                  {{ formatCurrency(item.cogsVarianceAmount) }}
                </td>
              </tr>
              <tr v-if="(reportStore.profitabilityAnalysis?.topOrdersByGrossProfit.length ?? 0) === 0">
                <td colspan="6" class="py-6 text-center text-slate-400">目前沒有訂單毛利資料。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </div>

    <div class="grid gap-6 xl:grid-cols-3">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Low Stock</p>
        <h3 class="mt-2 text-xl font-semibold text-white">商品低庫存</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.lowStockProducts ?? []"
            :key="`product-${item.sku}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <p class="font-medium text-white">{{ item.name }}</p>
            <p class="mt-1 text-xs text-slate-400">{{ item.sku }} / {{ item.secondaryLabel || "未分類" }}</p>
            <p class="mt-2 text-sm text-slate-300">
              現有 {{ item.quantityOnHand }} {{ item.unit }}，補貨線 {{ item.reorderLevel }} {{ item.unit }}
            </p>
          </div>
          <p v-if="(reportStore.inventoryAnalytics?.lowStockProducts.length ?? 0) === 0" class="text-sm text-slate-400">
            目前沒有商品低庫存項目。
          </p>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Consumption</p>
        <h3 class="mt-2 text-xl font-semibold text-white">原料耗用</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="item in reportStore.inventoryAnalytics?.materialConsumption ?? []"
            :key="`material-${item.sku}`"
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
          <p v-if="(reportStore.inventoryAnalytics?.materialConsumption.length ?? 0) === 0" class="text-sm text-slate-400">
            目前沒有原料耗用資料。
          </p>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Expiring Lots</p>
        <h3 class="mt-2 text-xl font-semibold text-white">批號效期</h3>
        <div class="mt-4 space-y-3">
          <div
            v-for="lot in expiringLots"
            :key="`${lot.scope}-${lot.sku}-${lot.batchCode}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-white">{{ lot.name }}</p>
                <p class="mt-1 text-xs text-slate-400">
                  {{ lot.sku }} / 批號 {{ lot.batchCode || "未設定" }}
                </p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold text-white">{{ lot.remainingQuantity }} {{ lot.unit }}</p>
                <p class="mt-1 text-xs text-brand-amber">{{ lot.daysUntilExpiry }} 天內到期</p>
              </div>
            </div>
            <p class="mt-3 text-xs text-slate-400">到期時間：{{ formatDateTime(lot.expiryDate) }}</p>
          </div>
          <p v-if="expiringLots.length === 0" class="text-sm text-slate-400">
            目前沒有近期到期批號。
          </p>
        </div>
      </article>
    </div>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="mb-5">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Waste Analysis</p>
        <h3 class="mt-2 text-xl font-semibold text-white">瑕疵與損耗</h3>
      </div>
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div
          v-for="item in reportStore.inventoryAnalytics?.defectiveAndWaste ?? []"
          :key="`${item.sku}-${item.movementType}`"
          class="rounded-2xl border border-white/8 bg-white/4 p-4"
        >
          <p class="font-medium text-white">{{ item.name }}</p>
          <p class="mt-1 text-xs text-slate-400">{{ item.sku }}</p>
          <div class="mt-3 flex items-center justify-between gap-3">
            <span class="text-sm text-slate-300">{{ movementLabel(item.movementType) }}</span>
            <span class="text-sm font-semibold text-brand-coral">{{ item.affectedQuantity }}</span>
          </div>
        </div>
        <p
          v-if="(reportStore.inventoryAnalytics?.defectiveAndWaste.length ?? 0) === 0"
          class="rounded-2xl border border-dashed border-white/10 px-4 py-6 text-sm text-slate-400 md:col-span-2 xl:col-span-4"
        >
          目前沒有瑕疵與損耗資料。
        </p>
      </div>
    </article>
  </section>
</template>
