<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

import { ApiError } from "@/api/http";
import { fetchStoreReceiptSettings, updateStoreReceiptSettings } from "@/api/store-settings";
import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useStoreContextStore } from "@/stores/store-context";

const authStore = useAuthStore();
const storeContextStore = useStoreContextStore();

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const receiptFooterText = ref("");

const selectedStore = computed(() => storeContextStore.selectedStore);
const canEdit = computed(() => authStore.hasPermission(PERMISSIONS.SETTINGS_EDIT));
const previewLines = computed(() =>
  receiptFooterText.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0),
);

async function loadSettings() {
  const store = selectedStore.value;
  if (!store) {
    receiptFooterText.value = "";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const settings = await fetchStoreReceiptSettings(store.id);
    receiptFooterText.value = settings.receiptFooterText ?? "";
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "載入收據自訂內容失敗。";
  } finally {
    loading.value = false;
  }
}

async function saveSettings() {
  const store = selectedStore.value;
  if (!store || !canEdit.value || saving.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const settings = await updateStoreReceiptSettings(store.id, receiptFooterText.value);
    receiptFooterText.value = settings.receiptFooterText ?? "";
    successMessage.value = `已更新 ${settings.storeName} 的收據自訂內容。`;
    await storeContextStore.loadStores();
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "儲存收據自訂內容失敗。";
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  if (storeContextStore.stores.length === 0) {
    await storeContextStore.loadStores();
  }
  await loadSettings();
});

watch(
  () => storeContextStore.selectedStoreCode,
  async () => {
    await loadSettings();
  },
);
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between border-b border-white/8 pb-5">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Receipt Custom Block</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">收據自訂內容</h3>
          <p class="mt-2 text-sm text-slate-400">
            可設定印在收據下方的固定備註，後台與 POS 會共用同一份門市內容。
          </p>
        </div>
        <button
          class="rounded-2xl border border-white/10 px-4 py-2 text-sm text-slate-200 transition hover:border-brand-aqua/30 hover:text-white disabled:opacity-60"
          :disabled="loading"
          @click="loadSettings"
        >
          重新整理
        </button>
      </div>

      <div class="mt-6 space-y-4">
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-500">Current Store</p>
          <p class="mt-3 text-lg font-semibold text-white">
            {{ selectedStore?.name || "尚未選擇門市" }}
          </p>
          <p class="mt-1 text-sm text-slate-400">
            {{ selectedStore?.code || "請先在左上角選擇門市" }}
          </p>
        </div>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">收據自訂內容</span>
          <textarea
            v-model="receiptFooterText"
            rows="8"
            maxlength="1000"
            class="w-full rounded-[1.5rem] border border-white/10 bg-slate-900/80 px-4 py-4 text-sm leading-7 text-white outline-none"
            placeholder="例如：\n本店商品售出恕不退換\n營業時間 10:00 - 22:00"
            :disabled="loading || !selectedStore"
          />
        </label>

        <p class="text-xs text-slate-500">
          會印在 Android 系統列印與熱感收據下方。熱感印表機若不支援中文，會自動轉成可列印字元。
        </p>

        <p v-if="errorMessage" class="text-sm text-brand-coral">{{ errorMessage }}</p>
        <p v-if="successMessage" class="text-sm text-emerald-300">{{ successMessage }}</p>

        <div class="flex gap-3">
          <button
            class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-70"
            :disabled="!canEdit || saving || !selectedStore"
            @click="saveSettings"
          >
            {{ saving ? "儲存中..." : "儲存內容" }}
          </button>
          <button
            class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200"
            :disabled="loading || saving"
            @click="receiptFooterText = ''"
          >
            清空
          </button>
        </div>
      </div>
    </article>

    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Receipt Preview</p>
      <h3 class="mt-2 text-2xl font-semibold text-white">列印預覽</h3>
      <div class="mt-6 rounded-[1.75rem] border border-white/8 bg-white/4 p-5">
        <p class="text-center text-sm font-semibold text-white">NUCOSMOS</p>
        <p class="mt-1 text-center text-xs text-slate-400">門市消費單據</p>
        <div class="my-4 border-t border-dashed border-white/10" />
        <div class="space-y-2 text-sm text-slate-300">
          <p>訂單編號：ORD-20260401-001</p>
          <p>付款方式：現金</p>
          <p>合計：$120.00</p>
        </div>
        <div class="my-4 border-t border-dashed border-white/10" />
        <div v-if="previewLines.length > 0" class="space-y-1 text-center text-sm leading-7 text-slate-200">
          <p v-for="line in previewLines" :key="line">{{ line }}</p>
        </div>
        <p v-else class="text-center text-sm text-slate-500">
          尚未設定自訂內容，收據將不會額外印出門市備註。
        </p>
      </div>
    </article>
  </section>
</template>
