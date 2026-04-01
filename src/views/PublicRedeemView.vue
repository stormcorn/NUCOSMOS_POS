<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";

import { claimRedeemTicket, fetchRedeemTicketByCode, fetchRedeemTicketByToken } from "@/api/public-redeem";
import { ApiError } from "@/api/http";
import type { ReceiptRedeemTicket } from "@/types/redeem";

const route = useRoute();

const loading = ref(false);
const claiming = ref(false);
const errorMessage = ref("");
const statusMessage = ref("");
const manualCode = ref("");
const ticket = ref<ReceiptRedeemTicket | null>(null);

const routeToken = computed(() => {
  const rawValue = route.params.token;
  return typeof rawValue === "string" ? rawValue.trim() : "";
});

const canSearchByCode = computed(() => manualCode.value.trim().length >= 6);
const canClaim = computed(() => Boolean(ticket.value?.claimable) && !claiming.value);

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-TW", {
    style: "currency",
    currency: "TWD",
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "尚未兌換";
  }

  return new Intl.DateTimeFormat("zh-TW", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

async function loadTicketByToken(token: string) {
  if (!token) {
    ticket.value = null;
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  statusMessage.value = "";

  try {
    const nextTicket = await fetchRedeemTicketByToken(token);
    ticket.value = nextTicket;
    manualCode.value = nextTicket.claimCode;
    statusMessage.value = nextTicket.message;
  } catch (error) {
    ticket.value = null;
    errorMessage.value = error instanceof ApiError ? error.message : "目前無法讀取這張收據的兌獎資訊。";
  } finally {
    loading.value = false;
  }
}

async function searchByCode() {
  if (!canSearchByCode.value) {
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  statusMessage.value = "";

  try {
    const nextTicket = await fetchRedeemTicketByCode(manualCode.value.trim().toUpperCase());
    ticket.value = nextTicket;
    statusMessage.value = nextTicket.message;
  } catch (error) {
    ticket.value = null;
    errorMessage.value = error instanceof ApiError ? error.message : "目前無法用這組兌獎碼查詢收據。";
  } finally {
    loading.value = false;
  }
}

async function claimTicket() {
  if (!ticket.value?.token || !canClaim.value) {
    return;
  }

  claiming.value = true;
  errorMessage.value = "";
  statusMessage.value = "";

  try {
    const nextTicket = await claimRedeemTicket(ticket.value.token);
    ticket.value = nextTicket;
    statusMessage.value = nextTicket.claimed
      ? "這張收據已完成兌換，可作為後續會員或活動流程的基礎。"
      : nextTicket.message;
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "兌換失敗，請稍後再試。";
  } finally {
    claiming.value = false;
  }
}

watch(routeToken, (token) => {
  void loadTicketByToken(token);
}, { immediate: true });

onMounted(() => {
  if (!routeToken.value) {
    statusMessage.value = "請掃描收據上的兌獎 QR Code，或輸入兌獎碼查詢。";
  }
});
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,_rgba(45,212,191,0.14),_transparent_35%),linear-gradient(180deg,_#07111b,_#020617)] px-5 py-10">
    <div class="w-full max-w-4xl rounded-[2.5rem] border border-white/10 bg-slate-950/75 p-8 shadow-soft shadow-black/40">
      <div class="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
        <section class="rounded-[2rem] border border-white/8 bg-white/4 p-6">
          <p class="text-xs uppercase tracking-[0.32em] text-brand-aqua/70">Receipt Redeem</p>
          <h1 class="mt-3 text-3xl font-semibold text-white">收據兌獎 / 會員綁定入口</h1>
          <p class="mt-3 max-w-xl text-sm leading-7 text-slate-300">
            掃描收據上的 QR Code，或輸入收據上的兌獎碼，即可確認這筆消費是否可兌換、是否已使用，以及後續串接會員與活動。
          </p>

          <div class="mt-8 rounded-[1.75rem] border border-white/8 bg-slate-950/55 p-5">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">兌獎碼</span>
              <div class="flex flex-col gap-3 sm:flex-row">
                <input
                  v-model="manualCode"
                  type="text"
                  placeholder="例如 ABCD2345"
                  class="min-w-0 flex-1 rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
                >
                <button
                  class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="loading || !canSearchByCode"
                  @click="searchByCode"
                >
                  {{ loading ? "查詢中..." : "查詢收據" }}
                </button>
              </div>
            </label>
            <p class="mt-3 text-xs leading-6 text-slate-500">
              第一版會先把收據變成可安全驗證的兌獎入口。之後可在此基礎上追加會員登入、集點、折價券或抽獎活動。
            </p>
          </div>

          <div
            v-if="statusMessage"
            class="mt-6 rounded-2xl border border-brand-aqua/20 bg-brand-aqua/10 px-4 py-3 text-sm text-brand-aqua"
          >
            {{ statusMessage }}
          </div>

          <div
            v-if="errorMessage"
            class="mt-4 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral"
          >
            {{ errorMessage }}
          </div>
        </section>

        <section class="rounded-[2rem] border border-white/8 bg-white/4 p-6">
          <div v-if="ticket" class="space-y-5">
            <div>
              <p class="text-xs uppercase tracking-[0.28em] text-slate-500">Ticket</p>
              <h2 class="mt-2 text-2xl font-semibold text-white">{{ ticket.orderNumber }}</h2>
              <p class="mt-2 text-sm text-slate-400">
                {{ ticket.storeName }} ({{ ticket.storeCode }}) ・ 兌獎碼 {{ ticket.claimCode }}
              </p>
            </div>

            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-500">消費金額</p>
                <p class="mt-2 text-xl font-semibold text-white">{{ formatCurrency(ticket.totalAmount) }}</p>
              </div>
              <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-500">品項數量</p>
                <p class="mt-2 text-xl font-semibold text-white">{{ ticket.itemCount }}</p>
              </div>
              <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-500">付款狀態</p>
                <p class="mt-2 text-xl font-semibold text-white">{{ ticket.paymentStatus }}</p>
              </div>
              <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-500">兌換狀態</p>
                <p class="mt-2 text-xl font-semibold text-white">{{ ticket.claimed ? "已兌換" : "未兌換" }}</p>
              </div>
            </div>

            <div class="rounded-[1.5rem] border border-white/8 bg-slate-950/50 p-5 text-sm text-slate-300">
              <p>消費時間：{{ formatDateTime(ticket.orderedAt) }}</p>
              <p class="mt-2">最近兌換：{{ formatDateTime(ticket.claimedAt) }}</p>
              <p class="mt-2 break-all text-slate-400">收據連結：{{ ticket.redeemUrl }}</p>
            </div>

            <button
              class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="!canClaim"
              @click="claimTicket"
            >
              {{ claiming ? "兌換中..." : ticket.claimed ? "已兌換完成" : "確認兌換" }}
            </button>
          </div>

          <div
            v-else
            class="flex min-h-[360px] items-center justify-center rounded-[1.75rem] border border-dashed border-white/10 px-6 text-center text-sm leading-7 text-slate-400"
          >
            掃描收據 QR Code 或輸入兌獎碼後，這裡會顯示這筆消費的兌獎資訊。
          </div>
        </section>
      </div>
    </div>
  </div>
</template>
