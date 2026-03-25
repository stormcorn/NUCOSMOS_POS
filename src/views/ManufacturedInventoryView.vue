<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useManufacturedStore } from "@/stores/manufactured";
import type { ManufacturedAdminItem, SupplyMovementType } from "@/types/manufactured";
import { formatCurrency, formatDateTime } from "@/utils/format";
import { isEmbeddedImage, readImageFileAsDataUrl } from "@/utils/image-upload";

const authStore = useAuthStore();
const manufacturedStore = useManufacturedStore();
const canEditInventory = computed(() => authStore.hasPermission(PERMISSIONS.INVENTORY_EDIT));

const isFormOpen = ref(false);
const editingId = ref<string | null>(null);
const selectedItemId = ref("");
const formError = ref("");

const form = reactive({
  sku: "",
  name: "",
  unit: "",
  purchaseUnit: "",
  purchaseToStockRatio: "1",
  imageUrl: "",
  imageUrlInput: "",
  uploadedImageName: "",
  description: "",
  reorderLevel: "0",
  latestUnitCost: "",
});

const movementForm = reactive<{
  movementType: SupplyMovementType;
  quantity: string;
  unitCost: string;
  batchCode: string;
  expiryDate: string;
  manufacturedAt: string;
  note: string;
}>({
  movementType: "PURCHASE_IN",
  quantity: "1",
  unitCost: "",
  batchCode: "",
  expiryDate: "",
  manufacturedAt: "",
  note: "",
});

const movementOptions: Array<{ value: SupplyMovementType; label: string; description: string }> = [
  { value: "PURCHASE_IN", label: "進貨入庫", description: "新增實際到貨的製成品庫存。" },
  { value: "ADJUSTMENT_IN", label: "盤盈調整", description: "補回盤點時多出的製成品庫存。" },
  { value: "ADJUSTMENT_OUT", label: "盤虧調整", description: "扣除盤點時短少的製成品庫存。" },
  { value: "DAMAGE_OUT", label: "報損出庫", description: "扣除破損或無法使用的製成品。" },
  { value: "CONSUME_OUT", label: "耗用出庫", description: "將製成品轉入其他流程時使用。" },
  { value: "RETURN_IN", label: "退回入庫", description: "退回倉內可再利用的製成品。" },
];

const selectedItem = computed(
  () => manufacturedStore.items.find((item) => item.id === selectedItemId.value) ?? null,
);

const itemLots = computed(() =>
  manufacturedStore.lots.filter((lot) => lot.manufacturedItemId === selectedItemId.value),
);

const itemMovements = computed(() =>
  manufacturedStore.movements.filter((movement) => movement.manufacturedItemId === selectedItemId.value).slice(0, 12),
);

function movementLabel(type: SupplyMovementType) {
  return movementOptions.find((option) => option.value === type)?.label ?? type;
}

function displayUnitCost(value: number | null, unit: string) {
  return value === null ? "--" : `${formatCurrency(value)} / ${unit}`;
}

function resetForm() {
  editingId.value = null;
  formError.value = "";
  form.sku = "";
  form.name = "";
  form.unit = "";
  form.purchaseUnit = "";
  form.purchaseToStockRatio = "1";
  form.imageUrl = "";
  form.imageUrlInput = "";
  form.uploadedImageName = "";
  form.description = "";
  form.reorderLevel = "0";
  form.latestUnitCost = "";
}

function resetMovementForm() {
  movementForm.movementType = "PURCHASE_IN";
  movementForm.quantity = "1";
  movementForm.unitCost = "";
  movementForm.batchCode = "";
  movementForm.expiryDate = "";
  movementForm.manufacturedAt = "";
  movementForm.note = "";
}

function openCreateForm() {
  if (!canEditInventory.value) return;
  resetForm();
  isFormOpen.value = true;
}

function openEditForm(item: ManufacturedAdminItem) {
  if (!canEditInventory.value) return;
  editingId.value = item.id;
  formError.value = "";
  form.sku = item.sku;
  form.name = item.name;
  form.unit = item.unit;
  form.purchaseUnit = item.purchaseUnit;
  form.purchaseToStockRatio = String(item.purchaseToStockRatio);
  form.imageUrl = item.imageUrl ?? "";
  form.imageUrlInput = isEmbeddedImage(item.imageUrl) ? "" : (item.imageUrl ?? "");
  form.uploadedImageName = isEmbeddedImage(item.imageUrl) ? "已上傳圖片" : "";
  form.description = item.description ?? "";
  form.reorderLevel = String(item.reorderLevel);
  form.latestUnitCost = item.latestUnitCost === null ? "" : item.latestUnitCost.toFixed(2);
  isFormOpen.value = true;
}

async function submitForm() {
  if (!canEditInventory.value) return;

  formError.value = "";
  if (!form.sku.trim() || !form.name.trim() || !form.unit.trim() || !form.purchaseUnit.trim()) {
    formError.value = "請完整填寫 SKU、名稱、庫存單位與採購單位。";
    return;
  }
  if (Number(form.purchaseToStockRatio) < 1) {
    formError.value = "採購換算比例至少要是 1。";
    return;
  }
  if (Number(form.reorderLevel) < 0) {
    formError.value = "補貨警戒值不可小於 0。";
    return;
  }

  const payload = {
    sku: form.sku.trim(),
    name: form.name.trim(),
    unit: form.unit.trim(),
    purchaseUnit: form.purchaseUnit.trim(),
    purchaseToStockRatio: Number(form.purchaseToStockRatio),
    imageUrl: form.imageUrlInput.trim() || form.imageUrl.trim(),
    description: form.description.trim(),
    reorderLevel: Number(form.reorderLevel),
    latestUnitCost: form.latestUnitCost ? Number(form.latestUnitCost) : null,
  };

  const success = editingId.value
    ? await manufacturedStore.updateItem(editingId.value, payload)
    : await manufacturedStore.createItem(payload);

  if (success) {
    resetForm();
    isFormOpen.value = false;
  }
}

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;

  try {
    form.imageUrl = await readImageFileAsDataUrl(file);
    form.imageUrlInput = "";
    form.uploadedImageName = file.name;
    formError.value = "";
  } catch (error) {
    formError.value = error instanceof Error ? error.message : "圖片上傳失敗。";
  } finally {
    input.value = "";
  }
}

function handleImageUrlInput() {
  form.imageUrl = form.imageUrlInput.trim();
  if (form.imageUrl) {
    form.uploadedImageName = "";
  }
}

function clearImage() {
  form.imageUrl = "";
  form.imageUrlInput = "";
  form.uploadedImageName = "";
}

async function submitMovement() {
  if (!canEditInventory.value) return;

  formError.value = "";
  if (!selectedItemId.value) {
    formError.value = "請先選擇要異動的製成品。";
    return;
  }
  if (Number(movementForm.quantity) <= 0) {
    formError.value = "異動數量必須大於 0。";
    return;
  }

  const success = await manufacturedStore.submitMovement(selectedItemId.value, {
    movementType: movementForm.movementType,
    quantity: Number(movementForm.quantity),
    unitCost: movementForm.unitCost ? Number(movementForm.unitCost) : null,
    batchCode: movementForm.batchCode.trim() || undefined,
    expiryDate: movementForm.expiryDate ? new Date(movementForm.expiryDate).toISOString() : null,
    manufacturedAt: movementForm.manufacturedAt ? new Date(movementForm.manufacturedAt).toISOString() : null,
    note: movementForm.note.trim(),
  });

  if (success) {
    resetMovementForm();
  }
}

async function deactivateItem(item: ManufacturedAdminItem) {
  if (!canEditInventory.value) return;
  if (!window.confirm(`確定要停用 ${item.name} 嗎？`)) return;
  await manufacturedStore.deactivateItem(item.id);
}

onMounted(async () => {
  await manufacturedStore.loadManufactured();
  selectedItemId.value = manufacturedStore.activeItems[0]?.id ?? manufacturedStore.items[0]?.id ?? "";
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.35fr_0.85fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Manufactured Inventory</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">製成品管理</h3>
          <p class="mt-2 text-sm text-slate-400">管理已完成加工、但尚未最終包裝或仍需後續處理的製成品庫存。</p>
        </div>
        <button
          v-if="canEditInventory"
          class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950"
          @click="openCreateForm"
        >
          新增製成品
        </button>
      </div>

      <p v-if="manufacturedStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ manufacturedStore.errorMessage }}</p>
      <p v-if="!canEditInventory" class="mt-4 text-sm text-amber-200">目前帳號只有瀏覽權限，不能建立或調整製成品庫存。</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">SKU</th>
              <th class="px-4 py-3 font-medium">製成品</th>
              <th class="px-4 py-3 font-medium">庫存單位</th>
              <th class="px-4 py-3 font-medium">採購單位</th>
              <th class="px-4 py-3 font-medium">換算</th>
              <th class="px-4 py-3 font-medium">現有庫存</th>
              <th class="px-4 py-3 font-medium">警戒值</th>
              <th class="px-4 py-3 font-medium">最新成本</th>
              <th class="px-4 py-3 font-medium">狀態</th>
              <th class="px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="manufacturedStore.loading">
              <td colspan="10" class="px-4 py-12 text-center text-slate-400">正在載入製成品資料...</td>
            </tr>
            <tr v-for="item in manufacturedStore.items" :key="item.id" class="bg-slate-950/25">
              <td class="px-4 py-4 text-slate-300">{{ item.sku }}</td>
              <td class="px-4 py-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70 text-[10px] text-slate-500">
                    <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" class="h-full w-full object-cover" />
                    <span v-else>無圖</span>
                  </div>
                  <div class="min-w-0">
                    <p class="font-medium text-white">{{ item.name }}</p>
                    <p class="mt-1 text-xs text-slate-400">{{ item.description || "尚無描述" }}</p>
                  </div>
                </div>
              </td>
              <td class="px-4 py-4 text-slate-300">{{ item.unit }}</td>
              <td class="px-4 py-4 text-slate-300">{{ item.purchaseUnit }}</td>
              <td class="px-4 py-4 text-slate-300">1 {{ item.purchaseUnit }} = {{ item.purchaseToStockRatio }} {{ item.unit }}</td>
              <td class="px-4 py-4 text-white">{{ item.quantityOnHand }} {{ item.unit }}</td>
              <td class="px-4 py-4 text-slate-300">{{ item.reorderLevel }} {{ item.unit }}</td>
              <td class="px-4 py-4 text-white">
                <p>{{ displayUnitCost(item.latestUnitCost, item.unit) }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ displayUnitCost(item.latestPurchaseUnitCost, item.purchaseUnit) }}</p>
              </td>
              <td class="px-4 py-4">
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="item.active ? (item.lowStock ? 'bg-brand-coral/15 text-brand-coral' : 'bg-brand-aqua/15 text-brand-aqua') : 'bg-slate-700/40 text-slate-400'"
                >
                  {{ !item.active ? "已停用" : item.lowStock ? "需補貨" : "正常" }}
                </span>
              </td>
              <td class="px-4 py-4">
                <div class="flex gap-2">
                  <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="openEditForm(item)">編輯</button>
                  <button class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" :disabled="!item.active" @click="deactivateItem(item)">停用</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="mt-6 grid gap-6 xl:grid-cols-2">
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-white">最近異動</p>
            <p class="text-xs text-slate-400">只顯示目前選取項目</p>
          </div>
          <div class="mt-4 space-y-3">
            <div v-for="movement in itemMovements" :key="movement.id" class="rounded-2xl border border-white/8 bg-slate-950/40 p-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="font-medium text-white">{{ movement.manufacturedName }}</p>
                  <p class="mt-1 text-xs text-slate-400">{{ movement.sku }} / {{ movementLabel(movement.movementType) }}</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-semibold" :class="movement.quantityDelta > 0 ? 'text-brand-aqua' : 'text-brand-coral'">
                    {{ movement.quantityDelta > 0 ? "+" : "" }}{{ movement.quantityDelta }} {{ movement.unit }}
                  </p>
                  <p class="text-xs text-slate-400">結存 {{ movement.quantityAfter }} {{ movement.unit }}</p>
                </div>
              </div>
              <div class="mt-3 flex flex-wrap gap-4 text-xs text-slate-400">
                <span>{{ formatDateTime(movement.occurredAt) }}</span>
                <span>單位成本 {{ movement.unitCost === null ? "--" : formatCurrency(movement.unitCost) }}</span>
                <span>{{ movement.note || "無備註" }}</span>
              </div>
            </div>
            <p v-if="itemMovements.length === 0" class="text-sm text-slate-400">目前沒有異動紀錄。</p>
          </div>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-white">FIFO 批次</p>
            <p class="text-xs text-slate-400">依有效日期與收貨時間排序</p>
          </div>
          <div class="mt-4 space-y-3">
            <div v-for="lot in itemLots" :key="lot.id" class="rounded-2xl border border-white/8 bg-slate-950/40 p-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="font-medium text-white">{{ lot.manufacturedName }}</p>
                  <p class="mt-1 text-xs text-slate-400">批號 {{ lot.batchCode || "未填寫" }}</p>
                </div>
                <div class="text-right text-sm text-white">
                  <p>{{ lot.remainingQuantity }} / {{ lot.receivedQuantity }} {{ lot.unit }}</p>
                  <p class="mt-1 text-xs text-slate-400">效期 {{ lot.expiryDate ? formatDateTime(lot.expiryDate) : "未設定" }}</p>
                </div>
              </div>
              <div class="mt-3 flex flex-wrap gap-4 text-xs text-slate-400">
                <span>來源 {{ lot.sourceType }}</span>
                <span>收貨 {{ formatDateTime(lot.receivedAt) }}</span>
                <span>製造 {{ lot.manufacturedAt ? formatDateTime(lot.manufacturedAt) : "未設定" }}</span>
                <span>成本 {{ lot.unitCost === null ? "--" : formatCurrency(lot.unitCost) }}</span>
              </div>
            </div>
            <p v-if="itemLots.length === 0" class="text-sm text-slate-400">目前沒有批次 lot。</p>
          </div>
        </div>
      </div>
    </article>

    <aside class="space-y-6">
      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Editor</p>
            <h3 class="mt-2 text-2xl font-semibold text-white">{{ editingId ? "編輯製成品" : "新增製成品" }}</h3>
          </div>
          <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="isFormOpen = !isFormOpen">
            {{ isFormOpen ? "收合" : "展開" }}
          </button>
        </div>

        <div v-if="canEditInventory && isFormOpen" class="mt-6 space-y-4">
          <div v-if="formError" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
            {{ formError }}
          </div>
          <input v-model="form.sku" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="SKU" />
          <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="製成品名稱" />
          <input v-model="form.unit" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="庫存單位，例如 ml / g / pcs" />
          <input v-model="form.purchaseUnit" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="採購單位，例如 桶 / 袋 / 箱" />
          <input v-model="form.purchaseToStockRatio" type="number" min="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="1 採購單位換算的庫存數量" />
          <input v-model="form.reorderLevel" type="number" min="0" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="補貨警戒值" />
          <input v-model="form.latestUnitCost" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="最新單位成本" />
          <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <div class="flex items-center justify-between gap-3">
              <div>
                <p class="text-sm font-semibold text-white">圖片</p>
                <p class="mt-1 text-xs text-slate-400">支援 JPG、PNG、GIF、WebP，大小不可超過 2MB。</p>
              </div>
              <button
                v-if="form.imageUrl"
                type="button"
                class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral"
                @click="clearImage"
              >
                清除圖片
              </button>
            </div>
            <div class="mt-4 flex items-start gap-4">
              <div class="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70 text-[11px] text-slate-500">
                <img v-if="form.imageUrl" :src="form.imageUrl" :alt="form.name || '製成品圖片'" class="h-full w-full object-cover" />
                <span v-else>尚未上傳</span>
              </div>
              <div class="min-w-0 flex-1 space-y-3">
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/gif,image/webp"
                  class="block w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-slate-200 file:mr-4 file:rounded-xl file:border-0 file:bg-brand-aqua file:px-3 file:py-2 file:text-sm file:font-semibold file:text-slate-950"
                  @change="handleImageUpload"
                />
                <p v-if="form.uploadedImageName" class="text-xs text-slate-400">{{ form.uploadedImageName }}</p>
                <input
                  v-model="form.imageUrlInput"
                  type="url"
                  class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
                  placeholder="或輸入外部圖片網址"
                  @input="handleImageUrlInput"
                />
              </div>
            </div>
          </div>
          <textarea v-model="form.description" rows="4" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="描述或備註" />
          <button class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="manufacturedStore.saving" @click="submitForm">
            {{ manufacturedStore.saving ? "處理中..." : editingId ? "更新製成品" : "建立製成品" }}
          </button>
        </div>
      </section>

      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Stock Movement</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">製成品異動</h3>
        <div v-if="canEditInventory" class="mt-6 space-y-4">
          <select v-model="selectedItemId" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option value="">請選擇製成品</option>
            <option v-for="item in manufacturedStore.activeItems" :key="item.id" :value="item.id">
              {{ item.name }} ({{ item.sku }})
            </option>
          </select>
          <select v-model="movementForm.movementType" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option v-for="option in movementOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <div class="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
            {{ movementOptions.find((option) => option.value === movementForm.movementType)?.description }}
          </div>
          <input v-model="movementForm.quantity" type="number" min="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="異動數量" />
          <input v-model="movementForm.unitCost" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="單位成本（庫存單位）" />
          <input v-model="movementForm.batchCode" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="批號" />
          <div class="grid gap-4 md:grid-cols-2">
            <input v-model="movementForm.manufacturedAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
            <input v-model="movementForm.expiryDate" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </div>
          <textarea v-model="movementForm.note" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="異動備註" />
          <div v-if="selectedItem" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
            <p class="font-semibold text-white">{{ selectedItem.name }}</p>
            <p class="mt-2">現有庫存：{{ selectedItem.quantityOnHand }} {{ selectedItem.unit }}</p>
            <p class="mt-1">換算：1 {{ selectedItem.purchaseUnit }} = {{ selectedItem.purchaseToStockRatio }} {{ selectedItem.unit }}</p>
            <p class="mt-1">最近採購單位成本：{{ displayUnitCost(selectedItem.latestPurchaseUnitCost, selectedItem.purchaseUnit) }}</p>
          </div>
          <button class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="manufacturedStore.saving" @click="submitMovement">
            {{ manufacturedStore.saving ? "處理中..." : "建立異動" }}
          </button>
        </div>
      </section>
    </aside>
  </section>
</template>
