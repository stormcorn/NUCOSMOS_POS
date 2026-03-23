<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useProcurementStore } from "@/stores/procurement";
import type { SupplierItem } from "@/types/procurement";

const authStore = useAuthStore();
const procurementStore = useProcurementStore();

const isFormOpen = ref(false);
const editingId = ref<string | null>(null);
const formError = ref("");
const form = reactive({
  code: "",
  name: "",
  contactName: "",
  phone: "",
  email: "",
  note: "",
});
const canEditSuppliers = computed(() => authStore.hasPermission(PERMISSIONS.SUPPLIERS_EDIT));

function resetForm() {
  editingId.value = null;
  formError.value = "";
  form.code = "";
  form.name = "";
  form.contactName = "";
  form.phone = "";
  form.email = "";
  form.note = "";
}

function openCreateForm() {
  if (!canEditSuppliers.value) {
    return;
  }
  resetForm();
  isFormOpen.value = true;
}

function openEditForm(item: SupplierItem) {
  if (!canEditSuppliers.value) {
    return;
  }
  editingId.value = item.id;
  form.code = item.code;
  form.name = item.name;
  form.contactName = item.contactName ?? "";
  form.phone = item.phone ?? "";
  form.email = item.email ?? "";
  form.note = item.note ?? "";
  isFormOpen.value = true;
}

async function submitForm() {
  if (!canEditSuppliers.value) {
    return;
  }
  formError.value = "";
  if (!form.code.trim() || !form.name.trim()) {
    formError.value = "供應商代碼與名稱為必填欄位。";
    return;
  }

  const payload = {
    code: form.code.trim(),
    name: form.name.trim(),
    contactName: form.contactName.trim(),
    phone: form.phone.trim(),
    email: form.email.trim(),
    note: form.note.trim(),
  };

  const success = editingId.value
    ? await procurementStore.updateSupplierRecord(editingId.value, payload)
    : await procurementStore.createSupplierRecord(payload);

  if (success) {
    resetForm();
    isFormOpen.value = false;
  }
}

async function deactivateSupplier(item: SupplierItem) {
  if (!canEditSuppliers.value) {
    return;
  }
  if (!window.confirm(`確定要停用供應商「${item.name}」嗎？`)) {
    return;
  }
  await procurementStore.deactivateSupplierRecord(item.id);
}

onMounted(() => {
  void procurementStore.loadSuppliers();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between border-b border-white/8 pb-5">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Supplier Directory</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">供應商管理</h3>
          <p class="mt-2 text-sm text-slate-400">維護原料與包材供應商基本資料，作為採購與進貨流程的資料來源。</p>
        </div>
        <button v-if="canEditSuppliers" class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" @click="openCreateForm">
          新增供應商
        </button>
      </div>

      <p v-if="procurementStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ procurementStore.errorMessage }}</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">代碼</th>
              <th class="px-4 py-3 font-medium">供應商</th>
              <th class="px-4 py-3 font-medium">聯絡資訊</th>
              <th class="px-4 py-3 font-medium">狀態</th>
              <th class="px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="procurementStore.loading">
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">供應商資料載入中...</td>
            </tr>
            <tr v-for="supplier in procurementStore.suppliers" :key="supplier.id" class="bg-slate-950/25">
              <td class="px-4 py-4 text-slate-300">{{ supplier.code }}</td>
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ supplier.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ supplier.note || "無備註" }}</p>
              </td>
              <td class="px-4 py-4 text-slate-300">
                <p>{{ supplier.contactName || "未填寫" }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ supplier.phone || "-" }} / {{ supplier.email || "-" }}</p>
              </td>
              <td class="px-4 py-4">
                <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="supplier.active ? 'bg-brand-aqua/15 text-brand-aqua' : 'bg-white/10 text-slate-300'">
                  {{ supplier.active ? "啟用中" : "已停用" }}
                </span>
              </td>
              <td class="px-4 py-4">
                <div class="flex gap-2">
                  <button v-if="canEditSuppliers" class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="openEditForm(supplier)">編輯</button>
                  <button v-if="canEditSuppliers" class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" :disabled="!supplier.active" @click="deactivateSupplier(supplier)">停用</button>
                </div>
              </td>
            </tr>
            <tr v-if="!procurementStore.loading && procurementStore.suppliers.length === 0">
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">目前尚未建立供應商。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Supplier Editor</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">{{ editingId ? "編輯供應商" : "新增供應商" }}</h3>
        </div>
        <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="isFormOpen = !isFormOpen">
          {{ isFormOpen ? "收合" : "展開" }}
        </button>
      </div>

      <div v-if="!canEditSuppliers" class="mt-6 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-10 text-center text-sm text-slate-400">
        你目前只有查看供應商資料的權限，不能新增、編輯或停用供應商。
      </div>
      <div v-else-if="isFormOpen" class="mt-6 space-y-4">
        <div v-if="formError" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
          {{ formError }}
        </div>
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">供應商代碼</span>
          <input v-model="form.code" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">供應商名稱</span>
          <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">聯絡人</span>
          <input v-model="form.contactName" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">電話</span>
          <input v-model="form.phone" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">Email</span>
          <input v-model="form.email" type="email" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">備註</span>
          <textarea v-model="form.note" rows="4" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>
        <div class="flex gap-3">
          <button class="flex-1 rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="procurementStore.saving" @click="submitForm">
            {{ procurementStore.saving ? "儲存中..." : editingId ? "更新供應商" : "建立供應商" }}
          </button>
          <button class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200" @click="resetForm">清空</button>
        </div>
      </div>
    </aside>
  </section>
</template>
