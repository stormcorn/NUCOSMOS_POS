<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { useInventoryStore } from "@/stores/inventory";

const inventoryStore = useInventoryStore();

const formError = ref("");
const reorderDrafts = reactive<Record<string, string>>({});
const movementForm = reactive({
  productId: "",
  movementType: "PURCHASE_IN",
  quantity: "1",
  unitCost: "",
  note: "",
});

const selectedStock = computed(
  () => inventoryStore.stocks.find((stock) => stock.productId === movementForm.productId) ?? null,
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
  switch (type) {
    case "PURCHASE_IN":
      return "進貨入庫";
    case "SALE_OUT":
      return "銷貨出庫";
    case "ADJUSTMENT_IN":
      return "盤點調增";
    case "ADJUSTMENT_OUT":
      return "盤點調減";
    case "REFUND_IN":
      return "退貨回庫";
    default:
      return type;
  }
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
    formError.value = "請選擇商品。";
    return;
  }

  if (Number(movementForm.quantity) <= 0) {
    formError.value = "異動數量必須大於 0。";
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
    formError.value = "補貨門檻不可小於 0。";
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
          <h3 class="mt-2 text-2xl font-semibold text-white">庫存管理</h3>
          <p class="mt-2 text-sm text-slate-400">查看現有庫存、補貨門檻與最近 100 筆庫存異動。</p>
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
              <th class="px-4 py-3 font-medium">現有庫存</th>
              <th class="px-4 py-3 font-medium">補貨門檻</th>
              <th class="px-4 py-3 font-medium">狀態</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="inventoryStore.loading">
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">庫存資料載入中...</td>
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
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">目前沒有庫存資料。</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-white">最近庫存異動</p>
          <p class="text-xs text-slate-400">顯示最近 100 筆</p>
        </div>
        <div class="mt-4 space-y-3">
          <div v-for="movement in inventoryStore.movements" :key="movement.id" class="rounded-2xl border border-white/8 bg-slate-950/40 p-4">
            <div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <p class="font-medium text-white">{{ movement.productName }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ movement.sku }} · {{ movementLabel(movement.movementType) }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold" :class="movement.quantityDelta > 0 ? 'text-brand-aqua' : 'text-brand-coral'">
                  {{ movement.quantityDelta > 0 ? "+" : "" }}{{ movement.quantityDelta }}
                </p>
                <p class="text-xs text-slate-400">異動後 {{ movement.quantityAfter }}</p>
              </div>
            </div>
            <div class="mt-3 flex flex-wrap gap-4 text-xs text-slate-400">
              <span>{{ formatDateTime(movement.occurredAt) }}</span>
              <span>成本 {{ formatCurrency(movement.unitCost) }}</span>
              <span>{{ movement.note || "無備註" }}</span>
            </div>
          </div>
        </div>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Stock Movement</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">進銷存異動</h3>
      <p class="mt-2 text-sm text-slate-400">手動登錄進貨、銷貨與盤點調整，建立庫存台帳。</p>

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

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">異動類型</span>
          <select v-model="movementForm.movementType" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option value="PURCHASE_IN">進貨入庫</option>
            <option value="SALE_OUT">銷貨出庫</option>
            <option value="ADJUSTMENT_IN">盤點調增</option>
            <option value="ADJUSTMENT_OUT">盤點調減</option>
            <option value="REFUND_IN">退貨回庫</option>
          </select>
        </label>

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
          <p class="mt-2">目前庫存：{{ selectedStock.quantityOnHand }}</p>
          <p class="mt-1">補貨門檻：{{ selectedStock.reorderLevel }}</p>
        </div>

        <button
          class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:opacity-60"
          :disabled="inventoryStore.saving"
          @click="submitMovement"
        >
          {{ inventoryStore.saving ? "儲存中..." : "建立庫存異動" }}
        </button>
      </div>
    </aside>
  </section>
</template>
