<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAccessControlStore } from "@/stores/access";
import { useAuthStore } from "@/stores/auth";
import type { RoleAdminItem } from "@/types/access";

const authStore = useAuthStore();
const accessStore = useAccessControlStore();

const selectedRoleId = ref<string | null>(null);
const isCreating = ref(false);
const formError = ref("");

const form = reactive({
  code: "",
  name: "",
  description: "",
  active: true,
  permissionKeys: [] as string[],
});

const canEditRoles = computed(() => authStore.hasPermission(PERMISSIONS.ROLES_EDIT));
const titleText = computed(() => (isCreating.value ? "新增角色" : selectedRoleId.value ? "編輯角色權限" : "角色權限"));

function resetForm() {
  isCreating.value = true;
  selectedRoleId.value = null;
  formError.value = "";
  form.code = "";
  form.name = "";
  form.description = "";
  form.active = true;
  form.permissionKeys = [];
}

function selectRole(role: RoleAdminItem) {
  isCreating.value = false;
  selectedRoleId.value = role.id;
  formError.value = "";
  form.code = role.code;
  form.name = role.name;
  form.description = role.description ?? "";
  form.active = role.active;
  form.permissionKeys = [...role.permissionKeys];
}

function togglePermission(permissionKey: string) {
  if (!canEditRoles.value) {
    return;
  }

  form.permissionKeys = form.permissionKeys.includes(permissionKey)
    ? form.permissionKeys.filter((item) => item !== permissionKey)
    : [...form.permissionKeys, permissionKey];
}

async function saveRole() {
  if (!canEditRoles.value) {
    return;
  }

  formError.value = "";

  if (!form.code || !form.name) {
    formError.value = "請輸入角色代碼與角色名稱。";
    return;
  }

  const payload = {
    code: form.code.trim().toUpperCase(),
    name: form.name.trim(),
    description: form.description.trim(),
    active: form.active,
    permissionKeys: [...form.permissionKeys],
  };

  const success = isCreating.value || !selectedRoleId.value
    ? await accessStore.createRole(payload)
    : await accessStore.updateRole(selectedRoleId.value, payload);

  if (success) {
    await accessStore.loadRoles();
    const updated = accessStore.roles.find((role) => role.code === payload.code);
    if (updated) {
      selectRole(updated);
    }
  }
}

onMounted(() => {
  void Promise.all([accessStore.loadRoles(), accessStore.loadPermissions()]);
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Role Matrix</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">角色權限</h3>
          <p class="mt-2 text-sm text-slate-400">建立角色並維護可查看、可編輯與可操作的權限矩陣。</p>
        </div>
        <button
          v-if="canEditRoles"
          class="rounded-2xl bg-brand-aqua px-4 py-2 text-sm font-semibold text-slate-950 transition hover:brightness-110"
          @click="resetForm"
        >
          新增角色
        </button>
      </div>

      <p v-if="accessStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ accessStore.errorMessage }}</p>

      <div class="mt-6 space-y-3">
        <button
          v-for="role in accessStore.roles"
          :key="role.id"
          class="w-full rounded-[1.5rem] border px-4 py-4 text-left transition"
          :class="selectedRoleId === role.id
            ? 'border-brand-aqua/30 bg-brand-aqua/10'
            : 'border-white/8 bg-white/4 hover:border-white/14 hover:bg-white/6'"
          @click="selectRole(role)"
        >
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-base font-semibold text-white">{{ role.name }}</p>
              <p class="mt-1 text-xs uppercase tracking-[0.18em] text-slate-400">{{ role.code }}</p>
            </div>
            <span
              class="rounded-full px-2.5 py-1 text-xs font-semibold"
              :class="role.active ? 'bg-emerald-400/15 text-emerald-300' : 'bg-slate-400/15 text-slate-300'"
            >
              {{ role.active ? "啟用" : "停用" }}
            </span>
          </div>
          <p class="mt-3 text-sm text-slate-400">{{ role.description || "未填寫角色說明" }}</p>
          <p class="mt-3 text-xs text-slate-500">權限數量：{{ role.permissionKeys.length }}</p>
        </button>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div>
        <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Editor</p>
        <h3 class="mt-2 text-2xl font-semibold text-white">{{ titleText }}</h3>
      </div>

      <div v-if="!canEditRoles" class="mt-6 rounded-[1.5rem] border border-dashed border-white/10 px-4 py-10 text-center text-sm text-slate-400">
        你目前只有查看角色與權限矩陣的權限，不能建立或修改角色。
      </div>

      <div v-else class="mt-6 space-y-5">
        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">角色代碼</span>
          <input v-model="form.code" type="text" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">角色名稱</span>
          <input v-model="form.name" type="text" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">角色說明</span>
          <textarea v-model="form.description" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="flex items-center gap-3 rounded-2xl border border-white/8 bg-white/4 px-4 py-3 text-sm text-slate-200">
          <input v-model="form.active" type="checkbox" />
          <span>角色啟用</span>
        </label>

        <div>
          <p class="mb-3 text-sm text-slate-300">權限矩陣</p>
          <div class="space-y-4">
            <section
              v-for="group in accessStore.permissionsByGroup"
              :key="group.groupName"
              class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4"
            >
              <h4 class="text-sm font-semibold text-white">{{ group.groupName }}</h4>
              <div class="mt-3 grid gap-3 md:grid-cols-2">
                <label
                  v-for="permission in group.items"
                  :key="permission.key"
                  class="flex items-start gap-3 rounded-2xl border border-white/8 bg-slate-950/35 px-4 py-3 text-sm text-slate-200"
                >
                  <input
                    type="checkbox"
                    :checked="form.permissionKeys.includes(permission.key)"
                    class="mt-1"
                    @change="togglePermission(permission.key)"
                  />
                  <span>
                    <span class="block font-medium text-white">{{ permission.label }}</span>
                    <span class="mt-1 block text-xs text-slate-400">{{ permission.description }}</span>
                  </span>
                </label>
              </div>
            </section>
          </div>
        </div>

        <p v-if="formError" class="text-sm text-brand-coral">{{ formError }}</p>

        <button
          class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-70"
          :disabled="accessStore.saving"
          @click="saveRole"
        >
          {{ accessStore.saving ? "儲存中..." : isCreating || !selectedRoleId ? "建立角色" : "更新角色" }}
        </button>
      </div>
    </aside>
  </section>
</template>
