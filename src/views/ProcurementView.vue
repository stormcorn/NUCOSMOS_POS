<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useManufacturedStore } from "@/stores/manufactured";
import { useMaterialsStore } from "@/stores/materials";
import { usePackagingStore } from "@/stores/packaging";
import { useProcurementStore } from "@/stores/procurement";
import { formatCurrency, formatDateTime } from "@/utils/format";

type ProcurementItemType = "MATERIAL" | "MANUFACTURED" | "PACKAGING";

type DraftLine = {
  itemType: ProcurementItemType;
  itemId: string;
  itemName: string;
  itemSku: string;
  stockUnit: string;
  purchaseUnit: string;
  purchaseToStockRatio: number;
  orderedQuantity: number;
  unitCost: number | null;
  batchCode: string;
  manufacturedAt: string;
  expiryDate: string;
  note: string;
};

const authStore = useAuthStore();
const procurementStore = useProcurementStore();
const materialsStore = useMaterialsStore();
const manufacturedStore = useManufacturedStore();
const packagingStore = usePackagingStore();

const formError = ref("");
const canEditProcurement = computed(() => authStore.hasPermission(PERMISSIONS.PROCUREMENT_EDIT));

const orderForm = reactive({
  supplierId: "",
  note: "",
});

const lineDraft = reactive({
  itemType: "MATERIAL" as ProcurementItemType,
  itemId: "",
  orderedQuantity: "1",
  unitCost: "",
  batchCode: "",
  manufacturedAt: "",
  expiryDate: "",
  note: "",
});

const draftLines = ref<DraftLine[]>([]);

const availableItems = computed(() => {
  if (lineDraft.itemType === "MANUFACTURED") {
    return manufacturedStore.activeItems;
  }
  if (lineDraft.itemType === "PACKAGING") {
    return packagingStore.activeItems;
  }
  return materialsStore.activeItems;
});

const activeSuppliers = computed(() =>
  procurementStore.suppliers.filter((supplier) => supplier.active),
);

const selectedItem = computed(
  () => availableItems.value.find((item) => item.id === lineDraft.itemId) ?? null,
);

const draftEstimatedCost = computed(() =>
  draftLines.value.reduce((sum, line) => sum + (line.unitCost ?? 0) * line.orderedQuantity, 0),
);

function resetLineDraft() {
  lineDraft.itemId = availableItems.value[0]?.id ?? "";
  lineDraft.orderedQuantity = "1";
  lineDraft.unitCost = "";
  lineDraft.batchCode = "";
  lineDraft.manufacturedAt = "";
  lineDraft.expiryDate = "";
  lineDraft.note = "";
}

watch(
  () => lineDraft.itemType,
  () => {
    resetLineDraft();
  },
);

function itemTypeLabel(value: ProcurementItemType | string) {
  if (value === "PACKAGING") {
    return "包裝";
  }
  if (value === "MANUFACTURED") {
    return "製成品";
  }
  return "原料";
}

function displayUnitCost(value: number | null, unit: string) {
  return value === null ? "--" : `${formatCurrency(value)} / ${unit}`;
}

function addLine() {
  if (!canEditProcurement.value) {
    return;
  }

  formError.value = "";
  const item = selectedItem.value;

  if (!item) {
    formError.value = "請先選擇要採購的品項。";
    return;
  }

  if (Number(lineDraft.orderedQuantity) <= 0) {
    formError.value = "採購數量必須大於 0。";
    return;
  }

  draftLines.value.push({
    itemType: lineDraft.itemType,
    itemId: item.id,
    itemName: item.name,
    itemSku: item.sku,
    stockUnit: item.unit,
    purchaseUnit: item.purchaseUnit,
    purchaseToStockRatio: item.purchaseToStockRatio,
    orderedQuantity: Number(lineDraft.orderedQuantity),
    unitCost: lineDraft.unitCost ? Number(lineDraft.unitCost) : null,
    batchCode: lineDraft.batchCode.trim(),
    manufacturedAt: lineDraft.manufacturedAt,
    expiryDate: lineDraft.expiryDate,
    note: lineDraft.note.trim(),
  });

  resetLineDraft();
}

function removeLine(index: number) {
  if (!canEditProcurement.value) {
    return;
  }
  draftLines.value.splice(index, 1);
}

async function submitPurchaseOrder() {
  if (!canEditProcurement.value) {
    return;
  }

  formError.value = "";

  if (!orderForm.supplierId) {
    formError.value = "請先選擇供應商。";
    return;
  }

  if (draftLines.value.length === 0) {
    formError.value = "請至少加入一筆採購明細。";
    return;
  }

  const success = await procurementStore.createPurchaseOrderRecord({
    supplierId: orderForm.supplierId,
    note: orderForm.note.trim() || undefined,
    lines: draftLines.value.map((line) => ({
      itemType: line.itemType,
      itemId: line.itemId,
      orderedQuantity: line.orderedQuantity,
      unitCost: line.unitCost,
      batchCode: line.batchCode || undefined,
      expiryDate: line.expiryDate ? new Date(line.expiryDate).toISOString() : null,
      manufacturedAt: line.manufacturedAt
        ? new Date(line.manufacturedAt).toISOString()
        : null,
      note: line.note || undefined,
    })),
  });

  if (success) {
    draftLines.value = [];
    orderForm.note = "";
  }
}

async function receiveOrder(orderId: string) {
  if (!canEditProcurement.value) {
    return;
  }

  await procurementStore.receivePurchaseOrderRecord(orderId, "Received from admin web");
  await Promise.all([
    materialsStore.loadMaterials(),
    manufacturedStore.loadManufactured(),
    packagingStore.loadPackaging(),
  ]);
}

onMounted(async () => {
  await Promise.all([
    procurementStore.loadSuppliers(),
    procurementStore.loadPurchaseOrders(),
    procurementStore.loadReplenishmentSuggestions(),
    materialsStore.loadMaterials(),
    manufacturedStore.loadManufactured(),
    packagingStore.loadPackaging(),
  ]);

  orderForm.supplierId = activeSuppliers.value[0]?.id ?? "";
  lineDraft.itemId = availableItems.value[0]?.id ?? "";
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
    <aside class="space-y-6">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Purchase Order Builder</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">建立採購單</h3>
        <p class="mt-2 text-sm text-slate-400">
          一次建立原料、製成品與包裝的採購明細，後續可依批號、生產時間與效期完成收貨。
        </p>

        <div
          v-if="formError || procurementStore.errorMessage"
          class="mt-4 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral"
        >
          {{ formError || procurementStore.errorMessage }}
        </div>

        <div class="mt-6 space-y-4">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">供應商</span>
            <select
              v-model="orderForm.supplierId"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            >
              <option value="">請選擇供應商</option>
              <option
                v-for="supplier in activeSuppliers"
                :key="supplier.id"
                :value="supplier.id"
              >
                {{ supplier.name }} ({{ supplier.code }})
              </option>
            </select>
          </label>

          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">採購單備註</span>
            <textarea
              v-model="orderForm.note"
              rows="3"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            />
          </label>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Order Lines</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">採購明細</h3>

        <div class="mt-6 grid gap-4">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">品項類型</span>
            <select
              v-model="lineDraft.itemType"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            >
              <option value="MATERIAL">原料</option>
              <option value="MANUFACTURED">製成品</option>
              <option value="PACKAGING">包裝</option>
            </select>
          </label>

          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">採購品項</span>
            <select
              v-model="lineDraft.itemId"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            >
              <option value="">請選擇品項</option>
              <option v-for="item in availableItems" :key="item.id" :value="item.id">
                {{ item.name }} ({{ item.sku }})
              </option>
            </select>
          </label>

          <div
            v-if="selectedItem"
            class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300"
          >
            <p class="font-semibold text-white">{{ selectedItem.name }}</p>
            <p class="mt-2">目前庫存：{{ selectedItem.quantityOnHand }} {{ selectedItem.unit }}</p>
            <p class="mt-1">採購單位：{{ selectedItem.purchaseUnit }}</p>
            <p class="mt-1">
              換算：1 {{ selectedItem.purchaseUnit }} = {{ selectedItem.purchaseToStockRatio }}
              {{ selectedItem.unit }}
            </p>
            <p class="mt-1">
              最近採購成本：{{ displayUnitCost(selectedItem.latestPurchaseUnitCost, selectedItem.purchaseUnit) }}
            </p>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">採購數量</span>
              <input
                v-model="lineDraft.orderedQuantity"
                type="number"
                min="1"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
            </label>
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">採購單位成本</span>
              <input
                v-model="lineDraft.unitCost"
                type="number"
                min="0"
                step="0.01"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
            </label>
          </div>

          <input
            v-model="lineDraft.batchCode"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            placeholder="批號（選填）"
          />

          <div class="grid gap-4 md:grid-cols-2">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">製造時間</span>
              <input
                v-model="lineDraft.manufacturedAt"
                type="datetime-local"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
            </label>
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">效期</span>
              <input
                v-model="lineDraft.expiryDate"
                type="datetime-local"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
            </label>
          </div>

          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">明細備註</span>
            <input
              v-model="lineDraft.note"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            />
          </label>

          <button
            v-if="canEditProcurement"
            class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-white"
            @click="addLine"
          >
            加入採購明細
          </button>
        </div>

        <div class="mt-6 space-y-3">
          <article
            v-for="(line, index) in draftLines"
            :key="`${line.itemType}-${line.itemId}-${index}`"
            class="rounded-2xl border border-white/8 bg-white/4 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-white">{{ line.itemName }}</p>
                <p class="mt-1 text-xs text-slate-400">
                  {{ itemTypeLabel(line.itemType) }} / {{ line.itemSku }}
                </p>
              </div>
              <button
                v-if="canEditProcurement"
                class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral"
                @click="removeLine(index)"
              >
                移除
              </button>
            </div>
            <div class="mt-3 grid gap-2 text-sm text-slate-300">
              <p>採購數量：{{ line.orderedQuantity }} {{ line.purchaseUnit }}</p>
              <p>入庫換算：{{ line.orderedQuantity * line.purchaseToStockRatio }} {{ line.stockUnit }}</p>
              <p>採購單位成本：{{ displayUnitCost(line.unitCost, line.purchaseUnit) }}</p>
              <p>批號：{{ line.batchCode || "未填寫" }}</p>
              <p>
                製造時間：{{
                  line.manufacturedAt
                    ? formatDateTime(new Date(line.manufacturedAt).toISOString())
                    : "未填寫"
                }}
              </p>
              <p>
                效期：{{
                  line.expiryDate
                    ? formatDateTime(new Date(line.expiryDate).toISOString())
                    : "未填寫"
                }}
              </p>
              <p>{{ line.note || "無備註" }}</p>
            </div>
          </article>

          <article
            v-if="draftLines.length === 0"
            class="rounded-2xl border border-white/8 bg-white/4 p-4 text-sm text-slate-400"
          >
            目前尚未加入採購明細。
          </article>
        </div>

        <div class="mt-6 rounded-[1.5rem] border border-brand-aqua/20 bg-brand-aqua/5 p-4">
          <p class="text-sm font-semibold text-white">採購摘要</p>
          <div class="mt-3 grid gap-3 sm:grid-cols-2">
            <div class="rounded-2xl border border-white/8 bg-slate-950/55 p-3">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">明細數</p>
              <p class="mt-2 text-lg font-semibold text-white">{{ draftLines.length }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/55 p-3">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">預估採購金額</p>
              <p class="mt-2 text-lg font-semibold text-brand-aqua">
                {{ formatCurrency(draftEstimatedCost) }}
              </p>
            </div>
          </div>
        </div>

        <button
          v-if="canEditProcurement"
          class="mt-6 w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950"
          :disabled="procurementStore.saving"
          @click="submitPurchaseOrder"
        >
          {{ procurementStore.saving ? "建立中..." : "建立採購單" }}
        </button>
      </article>
    </aside>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between border-b border-white/8 pb-5">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Recent Purchase Orders</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">最近採購單</h3>
        </div>
        <button
          class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200"
          @click="procurementStore.loadPurchaseOrders()"
        >
          重新整理
        </button>
      </div>

      <div class="mt-6 space-y-4">
        <article
          v-if="procurementStore.loading"
          class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400"
        >
          採購單載入中...
        </article>
        <article
          v-for="order in procurementStore.purchaseOrders"
          :key="order.id"
          class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4"
        >
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="font-semibold text-white">{{ order.orderNumber }}</p>
              <p class="mt-1 text-xs text-slate-400">
                {{ order.supplierName }} / {{ order.createdByEmployeeCode }}
              </p>
              <p class="mt-1 text-xs text-slate-500">
                {{
                  order.receivedAt
                    ? `收貨時間 ${formatDateTime(order.receivedAt)}`
                    : `建立時間 ${formatDateTime(order.expectedAt ?? null)}`
                }}
              </p>
            </div>
            <div class="flex items-center gap-3">
              <span
                class="rounded-full px-3 py-1 text-xs font-semibold"
                :class="
                  order.status === 'RECEIVED'
                    ? 'bg-brand-aqua/15 text-brand-aqua'
                    : 'bg-amber-200/10 text-brand-amber'
                "
              >
                {{ order.status === "RECEIVED" ? "已收貨" : "待收貨" }}
              </span>
              <button
                v-if="canEditProcurement && order.status !== 'RECEIVED'"
                class="rounded-xl border border-brand-aqua/20 px-3 py-2 text-xs text-brand-aqua"
                :disabled="procurementStore.saving"
                @click="receiveOrder(order.id)"
              >
                確認收貨
              </button>
            </div>
          </div>

          <ul class="mt-4 space-y-2">
            <li
              v-for="line in order.lines"
              :key="line.id"
              class="rounded-xl border border-white/8 bg-slate-950/50 px-3 py-3 text-sm text-slate-300"
            >
              <p class="font-medium text-white">
                {{ itemTypeLabel(line.itemType) }} / {{ line.itemName }}
              </p>
              <p class="mt-1">
                採購數量 {{ line.orderedQuantity }} {{ line.unit }}
                <span class="text-slate-500">
                  / 入庫換算 {{ line.orderedQuantity * line.purchaseToStockRatio }} {{ line.stockUnit }}
                </span>
              </p>
              <p class="mt-1">
                已收貨 {{ line.receivedQuantity }} {{ line.unit }}
                <span class="text-slate-500">
                  / 已入庫 {{ line.receivedStockQuantity }} {{ line.stockUnit }}
                </span>
              </p>
              <p class="mt-1 text-slate-500">單位成本 {{ displayUnitCost(line.unitCost, line.unit) }}</p>
              <p class="mt-1 text-slate-500">批號 {{ line.batchCode || "未填寫" }}</p>
              <p class="mt-1 text-slate-500">
                製造時間 {{ line.manufacturedAt ? formatDateTime(line.manufacturedAt) : "未填寫" }}
              </p>
              <p class="mt-1 text-slate-500">
                效期 {{ line.expiryDate ? formatDateTime(line.expiryDate) : "未填寫" }}
              </p>
              <p class="mt-1 text-slate-500">{{ line.note || "無備註" }}</p>
            </li>
          </ul>
        </article>
        <article
          v-if="!procurementStore.loading && procurementStore.purchaseOrders.length === 0"
          class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400"
        >
          目前還沒有採購單紀錄。
        </article>
      </div>
    </article>
  </section>
</template>
