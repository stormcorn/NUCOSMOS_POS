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
const phoneRegistrationPhoneNumber = ref("");
const phoneRegistrationFormError = ref("");

const form = reactive({
  employeeCode: "",
  displayName: "",
  pin: "",
  status: "ACTIVE",
  roleCodes: [] as string[],
  storeCodes: [] as string[],
});

const canEditUsers = computed(() => authStore.hasPermission(PERMISSIONS.USERS_EDIT));
const canAssignManagers = computed(() => canEditUsers.value && authStore.activeRole === "ADMIN");
const titleText = computed(() => (editingUserId.value ? "Edit user" : "Create user"));

function normalizePhoneNumber(rawValue: string) {
  const compact = rawValue.replace(/[\s\-()]/g, "").trim();

  if (/^09\d{8}$/.test(compact)) {
    return `+886${compact.slice(1)}`;
  }

  if (/^\d{10,15}$/.test(compact)) {
    return `+${compact}`;
  }

  return compact;
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "Never";
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

function hasRole(user: UserAdminItem, roleCode: string) {
  return user.roleCodes.includes(roleCode);
}

function canToggleManager(user: UserAdminItem) {
  return canAssignManagers.value && !hasRole(user, "ADMIN") && hasRole(user, "CASHIER");
}

async function toggleManagerAssignment(user: UserAdminItem) {
  if (!canToggleManager(user)) {
    return;
  }

  const roleCodes = hasRole(user, "MANAGER")
    ? user.roleCodes.filter((roleCode) => roleCode !== "MANAGER")
    : [...user.roleCodes, "MANAGER"];

  formError.value = "";
  await accessStore.updateUser(user.id, {
    employeeCode: user.employeeCode,
    displayName: user.displayName,
    pin: "",
    status: user.status,
    roleCodes,
    storeCodes: [...user.storeCodes],
  });
}

async function saveUser() {
  if (!canEditUsers.value) {
    return;
  }

  formError.value = "";

  if (!form.employeeCode || !form.displayName) {
    formError.value = "Employee code and display name are required.";
    return;
  }

  if (!editingUserId.value && !form.pin) {
    formError.value = "A new user must have a 6-digit PIN.";
    return;
  }

  if (form.pin && !/^\d{6}$/.test(form.pin.trim())) {
    formError.value = "PIN must contain exactly 6 digits.";
    return;
  }

  if (form.roleCodes.length === 0) {
    formError.value = "Select at least one role.";
    return;
  }

  if (form.storeCodes.length === 0) {
    formError.value = "Select at least one store.";
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

async function clearPendingRegistration() {
  if (!canEditUsers.value) {
    return;
  }

  phoneRegistrationFormError.value = "";
  accessStore.phoneRegistrationActionMessage = "";

  const normalizedPhoneNumber = normalizePhoneNumber(phoneRegistrationPhoneNumber.value);
  if (!/^\+\d{10,15}$/.test(normalizedPhoneNumber)) {
    phoneRegistrationFormError.value = "Enter a valid phone number, for example 0912345678 or +886912345678.";
    return;
  }

  const response = await accessStore.clearPendingRegistration(normalizedPhoneNumber);
  if (response) {
    phoneRegistrationPhoneNumber.value = normalizedPhoneNumber;
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
          <h3 class="mt-2 text-2xl font-semibold text-white">User directory</h3>
          <p class="mt-2 text-sm text-slate-400">
            Review staff access, assigned roles, store coverage, and the latest login activity.
          </p>
        </div>

        <div class="flex flex-wrap gap-3">
          <select
            v-model="accessStore.userStatusFilter"
            class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
            @change="accessStore.loadUsers(accessStore.userStatusFilter, accessStore.userStoreFilter)"
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>

          <select
            v-model="accessStore.userStoreFilter"
            class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
            @change="accessStore.loadUsers(accessStore.userStatusFilter, accessStore.userStoreFilter)"
          >
            <option value="">All stores</option>
            <option v-for="store in storeContextStore.stores" :key="store.code" :value="store.code">
              {{ store.name }}
            </option>
          </select>

          <button
            v-if="canEditUsers"
            class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110"
            @click="openCreateForm"
          >
            New user
          </button>
        </div>
      </div>

      <p v-if="accessStore.errorMessage" class="mt-4 text-sm text-brand-coral">
        {{ accessStore.errorMessage }}
      </p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-sm">
          <thead class="bg-white/4 text-left text-slate-400">
            <tr>
              <th class="px-4 py-3">Employee Code</th>
              <th class="px-4 py-3">Display Name</th>
              <th class="px-4 py-3">Roles</th>
              <th class="px-4 py-3">Stores</th>
              <th class="px-4 py-3">Status</th>
              <th class="px-4 py-3">Last Login</th>
              <th class="px-4 py-3 text-right">Actions</th>
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
                  {{ user.status === "ACTIVE" ? "Active" : "Inactive" }}
                </span>
              </td>
              <td class="px-4 py-3 text-slate-400">{{ formatDateTime(user.lastLoginAt) }}</td>
              <td class="px-4 py-3 text-right">
                <div class="flex justify-end gap-2">
                  <button
                    v-if="canToggleManager(user)"
                    class="rounded-xl border px-3 py-2 text-xs transition"
                    :class="hasRole(user, 'MANAGER')
                      ? 'border-brand-coral/20 text-brand-coral hover:border-brand-coral/40'
                      : 'border-brand-aqua/30 text-brand-aqua hover:border-brand-aqua/50'"
                    @click="toggleManagerAssignment(user)"
                  >
                    {{ hasRole(user, "MANAGER") ? "Remove manager" : "Assign manager" }}
                  </button>
                  <button
                    v-if="canEditUsers"
                    class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-brand-aqua/30 hover:text-white"
                    @click="openEditForm(user)"
                  >
                    Edit
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="accessStore.users.length === 0">
              <td colspan="7" class="px-4 py-10 text-center text-slate-400">
                {{ accessStore.loadingUsers ? "Loading users..." : "No users match the current filters." }}
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
        <button
          v-if="isFormOpen && canEditUsers"
          class="text-sm text-slate-400 transition hover:text-white"
          @click="isFormOpen = false"
        >
          Close
        </button>
      </div>

      <div
        v-if="!canEditUsers"
        class="mt-8 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-10 text-center text-sm text-slate-400"
      >
        You do not have permission to create or edit staff accounts.
      </div>

      <div
        v-else-if="!isFormOpen"
        class="mt-8 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-10 text-center text-sm text-slate-400"
      >
        Pick an existing user to edit, or create a new account for staff onboarding.
      </div>

      <div v-else class="mt-6 space-y-5">
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">Employee Code</span>
          <input
            v-model="form.employeeCode"
            type="text"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
          />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">Display Name</span>
          <input
            v-model="form.displayName"
            type="text"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
          />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">PIN</span>
          <input
            v-model="form.pin"
            type="password"
            inputmode="numeric"
            maxlength="6"
            placeholder="123456"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
          />
          <span class="mt-2 block text-xs text-slate-500">
            {{ editingUserId ? "Leave blank to keep the existing 6-digit PIN." : "Set a 6-digit PIN for the new user." }}
          </span>
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">Status</span>
          <select
            v-model="form.status"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
          >
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </label>

        <div>
          <p class="mb-2 text-sm text-slate-300">Roles</p>
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
                <span class="mt-1 block text-xs text-slate-400">
                  {{ role.code }} | {{ role.description || "No description" }}
                </span>
              </span>
            </label>
          </div>
        </div>

        <div>
          <p class="mb-2 text-sm text-slate-300">Stores</p>
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
          {{ accessStore.saving ? "Saving..." : editingUserId ? "Update user" : "Create user" }}
        </button>
      </div>

      <div v-if="canEditUsers" class="mt-8 rounded-[1.5rem] border border-white/8 bg-white/4 p-5">
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Registration Recovery</p>
        <h4 class="mt-2 text-lg font-semibold text-white">Clear pending phone registration</h4>
        <p class="mt-2 text-sm leading-6 text-slate-400">
          Use this when a staff member is stuck in SMS registration and needs the current pending request cleared before retrying.
        </p>

        <label class="mt-5 block">
          <span class="mb-2 block text-sm text-slate-300">Phone Number</span>
          <input
            v-model="phoneRegistrationPhoneNumber"
            type="tel"
            placeholder="0912345678 or +886912345678"
            class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
          />
          <span class="mt-2 block text-xs text-slate-500">
            Taiwan mobile numbers entered as <code class="font-mono">09xxxxxxxx</code> will be converted to
            <code class="font-mono">+8869xxxxxxxx</code> automatically.
          </span>
        </label>

        <p v-if="phoneRegistrationFormError" class="mt-3 text-sm text-brand-coral">{{ phoneRegistrationFormError }}</p>
        <p v-else-if="accessStore.phoneRegistrationActionMessage" class="mt-3 text-sm text-emerald-300">
          {{ accessStore.phoneRegistrationActionMessage }}
        </p>

        <button
          class="mt-4 w-full rounded-2xl border border-white/10 px-5 py-3 text-sm font-semibold text-white transition hover:border-brand-aqua/30 disabled:cursor-not-allowed disabled:opacity-70"
          :disabled="accessStore.saving"
          @click="clearPendingRegistration"
        >
          {{ accessStore.saving ? "Clearing..." : "Clear pending registration" }}
        </button>
      </div>
    </aside>
  </section>
</template>
