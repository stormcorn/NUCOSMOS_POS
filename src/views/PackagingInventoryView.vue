<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { usePackagingStore } from "@/stores/packaging";
import type { PackagingAdminItem, SupplyMovementType } from "@/types/packaging";
import { isEmbeddedImage, readImageFileAsDataUrl } from "@/utils/image-upload";
import { formatCurrency, formatDateTime } from "@/utils/format";

const authStore = useAuthStore();
const packagingStore = usePackagingStore();
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
  specification: "",
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
  { value: "PURCHASE_IN", label: "進貨入庫", description: "增加包材庫存，並建立 FIFO 批號 lot。" },
  { value: "ADJUSTMENT_IN", label: "盤點調增", description: "盤點後補回差異，增加包材庫存。" },
  { value: "ADJUSTMENT_OUT", label: "盤點調減", description: "盤點後扣除差異，減少包材庫存。" },
  { value: "DAMAGE_OUT", label: "損耗出庫", description: "破損、過期或汰除時使用，直接減少包材庫存。" },
  { value: "CONSUME_OUT", label: "耗用出庫", description: "非訂單流程的手動耗用，例如內部測試或報廢。" },
  { value: "RETURN_IN", label: "退回入庫", description: "退貨、更正或回補時增加包材庫存。" },
];

const selectedItem = computed(
  () => packagingStore.items.find((item) => item.id === selectedItemId.value) ?? null,
);

const itemLots = computed(() =>
  packagingStore.lots.filter((lot) => lot.packagingItemId === selectedItemId.value),
);

const itemMovements = computed(() =>
  packagingStore.movements.filter((movement) => movement.packagingItemId === selectedItemId.value).slice(0, 12),
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
  form.specification = "";
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
  if (!canEditInventory.value) {
    return;
  }

  resetForm();
  isFormOpen.value = true;
}

function openEditForm(item: PackagingAdminItem) {
  if (!canEditInventory.value) {
    return;
  }

  editingId.value = item.id;
  formError.value = "";
  form.sku = item.sku;
  form.name = item.name;
  form.unit = item.unit;
  form.purchaseUnit = item.purchaseUnit;
  form.purchaseToStockRatio = String(item.purchaseToStockRatio);
  form.specification = item.specification ?? "";
  form.imageUrl = item.imageUrl ?? "";
  form.imageUrlInput = isEmbeddedImage(item.imageUrl) ? "" : (item.imageUrl ?? "");
  form.uploadedImageName = isEmbeddedImage(item.imageUrl) ? "已上傳圖片" : "";
  form.description = item.description ?? "";
  form.reorderLevel = String(item.reorderLevel);
  form.latestUnitCost = item.latestUnitCost === null ? "" : item.latestUnitCost.toFixed(2);
  isFormOpen.value = true;
}

async function submitForm() {
  if (!canEditInventory.value) {
    return;
  }

  formError.value = "";

  if (!form.sku.trim() || !form.name.trim() || !form.unit.trim() || !form.purchaseUnit.trim()) {
    formError.value = "請完整填寫 SKU、名稱、庫存單位與採購單位。";
    return;
  }

  if (Number(form.purchaseToStockRatio) < 1) {
    formError.value = "單位換算比例至少要為 1。";
    return;
  }

  if (Number(form.reorderLevel) < 0) {
    formError.value = "補貨門檻不可小於 0。";
    return;
  }

  const payload = {
    sku: form.sku.trim(),
    name: form.name.trim(),
    unit: form.unit.trim(),
    purchaseUnit: form.purchaseUnit.trim(),
    purchaseToStockRatio: Number(form.purchaseToStockRatio),
    specification: form.specification.trim(),
    imageUrl: form.imageUrlInput.trim() || form.imageUrl.trim(),
    description: form.description.trim(),
    reorderLevel: Number(form.reorderLevel),
    latestUnitCost: form.latestUnitCost ? Number(form.latestUnitCost) : null,
  };

  const success = editingId.value
    ? await packagingStore.updateItem(editingId.value, payload)
    : await packagingStore.createItem(payload);

  if (success) {
    resetForm();
    isFormOpen.value = false;
  }
}

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

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
  if (!canEditInventory.value) {
    return;
  }

  formError.value = "";

  if (!selectedItemId.value) {
    formError.value = "請先選擇要異動的包裝品項。";
    return;
  }

  if (Number(movementForm.quantity) <= 0) {
    formError.value = "異動數量必須大於 0。";
    return;
  }

  const success = await packagingStore.submitMovement(selectedItemId.value, {
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

async function deactivateItem(item: PackagingAdminItem) {
  if (!canEditInventory.value) {
    return;
  }

  if (!window.confirm(`確定要停用包裝「${item.name}」嗎？`)) {
    return;
  }

  await packagingStore.deactivateItem(item.id);
}

onMounted(async () => {
  await packagingStore.loadPackaging();
  selectedItemId.value = packagingStore.activeItems[0]?.id ?? packagingStore.items[0]?.id ?? "";
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.35fr_0.85fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Packaging Inventory</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">包裝管理</h3>
          <p class="mt-2 text-sm text-slate-400">
            管理杯子、杯蓋、吸管等包材主檔、單位換算、FIFO lot 與最近異動。
          </p>
        </div>
        <button v-if="canEditInventory" class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" @click="openCreateForm">
          新增包裝
        </button>
      </div>

      <p v-if="packagingStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ packagingStore.errorMessage }}</p>
      <p v-if="!canEditInventory" class="mt-4 text-sm text-amber-200">你目前只有檢視權限，不能新增、編輯、停用包裝或建立包裝異動。</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">SKU</th>
              <th class="px-4 py-3 font-medium">包裝</th>
              <th class="px-4 py-3 font-medium">庫存單位</th>
              <th class="px-4 py-3 font-medium">採購單位</th>
              <th class="px-4 py-3 font-medium">換算</th>
              <th class="px-4 py-3 font-medium">目前庫存</th>
              <th class="px-4 py-3 font-medium">補貨門檻</th>
              <th class="px-4 py-3 font-medium">最近成本</th>
              <th class="px-4 py-3 font-medium">狀態</th>
              <th class="px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="packagingStore.loading">
              <td colspan="10" class="px-4 py-12 text-center text-slate-400">包裝資料載入中...</td>
            </tr>
            <tr v-for="item in packagingStore.items" :key="item.id" class="bg-slate-950/25">
              <td class="px-4 py-4 text-slate-300">{{ item.sku }}</td>
              <td class="px-4 py-4">
                <div class="flex items-center gap-3">
                  <div
                    class="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70 text-[10px] text-slate-500"
                  >
                    <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" class="h-full w-full object-cover" />
                    <span v-else>無圖</span>
                  </div>
                  <div class="min-w-0">
                    <p class="font-medium text-white">{{ item.name }}</p>
                    <p class="mt-1 text-xs text-slate-400">{{ item.specification || item.description || "無備註" }}</p>
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
                  {{ !item.active ? "已停用" : item.lowStock ? "低庫存" : "正常" }}
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
            <p class="text-xs text-slate-400">依目前選擇包裝顯示</p>
          </div>
          <div class="mt-4 space-y-3">
            <div v-for="movement in itemMovements" :key="movement.id" class="rounded-2xl border border-white/8 bg-slate-950/40 p-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="font-medium text-white">{{ movement.packagingName }}</p>
                  <p class="mt-1 text-xs text-slate-400">{{ movement.sku }} / {{ movementLabel(movement.movementType) }}</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-semibold" :class="movement.quantityDelta > 0 ? 'text-brand-aqua' : 'text-brand-coral'">
                    {{ movement.quantityDelta > 0 ? "+" : "" }}{{ movement.quantityDelta }} {{ movement.unit }}
                  </p>
                  <p class="text-xs text-slate-400">結餘 {{ movement.quantityAfter }} {{ movement.unit }}</p>
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
            <p class="text-sm font-semibold text-white">FIFO 批號庫存</p>
            <p class="text-xs text-slate-400">依效期優先扣庫</p>
          </div>
          <div class="mt-4 space-y-3">
            <div v-for="lot in itemLots" :key="lot.id" class="rounded-2xl border border-white/8 bg-slate-950/40 p-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="font-medium text-white">{{ lot.packagingName }}</p>
                  <p class="mt-1 text-xs text-slate-400">批號 {{ lot.batchCode || "未設定" }}</p>
                </div>
                <div class="text-right text-sm text-white">
                  <p>{{ lot.remainingQuantity }} / {{ lot.receivedQuantity }} {{ lot.unit }}</p>
                  <p class="mt-1 text-xs text-slate-400">效期 {{ lot.expiryDate ? formatDateTime(lot.expiryDate) : "未設定" }}</p>
                </div>
              </div>
              <div class="mt-3 flex flex-wrap gap-4 text-xs text-slate-400">
                <span>來源 {{ lot.sourceType }}</span>
                <span>入庫 {{ formatDateTime(lot.receivedAt) }}</span>
                <span>製造 {{ lot.manufacturedAt ? formatDateTime(lot.manufacturedAt) : "未設定" }}</span>
                <span>成本 {{ lot.unitCost === null ? "--" : formatCurrency(lot.unitCost) }}</span>
              </div>
            </div>
            <p v-if="itemLots.length === 0" class="text-sm text-slate-400">目前沒有批號 lot 資料。</p>
          </div>
        </div>
      </div>
    </article>

    <aside class="space-y-6">
      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Editor</p>
            <h3 class="mt-2 text-2xl font-semibold text-white">{{ editingId ? "編輯包裝" : "新增包裝" }}</h3>
          </div>
          <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="isFormOpen = !isFormOpen">
            {{ isFormOpen ? "收合" : "展開" }}
          </button>
        </div>

        <div v-if="canEditInventory && isFormOpen" class="mt-6 space-y-4">
          <div v-if="formError" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
            {{ formError }}
          </div>
          <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <p class="text-sm font-semibold text-white">基本資料</p>
            <div class="mt-4 grid gap-4">
              <label class="block">
                <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">SKU</span>
                <input v-model="form.sku" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="SKU" />
              </label>
              <label class="block">
                <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">包裝名稱</span>
                <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="輸入包裝名稱" />
              </label>
              <div class="grid gap-4 md:grid-cols-2">
                <label class="block">
                  <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">庫存單位</span>
                  <input v-model="form.unit" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="?? pcs" />
                </label>
                <label class="block">
                  <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">採購單位</span>
                  <input v-model="form.purchaseUnit" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="?? sleeve / box / carton" />
                </label>
              </div>
              <div class="grid gap-4 md:grid-cols-2">
                <label class="block">
                  <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">換算倍率</span>
                  <input v-model="form.purchaseToStockRatio" type="number" min="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="1 採購單位可換算多少庫存單位" />
                </label>
                <label class="block">
                  <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">??</span>
                  <input v-model="form.specification" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="?? 700cc ? / ???" />
                </label>
              </div>
              <div class="grid gap-4 md:grid-cols-2">
                <label class="block">
                  <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">補貨門檻</span>
                  <input v-model="form.reorderLevel" type="number" min="0" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="輸入補貨門檻" />
                </label>
                <label class="block">
                  <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">最近庫存單位成本</span>
                  <input v-model="form.latestUnitCost" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="輸入最新庫存單位成本" />
                </label>
              </div>
            </div>
          </div>
          <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <div class="flex items-center justify-between gap-3">
              <div>
                <p class="text-sm font-semibold text-white">包裝圖片</p>
                <p class="mt-1 text-xs text-slate-400">支援 JPG、PNG、GIF、WebP，上傳檔案不可超過 2MB。</p>
              </div>
              <button
                v-if="form.imageUrl"
                type="button"
                class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral"
                @click="clearImage"
              >
                移除圖片
              </button>
            </div>
            <div class="mt-4 flex items-start gap-4">
              <div class="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70 text-[11px] text-slate-500">
                <img v-if="form.imageUrl" :src="form.imageUrl" :alt="form.name || '包裝圖片'" class="h-full w-full object-cover" />
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
                  placeholder="圖片網址（可留空，改用上傳）"
                  @input="handleImageUrlInput"
                />
              </div>
            </div>
          </div>
          <label class="block">
            <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-400">包裝描述</span>
            <textarea v-model="form.description" rows="4" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="輸入包裝描述" />
          </label>
          <button class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="packagingStore.saving" @click="submitForm">
            {{ packagingStore.saving ? "處理中..." : editingId ? "更新包裝" : "建立包裝" }}
          </button>
        </div>
      </section>

      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Stock Movement</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">包裝異動</h3>
        <div v-if="canEditInventory" class="mt-6 space-y-4">
          <select v-model="selectedItemId" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option value="">請選擇包裝品項</option>
            <option v-for="item in packagingStore.activeItems" :key="item.id" :value="item.id">
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
          <input v-model="movementForm.unitCost" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="單位成本，進貨時建議填寫" />
          <input v-model="movementForm.batchCode" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="批號，進貨時可選填" />
          <div class="grid gap-4 md:grid-cols-2">
            <input v-model="movementForm.manufacturedAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
            <input v-model="movementForm.expiryDate" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </div>
          <textarea v-model="movementForm.note" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="異動備註" />
          <div v-if="selectedItem" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
            <p class="font-semibold text-white">{{ selectedItem.name }}</p>
            <p class="mt-2">目前庫存：{{ selectedItem.quantityOnHand }} {{ selectedItem.unit }}</p>
            <p class="mt-1">換算：1 {{ selectedItem.purchaseUnit }} = {{ selectedItem.purchaseToStockRatio }} {{ selectedItem.unit }}</p>
            <p class="mt-1">最近採購成本：{{ displayUnitCost(selectedItem.latestPurchaseUnitCost, selectedItem.purchaseUnit) }}</p>
          </div>
          <button class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="!canEditInventory || packagingStore.saving" @click="submitMovement">
            {{ packagingStore.saving ? "處理中..." : "建立異動" }}
          </button>
        </div>
      </section>
    </aside>
  </section>
</template>
