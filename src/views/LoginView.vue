<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { useAuthStore } from "@/stores/auth";
import type { RoleCode } from "@/types/auth";

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
  storeCode: "TW001",
  roleCode: "MANAGER" as RoleCode,
  pin: "",
  deviceCode: "",
});

const roleOptions: { label: string; value: RoleCode; hint: string }[] = [
  { label: "Manager", value: "MANAGER", hint: "商品、報表與裝置管理" },
  { label: "Admin", value: "ADMIN", hint: "保留給系統管理角色" },
];

const validationMessage = ref("");

async function submit() {
  validationMessage.value = "";

  if (!form.storeCode || !form.pin) {
    validationMessage.value = "請填入門市代碼與 PIN";
    return;
  }

  const success = await authStore.signIn({
    storeCode: form.storeCode.trim(),
    roleCode: form.roleCode,
    pin: form.pin.trim(),
    deviceCode: form.deviceCode.trim() || undefined,
  });

  if (!success) {
    return;
  }

  const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/";
  await router.push(redirect);
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center px-5 py-10">
    <div class="grid w-full max-w-5xl overflow-hidden rounded-[2.25rem] border border-white/10 bg-slate-950/70 shadow-soft shadow-black/30 lg:grid-cols-[1.05fr_0.95fr]">
      <section class="border-b border-white/8 bg-[radial-gradient(circle_at_top_left,_rgba(81,245,214,0.18),_transparent_42%),linear-gradient(180deg,rgba(255,255,255,0.03),transparent)] px-6 py-8 lg:border-b-0 lg:border-r lg:px-8 lg:py-10">
        <p class="text-xs uppercase tracking-[0.4em] text-brand-aqua/70">NUCOSMOS POS</p>
        <h1 class="mt-4 max-w-md text-4xl font-semibold leading-tight text-white">管理後台登入已接上 Spring Boot 驗證流程。</h1>
        <p class="mt-4 max-w-xl text-sm leading-7 text-slate-300">
          這一版使用 PIN + JWT 工作階段。登入成功後，前端會自動保存 access token，並以 `/api/v1/auth/me`
          還原當前工作階段。
        </p>

        <div class="mt-10 grid gap-4 md:grid-cols-3">
          <div class="rounded-[1.5rem] border border-white/8 bg-white/5 p-4">
            <p class="text-xs uppercase tracking-[0.24em] text-slate-500">測試門市</p>
            <p class="mt-3 text-lg font-semibold text-white">TW001</p>
          </div>
          <div class="rounded-[1.5rem] border border-white/8 bg-white/5 p-4">
            <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Manager PIN</p>
            <p class="mt-3 text-lg font-semibold text-white">9999</p>
          </div>
          <div class="rounded-[1.5rem] border border-white/8 bg-white/5 p-4">
            <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Supervisor PIN</p>
            <p class="mt-3 text-lg font-semibold text-white">5678</p>
          </div>
        </div>
      </section>

      <section class="px-6 py-8 lg:px-8 lg:py-10">
        <div class="mx-auto max-w-md">
          <p class="text-xs uppercase tracking-[0.36em] text-slate-500">Admin Sign In</p>
          <h2 class="mt-3 text-3xl font-semibold text-white">門市管理登入</h2>

          <form class="mt-8 space-y-5" @submit.prevent="submit">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">Store Code</span>
              <input
                v-model="form.storeCode"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none transition focus:border-brand-aqua/40 focus:ring-2 focus:ring-brand-aqua/15"
                placeholder="TW001"
              />
            </label>

            <div>
              <span class="mb-2 block text-sm text-slate-300">登入角色</span>
              <div class="grid gap-3 sm:grid-cols-2">
                <button
                  v-for="role in roleOptions"
                  :key="role.value"
                  type="button"
                  class="rounded-2xl border px-4 py-4 text-left transition"
                  :class="form.roleCode === role.value
                    ? 'border-brand-aqua/30 bg-brand-aqua/10 text-white shadow-glow'
                    : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'"
                  @click="form.roleCode = role.value"
                >
                  <p class="text-base font-semibold">{{ role.label }}</p>
                  <p class="mt-2 text-sm text-slate-400">{{ role.hint }}</p>
                </button>
              </div>
            </div>

            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">PIN</span>
              <input
                v-model="form.pin"
                type="password"
                inputmode="numeric"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none transition focus:border-brand-aqua/40 focus:ring-2 focus:ring-brand-aqua/15"
                placeholder="輸入 PIN"
              />
            </label>

            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">Device Code</span>
              <input
                v-model="form.deviceCode"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none transition focus:border-brand-aqua/40 focus:ring-2 focus:ring-brand-aqua/15"
                placeholder="POS-TABLET-001"
              />
            </label>

            <div v-if="validationMessage || authStore.errorMessage" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
              {{ validationMessage || authStore.errorMessage }}
            </div>

            <button
              type="submit"
              class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="authStore.loading"
            >
              {{ authStore.loading ? "登入中..." : "SIGN IN" }}
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>
