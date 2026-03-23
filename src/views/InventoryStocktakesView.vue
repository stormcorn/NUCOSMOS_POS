<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useInventoryStore } from "@/stores/inventory";
import { useStocktakeStore } from "@/stores/stocktakes";
import { formatDateTime } from "@/utils/format";

const authStore = useAuthStore();
const inventoryStore = useInventoryStore();
const stocktakeStore = useStocktakeStore();
const canEditInventory = computed(() => authStore.hasPermission(PERMISSIONS.INVENTORY_EDIT));

const formError = ref("");
const stocktakeNote = ref("");
const countedQuantities = reactive<Record<string, string>>({});
const reasonCodes = reactive<Record<string, string>>({});
const itemNotes = reactive<Record<string, string>>({});

const variancePreview = computed(() =>
  inventoryStore.stocks
    .map((stock) => {
      const countedValue = Number(countedQuantities[stock.productId] ?? stock.sellableQuantity);
      const variance = countedValue - stock.sellableQuantity;
      return {
        ...stock,
        countedSellableQuantity: countedValue,
        variance,
      };
    })
    .filter((stock) => stock.variance !== 0),
);

function initializeDrafts() {
  for (const stock of inventoryStore.stocks) {
    countedQuantities[stock.productId] = String(stock.sellableQuantity);
    reasonCodes[stock.productId] = "";
    itemNotes[stock.productId] = "";
  }
}

async function loadPage() {
  await Promise.all([
    inventoryStore.loadInventory(),
    stocktakeStore.loadStocktakes(),
  ]);
  initializeDrafts();
}

async function submitStocktake() {
  if (!canEditInventory.value) {
    return;
  }

  formError.value = "";

  if (inventoryStore.stocks.length === 0) {
    formError.value = "目前沒有可盤點的商品資料。";
    return;
  }

  const items = inventoryStore.stocks.map((stock) => {
    const countedSellableQuantity = Number(countedQuantities[stock.productId] ?? stock.sellableQuantity);
    return {
      productId: stock.productId,
      countedSellableQuantity,
      reasonCode: reasonCodes[stock.productId] || undefined,
      note: itemNotes[stock.productId] || undefined,
    };
  });

  if (items.some((item) => Number.isNaN(item.countedSellableQuantity) || item.countedSellableQuantity < 0)) {
    formError.value = "盤點數量必須是 0 以上的整數。";
    return;
  }

  const success = await stocktakeStore.submitStocktake({
    note: stocktakeNote.value || undefined,
    items,
  });

  if (success) {
    stocktakeNote.value = "";
    await inventoryStore.loadInventory();
    initializeDrafts();
  }
}

onMounted(() => {
  void loadPage();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-3 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Stocktake Worksheet</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">商品盤點單</h3>
          <p class="mt-2 text-sm text-slate-400">
            以目前可售庫存為基準輸入實盤數量，送出後系統會自動產生盤點差異調整。
          </p>
        </div>
        <div class="rounded-2xl border border-white/8 bg-white/5 px-4 py-3 text-sm text-slate-300">
          差異品項 {{ variancePreview.length }} 筆
        </div>
      </div>

      <p v-if="inventoryStore.errorMessage || stocktakeStore.errorMessage" class="mt-4 text-sm text-brand-coral">
        {{ inventoryStore.errorMessage || stocktakeStore.errorMessage }}
      </p>
      <p v-if="!canEditInventory" class="mt-4 text-sm text-amber-200">你目前只有檢視權限，不能建立或過帳盤點單。</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">商品</th>
              <th class="px-4 py-3 font-medium">分類</th>
              <th class="px-4 py-3 font-medium">系統可售</th>
              <th class="px-4 py-3 font-medium">實盤可售</th>
              <th class="px-4 py-3 font-medium">差異</th>
              <th class="px-4 py-3 font-medium">原因碼</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="inventoryStore.loading">
              <td colspan="6" class="px-4 py-12 text-center text-slate-400">盤點資料載入中...</td>
            </tr>
            <tr v-for="stock in inventoryStore.stocks" :key="stock.productId" class="bg-slate-950/25">
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ stock.name }}</p>
                <p class="text-xs text-slate-400">{{ stock.sku }}</p>
              </td>
              <td class="px-4 py-4 text-slate-300">{{ stock.categoryName }}</td>
              <td class="px-4 py-4 text-white">{{ stock.sellableQuantity }}</td>
              <td class="px-4 py-4">
                <input
                  v-model="countedQuantities[stock.productId]"
                  :disabled="!canEditInventory"
                  type="number"
                  min="0"
                  step="1"
                  class="w-24 rounded-xl border border-white/10 bg-slate-900/80 px-3 py-2 text-white outline-none disabled:cursor-not-allowed disabled:opacity-50"
                />
              </td>
              <td class="px-4 py-4">
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="Number(countedQuantities[stock.productId] ?? stock.sellableQuantity) - stock.sellableQuantity === 0
                    ? 'bg-white/10 text-slate-300'
                    : Number(countedQuantities[stock.productId] ?? stock.sellableQuantity) - stock.sellableQuantity > 0
                      ? 'bg-brand-aqua/15 text-brand-aqua'
                      : 'bg-brand-coral/15 text-brand-coral'"
                >
                  {{ Number(countedQuantities[stock.productId] ?? stock.sellableQuantity) - stock.sellableQuantity > 0 ? "+" : "" }}
                  {{ Number(countedQuantities[stock.productId] ?? stock.sellableQuantity) - stock.sellableQuantity }}
                </span>
              </td>
              <td class="px-4 py-4">
                <input
                  v-model="reasonCodes[stock.productId]"
                  :disabled="!canEditInventory"
                  type="text"
                  maxlength="50"
                  placeholder="例如 COUNT_VARIANCE"
                  class="w-full rounded-xl border border-white/10 bg-slate-900/80 px-3 py-2 text-white outline-none disabled:cursor-not-allowed disabled:opacity-50"
                />
              </td>
            </tr>
            <tr v-if="!inventoryStore.loading && inventoryStore.stocks.length === 0">
              <td colspan="6" class="px-4 py-12 text-center text-slate-400">目前沒有可盤點商品。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="space-y-6">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Post Stocktake</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">送出盤點</h3>

        <div v-if="formError" class="mt-4 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
          {{ formError }}
        </div>

        <label class="mt-6 block">
          <span class="mb-2 block text-sm text-slate-300">盤點備註</span>
          <textarea
            v-model="stocktakeNote"
            :disabled="!canEditInventory"
            rows="4"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-50"
            placeholder="例如：閉店盤點 / 月底盤點"
          />
        </label>

        <div class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
          <p class="font-semibold text-white">差異預覽</p>
          <ul class="mt-3 space-y-3">
            <li v-for="item in variancePreview.slice(0, 6)" :key="item.productId" class="rounded-xl border border-white/8 bg-slate-950/50 px-3 py-3">
              <p class="font-medium text-white">{{ item.name }}</p>
              <p class="mt-1 text-xs text-slate-400">
                系統 {{ item.sellableQuantity }} / 實盤 {{ item.countedSellableQuantity }} / 差異 {{ item.variance > 0 ? "+" : "" }}{{ item.variance }}
              </p>
            </li>
            <li v-if="variancePreview.length === 0" class="text-slate-400">目前沒有差異，送出後會建立零差異盤點單。</li>
          </ul>
        </div>

        <button
          class="mt-6 w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:opacity-60"
          :disabled="!canEditInventory || stocktakeStore.saving || inventoryStore.loading"
          @click="submitStocktake"
        >
          {{ stocktakeStore.saving ? "送出中..." : "建立盤點單" }}
        </button>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Recent Stocktakes</p>
            <h3 class="mt-2 text-2xl font-semibold text-white">最近盤點單</h3>
          </div>
          <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="stocktakeStore.loadStocktakes()">
            重新整理
          </button>
        </div>

        <div class="mt-6 space-y-4">
          <article v-if="stocktakeStore.loading" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
            盤點單載入中...
          </article>
          <article v-for="stocktake in stocktakeStore.items" :key="stocktake.id" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-semibold text-white">{{ formatDateTime(stocktake.countedAt) }}</p>
                <p class="mt-1 text-xs text-slate-400">
                  {{ stocktake.createdByEmployeeCode }} · {{ stocktake.status }} · {{ stocktake.storeCode }}
                </p>
              </div>
              <span class="rounded-full bg-brand-aqua/15 px-3 py-1 text-xs font-semibold text-brand-aqua">
                {{ stocktake.items.length }} 項
              </span>
            </div>
            <p class="mt-3 text-sm text-slate-300">{{ stocktake.note || "無備註" }}</p>
            <div class="mt-3 grid gap-2 text-xs text-slate-400">
              <p v-for="item in stocktake.items.slice(0, 3)" :key="item.id">
                {{ item.productName }}：{{ item.expectedSellableQuantity }} -> {{ item.countedSellableQuantity }}
                <span :class="item.varianceQuantity > 0 ? 'text-brand-aqua' : item.varianceQuantity < 0 ? 'text-brand-coral' : 'text-slate-400'">
                  ({{ item.varianceQuantity > 0 ? "+" : "" }}{{ item.varianceQuantity }})
                </span>
              </p>
            </div>
          </article>
          <article v-if="!stocktakeStore.loading && stocktakeStore.items.length === 0" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
            目前還沒有盤點單。
          </article>
        </div>
      </article>
    </aside>
  </section>
</template>
