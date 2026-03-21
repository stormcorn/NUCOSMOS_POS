<script setup lang="ts">
import { computed, onMounted, reactive } from "vue";

import { useReportStore } from "@/stores/reports";
import { formatCurrency, formatDateTime } from "@/utils/format";

const reportStore = useReportStore();

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

const cards = computed(() => {
  const summary = reportStore.salesSummary;
  if (!summary) {
    return [];
  }

  return [
    { label: "毛營收", value: formatCurrency(summary.grossSalesAmount) },
    { label: "退款金額", value: formatCurrency(summary.refundedAmount) },
    { label: "淨營收", value: formatCurrency(summary.netSalesAmount) },
    { label: "平均客單", value: formatCurrency(summary.averageOrderAmount) },
  ];
});

async function refresh() {
  await reportStore.loadSalesSummary(new Date(filters.from).toISOString(), new Date(filters.to).toISOString());
}

onMounted(() => {
  void refresh();
});
</script>

<template>
  <section class="space-y-6">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Sales Summary</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">營收報表</h3>
          <p class="mt-2 text-sm text-slate-400">這一頁已接上 `GET /api/v1/reports/sales-summary`。</p>
        </div>

        <div class="flex flex-wrap gap-3">
          <label class="block">
            <span class="mb-2 block text-xs uppercase tracking-[0.24em] text-slate-500">From</span>
            <input v-model="filters.from" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-xs uppercase tracking-[0.24em] text-slate-500">To</span>
            <input v-model="filters.to" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none" />
          </label>
          <button class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110" @click="refresh">
            套用查詢
          </button>
        </div>
      </div>
    </article>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article v-for="card in cards" :key="card.label" class="rounded-[1.75rem] border border-white/10 bg-slate-950/55 p-5 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-slate-500">{{ card.label }}</p>
        <p class="mt-4 text-3xl font-semibold text-white">{{ card.value }}</p>
      </article>
      <article v-if="cards.length === 0" class="rounded-[1.75rem] border border-white/10 bg-slate-950/55 p-5 text-sm text-slate-400 md:col-span-2 xl:col-span-4">
        {{ reportStore.loading ? "報表資料載入中..." : "尚未取得報表資料。" }}
      </article>
    </div>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Breakdown</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">付款結構與期間資訊</h3>

      <div v-if="reportStore.salesSummary" class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">現金營收</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(reportStore.salesSummary.cashSalesAmount) }}</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">刷卡營收</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(reportStore.salesSummary.cardSalesAmount) }}</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">交易筆數</p>
          <p class="mt-2 text-2xl font-semibold text-white">{{ reportStore.salesSummary.orderCount }}</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 md:col-span-2 xl:col-span-3">
          <p class="text-sm text-slate-400">統計區間</p>
          <p class="mt-2 text-base font-semibold text-white">
            {{ formatDateTime(reportStore.salesSummary.from) }} 至 {{ formatDateTime(reportStore.salesSummary.to) }}
          </p>
        </div>
      </div>

      <p v-if="reportStore.errorMessage" class="mt-6 text-sm text-brand-coral">{{ reportStore.errorMessage }}</p>
    </article>
  </section>
</template>
