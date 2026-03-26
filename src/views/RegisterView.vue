<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
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
      phoneNumber: form.phoneNumber.trim(),
      pin: form.pin.trim(),
    });
    const verifier = await ensureRecaptchaVerifier();
    confirmationResult = await signInWithPhoneNumber(
      auth,
      form.phoneNumber.trim(),
      verifier,
    );
    registrationId.value = response.registrationId;
    infoMessage.value =
      `Verification code sent to ${response.phoneNumber}. ` +
      "Enter the SMS code to activate your account.";
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "Failed to start registration.";
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
      throw new Error("Firebase confirmation has not started yet.");
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
      `Registration completed for ${response.phoneNumber}. You can now sign in with your 6-digit PIN.`;
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "Failed to complete registration.";
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
          <p class="text-xs uppercase tracking-[0.36em] text-brand-aqua/70">Self Registration</p>
          <h1 class="mt-3 text-3xl font-semibold text-white">Register with phone number and 6-digit PIN</h1>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
            Complete Firebase SMS verification once, and your phone number plus 6-digit PIN will be activated for future sign-in.
          </p>
        </div>
        <button
          class="rounded-2xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:border-brand-aqua/30 hover:text-white"
          @click="router.push({ name: 'login' })"
        >
          Back to login
        </button>
      </div>

      <div class="mt-8 grid gap-6 lg:grid-cols-2">
        <section class="rounded-[1.75rem] border border-white/8 bg-white/5 p-5">
          <p class="text-xs uppercase tracking-[0.28em] text-slate-500">Step 1</p>
          <h2 class="mt-2 text-xl font-semibold text-white">Request verification code</h2>

          <div class="mt-5 space-y-4">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">Store Code</span>
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
              <span class="mb-2 block text-sm text-slate-300">Phone Number</span>
              <input
                v-model="form.phoneNumber"
                type="tel"
                autocomplete="tel"
                placeholder="+886912345678"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
              />
            </label>

            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">6-digit PIN</span>
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
              {{ sendingCode ? "Requesting..." : "Get verification code" }}
            </button>

            <div :id="recaptchaContainerId" />
          </div>
        </section>

        <section class="rounded-[1.75rem] border border-white/8 bg-white/5 p-5">
          <p class="text-xs uppercase tracking-[0.28em] text-slate-500">Step 2</p>
          <h2 class="mt-2 text-xl font-semibold text-white">Verify and activate</h2>

          <div class="mt-5 space-y-4">
            <label class="block">
              <span class="mb-2 block text-sm text-slate-300">SMS Verification Code</span>
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
                ? "After the SMS code is verified, the backend will validate the Firebase ID token before activating the account."
                : "Firebase web auth env vars are not configured yet. Add the VITE_FIREBASE_* values before using SMS registration." }}
            </div>

            <button
              class="w-full rounded-2xl border border-white/10 px-5 py-3 text-sm font-semibold text-white transition hover:border-brand-aqua/30 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="verifying || !canVerifyCode"
              @click="verifyCode"
            >
              {{ verifying ? "Verifying..." : "Complete registration" }}
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
