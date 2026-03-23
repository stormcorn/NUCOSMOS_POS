<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useInventoryStore } from "@/stores/inventory";

const authStore = useAuthStore();
const inventoryStore = useInventoryStore();
const canEditInventory = computed(() => authStore.hasPermission(PERMISSIONS.INVENTORY_EDIT));

const selectedProductId = ref("");
const actionError = ref("");
const actionForm = reactive({
  quantity: "1",
  reasonCode: "DAMAGE",
  note: "",
});

const reasonOptions = [
  { value: "DAMAGE", label: "破損" },
  { value: "EXPIRED", label: "過期" },
  { value: "CUSTOMER_RETURN", label: "客退瑕疵" },
  { value: "QUALITY_CHECK", label: "檢驗判定" },
];

const selectedStock = computed(
  () => inventoryStore.defectiveStocks.find((stock) => stock.productId === selectedProductId.value) ?? null,
);

const defectiveSummary = computed(() => ({
  productCount: inventoryStore.defectiveStocks.length,
  quantity: inventoryStore.defectiveStocks.reduce((sum, stock) => sum + stock.defectiveQuantity, 0),
  affectedSellableQuantity: inventoryStore.defectiveStocks.reduce((sum, stock) => sum + stock.sellableQuantity, 0),
}));

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("zh-TW", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function movementLabel(type: string) {
  switch (type) {
    case "REFUND_DEFECT":
      return "客退瑕疵";
    case "SCRAP_OUT":
      return "報廢出庫";
    case "DEFECTIVE_RESTORE":
      return "轉回可售";
    default:
      return type;
  }
}

function resetActionForm() {
  actionForm.quantity = "1";
  actionForm.reasonCode = "DAMAGE";
  actionForm.note = "";
  actionError.value = "";
}

async function handleScrap() {
  if (!canEditInventory.value) {
    return;
  }

  actionError.value = "";

  if (!selectedProductId.value) {
    actionError.value = "請先選擇瑕疵商品";
    return;
  }

  if (Number(actionForm.quantity) <= 0) {
    actionError.value = "數量必須大於 0";
    return;
  }

  const success = await inventoryStore.scrapDefective(selectedProductId.value, {
    quantity: Number(actionForm.quantity),
    reasonCode: actionForm.reasonCode,
    note: actionForm.note,
  });

  if (success) {
    resetActionForm();
  }
}

async function handleRestore() {
  if (!canEditInventory.value) {
    return;
  }

  actionError.value = "";

  if (!selectedProductId.value) {
    actionError.value = "請先選擇瑕疵商品";
    return;
  }

  if (Number(actionForm.quantity) <= 0) {
    actionError.value = "數量必須大於 0";
    return;
  }

  const success = await inventoryStore.restoreDefective(selectedProductId.value, {
    quantity: Number(actionForm.quantity),
    reasonCode: actionForm.reasonCode,
    note: actionForm.note,
  });

  if (success) {
    resetActionForm();
  }
}

onMounted(async () => {
  await inventoryStore.loadDefectiveInventory();
  selectedProductId.value = inventoryStore.defectiveStocks[0]?.productId ?? "";
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.3fr_0.95fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-amber-200/70">Defective Stock</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">瑕疵庫存</h3>
          <p class="mt-2 text-sm text-slate-400">集中查看目前瑕疵庫存、相關良品數量，以及後續報廢或轉回可售的處理狀態。</p>
        </div>
        <div class="grid gap-3 sm:grid-cols-3">
          <div class="rounded-2xl border border-white/8 bg-white/5 px-4 py-3">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">品項數</p>
            <p class="mt-2 text-xl font-semibold text-white">{{ defectiveSummary.productCount }}</p>
          </div>
          <div class="rounded-2xl border border-amber-300/20 bg-amber-200/10 px-4 py-3">
            <p class="text-xs uppercase tracking-[0.2em] text-amber-200/70">瑕疵總量</p>
            <p class="mt-2 text-xl font-semibold text-amber-100">{{ defectiveSummary.quantity }}</p>
          </div>
          <div class="rounded-2xl border border-white/8 bg-white/5 px-4 py-3">
            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">相關可售量</p>
            <p class="mt-2 text-xl font-semibold text-white">{{ defectiveSummary.affectedSellableQuantity }}</p>
          </div>
        </div>
      </div>

      <p v-if="inventoryStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ inventoryStore.errorMessage }}</p>
      <p v-if="!canEditInventory" class="mt-4 text-sm text-amber-200">你目前只有檢視權限，不能執行瑕疵報廢或轉回可售。</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">商品</th>
              <th class="px-4 py-3 font-medium">分類</th>
              <th class="px-4 py-3 font-medium">瑕疵庫存</th>
              <th class="px-4 py-3 font-medium">可售庫存</th>
              <th class="px-4 py-3 font-medium">總庫存</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="inventoryStore.loading">
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">瑕疵庫存資料載入中...</td>
            </tr>
            <tr v-for="stock in inventoryStore.defectiveStocks" :key="stock.productId" class="bg-slate-950/25">
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ stock.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ stock.sku }}</p>
              </td>
              <td class="px-4 py-4 text-slate-300">{{ stock.categoryName }}</td>
              <td class="px-4 py-4 font-semibold text-amber-200">{{ stock.defectiveQuantity }}</td>
              <td class="px-4 py-4 text-slate-200">{{ stock.sellableQuantity }}</td>
              <td class="px-4 py-4 text-white">{{ stock.quantityOnHand }}</td>
            </tr>
            <tr v-if="!inventoryStore.loading && inventoryStore.defectiveStocks.length === 0">
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">目前沒有瑕疵庫存資料。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="space-y-6">
      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-amber-200/70">Defective Actions</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">瑕疵處理</h3>

        <div v-if="actionError" class="mt-6 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
          {{ actionError }}
        </div>

        <div class="mt-6 space-y-4">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">瑕疵商品</span>
            <select v-model="selectedProductId" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
              <option v-for="stock in inventoryStore.defectiveStocks" :key="stock.productId" :value="stock.productId">
                {{ stock.name }} ({{ stock.sku }})
              </option>
            </select>
          </label>

          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">處理原因</span>
            <select v-model="actionForm.reasonCode" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
              <option v-for="reason in reasonOptions" :key="reason.value" :value="reason.value">
                {{ reason.label }}
              </option>
            </select>
          </label>

          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">數量</span>
            <input v-model="actionForm.quantity" type="number" min="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>

          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">備註</span>
            <textarea v-model="actionForm.note" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>

          <div v-if="selectedStock" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
            <p class="font-semibold text-white">{{ selectedStock.name }}</p>
            <p class="mt-2">瑕疵庫存：{{ selectedStock.defectiveQuantity }}</p>
            <p class="mt-1">可售庫存：{{ selectedStock.sellableQuantity }}</p>
          </div>

          <p v-if="!canEditInventory" class="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
            目前帳號為唯讀模式，這裡僅可查看瑕疵庫存資料。
          </p>

          <div class="grid gap-3 sm:grid-cols-2">
            <button class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-5 py-3 text-sm font-semibold text-brand-coral" :disabled="!canEditInventory || inventoryStore.saving" @click="handleScrap">
              報廢處理
            </button>
            <button class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="!canEditInventory || inventoryStore.saving" @click="handleRestore">
              轉回可售
            </button>
          </div>
        </div>
      </section>

      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-amber-200/70">Defective Movements</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">瑕疵異動紀錄</h3>

        <div class="mt-6 space-y-3">
          <div v-for="movement in inventoryStore.defectiveMovements" :key="movement.id" class="rounded-2xl border border-white/8 bg-white/4 p-4">
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="font-medium text-white">{{ movement.productName }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ movement.sku }} 繚 {{ movementLabel(movement.movementType) }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold" :class="movement.quantityDelta > 0 ? 'text-amber-200' : movement.movementType === 'DEFECTIVE_RESTORE' ? 'text-brand-aqua' : 'text-brand-coral'">
                  {{ movement.quantityDelta > 0 ? "+" : "" }}{{ movement.quantityDelta }}
                </p>
                <p class="text-xs text-slate-400">瑕疵結餘 {{ movement.defectiveQuantityAfter }}</p>
              </div>
            </div>
            <div class="mt-3 grid gap-2 rounded-2xl border border-white/8 bg-slate-950/40 px-3 py-3 text-xs text-slate-300">
              <p>原因：{{ movement.reasonCode || "未填寫" }}</p>
              <p>可售結餘：{{ movement.sellableQuantityAfter }}</p>
              <p>瑕疵結餘：{{ movement.defectiveQuantityAfter }}</p>
              <p>{{ movement.note || "未填寫備註" }}</p>
              <p>{{ formatDateTime(movement.occurredAt) }}</p>
            </div>
          </div>

          <div v-if="!inventoryStore.loading && inventoryStore.defectiveMovements.length === 0" class="rounded-2xl border border-dashed border-white/10 bg-slate-950/40 px-4 py-8 text-center text-sm text-slate-400">
            目前沒有瑕疵異動紀錄。
          </div>
        </div>
      </section>
    </aside>
  </section>
</template>
