<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { ApiError } from "@/api/http";
import {
  createRedeemPrize,
  deactivateRedeemPrize,
  fetchRedeemPrizes,
  type RedeemPrizeAdminItem,
  updateRedeemPrize,
} from "@/api/redeem-prizes";
import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";

type PrizeForm = {
  name: string;
  description: string;
  probabilityPercent: string;
  remainingQuantity: string;
  active: boolean;
  displayOrder: string;
};

const authStore = useAuthStore();

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const selectedPrizeId = ref<string | null>(null);
const prizes = ref<RedeemPrizeAdminItem[]>([]);
const form = ref<PrizeForm>(createEmptyForm());

const canEdit = computed(() => authStore.hasPermission(PERMISSIONS.SETTINGS_EDIT));
const selectedPrize = computed(() =>
  prizes.value.find((prize) => prize.id === selectedPrizeId.value) ?? null,
);
const activeProbabilityTotal = computed(() =>
  prizes.value
    .filter((prize) => prize.active)
    .reduce((sum, prize) => sum + Number(prize.probabilityPercent ?? 0), 0),
);

function createEmptyForm(): PrizeForm {
  return {
    name: "",
    description: "",
    probabilityPercent: "0",
    remainingQuantity: "0",
    active: true,
    displayOrder: String(prizes.value.length),
  };
}

function resetForm() {
  selectedPrizeId.value = null;
  form.value = createEmptyForm();
  errorMessage.value = "";
  successMessage.value = "";
}

function selectPrize(prize: RedeemPrizeAdminItem) {
  selectedPrizeId.value = prize.id;
  form.value = {
    name: prize.name,
    description: prize.description ?? "",
    probabilityPercent: String(prize.probabilityPercent),
    remainingQuantity: String(prize.remainingQuantity),
    active: prize.active,
    displayOrder: String(prize.displayOrder),
  };
  errorMessage.value = "";
  successMessage.value = "";
}

async function loadPrizes() {
  loading.value = true;
  errorMessage.value = "";

  try {
    prizes.value = await fetchRedeemPrizes();
    if (selectedPrizeId.value) {
      const latest = prizes.value.find((prize) => prize.id === selectedPrizeId.value);
      if (latest) {
        selectPrize(latest);
      }
    }
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "載入抽獎獎項失敗。";
  } finally {
    loading.value = false;
  }
}

async function savePrize() {
  if (!canEdit.value || saving.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const payload = {
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      probabilityPercent: Number(form.value.probabilityPercent),
      remainingQuantity: Number(form.value.remainingQuantity),
      active: form.value.active,
      displayOrder: Number(form.value.displayOrder),
    };

    const saved = selectedPrizeId.value
      ? await updateRedeemPrize(selectedPrizeId.value, payload)
      : await createRedeemPrize(payload);

    successMessage.value = `已儲存獎項：${saved.name}`;
    await loadPrizes();
    selectPrize(saved);
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "儲存抽獎獎項失敗。";
  } finally {
    saving.value = false;
  }
}

async function deactivateSelectedPrize() {
  if (!canEdit.value || !selectedPrizeId.value || saving.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const result = await deactivateRedeemPrize(selectedPrizeId.value);
    successMessage.value = `已停用獎項：${result.name}`;
    await loadPrizes();
    selectPrize(result);
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "停用抽獎獎項失敗。";
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  await loadPrizes();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between border-b border-white/8 pb-5">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Redeem Prize Pool</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">抽獎獎項管理</h3>
          <p class="mt-2 text-sm text-slate-400">
            設定各獎品的中獎機率與剩餘數量。啟用中的獎項機率總和不可超過 100%。
          </p>
        </div>
        <button
          class="rounded-2xl border border-white/10 px-4 py-2 text-sm text-slate-200 transition hover:border-brand-aqua/30 hover:text-white disabled:opacity-60"
          :disabled="loading"
          @click="loadPrizes"
        >
          重新整理
        </button>
      </div>

      <div class="mt-6 grid gap-4 md:grid-cols-3">
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-500">Active Probability</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ activeProbabilityTotal.toFixed(2) }}%</p>
          <p class="mt-2 text-xs text-slate-400">啟用中獎項的總機率。</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-500">Prize Count</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ prizes.length }}</p>
          <p class="mt-2 text-xs text-slate-400">包含停用或售完後保留紀錄的獎項。</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-500">Lose Reward</p>
          <p class="mt-3 text-lg font-semibold text-white">未中獎 +1 點</p>
          <p class="mt-2 text-xs text-slate-400">每累積 5 點自動發 50 元抵用券。</p>
        </div>
      </div>

      <p v-if="errorMessage" class="mt-4 text-sm text-brand-coral">{{ errorMessage }}</p>
      <p v-if="successMessage" class="mt-4 text-sm text-emerald-300">{{ successMessage }}</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-sm">
          <thead class="bg-white/4 text-left text-slate-400">
            <tr>
              <th class="px-4 py-3">獎項</th>
              <th class="px-4 py-3">機率</th>
              <th class="px-4 py-3">剩餘數量</th>
              <th class="px-4 py-3">狀態</th>
              <th class="px-4 py-3 text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/6 bg-slate-950/35 text-slate-200">
            <tr v-for="prize in prizes" :key="prize.id">
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ prize.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ prize.description || "未填寫說明" }}</p>
              </td>
              <td class="px-4 py-4">{{ Number(prize.probabilityPercent).toFixed(2) }}%</td>
              <td class="px-4 py-4">{{ prize.remainingQuantity }}</td>
              <td class="px-4 py-4">
                <span
                  :class="[
                    'rounded-full px-2.5 py-1 text-xs font-semibold',
                    prize.active ? 'bg-emerald-400/15 text-emerald-300' : 'bg-slate-400/15 text-slate-300',
                  ]"
                >
                  {{ prize.active ? "啟用中" : "已停用" }}
                </span>
              </td>
              <td class="px-4 py-4 text-right">
                <button
                  class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-brand-aqua/30 hover:text-white"
                  @click="selectPrize(prize)"
                >
                  編輯
                </button>
              </td>
            </tr>
            <tr v-if="!loading && prizes.length === 0">
              <td colspan="5" class="px-4 py-10 text-center text-slate-400">目前尚未建立抽獎獎項。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Prize Editor</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">
            {{ selectedPrize ? "編輯獎項" : "新增獎項" }}
          </h3>
        </div>
        <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="resetForm">
          新增獎項
        </button>
      </div>

      <div class="mt-6 space-y-4">
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">獎項名稱</span>
          <input
            v-model="form.name"
            type="text"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            placeholder="例如：免費升級大杯"
          />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">獎項說明</span>
          <textarea
            v-model="form.description"
            rows="3"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            placeholder="例如：請向店員出示中獎畫面兌領"
          />
        </label>

        <div class="grid gap-4 md:grid-cols-2">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">中獎機率 (%)</span>
            <input
              v-model="form.probabilityPercent"
              type="number"
              min="0"
              max="100"
              step="0.01"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            />
          </label>

          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">剩餘數量</span>
            <input
              v-model="form.remainingQuantity"
              type="number"
              min="0"
              step="1"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            />
          </label>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <label class="block">
            <span class="mb-2 block text-sm text-slate-300">顯示順序</span>
            <input
              v-model="form.displayOrder"
              type="number"
              min="0"
              step="1"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
            />
          </label>

          <label class="flex items-center gap-3 rounded-2xl border border-white/8 bg-white/4 px-4 py-4 text-sm text-slate-200">
            <input v-model="form.active" type="checkbox" />
            <span>啟用此獎項</span>
          </label>
        </div>

        <p class="rounded-2xl border border-white/8 bg-white/4 px-4 py-4 text-sm leading-7 text-slate-300">
          公開兌獎頁會顯示每個獎項的中獎機率與剩餘數量。若顧客未中獎，會看到「銘謝惠顧，再接再厲」並獲得 1 點；
          每累積 5 點會自動發 50 元抵用券。
        </p>

        <div class="flex gap-3">
          <button
            class="flex-1 rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-70"
            :disabled="!canEdit || saving"
            @click="savePrize"
          >
            {{ saving ? "儲存中..." : selectedPrizeId ? "更新獎項" : "建立獎項" }}
          </button>
          <button
            v-if="selectedPrizeId"
            class="rounded-2xl border border-brand-coral/20 px-4 py-3 text-sm text-brand-coral"
            :disabled="!canEdit || saving"
            @click="deactivateSelectedPrize"
          >
            停用
          </button>
        </div>
      </div>
    </aside>
  </section>
</template>
