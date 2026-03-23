<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useProductCategoryStore } from "@/stores/product-categories";
import type { ProductCategory } from "@/types/product";

const authStore = useAuthStore();
const categoryStore = useProductCategoryStore();

const isFormOpen = ref(false);
const editingCategoryId = ref<string | null>(null);
const formError = ref("");
const form = reactive({
  code: "",
  name: "",
  displayOrder: "0",
});
const canEditCategories = computed(() => authStore.hasPermission(PERMISSIONS.PRODUCTS_EDIT));

const titleText = computed(() => (editingCategoryId.value ? "編輯分類" : "新增分類"));

function resetForm() {
  editingCategoryId.value = null;
  formError.value = "";
  form.code = "";
  form.name = "";
  form.displayOrder = "0";
}

function openCreateForm() {
  if (!canEditCategories.value) {
    return;
  }
  resetForm();
  isFormOpen.value = true;
}

function openEditForm(category: ProductCategory) {
  if (!canEditCategories.value) {
    return;
  }
  editingCategoryId.value = category.id;
  form.code = category.code;
  form.name = category.name;
  form.displayOrder = String(category.displayOrder);
  isFormOpen.value = true;
}

async function submitForm() {
  if (!canEditCategories.value) {
    return;
  }
  formError.value = "";

  if (!form.code.trim() || !form.name.trim()) {
    formError.value = "分類代碼與分類名稱為必填欄位。";
    return;
  }

  if (Number(form.displayOrder) < 0) {
    formError.value = "排序不可小於 0。";
    return;
  }

  const payload = {
    code: form.code.trim(),
    name: form.name.trim(),
    displayOrder: Number(form.displayOrder),
  };

  const success = editingCategoryId.value
    ? await categoryStore.updateCategory(editingCategoryId.value, payload)
    : await categoryStore.createCategory(payload);

  if (success) {
    resetForm();
    isFormOpen.value = false;
  }
}

async function deactivate(category: ProductCategory) {
  if (!canEditCategories.value) {
    return;
  }
  const confirmed = window.confirm(`確認要停用分類「${category.name}」嗎？`);
  if (!confirmed) {
    return;
  }

  await categoryStore.deactivateCategory(category.id);
}

onMounted(async () => {
  await categoryStore.loadCategories();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Category Matrix</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">商品分類管理</h3>
          <p class="mt-2 text-sm text-slate-400">集中維護商品分類代碼、名稱、排序與啟用狀態。</p>
        </div>
        <button v-if="canEditCategories" class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110" @click="openCreateForm">
          新增分類
        </button>
      </div>

      <p v-if="categoryStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ categoryStore.errorMessage }}</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">分類代碼</th>
              <th class="px-4 py-3 font-medium">分類名稱</th>
              <th class="px-4 py-3 font-medium">排序</th>
              <th class="px-4 py-3 font-medium">狀態</th>
              <th class="px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="categoryStore.loading">
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">商品分類載入中...</td>
            </tr>
            <tr v-for="category in categoryStore.categories" :key="category.id" class="bg-slate-950/25">
              <td class="px-4 py-4 font-mono text-slate-300">{{ category.code }}</td>
              <td class="px-4 py-4 font-medium text-white">{{ category.name }}</td>
              <td class="px-4 py-4 text-slate-300">{{ category.displayOrder }}</td>
              <td class="px-4 py-4">
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="category.active ? 'bg-brand-aqua/15 text-brand-aqua' : 'bg-amber-200/10 text-brand-amber'"
                >
                  {{ category.active ? "啟用中" : "已停用" }}
                </span>
              </td>
              <td class="px-4 py-4">
                <div class="flex gap-2">
                  <button v-if="canEditCategories" class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-brand-aqua/30 hover:text-white" @click="openEditForm(category)">
                    編輯
                  </button>
                  <button
                    v-if="canEditCategories"
                    class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral transition hover:bg-brand-coral/10 disabled:opacity-40"
                    :disabled="!category.active"
                    @click="deactivate(category)"
                  >
                    停用
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!categoryStore.loading && categoryStore.categories.length === 0">
              <td colspan="5" class="px-4 py-12 text-center text-slate-400">目前沒有商品分類。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Category Editor</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">{{ titleText }}</h3>
        </div>
        <button
          class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-white/20 hover:text-white"
          @click="isFormOpen = !isFormOpen"
        >
          {{ isFormOpen ? "收合" : "展開" }}
        </button>
      </div>

      <div v-if="!canEditCategories" class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
        你目前只有查看商品分類的權限，不能新增、編輯或停用分類。
      </div>

      <div v-else-if="isFormOpen" class="mt-6 space-y-4">
        <div v-if="formError" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
          {{ formError }}
        </div>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">分類代碼</span>
          <input v-model="form.code" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="tea-drinks" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">分類名稱</span>
          <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="Tea & Drinks" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">排序</span>
          <input v-model="form.displayOrder" type="number" min="0" step="1" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <div class="flex gap-3">
          <button
            class="flex-1 rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:opacity-60"
            :disabled="categoryStore.saving"
            @click="submitForm"
          >
            {{ categoryStore.saving ? "儲存中..." : editingCategoryId ? "更新分類" : "建立分類" }}
          </button>
          <button class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200" @click="resetForm">
            重設
          </button>
        </div>
      </div>

      <div v-else class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
        分類停用前會檢查是否仍有啟用中的商品使用該分類，避免商品資料失效。
      </div>

      <div class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
        <p class="text-sm font-semibold text-white">分類統計</p>
        <p class="mt-2 text-sm text-slate-300">目前共 {{ categoryStore.categories.length }} 個分類。</p>
      </div>
    </aside>
  </section>
</template>
