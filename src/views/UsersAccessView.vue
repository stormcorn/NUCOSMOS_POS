<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAccessControlStore } from "@/stores/access";
import { useAuthStore } from "@/stores/auth";
import { useStoreContextStore } from "@/stores/store-context";
import type { UserAdminItem } from "@/types/access";

const authStore = useAuthStore();
const accessStore = useAccessControlStore();
const storeContextStore = useStoreContextStore();

const isFormOpen = ref(false);
const editingUserId = ref<string | null>(null);
const formError = ref("");

const form = reactive({
  employeeCode: "",
  displayName: "",
  pin: "",
  status: "ACTIVE",
  roleCodes: [] as string[],
  storeCodes: [] as string[],
});

const canEditUsers = computed(() => authStore.hasPermission(PERMISSIONS.USERS_EDIT));
const titleText = computed(() => (editingUserId.value ? "編輯使用者" : "新增使用者"));

function formatDateTime(value: string | null) {
  if (!value) {
    return "尚未登入";
  }

  return new Intl.DateTimeFormat("zh-TW", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function resetForm() {
  editingUserId.value = null;
  formError.value = "";
  form.employeeCode = "";
  form.displayName = "";
  form.pin = "";
  form.status = "ACTIVE";
  form.roleCodes = accessStore.roles.find((role) => role.code === "CASHIER") ? ["CASHIER"] : [];
  form.storeCodes = storeContextStore.stores[0] ? [storeContextStore.stores[0].code] : [];
}

function openCreateForm() {
  if (!canEditUsers.value) {
    return;
  }

  resetForm();
  isFormOpen.value = true;
}

function openEditForm(user: UserAdminItem) {
  if (!canEditUsers.value) {
    return;
  }

  editingUserId.value = user.id;
  formError.value = "";
  form.employeeCode = user.employeeCode;
  form.displayName = user.displayName;
  form.pin = "";
  form.status = user.status;
  form.roleCodes = [...user.roleCodes];
  form.storeCodes = [...user.storeCodes];
  isFormOpen.value = true;
}

function toggleSelection(list: string[], value: string) {
  return list.includes(value) ? list.filter((item) => item !== value) : [...list, value];
}

async function saveUser() {
  if (!canEditUsers.value) {
    return;
  }

  formError.value = "";

  if (!form.employeeCode || !form.displayName) {
    formError.value = "請輸入員工代碼與顯示名稱。";
    return;
  }

  if (!editingUserId.value && !form.pin) {
    formError.value = "新增使用者時必須設定 PIN。";
    return;
  }

  if (form.roleCodes.length === 0) {
    formError.value = "請至少選擇一個角色。";
    return;
  }

  if (form.storeCodes.length === 0) {
    formError.value = "請至少選擇一間門市。";
    return;
  }

  const payload = {
    employeeCode: form.employeeCode.trim(),
    displayName: form.displayName.trim(),
    pin: form.pin.trim(),
    status: form.status,
    roleCodes: [...form.roleCodes],
    storeCodes: [...form.storeCodes],
  };

  const success = editingUserId.value
    ? await accessStore.updateUser(editingUserId.value, payload)
    : await accessStore.createUser(payload);

  if (success) {
    isFormOpen.value = false;
    resetForm();
  }
}

async function refresh() {
  await Promise.all([
    storeContextStore.loadStores(),
    accessStore.loadRoles(),
  ]);

  if (accessStore.permissions.length === 0) {
    await accessStore.loadPermissions();
  }

  await accessStore.loadUsers(accessStore.userStatusFilter, accessStore.userStoreFilter);
}

onMounted(() => {
  void refresh();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">User Access</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">使用者管理</h3>
          <p class="mt-2 text-sm text-slate-400">管理後台使用者、角色指派與可登入門市。</p>
        </div>

        <div class="flex flex-wrap gap-3">
          <select
            v-model="accessStore.userStatusFilter"
            class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
            @change="accessStore.loadUsers(accessStore.userStatusFilter, accessStore.userStoreFilter)"
          >
            <option value="">全部狀態</option>
            <option value="ACTIVE">啟用</option>
            <option value="INACTIVE">停用</option>
          </select>

          <select
            v-model="accessStore.userStoreFilter"
            class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
            @change="accessStore.loadUsers(accessStore.userStatusFilter, accessStore.userStoreFilter)"
          >
            <option value="">全部門市</option>
            <option v-for="store in storeContextStore.stores" :key="store.code" :value="store.code">
              {{ store.name }}
            </option>
          </select>

          <button
            v-if="canEditUsers"
            class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110"
            @click="openCreateForm"
          >
            新增使用者
          </button>
        </div>
      </div>

      <p v-if="accessStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ accessStore.errorMessage }}</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-sm">
          <thead class="bg-white/4 text-left text-slate-400">
            <tr>
              <th class="px-4 py-3">員工代碼</th>
              <th class="px-4 py-3">名稱</th>
              <th class="px-4 py-3">角色</th>
              <th class="px-4 py-3">門市</th>
              <th class="px-4 py-3">狀態</th>
              <th class="px-4 py-3">最後登入</th>
              <th class="px-4 py-3 text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/6 bg-slate-950/35">
            <tr v-for="user in accessStore.users" :key="user.id" class="text-slate-200">
              <td class="px-4 py-3 font-medium">{{ user.employeeCode }}</td>
              <td class="px-4 py-3">{{ user.displayName }}</td>
              <td class="px-4 py-3">{{ user.roleCodes.join(", ") }}</td>
              <td class="px-4 py-3">{{ user.storeNames.join(", ") }}</td>
              <td class="px-4 py-3">
                <span
                  class="rounded-full px-2.5 py-1 text-xs font-semibold"
                  :class="user.status === 'ACTIVE' ? 'bg-emerald-400/15 text-emerald-300' : 'bg-slate-400/15 text-slate-300'"
                >
                  {{ user.status === "ACTIVE" ? "啟用" : "停用" }}
                </span>
              </td>
              <td class="px-4 py-3 text-slate-400">{{ formatDateTime(user.lastLoginAt) }}</td>
              <td class="px-4 py-3 text-right">
                <button
                  v-if="canEditUsers"
                  class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-brand-aqua/30 hover:text-white"
                  @click="openEditForm(user)"
                >
                  編輯
                </button>
              </td>
            </tr>
            <tr v-if="accessStore.users.length === 0">
              <td colspan="7" class="px-4 py-10 text-center text-slate-400">
                {{ accessStore.loadingUsers ? "載入使用者中..." : "目前沒有符合條件的使用者。" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Editor</p>
          <h3 class="mt-2 text-xl font-semibold text-white">{{ titleText }}</h3>
        </div>
        <button v-if="isFormOpen && canEditUsers" class="text-sm text-slate-400 transition hover:text-white" @click="isFormOpen = false">關閉</button>
      </div>

      <div v-if="!canEditUsers" class="mt-8 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-10 text-center text-sm text-slate-400">
        你目前只有查看使用者清單的權限，不能新增或編輯使用者。
      </div>

      <div v-else-if="!isFormOpen" class="mt-8 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-10 text-center text-sm text-slate-400">
        點選左側的「新增使用者」或選擇一筆使用者開始編輯。
      </div>

      <div v-else class="mt-6 space-y-5">
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">員工代碼</span>
          <input v-model="form.employeeCode" type="text" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">顯示名稱</span>
          <input v-model="form.displayName" type="text" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">PIN</span>
          <input v-model="form.pin" type="password" inputmode="numeric" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
          <span class="mt-2 block text-xs text-slate-500">{{ editingUserId ? "留空表示不更改 PIN" : "新增使用者時必填" }}</span>
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">狀態</span>
          <select v-model="form.status" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option value="ACTIVE">啟用</option>
            <option value="INACTIVE">停用</option>
          </select>
        </label>

        <div>
          <p class="mb-2 text-sm text-slate-300">角色指派</p>
          <div class="grid gap-2">
            <label
              v-for="role in accessStore.roles"
              :key="role.id"
              class="flex items-start gap-3 rounded-2xl border border-white/8 bg-white/4 px-4 py-3 text-sm text-slate-200"
            >
              <input
                type="checkbox"
                :checked="form.roleCodes.includes(role.code)"
                class="mt-1"
                @change="form.roleCodes = toggleSelection(form.roleCodes, role.code)"
              />
              <span>
                <span class="block font-medium text-white">{{ role.name }}</span>
                <span class="mt-1 block text-xs text-slate-400">{{ role.code }} · {{ role.description || "未填寫角色說明" }}</span>
              </span>
            </label>
          </div>
        </div>

        <div>
          <p class="mb-2 text-sm text-slate-300">可登入門市</p>
          <div class="grid gap-2">
            <label
              v-for="store in storeContextStore.stores"
              :key="store.code"
              class="flex items-center gap-3 rounded-2xl border border-white/8 bg-white/4 px-4 py-3 text-sm text-slate-200"
            >
              <input
                type="checkbox"
                :checked="form.storeCodes.includes(store.code)"
                @change="form.storeCodes = toggleSelection(form.storeCodes, store.code)"
              />
              <span>{{ store.name }} ({{ store.code }})</span>
            </label>
          </div>
        </div>

        <p v-if="formError" class="text-sm text-brand-coral">{{ formError }}</p>

        <button
          class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-70"
          :disabled="accessStore.saving"
          @click="saveUser"
        >
          {{ accessStore.saving ? "儲存中..." : editingUserId ? "更新使用者" : "建立使用者" }}
        </button>
      </div>
    </aside>
  </section>
</template>
