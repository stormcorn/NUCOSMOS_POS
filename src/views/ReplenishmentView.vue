<script setup lang="ts">
import { computed, onMounted } from "vue";

import { useProcurementStore } from "@/stores/procurement";
import { formatCurrency } from "@/utils/format";

const procurementStore = useProcurementStore();

const totalEstimatedCost = computed(() =>
  procurementStore.replenishmentSuggestions.reduce(
    (sum, item) => sum + (item.estimatedOrderCost ?? 0),
    0,
  ),
);

const materialCount = computed(
  () =>
    procurementStore.replenishmentSuggestions.filter((item) => item.itemType === "MATERIAL")
      .length,
);

const manufacturedCount = computed(
  () =>
    procurementStore.replenishmentSuggestions.filter((item) => item.itemType === "MANUFACTURED")
      .length,
);

const packagingCount = computed(
  () =>
    procurementStore.replenishmentSuggestions.filter((item) => item.itemType === "PACKAGING")
      .length,
);

function itemTypeLabel(value: string) {
  if (value === "PACKAGING") {
    return "包裝";
  }
  if (value === "MANUFACTURED") {
    return "製成品";
  }
  return "原料";
}

onMounted(() => {
  void procurementStore.loadReplenishmentSuggestions();
});
</script>

<template>
  <section class="space-y-6">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between border-b border-white/8 pb-5">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Replenishment Radar</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">補貨建議</h3>
          <p class="mt-2 text-sm text-slate-400">
            依目前庫存、補貨點與最近採購成本，整理原料、製成品與包裝的建議進貨量。
          </p>
        </div>
        <button
          class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200"
          @click="procurementStore.loadReplenishmentSuggestions()"
        >
          重新整理
        </button>
      </div>

      <p v-if="procurementStore.errorMessage" class="mt-4 text-sm text-brand-coral">
        {{ procurementStore.errorMessage }}
      </p>

      <div class="mt-6 grid gap-4 md:grid-cols-5">
        <article class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">建議品項</p>
          <p class="mt-3 text-2xl font-semibold text-white">
            {{ procurementStore.replenishmentSuggestions.length }}
          </p>
        </article>
        <article class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">原料</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ materialCount }}</p>
        </article>
        <article class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">製成品</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ manufacturedCount }}</p>
        </article>
        <article class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">包裝</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ packagingCount }}</p>
        </article>
        <article class="rounded-[1.5rem] border border-brand-aqua/20 bg-brand-aqua/5 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">預估採購金額</p>
          <p class="mt-3 text-2xl font-semibold text-brand-aqua">
            {{ formatCurrency(totalEstimatedCost) }}
          </p>
        </article>
      </div>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">類型</th>
              <th class="px-4 py-3 font-medium">品項</th>
              <th class="px-4 py-3 font-medium">目前庫存</th>
              <th class="px-4 py-3 font-medium">補貨點</th>
              <th class="px-4 py-3 font-medium">建議採購量</th>
              <th class="px-4 py-3 font-medium">換算</th>
              <th class="px-4 py-3 font-medium">最近進貨成本</th>
              <th class="px-4 py-3 font-medium">預估金額</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="procurementStore.loading">
              <td colspan="8" class="px-4 py-12 text-center text-slate-400">載入補貨建議中...</td>
            </tr>
            <tr
              v-for="item in procurementStore.replenishmentSuggestions"
              :key="`${item.itemType}-${item.itemId}`"
              class="bg-slate-950/25"
            >
              <td class="px-4 py-4 text-slate-300">{{ itemTypeLabel(item.itemType) }}</td>
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ item.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ item.sku }}</p>
              </td>
              <td class="px-4 py-4 text-white">{{ item.quantityOnHand }} {{ item.stockUnit }}</td>
              <td class="px-4 py-4 text-slate-300">{{ item.reorderLevel }} {{ item.stockUnit }}</td>
              <td class="px-4 py-4 text-brand-aqua">
                {{ item.suggestedOrderQuantity }} {{ item.purchaseUnit }}
              </td>
              <td class="px-4 py-4 text-slate-300">
                1 {{ item.purchaseUnit }} = {{ item.purchaseToStockRatio }} {{ item.stockUnit }}
              </td>
              <td class="px-4 py-4 text-white">
                {{
                  item.latestPurchaseUnitCost === null
                    ? "--"
                    : `${formatCurrency(item.latestPurchaseUnitCost)} / ${item.purchaseUnit}`
                }}
              </td>
              <td class="px-4 py-4 text-white">
                {{ item.estimatedOrderCost === null ? "--" : formatCurrency(item.estimatedOrderCost) }}
              </td>
            </tr>
            <tr
              v-if="!procurementStore.loading && procurementStore.replenishmentSuggestions.length === 0"
            >
              <td colspan="8" class="px-4 py-12 text-center text-slate-400">
                目前沒有需要補貨的原料、製成品或包裝。
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </section>
</template>
