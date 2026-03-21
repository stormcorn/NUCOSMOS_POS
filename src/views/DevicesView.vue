<script setup lang="ts">
import { computed, onMounted, watch } from "vue";

import { useDeviceStore } from "@/stores/devices";
import { useStoreContextStore } from "@/stores/store-context";
import { formatDateTime, formatRelativeTime } from "@/utils/format";

const deviceStore = useDeviceStore();
const storeContextStore = useStoreContextStore();

const statusOptions = [
  { label: "全部狀態", value: "" },
  { label: "ACTIVE", value: "ACTIVE" },
  { label: "INACTIVE", value: "INACTIVE" },
];

const statusClasses = computed(() => ({
  ACTIVE: "bg-brand-aqua/15 text-brand-aqua",
  INACTIVE: "bg-brand-coral/12 text-brand-coral",
}));

onMounted(async () => {
  await storeContextStore.loadStores();
  await deviceStore.loadDevices(storeContextStore.selectedStoreCode || undefined);
});

watch(
  () => storeContextStore.selectedStoreCode,
  (storeCode) => {
    if (storeCode) {
      void deviceStore.loadDevices(storeCode, deviceStore.statusFilter);
    }
  },
);
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between border-b border-white/8 pb-5">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Device Fleet</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">POS 裝置狀態</h3>
        </div>
        <button class="rounded-2xl border border-white/10 px-4 py-2 text-sm text-slate-200 transition hover:border-brand-aqua/30 hover:text-white" @click="deviceStore.loadDevices(storeContextStore.selectedStoreCode || undefined, deviceStore.statusFilter)">
          手動刷新
        </button>
      </div>

      <div class="mt-5 flex flex-wrap gap-3">
        <select
          :value="deviceStore.statusFilter"
          class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
          @change="deviceStore.loadDevices(storeContextStore.selectedStoreCode || undefined, ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="option in statusOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>

      <p v-if="deviceStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ deviceStore.errorMessage }}</p>

      <div class="mt-6 space-y-4">
        <article v-if="deviceStore.loading" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
          裝置資料載入中...
        </article>
        <article
          v-for="device in deviceStore.devices"
          :key="device.id"
          class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4"
        >
          <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-base font-semibold text-white">{{ device.name }}</p>
              <p class="mt-1 text-sm text-slate-400">{{ device.deviceCode }} ・ {{ device.storeCode }} ・ {{ device.platform }}</p>
            </div>
            <div class="flex items-center gap-3">
              <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="statusClasses[device.status] || 'bg-amber-200/10 text-brand-amber'">
                {{ device.status }}
              </span>
              <span class="text-sm text-slate-400">最後回報 {{ formatRelativeTime(device.lastSeenAt) }}</span>
            </div>
          </div>
          <p class="mt-3 text-sm text-slate-500">最後 heartbeat 時間：{{ formatDateTime(device.lastSeenAt) }}</p>
        </article>
        <article v-if="!deviceStore.loading && deviceStore.devices.length === 0" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-400">
          目前沒有符合條件的裝置。
        </article>
      </div>
    </article>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Heartbeat Insight</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">現場巡檢建議</h3>

      <ul class="mt-6 space-y-4 text-sm text-slate-300">
        <li class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          目前 `ACTIVE` 裝置共有 {{ deviceStore.onlineCount }} 台，適合優先作為前台串接驗證目標。
        </li>
        <li class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          若某台裝置超過 10 分鐘沒有 heartbeat，管理後台後續可補警示與推播。
        </li>
        <li class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          目前選定門市為 {{ storeContextStore.selectedStore?.name || storeContextStore.selectedStoreCode || "未選擇" }}。
        </li>
      </ul>
    </article>
  </section>
</template>
