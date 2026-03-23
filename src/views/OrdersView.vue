<script setup lang="ts">
import { computed, onMounted } from "vue";

import { useOrderStore } from "@/stores/orders";
import { formatCurrency, formatDateTime } from "@/utils/format";

const orderStore = useOrderStore();

const paymentStatusOptions = [
  { label: "全部付款狀態", value: "" },
  { label: "PAID", value: "PAID" },
  { label: "PARTIALLY_PAID", value: "PARTIALLY_PAID" },
  { label: "UNPAID", value: "UNPAID" },
  { label: "PARTIALLY_REFUNDED", value: "PARTIALLY_REFUNDED" },
  { label: "REFUNDED", value: "REFUNDED" },
];

const statusClasses = computed(() => ({
  CREATED: "bg-white/10 text-slate-200",
  PAID: "bg-brand-aqua/15 text-brand-aqua",
  PARTIALLY_REFUNDED: "bg-amber-200/10 text-brand-amber",
  REFUNDED: "bg-brand-coral/12 text-brand-coral",
  VOIDED: "bg-brand-coral/12 text-brand-coral",
}));

const paymentCards = computed(() => {
  const totalPaid = orderStore.items.reduce((sum, item) => sum + item.paidAmount, 0);
  const totalRefunded = orderStore.items.reduce((sum, item) => sum + item.refundedAmount, 0);

  return [
    { label: "已收金額", value: formatCurrency(totalPaid) },
    { label: "已退金額", value: formatCurrency(totalRefunded) },
    { label: "訂單筆數", value: `${orderStore.items.length}` },
  ];
});

function refundDispositionLabel(value: string) {
  return value === "DEFECTIVE" ? "回瑕疵庫存" : "回可售庫存";
}

onMounted(() => {
  void orderStore.loadOrders();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr_0.95fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Transaction Lane</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">訂單列表</h3>

      <div class="mt-6 flex flex-wrap gap-3">
        <select
          :value="orderStore.paymentStatusFilter"
          class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
          @change="orderStore.paymentStatusFilter = ($event.target as HTMLSelectElement).value; orderStore.loadOrders()"
        >
          <option v-for="option in paymentStatusOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>

      <p v-if="orderStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ orderStore.errorMessage }}</p>

      <div class="mt-6 space-y-4">
        <article v-if="orderStore.loading" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
          訂單資料載入中...
        </article>
        <article
          v-for="order in orderStore.items"
          :key="order.id"
          class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4"
        >
          <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="font-semibold text-white">{{ order.orderNumber }}</p>
              <p class="mt-1 text-sm text-slate-400">{{ order.storeCode }} · {{ formatDateTime(order.orderedAt) }}</p>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-base font-semibold text-white">{{ formatCurrency(order.totalAmount) }}</span>
              <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="statusClasses[order.status] || 'bg-white/10 text-slate-200'">
                {{ order.status }}
              </span>
            </div>
          </div>
          <p class="mt-3 text-sm text-slate-400">
            付款狀態 {{ order.paymentStatus }} · 已收 {{ formatCurrency(order.paidAmount) }} · 已退 {{ formatCurrency(order.refundedAmount) }}
          </p>
          <button
            class="mt-4 rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-brand-aqua/30 hover:text-white"
            @click="orderStore.loadOrderDetail(order.id)"
          >
            查看明細
          </button>
        </article>
        <article v-if="!orderStore.loading && orderStore.items.length === 0" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
          目前沒有符合條件的訂單。
        </article>
      </div>
    </article>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Settlement Notes</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">付款與退款摘要</h3>

      <div class="mt-6 grid gap-4 md:grid-cols-3">
        <div v-for="card in paymentCards" :key="card.label" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">{{ card.label }}</p>
          <p class="mt-2 text-3xl font-semibold text-white">{{ card.value }}</p>
        </div>
      </div>

      <div class="mt-6 rounded-[1.5rem] border border-brand-aqua/15 bg-gradient-to-br from-brand-aqua/12 to-transparent p-5">
        <p class="text-sm font-semibold text-white">第一階段已接上庫存流程</p>
        <p class="mt-2 text-sm leading-7 text-slate-300">
          訂單完成付款後會自動扣商品可售庫存，並依商品配方扣除原料與包裝。
          退款時若帶入退款品項，系統也會依回補庫位將商品回補到可售或瑕疵庫存。
        </p>
      </div>
    </article>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Order Detail</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">訂單明細</h3>
        </div>
        <button
          v-if="orderStore.detail"
          class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-white/20 hover:text-white"
          @click="orderStore.clearDetail()"
        >
          清除
        </button>
      </div>

      <div v-if="orderStore.detailLoading" class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
        訂單明細載入中...
      </div>

      <div v-else-if="orderStore.detail" class="mt-6 space-y-4">
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">訂單編號</p>
          <p class="mt-2 text-xl font-semibold text-white">{{ orderStore.detail.orderNumber }}</p>
          <p class="mt-2 text-sm text-slate-400">
            {{ orderStore.detail.storeCode }} · {{ formatDateTime(orderStore.detail.orderedAt) }}
          </p>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm font-semibold text-white">商品明細</p>
          <ul class="mt-3 space-y-3">
            <li v-for="item in orderStore.detail.items" :key="item.id" class="rounded-xl border border-white/6 bg-slate-950/50 px-3 py-3 text-sm text-slate-300">
              {{ item.productName }} × {{ item.quantity }} · {{ formatCurrency(item.lineTotalAmount) }}
            </li>
          </ul>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm font-semibold text-white">付款紀錄</p>
          <ul class="mt-3 space-y-3">
            <li v-for="payment in orderStore.detail.payments" :key="payment.id" class="rounded-xl border border-white/6 bg-slate-950/50 px-3 py-3 text-sm text-slate-300">
              {{ payment.paymentMethod }} · {{ payment.status }} · {{ formatCurrency(payment.amount) }}
            </li>
          </ul>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm font-semibold text-white">退款紀錄</p>
          <p v-if="orderStore.detail.refunds.length === 0" class="mt-3 text-sm text-slate-400">目前沒有退款紀錄。</p>
          <ul v-else class="mt-3 space-y-3">
            <li v-for="refund in orderStore.detail.refunds" :key="refund.id" class="rounded-xl border border-white/6 bg-slate-950/50 px-3 py-3 text-sm text-slate-300">
              <p class="font-medium text-white">
                {{ refund.refundMethod }} · {{ refund.status }} · {{ formatCurrency(refund.amount) }}
              </p>
              <p class="mt-1 text-xs text-slate-400">
                {{ refund.reason || "無退款原因" }} · {{ formatDateTime(refund.refundedAt) }}
              </p>
              <ul v-if="refund.items.length > 0" class="mt-3 space-y-2">
                <li v-for="refundItem in refund.items" :key="refundItem.id" class="rounded-xl border border-white/8 bg-white/5 px-3 py-2 text-xs text-slate-300">
                  {{ refundItem.productName }} × {{ refundItem.quantity }} · {{ refundDispositionLabel(refundItem.inventoryDisposition) }}
                </li>
              </ul>
            </li>
          </ul>
        </div>
      </div>

      <div v-else class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
        從左側選一筆訂單，即可查看商品、付款與退款回補明細。
      </div>

      <p v-if="orderStore.detailErrorMessage" class="mt-6 text-sm text-brand-coral">{{ orderStore.detailErrorMessage }}</p>
    </article>
  </section>
</template>
