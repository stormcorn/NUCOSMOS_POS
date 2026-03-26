<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { FirebaseError } from "firebase/app";
import { type ConfirmationResult, RecaptchaVerifier, signInWithPhoneNumber, signOut } from "firebase/auth";

import { completeRegistration, fetchAvailableStores, startRegistration } from "@/api/auth";
import { ApiError } from "@/api/http";
import { getFirebaseAuthClient, isFirebaseWebAuthConfigured } from "@/services/firebase";
import type { AuthStore } from "@/types/auth";

const router = useRouter();

const stores = ref<AuthStore[]>([]);
const loadingStores = ref(false);
const sendingCode = ref(false);
const verifying = ref(false);
const errorMessage = ref("");
const infoMessage = ref("");
const registrationId = ref("");
const recaptchaContainerId = "firebase-register-recaptcha";

const firebaseConfigured = isFirebaseWebAuthConfigured();
let recaptchaVerifier: RecaptchaVerifier | null = null;
let confirmationResult: ConfirmationResult | null = null;

const form = reactive({
  storeCode: "TW001",
  phoneNumber: "",
  pin: "",
  verificationCode: "",
});

const canRequestCode = computed(() =>
  firebaseConfigured &&
  form.storeCode.trim().length > 0 &&
  form.phoneNumber.trim().length > 0 &&
  /^\d{6}$/.test(form.pin.trim()),
);

const canVerifyCode = computed(() =>
  registrationId.value.length > 0 &&
  /^\d{6}$/.test(form.verificationCode.trim()),
);

function normalizePhoneNumber(rawValue: string) {
  const compact = rawValue.replace(/[\s\-()]/g, "").trim();

  if (/^09\d{8}$/.test(compact)) {
    return `+886${compact.slice(1)}`;
  }

  if (/^\+?\d{10,15}$/.test(compact)) {
    return compact.startsWith("+") ? compact : `+${compact}`;
  }

  return compact;
}

const normalizedPhoneNumber = computed(() => normalizePhoneNumber(form.phoneNumber));

async function ensureRecaptchaVerifier() {
  if (recaptchaVerifier) {
    return recaptchaVerifier;
  }

  await nextTick();
  const auth = getFirebaseAuthClient();
  recaptchaVerifier = new RecaptchaVerifier(auth, recaptchaContainerId, {
    size: "invisible",
  });
  await recaptchaVerifier.render();
  return recaptchaVerifier;
}

async function loadStores() {
  loadingStores.value = true;
  try {
    const nextStores = await fetchAvailableStores();
    stores.value = nextStores;
    if (!stores.value.some((store) => store.code === form.storeCode) && stores.value[0]) {
      form.storeCode = stores.value[0].code;
    }
  } catch {
    stores.value = [{ code: "TW001", name: "TW001" }];
  } finally {
    loadingStores.value = false;
  }
}

async function requestCode() {
  if (!canRequestCode.value) {
    return;
  }

  sendingCode.value = true;
  errorMessage.value = "";
  infoMessage.value = "";

  try {
    const auth = getFirebaseAuthClient();
    const response = await startRegistration({
      storeCode: form.storeCode.trim(),
      phoneNumber: normalizedPhoneNumber.value,
      pin: form.pin.trim(),
    });
    const verifier = await ensureRecaptchaVerifier();
    confirmationResult = await signInWithPhoneNumber(
      auth,
      normalizedPhoneNumber.value,
      verifier,
    );
    registrationId.value = response.registrationId;
    infoMessage.value =
      `驗證碼已發送至 ${response.phoneNumber}。` +
      "請輸入收到的簡訊驗證碼以啟用帳號。";
  } catch (error) {
    if (error instanceof ApiError) {
      errorMessage.value = error.message;
    } else if (error instanceof FirebaseError) {
      errorMessage.value = `Firebase 簡訊驗證錯誤：${error.code}`;
    } else {
      errorMessage.value = error instanceof Error ? error.message : "無法開始註冊流程。";
    }
    if (!(error instanceof ApiError)) {
      registrationId.value = "";
      confirmationResult = null;
      recaptchaVerifier?.clear();
      recaptchaVerifier = null;
    }
  } finally {
    sendingCode.value = false;
  }
}

async function verifyCode() {
  if (!canVerifyCode.value) {
    return;
  }

  verifying.value = true;
  errorMessage.value = "";
  infoMessage.value = "";

  try {
    if (!confirmationResult) {
      throw new Error("Firebase 驗證流程尚未開始。");
    }

    const credential = await confirmationResult.confirm(form.verificationCode.trim());
    const firebaseIdToken = await credential.user.getIdToken();
    const response = await completeRegistration({
      registrationId: registrationId.value,
      verificationCode: form.verificationCode.trim(),
      firebaseIdToken,
    });
    await signOut(getFirebaseAuthClient()).catch(() => null);
    registrationId.value = "";
    confirmationResult = null;
    infoMessage.value =
      `${response.phoneNumber} 已完成註冊，現在可以使用 6 位數 PIN 登入。`;
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "無法完成註冊。";
  } finally {
    verifying.value = false;
  }
}

onMounted(() => {
  void loadStores();
});

onBeforeUnmount(() => {
  recaptchaVerifier?.clear();
  recaptchaVerifier = null;
  confirmationResult = null;
});
</script>

<template>
  <div class="flex min-h-screen items-center justify-center px-5 py-10">
    <div class="w-full max-w-3xl rounded-[2.25rem] border border-white/10 bg-slate-950/70 p-8 shadow-soft shadow-black/30">
      <div class="flex items-start justify-between gap-6">
        <div>
          <p class="text-xs uppercase tracking-[0.36em] text-brand-aqua/70">自行註冊</p>
          <h1 class="mt-3 text-3xl font-semibold text-white">使用手機號碼與 6 位數 PIN 註冊</h1>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
            完成一次 Firebase 簡訊驗證後，你的手機號碼與 6 位數 PIN 就能用於後續登入。
          </p>
        </div>
        <button
          class="rounded-2xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:border-brand-aqua/30 hover:text-white"
          @click="router.push({ name: 'login' })"
        >
          返回登入
        </button>
      </div>

      <div class="mt-8 grid gap-6 lg:grid-cols-2">
        <section class="rounded-[1.75rem] border border-white/8 bg-white/5 p-5">
          <p class="text-xs uppercase tracking-[0.28em] text-slate-500">步驟 1</p>
          <h2 class="mt-2 text-xl font-semibold text-white">申請驗證碼</h2>

          <div class="mt-5 space-y-4">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">門市代碼</span>
              <select
                v-model="form.storeCode"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
                :disabled="loadingStores || sendingCode"
              >
                <option v-for="store in stores" :key="store.code" :value="store.code">
                  {{ store.code }} | {{ store.name }}
                </option>
              </select>
            </label>

            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">手機號碼</span>
              <input
                v-model="form.phoneNumber"
                type="tel"
                autocomplete="tel"
                placeholder="+886912345678 或 0912345678"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
              <span class="mt-2 block text-xs text-slate-500">
                台灣手機若輸入為 <code class="font-mono">09xxxxxxxx</code>，系統會自動轉換為
                <code class="font-mono">+8869xxxxxxxx</code>。
              </span>
            </label>

            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">6 位數 PIN</span>
              <input
                v-model="form.pin"
                type="password"
                inputmode="numeric"
                maxlength="6"
                placeholder="123456"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
            </label>

            <button
              class="w-full rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="sendingCode || !canRequestCode"
              @click="requestCode"
            >
              {{ sendingCode ? "發送中..." : "取得驗證碼" }}
            </button>

            <div :id="recaptchaContainerId" />
          </div>
        </section>

        <section class="rounded-[1.75rem] border border-white/8 bg-white/5 p-5">
          <p class="text-xs uppercase tracking-[0.28em] text-slate-500">步驟 2</p>
          <h2 class="mt-2 text-xl font-semibold text-white">驗證並啟用</h2>

          <div class="mt-5 space-y-4">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">簡訊驗證碼</span>
              <input
                v-model="form.verificationCode"
                type="text"
                inputmode="numeric"
                maxlength="6"
                placeholder="654321"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
            </label>

            <div class="rounded-2xl border border-white/8 bg-slate-900/70 px-4 py-4 text-sm text-slate-300">
              {{ firebaseConfigured
                ? "簡訊驗證成功後，後端會再驗證 Firebase ID Token，確認無誤後才會啟用帳號。"
                : "Firebase 網頁驗證環境變數尚未設定，請先補上 VITE_FIREBASE_* 後再使用簡訊註冊。" }}
            </div>

            <button
              class="w-full rounded-2xl border border-white/10 px-5 py-3 text-sm font-semibold text-white transition hover:border-brand-aqua/30 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="verifying || !canVerifyCode"
              @click="verifyCode"
            >
              {{ verifying ? "驗證中..." : "完成註冊" }}
            </button>
          </div>
        </section>
      </div>

      <div v-if="infoMessage" class="mt-6 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-300">
        {{ infoMessage }}
      </div>

      <div v-if="errorMessage" class="mt-4 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>
