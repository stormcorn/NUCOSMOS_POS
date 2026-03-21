<script setup lang="ts">
import { onMounted, reactive } from "vue";

import { useShiftStore } from "@/stores/shifts";
import { formatCurrency, formatDateTime } from "@/utils/format";

const shiftStore = useShiftStore();

const openForm = reactive({
  openingCashAmount: "0.00",
  note: "",
});

const closeForm = reactive({
  closingCashAmount: "0.00",
  note: "",
});

async function submitOpenShift() {
  await shiftStore.submitOpenShift(Number(openForm.openingCashAmount), openForm.note.trim());
}

async function submitCloseShift() {
  await shiftStore.submitCloseShift(Number(closeForm.closingCashAmount), closeForm.note.trim());
}

onMounted(() => {
  void shiftStore.loadCurrentShift();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Shift State</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">班次管理</h3>

      <div v-if="shiftStore.loading" class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
        班次資料載入中...
      </div>

      <div v-else-if="shiftStore.currentShift" class="mt-6 space-y-4">
        <div class="rounded-[1.5rem] border border-brand-aqua/20 bg-brand-aqua/10 p-5">
          <p class="text-sm text-slate-200">目前班次狀態</p>
          <p class="mt-2 text-3xl font-semibold text-white">{{ shiftStore.currentShift.status }}</p>
          <p class="mt-2 text-sm text-slate-300">
            {{ shiftStore.currentShift.storeCode }} ・ {{ shiftStore.currentShift.deviceCode }} ・ 開班人員 {{ shiftStore.currentShift.openedByEmployeeCode }}
          </p>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <p class="text-sm text-slate-400">開班現金</p>
            <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(shiftStore.currentShift.openingCashAmount) }}</p>
          </div>
          <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <p class="text-sm text-slate-400">預估現金</p>
            <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(shiftStore.currentShift.expectedCashAmount || 0) }}</p>
          </div>
          <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <p class="text-sm text-slate-400">現金營收</p>
            <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(shiftStore.currentShift.cashSalesAmount || 0) }}</p>
          </div>
          <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <p class="text-sm text-slate-400">刷卡營收</p>
            <p class="mt-2 text-2xl font-semibold text-white">{{ formatCurrency(shiftStore.currentShift.cardSalesAmount || 0) }}</p>
          </div>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm text-slate-400">開班時間</p>
          <p class="mt-2 text-base font-semibold text-white">{{ formatDateTime(shiftStore.currentShift.openedAt) }}</p>
        </div>
      </div>

      <div v-else class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
        目前沒有開啟中的班次，可以直接在右側建立新班次。
      </div>

      <p v-if="shiftStore.errorMessage" class="mt-6 text-sm text-brand-coral">{{ shiftStore.errorMessage }}</p>
    </article>

    <aside class="space-y-6">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Open Shift</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">建立班次</h3>

        <div class="mt-6 space-y-4">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">開班現金</span>
            <input v-model="openForm.openingCashAmount" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">備註</span>
            <textarea v-model="openForm.note" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <button class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:opacity-60" :disabled="shiftStore.saving" @click="submitOpenShift">
            {{ shiftStore.saving ? "送出中..." : "開班" }}
          </button>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Close Shift</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">關班結算</h3>

        <div class="mt-6 space-y-4">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">實際收現</span>
            <input v-model="closeForm.closingCashAmount" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">關班備註</span>
            <textarea v-model="closeForm.note" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          </label>
          <button class="w-full rounded-2xl border border-white/10 px-5 py-3 text-sm font-semibold text-slate-100 transition hover:border-brand-aqua/30 hover:text-white disabled:opacity-60" :disabled="shiftStore.saving || !shiftStore.currentShift" @click="submitCloseShift">
            {{ shiftStore.saving ? "送出中..." : "關班" }}
          </button>
        </div>
      </article>
    </aside>
  </section>
</template>
