<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { useInventoryStore } from "@/stores/inventory";
import type { InventoryMovementType } from "@/types/inventory";

const inventoryStore = useInventoryStore();

const formError = ref("");
const reorderDrafts = reactive<Record<string, string>>({});
const movementForm = reactive<{
  productId: string;
  movementType: InventoryMovementType;
  quantity: string;
  unitCost: string;
  note: string;
}>({
  productId: "",
  movementType: "PURCHASE_IN",
  quantity: "1",
  unitCost: "",
  note: "",
});

const selectedStock = computed(
  () => inventoryStore.stocks.find((stock) => stock.productId === movementForm.productId) ?? null,
);

const movementTypeOptions = [
  { value: "PURCHASE_IN", label: "進貨入庫", description: "可售庫存增加，通常用於供應商到貨或門市補貨入庫。" },
  { value: "SALE_OUT", label: "銷貨出庫", description: "可售庫存減少，通常用於商品售出或手動補記銷售扣庫。" },
  { value: "REFUND_IN", label: "客退回庫", description: "可售庫存增加，適用於客退良品重新回到可販售庫存。" },
  { value: "REFUND_DEFECT", label: "客退瑕疵", description: "目前不回補可售庫存，先記錄為瑕疵退回，後續可接雙庫存模型。" },
  { value: "ADJUSTMENT_IN", label: "盤點調增", description: "可售庫存增加，用於盤點後補回短少前未入帳的數量。" },
  { value: "ADJUSTMENT_OUT", label: "盤點調減", description: "可售庫存減少，用於盤點後扣除帳上多出的數量。" },
  { value: "DAMAGE_OUT", label: "損耗出庫", description: "可售庫存減少，用於破損、過期、報損或不可售損耗。" },
  { value: "SCRAP_OUT", label: "報廢出庫", description: "可售庫存減少，用於確認報廢、不可恢復使用的商品。" },
  { value: "SAMPLE_OUT", label: "試吃試飲", description: "可售庫存減少，用於試吃、試飲、贈品或行銷樣品消耗。" },
  { value: "PRODUCTION_CONSUME", label: "生產耗用", description: "可售庫存減少，預留給原料或半成品在製作流程中的耗用。" },
] satisfies Array<{ value: InventoryMovementType; label: string; description: string }>;

const selectedMovementDescription = computed(
  () => movementTypeOptions.find((option) => option.value === movementForm.movementType)?.description ?? "請選擇異動類型",
);

function formatCurrency(value: number | null) {
  if (value === null) {
    return "--";
  }

  return new Intl.NumberFormat("zh-TW", {
    style: "currency",
    currency: "TWD",
    minimumFractionDigits: 2,
  }).format(value);
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("zh-TW", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function movementLabel(type: string) {
  return movementTypeOptions.find((option) => option.value === type)?.label ?? type;
}

function bucketLabel(bucket: "SELLABLE" | "DEFECTIVE") {
  return bucket === "DEFECTIVE" ? "瑕疵庫存" : "可售庫存";
}

function resetMovementForm() {
  movementForm.productId = inventoryStore.stocks[0]?.productId ?? "";
  movementForm.movementType = "PURCHASE_IN";
  movementForm.quantity = "1";
  movementForm.unitCost = "";
  movementForm.note = "";
  formError.value = "";
}

async function submitMovement() {
  formError.value = "";

  if (!movementForm.productId) {
    formError.value = "請先選擇商品";
    return;
  }

  if (Number(movementForm.quantity) <= 0) {
    formError.value = "異動數量必須大於 0";
    return;
  }

  const success = await inventoryStore.submitMovement({
    productId: movementForm.productId,
    movementType: movementForm.movementType,
    quantity: Number(movementForm.quantity),
    unitCost: movementForm.unitCost ? Number(movementForm.unitCost) : null,
    note: movementForm.note,
  });

  if (success) {
    resetMovementForm();
  }
}

async function saveReorderLevel(productId: string) {
  const nextValue = Number(reorderDrafts[productId] ?? "0");
  if (nextValue < 0) {
    formError.value = "補貨門檻不能小於 0";
    return;
  }

  await inventoryStore.saveReorderLevel(productId, nextValue);
}

onMounted(async () => {
  await inventoryStore.loadInventory();
  for (const stock of inventoryStore.stocks) {
    reorderDrafts[stock.productId] = String(stock.reorderLevel);
  }
  resetMovementForm();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.5fr_0.9fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Inventory Control</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">庫存總覽</h3>
          <p class="mt-2 text-sm text-slate-400">查看目前可售庫存、補貨門檻與最近 100 筆庫存異動。</p>
        </div>
        <label class="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-200">
          <input :checked="inventoryStore.lowStockOnly" type="checkbox" class="h-4 w-4" @change="inventoryStore.setLowStockFilter(($event.target as HTMLInputElement).checked)" />
          只看低庫存
        </label>
      </div>

      <p v-if="inventoryStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ inventoryStore.errorMessage }}</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">商品</th>
              <th class="px-4 py-3 font-medium">分類</th>
              <th class="px-4 py-3 font-medium">可售庫存</th>
              <th class="px-4 py-3 font-medium">瑕疵庫存</th>
              <th class="px-4 py-3 font-medium">總庫存</th>
              <th class="px-4 py-3 font-medium">補貨門檻</th>
              <th class="px-4 py-3 font-medium">狀態</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="inventoryStore.loading">
              <td colspan="7" class="px-4 py-12 text-center text-slate-400">庫存資料載入中...</td>
            </tr>
            <tr v-for="stock in inventoryStore.stocks" :key="stock.productId" class="bg-slate-950/25">
              <td class="px-4 py-4">
                <div class="flex items-center gap-3">
                  <div v-if="stock.imageUrl" class="h-14 w-14 overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70">
                    <img :src="stock.imageUrl" :alt="stock.name" class="h-full w-full object-cover" />
                  </div>
                  <div v-else class="flex h-14 w-14 items-center justify-center rounded-2xl border border-dashed border-white/10 bg-slate-900/50 text-[11px] text-slate-500">
                    No Image
                  </div>
                  <div>
                    <p class="font-medium text-white">{{ stock.name }}</p>
                    <p class="text-xs text-slate-400">{{ stock.sku }}</p>
                  </div>
                </div>
              </td>
              <td class="px-4 py-4 text-slate-300">{{ stock.categoryName }}</td>
              <td class="px-4 py-4 text-white">{{ stock.sellableQuantity }}</td>
              <td class="px-4 py-4 text-amber-300">{{ stock.defectiveQuantity }}</td>
              <td class="px-4 py-4 text-white">{{ stock.quantityOnHand }}</td>
              <td class="px-4 py-4">
                <div class="flex items-center gap-2">
                  <input v-model="reorderDrafts[stock.productId]" type="number" min="0" class="w-20 rounded-xl border border-white/10 bg-slate-900/80 px-3 py-2 text-white outline-none" />
                  <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="saveReorderLevel(stock.productId)">更新</button>
                </div>
              </td>
              <td class="px-4 py-4">
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="stock.lowStock ? 'bg-brand-coral/15 text-brand-coral' : 'bg-brand-aqua/15 text-brand-aqua'"
                >
                  {{ stock.lowStock ? "低庫存" : "正常" }}
                </span>
              </td>
            </tr>
            <tr v-if="!inventoryStore.loading && inventoryStore.stocks.length === 0">
              <td colspan="7" class="px-4 py-12 text-center text-slate-400">目前沒有符合條件的庫存資料。</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-white">最近庫存異動</p>
          <p class="text-xs text-slate-400">僅顯示最近 100 筆</p>
        </div>
        <div class="mt-4 space-y-3">
          <div v-for="movement in inventoryStore.movements" :key="movement.id" class="rounded-2xl border border-white/8 bg-slate-950/40 p-4">
            <div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <p class="font-medium text-white">{{ movement.productName }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ movement.sku }} 繚 {{ movementLabel(movement.movementType) }}</p>
                <p class="mt-1 text-xs text-slate-500">作用庫位：{{ bucketLabel(movement.stockBucket) }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold" :class="movement.quantityDelta > 0 ? 'text-brand-aqua' : movement.quantityDelta < 0 ? 'text-brand-coral' : 'text-slate-300'">
                  {{ movement.quantityDelta > 0 ? "+" : "" }}{{ movement.quantityDelta }}
                </p>
                <p class="text-xs text-slate-400">異動後總庫存 {{ movement.quantityAfter }}</p>
              </div>
            </div>
            <div class="mt-3 grid gap-2 rounded-2xl border border-white/8 bg-white/4 px-3 py-3 text-xs text-slate-300 sm:grid-cols-2">
              <p>可售異動：{{ movement.sellableQuantityDelta > 0 ? "+" : "" }}{{ movement.sellableQuantityDelta }}</p>
              <p>瑕疵異動：{{ movement.defectiveQuantityDelta > 0 ? "+" : "" }}{{ movement.defectiveQuantityDelta }}</p>
              <p>可售結餘：{{ movement.sellableQuantityAfter }}</p>
              <p>瑕疵結餘：{{ movement.defectiveQuantityAfter }}</p>
            </div>
            <div class="mt-3 flex flex-wrap gap-4 text-xs text-slate-400">
              <span>{{ formatDateTime(movement.occurredAt) }}</span>
              <span>單位成本 {{ formatCurrency(movement.unitCost) }}</span>
              <span>{{ movement.note || "未填寫備註" }}</span>
            </div>
          </div>
        </div>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Stock Movement</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">建立庫存異動</h3>
      <p class="mt-2 text-sm text-slate-400">先選擇商品與異動類型，再填寫數量、單位成本與備註。之後接上原料管理時，也會沿用這組異動模型。</p>

      <div v-if="formError" class="mt-6 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
        {{ formError }}
      </div>

      <div class="mt-6 space-y-4">
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">商品</span>
          <select v-model="movementForm.productId" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option v-for="stock in inventoryStore.stocks" :key="stock.productId" :value="stock.productId">
              {{ stock.name }} ({{ stock.sku }})
            </option>
          </select>
        </label>

        <div class="grid gap-3 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:items-end">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">異動類型</span>
            <select v-model="movementForm.movementType" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
              <option v-for="option in movementTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <div class="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
            {{ selectedMovementDescription }}
          </div>
        </div>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">數量</span>
          <input v-model="movementForm.quantity" type="number" min="1" step="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">單位成本</span>
          <input v-model="movementForm.unitCost" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">備註</span>
          <textarea v-model="movementForm.note" rows="4" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <div v-if="selectedStock" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
          <p class="font-semibold text-white">{{ selectedStock.name }}</p>
          <p class="mt-2">可售庫存：{{ selectedStock.sellableQuantity }}</p>
          <p class="mt-1">瑕疵庫存：{{ selectedStock.defectiveQuantity }}</p>
          <p class="mt-1">總庫存：{{ selectedStock.quantityOnHand }}</p>
          <p class="mt-1">補貨門檻：{{ selectedStock.reorderLevel }}</p>
        </div>

        <button
          class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:opacity-60"
          :disabled="inventoryStore.saving"
          @click="submitMovement"
        >
          {{ inventoryStore.saving ? "送出中..." : "建立庫存異動" }}
        </button>
      </div>
    </aside>
  </section>
</template>
