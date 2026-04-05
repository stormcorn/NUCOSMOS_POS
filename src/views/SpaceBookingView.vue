<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import {
  approveAdminBooking,
  cancelAdminBooking,
  createAdminBlockout,
  createAdminBooking,
  deleteAdminBlockout,
  fetchAdminBlockouts,
  fetchAdminBooking,
  fetchAdminBookings,
  fetchAdminSpaces,
  fetchPublicAvailability,
  updateAdminBooking,
} from "@/api/space-booking";
import { ApiError } from "@/api/http";
import type {
  AdminSpaceBlockout,
  AdminSpaceBooking,
  AdminSpaceBookingSummary,
  AdminSpaceResource,
  PublicSpaceAvailability,
} from "@/types/space-booking";
import { formatCurrency, formatDateTime } from "@/utils/format";

const today = new Date();
const startDate = today.toISOString().slice(0, 10);
const endDate = new Date(today.getTime() + 6 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const spaces = ref<AdminSpaceResource[]>([]);
const bookings = ref<AdminSpaceBookingSummary[]>([]);
const blockouts = ref<AdminSpaceBlockout[]>([]);
const availability = ref<PublicSpaceAvailability | null>(null);
const selectedSpaceId = ref("");
const selectedBooking = ref<AdminSpaceBooking | null>(null);

const filters = reactive({
  from: startDate,
  to: endDate,
  status: "",
});

const manualBookingForm = reactive({
  customerName: "",
  customerPhone: "",
  customerEmail: "",
  purpose: "",
  eventLink: "",
  attendeeCount: "10",
  note: "",
  internalNote: "",
  startAt: `${startDate}T10:00`,
  endAt: `${startDate}T12:00`,
});

const selectedBookingForm = reactive({
  customerName: "",
  customerPhone: "",
  customerEmail: "",
  purpose: "",
  eventLink: "",
  attendeeCount: "10",
  note: "",
  internalNote: "",
  startAt: `${startDate}T10:00`,
  endAt: `${startDate}T12:00`,
});

const blockoutForm = reactive({
  title: "",
  reason: "",
  startAt: `${startDate}T10:00`,
  endAt: `${startDate}T12:00`,
});

const selectedSpace = computed(
  () => spaces.value.find((space) => space.id === selectedSpaceId.value) ?? null,
);

function asOffsetDateTime(value: string) {
  return `${value}:00+08:00`;
}

function setError(error: unknown, fallback: string) {
  errorMessage.value = error instanceof ApiError ? error.message : fallback;
}

function clearMessages() {
  errorMessage.value = "";
  successMessage.value = "";
}

function statusTone(status: string) {
  switch (status) {
    case "CONFIRMED":
      return "border-emerald-400/20 bg-emerald-400/10 text-emerald-200";
    case "PENDING":
      return "border-amber-300/20 bg-amber-300/10 text-amber-100";
    case "REJECTED":
    case "CANCELLED":
      return "border-rose-400/20 bg-rose-400/10 text-rose-200";
    default:
      return "border-white/10 bg-white/5 text-slate-300";
  }
}

function statusLabel(status: string) {
  switch (status) {
    case "PENDING":
      return "待審核";
    case "CONFIRMED":
      return "已確認";
    case "CANCELLED":
      return "已取消";
    case "REJECTED":
      return "已拒絕";
    default:
      return status;
  }
}

function syncSelectedBookingForm() {
  if (!selectedBooking.value) {
    return;
  }
  selectedBookingForm.customerName = selectedBooking.value.customerName ?? "";
  selectedBookingForm.customerPhone = selectedBooking.value.customerPhone ?? "";
  selectedBookingForm.customerEmail = selectedBooking.value.customerEmail ?? "";
  selectedBookingForm.purpose = selectedBooking.value.purpose ?? "";
  selectedBookingForm.eventLink = selectedBooking.value.eventLink ?? "";
  selectedBookingForm.attendeeCount = String(selectedBooking.value.attendeeCount ?? 10);
  selectedBookingForm.note = selectedBooking.value.note ?? "";
  selectedBookingForm.internalNote = selectedBooking.value.internalNote ?? "";
  selectedBookingForm.startAt = selectedBooking.value.startAt.slice(0, 16);
  selectedBookingForm.endAt = selectedBooking.value.endAt.slice(0, 16);
}

async function loadSelectedBooking(bookingId: string) {
  try {
    selectedBooking.value = await fetchAdminBooking(bookingId);
    syncSelectedBookingForm();
  } catch (error) {
    setError(error, "載入預約詳情失敗。");
  }
}

async function loadWorkspace() {
  if (!selectedSpaceId.value) {
    return;
  }

  loading.value = true;
  clearMessages();
  try {
    const currentSpace = selectedSpace.value;
    const [nextBookings, nextBlockouts, nextAvailability] = await Promise.all([
      fetchAdminBookings(filters),
      fetchAdminBlockouts(filters),
      currentSpace
        ? fetchPublicAvailability(currentSpace.slug, { from: filters.from, to: filters.to })
        : Promise.resolve(null),
    ]);

    bookings.value = nextBookings;
    blockouts.value = nextBlockouts;
    availability.value = nextAvailability;

    if (selectedBooking.value) {
      const stillExists = nextBookings.some((booking) => booking.id === selectedBooking.value?.id);
      if (stillExists && selectedBooking.value.id) {
        await loadSelectedBooking(selectedBooking.value.id);
      } else {
        selectedBooking.value = null;
      }
    }
  } catch (error) {
    setError(error, "載入空間租借資料失敗。");
  } finally {
    loading.value = false;
  }
}

async function bootstrap() {
  loading.value = true;
  clearMessages();
  try {
    const nextSpaces = await fetchAdminSpaces();
    spaces.value = nextSpaces;
    selectedSpaceId.value = nextSpaces[0]?.id ?? "";
    await loadWorkspace();
  } catch (error) {
    setError(error, "載入空間設定失敗。");
  } finally {
    loading.value = false;
  }
}

async function submitManualBooking() {
  if (!selectedSpaceId.value) {
    return;
  }
  saving.value = true;
  clearMessages();
  try {
    const booking = await createAdminBooking({
      spaceResourceId: selectedSpaceId.value,
      customerName: manualBookingForm.customerName,
      customerPhone: manualBookingForm.customerPhone,
      customerEmail: manualBookingForm.customerEmail || null,
      purpose: manualBookingForm.purpose || null,
      eventLink: manualBookingForm.eventLink || null,
      attendeeCount: Number(manualBookingForm.attendeeCount),
      note: manualBookingForm.note || null,
      internalNote: manualBookingForm.internalNote || null,
      startAt: asOffsetDateTime(manualBookingForm.startAt),
      endAt: asOffsetDateTime(manualBookingForm.endAt),
      source: "MANUAL",
      status: "CONFIRMED",
    });
    successMessage.value = `已建立人工預約 ${booking.bookingNumber}。`;
    selectedBooking.value = booking;
    syncSelectedBookingForm();
    await loadWorkspace();
  } catch (error) {
    setError(error, "建立預約失敗。");
  } finally {
    saving.value = false;
  }
}

async function saveSelectedBooking() {
  if (!selectedBooking.value) {
    return;
  }
  saving.value = true;
  clearMessages();
  try {
    selectedBooking.value = await updateAdminBooking(selectedBooking.value.id, {
      customerName: selectedBookingForm.customerName,
      customerPhone: selectedBookingForm.customerPhone,
      customerEmail: selectedBookingForm.customerEmail || null,
      purpose: selectedBookingForm.purpose || null,
      eventLink: selectedBookingForm.eventLink || null,
      attendeeCount: Number(selectedBookingForm.attendeeCount),
      note: selectedBookingForm.note || null,
      internalNote: selectedBookingForm.internalNote || null,
      startAt: asOffsetDateTime(selectedBookingForm.startAt),
      endAt: asOffsetDateTime(selectedBookingForm.endAt),
    });
    syncSelectedBookingForm();
    successMessage.value = "預約資料已更新。";
    await loadWorkspace();
  } catch (error) {
    setError(error, "更新預約失敗。");
  } finally {
    saving.value = false;
  }
}

async function submitBlockout() {
  if (!selectedSpaceId.value) {
    return;
  }
  saving.value = true;
  clearMessages();
  try {
    await createAdminBlockout({
      spaceResourceId: selectedSpaceId.value,
      title: blockoutForm.title,
      reason: blockoutForm.reason || null,
      startAt: asOffsetDateTime(blockoutForm.startAt),
      endAt: asOffsetDateTime(blockoutForm.endAt),
    });
    successMessage.value = "已新增封鎖時段。";
    await loadWorkspace();
  } catch (error) {
    setError(error, "新增封鎖時段失敗。");
  } finally {
    saving.value = false;
  }
}

async function runDecision(action: "approve" | "cancel") {
  if (!selectedBooking.value) {
    return;
  }
  const note = window.prompt("請輸入內部備註", selectedBookingForm.internalNote ?? "") ?? "";
  saving.value = true;
  clearMessages();
  try {
    const bookingId = selectedBooking.value.id;
    selectedBooking.value = action === "approve"
      ? await approveAdminBooking(bookingId, note)
      : await cancelAdminBooking(bookingId, note);
    syncSelectedBookingForm();
    successMessage.value = action === "approve" ? "預約已審核通過。" : "預約已取消。";
    await loadWorkspace();
  } catch (error) {
    setError(error, action === "approve" ? "審核通過失敗。" : "取消預約失敗。");
  } finally {
    saving.value = false;
  }
}

async function removeBlockout(blockoutId: string) {
  if (!window.confirm("確定要刪除這筆封鎖時段嗎？")) {
    return;
  }
  saving.value = true;
  clearMessages();
  try {
    await deleteAdminBlockout(blockoutId);
    successMessage.value = "封鎖時段已刪除。";
    await loadWorkspace();
  } catch (error) {
    setError(error, "刪除封鎖時段失敗。");
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  void bootstrap();
});
</script>

<template>
  <section class="space-y-6">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.32em] text-brand-aqua/70">Space Booking</p>
          <h2 class="mt-2 text-3xl font-semibold text-white">2F 活動空間租借</h2>
          <p class="mt-2 text-sm text-slate-400">管理預約申請、編輯預約內容、建立人工預約與封鎖時段。</p>
        </div>
        <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <label class="space-y-2">
            <span class="text-xs uppercase tracking-[0.18em] text-slate-500">空間</span>
            <select v-model="selectedSpaceId" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none" @change="loadWorkspace">
              <option v-for="space in spaces" :key="space.id" :value="space.id">{{ space.name }}</option>
            </select>
          </label>
          <label class="space-y-2">
            <span class="text-xs uppercase tracking-[0.18em] text-slate-500">開始日期</span>
            <input v-model="filters.from" type="date" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none" />
          </label>
          <label class="space-y-2">
            <span class="text-xs uppercase tracking-[0.18em] text-slate-500">結束日期</span>
            <input v-model="filters.to" type="date" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none" />
          </label>
          <label class="space-y-2">
            <span class="text-xs uppercase tracking-[0.18em] text-slate-500">狀態</span>
            <select v-model="filters.status" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none">
              <option value="">全部狀態</option>
              <option value="PENDING">待審核</option>
              <option value="CONFIRMED">已確認</option>
              <option value="REJECTED">已拒絕</option>
              <option value="CANCELLED">已取消</option>
            </select>
          </label>
          <button class="rounded-2xl border border-brand-aqua/30 bg-brand-aqua/10 px-5 py-3 text-sm font-semibold text-brand-aqua transition hover:border-brand-aqua/50" @click="loadWorkspace">重新整理</button>
        </div>
      </div>

      <p v-if="errorMessage" class="mt-4 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">{{ errorMessage }}</p>
      <p v-if="successMessage" class="mt-4 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">{{ successMessage }}</p>
    </article>

    <div class="grid gap-6 xl:grid-cols-[1.25fr_1fr_1fr]">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h3 class="text-xl font-semibold text-white">可預約時段</h3>
            <p class="mt-1 text-sm text-slate-400">{{ selectedSpace?.name || "尚未選擇空間" }}</p>
          </div>
          <span v-if="selectedSpace" class="rounded-full border border-white/10 px-3 py-1 text-xs text-slate-400">
            {{ formatCurrency(Number(selectedSpace.hourlyRate), selectedSpace.currencyCode) }}/小時
          </span>
        </div>

        <div v-if="availability" class="mt-6 space-y-4">
          <div v-for="day in availability.days" :key="day.date" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
            <div class="flex items-center justify-between gap-3">
              <div class="text-sm font-semibold text-white">{{ day.date }}</div>
              <div class="text-xs text-slate-500">{{ day.slots.length }} 個時段</div>
            </div>
            <div class="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
              <div
                v-for="slot in day.slots"
                :key="slot.startAt"
                class="rounded-2xl border px-3 py-3 text-xs"
                :class="slot.status === 'AVAILABLE' ? 'border-emerald-400/20 bg-emerald-400/10 text-emerald-200' : 'border-rose-400/20 bg-rose-400/10 text-rose-200'"
              >
                <div class="font-semibold">{{ formatDateTime(slot.startAt).slice(11) }} - {{ formatDateTime(slot.endAt).slice(11) }}</div>
                <div class="mt-1 opacity-80">{{ slot.label }}</div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="mt-6 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-8 text-sm text-slate-400">
          {{ loading ? "載入時段中..." : "選擇空間後即可查看可預約時段。" }}
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <h3 class="text-xl font-semibold text-white">預約清單</h3>
        <div class="mt-6 space-y-3">
          <button
            v-for="booking in bookings"
            :key="booking.id"
            class="w-full rounded-[1.5rem] border px-4 py-4 text-left transition hover:border-white/20"
            :class="selectedBooking?.id === booking.id ? 'border-brand-aqua/30 bg-brand-aqua/10' : 'border-white/8 bg-white/4'"
            @click="loadSelectedBooking(booking.id)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="font-semibold text-white">{{ booking.customerName }}</div>
                <div class="mt-1 text-xs text-slate-400">{{ booking.bookingNumber }} ｜ {{ booking.spaceName }}</div>
              </div>
              <span class="rounded-full border px-2 py-1 text-[11px]" :class="statusTone(booking.status)">{{ statusLabel(booking.status) }}</span>
            </div>
            <div class="mt-3 text-sm text-slate-300">{{ formatDateTime(booking.startAt) }} ～ {{ formatDateTime(booking.endAt) }}</div>
            <div class="mt-2 text-xs text-slate-500">{{ booking.customerPhone }} ｜ {{ formatCurrency(booking.subtotalAmount) }}</div>
          </button>
        </div>

        <div class="mt-8">
          <h4 class="text-sm font-semibold uppercase tracking-[0.2em] text-slate-500">封鎖時段</h4>
          <div class="mt-4 space-y-3">
            <div v-for="blockout in blockouts" :key="blockout.id" class="rounded-[1.5rem] border border-white/8 bg-white/4 px-4 py-4">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <div class="font-semibold text-white">{{ blockout.title }}</div>
                  <div class="mt-1 text-xs text-slate-400">{{ formatDateTime(blockout.startAt) }} ～ {{ formatDateTime(blockout.endAt) }}</div>
                </div>
                <button class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" @click="removeBlockout(blockout.id)">刪除</button>
              </div>
              <p v-if="blockout.reason" class="mt-2 text-sm text-slate-300">{{ blockout.reason }}</p>
            </div>
          </div>
        </div>
      </article>

      <aside class="space-y-6">
        <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
          <h3 class="text-xl font-semibold text-white">預約詳情</h3>
          <div v-if="selectedBooking" class="mt-6 space-y-4">
            <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
              <div class="font-semibold text-white">{{ selectedBooking.bookingNumber }}</div>
              <div class="mt-2">狀態：{{ statusLabel(selectedBooking.status) }}</div>
              <div>空間：{{ selectedBooking.spaceName }}</div>
              <div>金額：{{ formatCurrency(selectedBooking.subtotalAmount) }}</div>
              <div>已付款：{{ formatCurrency(selectedBooking.paidAmount) }}</div>
              <div>待付款：{{ formatCurrency(selectedBooking.balanceAmount) }}</div>
              <div v-if="selectedBooking.approvedBy">審核者：{{ selectedBooking.approvedBy }}</div>
            </div>

            <div class="grid gap-4">
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">姓名</span>
                <input v-model="selectedBookingForm.customerName" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入姓名" />
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">手機號碼</span>
                <input v-model="selectedBookingForm.customerPhone" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入手機號碼" />
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">Email</span>
                <input v-model="selectedBookingForm.customerEmail" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="name@example.com" />
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">活動名稱</span>
                <input v-model="selectedBookingForm.purpose" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入活動名稱" />
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">活動連結</span>
                <input v-model="selectedBookingForm.eventLink" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入活動連結" />
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">預計人數</span>
                <input v-model="selectedBookingForm.attendeeCount" type="number" min="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
              </label>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="block space-y-2">
                  <span class="text-xs uppercase tracking-[0.18em] text-slate-500">開始時間</span>
                  <input v-model="selectedBookingForm.startAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
                </label>
                <label class="block space-y-2">
                  <span class="text-xs uppercase tracking-[0.18em] text-slate-500">結束時間</span>
                  <input v-model="selectedBookingForm.endAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
                </label>
              </div>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">客戶備註</span>
                <textarea v-model="selectedBookingForm.note" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入客戶備註"></textarea>
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">內部備註</span>
                <textarea v-model="selectedBookingForm.internalNote" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入內部備註"></textarea>
              </label>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <button class="rounded-2xl border border-brand-aqua/30 bg-brand-aqua/10 px-4 py-3 text-sm font-semibold text-brand-aqua" :disabled="saving" @click="saveSelectedBooking">儲存預約</button>
              <button class="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm font-semibold text-emerald-200" :disabled="saving" @click="runDecision('approve')">審核通過</button>
              <button class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm font-semibold text-brand-coral" :disabled="saving" @click="runDecision('cancel')">取消預約</button>
            </div>
          </div>
          <div v-else class="mt-6 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-8 text-sm text-slate-400">
            請先從左側選擇一筆預約，右側才會顯示可編輯的預約欄位。
          </div>
        </article>

        <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
          <h3 class="text-xl font-semibold text-white">新增人工預約</h3>
          <div class="mt-6 space-y-4">
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">姓名</span>
              <input v-model="manualBookingForm.customerName" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入客戶姓名" />
            </label>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">手機號碼</span>
              <input v-model="manualBookingForm.customerPhone" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入手機號碼" />
            </label>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">Email</span>
              <input v-model="manualBookingForm.customerEmail" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="name@example.com" />
            </label>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">活動名稱</span>
              <input v-model="manualBookingForm.purpose" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入活動名稱" />
            </label>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">活動連結</span>
              <input v-model="manualBookingForm.eventLink" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入活動連結" />
            </label>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">預計人數</span>
              <input v-model="manualBookingForm.attendeeCount" type="number" min="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
            </label>
            <div class="grid gap-3 sm:grid-cols-2">
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">開始時間</span>
                <input v-model="manualBookingForm.startAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">結束時間</span>
                <input v-model="manualBookingForm.endAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
              </label>
            </div>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">客戶備註</span>
              <textarea v-model="manualBookingForm.note" rows="2" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入客戶備註"></textarea>
            </label>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">內部備註</span>
              <textarea v-model="manualBookingForm.internalNote" rows="2" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入內部備註"></textarea>
            </label>
            <button class="w-full rounded-2xl border border-brand-aqua/30 bg-brand-aqua/10 px-4 py-3 text-sm font-semibold text-brand-aqua" :disabled="saving" @click="submitManualBooking">建立人工預約</button>
          </div>
        </article>

        <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
          <h3 class="text-xl font-semibold text-white">新增封鎖時段</h3>
          <div class="mt-6 space-y-4">
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">封鎖標題</span>
              <input v-model="blockoutForm.title" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入封鎖標題" />
            </label>
            <label class="block space-y-2">
              <span class="text-xs uppercase tracking-[0.18em] text-slate-500">封鎖原因</span>
              <textarea v-model="blockoutForm.reason" rows="2" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="請輸入封鎖原因"></textarea>
            </label>
            <div class="grid gap-3 sm:grid-cols-2">
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">開始時間</span>
                <input v-model="blockoutForm.startAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
              </label>
              <label class="block space-y-2">
                <span class="text-xs uppercase tracking-[0.18em] text-slate-500">結束時間</span>
                <input v-model="blockoutForm.endAt" type="datetime-local" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
              </label>
            </div>
            <button class="w-full rounded-2xl border border-white/10 px-4 py-3 text-sm font-semibold text-slate-100" :disabled="saving" @click="submitBlockout">建立封鎖時段</button>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>
