<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { usePackagingStore } from "@/stores/packaging";
import type { PackagingAdminItem, SupplyMovementType } from "@/types/packaging";

const packagingStore = usePackagingStore();

const isFormOpen = ref(false);
const editingId = ref<string | null>(null);
const selectedItemId = ref("");
const formError = ref("");
const form = reactive({
  sku: "",
  name: "",
  unit: "",
  specification: "",
  description: "",
  reorderLevel: "0",
  latestUnitCost: "",
});
const movementForm = reactive<{
  movementType: SupplyMovementType;
  quantity: string;
  unitCost: string;
  note: string;
}>({
  movementType: "PURCHASE_IN",
  quantity: "1",
  unitCost: "",
  note: "",
});

const movementOptions: Array<{ value: SupplyMovementType; label: string; description: string }> = [
  { value: "PURCHASE_IN", label: "進貨入庫", description: "杯、蓋、吸管與提袋等包材入庫。" },
  { value: "ADJUSTMENT_IN", label: "盤點調增", description: "盤點後補回帳上少記的包材數量。" },
  { value: "ADJUSTMENT_OUT", label: "盤點調減", description: "盤點後扣除帳上多記的包材數量。" },
  { value: "DAMAGE_OUT", label: "報損出庫", description: "破損、污染或不可用包材扣減。" },
  { value: "CONSUME_OUT", label: "營運耗用", description: "門市使用或贈送造成的包材消耗。" },
  { value: "RETURN_IN", label: "退貨回庫", description: "原先出庫的包材重新回庫。" },
];

const selectedItem = computed(
  () => packagingStore.items.find((item) => item.id === selectedItemId.value) ?? null,
);

const selectedMovementDescription = computed(
  () => movementOptions.find((option) => option.value === movementForm.movementType)?.description ?? "",
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

function movementLabel(type: SupplyMovementType) {
  return movementOptions.find((option) => option.value === type)?.label ?? type;
}

function resetForm() {
  editingId.value = null;
  formError.value = "";
  form.sku = "";
  form.name = "";
  form.unit = "";
  form.specification = "";
  form.description = "";
  form.reorderLevel = "0";
  form.latestUnitCost = "";
}

function resetMovementForm() {
  movementForm.movementType = "PURCHASE_IN";
  movementForm.quantity = "1";
  movementForm.unitCost = "";
  movementForm.note = "";
}

function openCreateForm() {
  resetForm();
  isFormOpen.value = true;
}

function openEditForm(item: PackagingAdminItem) {
  editingId.value = item.id;
  form.sku = item.sku;
  form.name = item.name;
  form.unit = item.unit;
  form.specification = item.specification ?? "";
  form.description = item.description ?? "";
  form.reorderLevel = String(item.reorderLevel);
  form.latestUnitCost = item.latestUnitCost === null ? "" : item.latestUnitCost.toFixed(2);
  isFormOpen.value = true;
}

async function submitForm() {
  formError.value = "";

  if (!form.sku.trim() || !form.name.trim() || !form.unit.trim()) {
    formError.value = "SKU、名稱與單位為必填欄位";
    return;
  }

  if (Number(form.reorderLevel) < 0) {
    formError.value = "補貨門檻不能小於 0";
    return;
  }

  const payload = {
    sku: form.sku.trim(),
    name: form.name.trim(),
    unit: form.unit.trim(),
    specification: form.specification.trim(),
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

async function submitMovement() {
  formError.value = "";

  if (!selectedItemId.value) {
    formError.value = "請先選擇包裝品項";
    return;
  }

  if (Number(movementForm.quantity) <= 0) {
    formError.value = "異動數量必須大於 0";
    return;
  }

  const success = await packagingStore.submitMovement(selectedItemId.value, {
    movementType: movementForm.movementType,
    quantity: Number(movementForm.quantity),
    unitCost: movementForm.unitCost ? Number(movementForm.unitCost) : null,
    note: movementForm.note,
  });

  if (success) {
    resetMovementForm();
  }
}

async function deactivateItem(item: PackagingAdminItem) {
  if (!window.confirm(`確定要停用包裝品項「${item.name}」嗎？`)) {
    return;
  }

  await packagingStore.deactivateItem(item.id);
}

onMounted(async () => {
  await packagingStore.loadPackaging();
  selectedItemId.value = packagingStore.activeItems[0]?.id ?? packagingStore.items[0]?.id ?? "";
  resetMovementForm();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.35fr_0.85fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-amber-200/70">Packaging Inventory</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">包裝管理</h3>
          <p class="mt-2 text-sm text-slate-400">管理杯、蓋、吸管、提袋等包裝主檔、庫存與補貨門檻。</p>
        </div>
        <button class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110" @click="openCreateForm">
          新增包裝品項
        </button>
      </div>

      <p v-if="packagingStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ packagingStore.errorMessage }}</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">SKU</th>
              <th class="px-4 py-3 font-medium">品項</th>
              <th class="px-4 py-3 font-medium">規格</th>
              <th class="px-4 py-3 font-medium">單位</th>
              <th class="px-4 py-3 font-medium">庫存</th>
              <th class="px-4 py-3 font-medium">補貨門檻</th>
              <th class="px-4 py-3 font-medium">最新成本</th>
              <th class="px-4 py-3 font-medium">狀態</th>
              <th class="px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="packagingStore.loading">
              <td colspan="9" class="px-4 py-12 text-center text-slate-400">包裝資料載入中...</td>
            </tr>
            <tr v-for="item in packagingStore.items" :key="item.id" class="bg-slate-950/25">
              <td class="px-4 py-4 text-slate-300">{{ item.sku }}</td>
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ item.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ item.description || "未填寫說明" }}</p>
              </td>
              <td class="px-4 py-4 text-slate-300">{{ item.specification || "--" }}</td>
              <td class="px-4 py-4 text-slate-300">{{ item.unit }}</td>
              <td class="px-4 py-4 text-white">{{ item.quantityOnHand }}</td>
              <td class="px-4 py-4 text-slate-300">{{ item.reorderLevel }}</td>
              <td class="px-4 py-4 text-white">{{ formatCurrency(item.latestUnitCost) }}</td>
              <td class="px-4 py-4">
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="item.lowStock ? 'bg-brand-coral/15 text-brand-coral' : 'bg-brand-aqua/15 text-brand-aqua'"
                >
                  {{ item.lowStock ? "低庫存" : item.active ? "正常" : "已停用" }}
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

      <div class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold text-white">最近包裝異動</p>
          <p class="text-xs text-slate-400">最近 100 筆</p>
        </div>
        <div class="mt-4 space-y-3">
          <div v-for="movement in packagingStore.movements" :key="movement.id" class="rounded-2xl border border-white/8 bg-slate-950/40 p-4">
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="font-medium text-white">{{ movement.packagingName }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ movement.sku }} 繚 {{ movementLabel(movement.movementType) }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold" :class="movement.quantityDelta > 0 ? 'text-brand-aqua' : 'text-brand-coral'">
                  {{ movement.quantityDelta > 0 ? "+" : "" }}{{ movement.quantityDelta }}
                </p>
                <p class="text-xs text-slate-400">結餘 {{ movement.quantityAfter }} {{ movement.unit }}</p>
              </div>
            </div>
            <div class="mt-3 flex flex-wrap gap-4 text-xs text-slate-400">
              <span>{{ formatDateTime(movement.occurredAt) }}</span>
              <span>成本 {{ formatCurrency(movement.unitCost) }}</span>
              <span>{{ movement.note || "未填寫備註" }}</span>
            </div>
          </div>
        </div>
      </div>
    </article>

    <aside class="space-y-6">
      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-amber-200/70">Editor</p>
            <h3 class="mt-2 text-2xl font-semibold text-white">{{ editingId ? "編輯包裝品項" : "新增包裝品項" }}</h3>
          </div>
          <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="isFormOpen = !isFormOpen">
            {{ isFormOpen ? "收合" : "展開" }}
          </button>
        </div>

        <div v-if="isFormOpen" class="mt-6 space-y-4">
          <div v-if="formError" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
            {{ formError }}
          </div>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">SKU</span>
            <input v-model="form.sku" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">品項名稱</span>
            <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">單位</span>
            <input v-model="form.unit" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="pcs / pack / roll" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">規格</span>
            <input v-model="form.specification" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="700ml / 大杯 / 紙袋 L" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">補貨門檻</span>
            <input v-model="form.reorderLevel" type="number" min="0" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">最新單位成本</span>
            <input v-model="form.latestUnitCost" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">說明</span>
            <textarea v-model="form.description" rows="4" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <div class="flex gap-3">
            <button class="flex-1 rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="packagingStore.saving" @click="submitForm">
              {{ packagingStore.saving ? "送出中..." : editingId ? "更新包裝品項" : "建立包裝品項" }}
            </button>
            <button class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200" @click="resetForm">重置</button>
          </div>
        </div>
      </section>

      <section class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-amber-200/70">Stock Movement</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">包裝庫存異動</h3>
        <div class="mt-6 space-y-4">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">包裝品項</span>
            <select v-model="selectedItemId" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
              <option v-for="item in packagingStore.activeItems" :key="item.id" :value="item.id">
                {{ item.name }} ({{ item.sku }})
              </option>
            </select>
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">異動類型</span>
            <select v-model="movementForm.movementType" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
              <option v-for="option in movementOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <div class="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
            {{ selectedMovementDescription }}
          </div>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">數量</span>
            <input v-model="movementForm.quantity" type="number" min="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">單位成本</span>
            <input v-model="movementForm.unitCost" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">備註</span>
            <textarea v-model="movementForm.note" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <div v-if="selectedItem" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
            <p class="font-semibold text-white">{{ selectedItem.name }}</p>
            <p class="mt-2">目前庫存：{{ selectedItem.quantityOnHand }} {{ selectedItem.unit }}</p>
            <p class="mt-1">補貨門檻：{{ selectedItem.reorderLevel }}</p>
            <p class="mt-1">最新成本：{{ formatCurrency(selectedItem.latestUnitCost) }}</p>
          </div>
          <button class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="packagingStore.saving" @click="submitMovement">
            {{ packagingStore.saving ? "送出中..." : "建立包裝異動" }}
          </button>
        </div>
      </section>
    </aside>
  </section>
</template>
