<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { useProductStore } from "@/stores/products";
import type { ProductAdminItem } from "@/types/product";

const productStore = useProductStore();

const isFormOpen = ref(false);
const editingProductId = ref<string | null>(null);
const formError = ref("");
const selectedCategoryId = ref("all");
const form = reactive({
  categoryId: "",
  sku: "",
  name: "",
  description: "",
  imageUrl: "",
  price: "0.00",
});

const titleText = computed(() => (editingProductId.value ? "編輯商品" : "新增商品"));
const filteredProducts = computed(() =>
  selectedCategoryId.value === "all"
    ? productStore.products
    : productStore.products.filter((product) => product.categoryId === selectedCategoryId.value),
);
const selectedCategoryName = computed(() => {
  if (selectedCategoryId.value === "all") {
    return "全部分類";
  }

  return productStore.categories.find((category) => category.id === selectedCategoryId.value)?.name ?? "未分類";
});

function resetForm() {
  editingProductId.value = null;
  formError.value = "";
  form.categoryId = productStore.categories[0]?.id ?? "";
  form.sku = "";
  form.name = "";
  form.description = "";
  form.imageUrl = "";
  form.price = "0.00";
}

function openCreateForm() {
  resetForm();
  isFormOpen.value = true;
}

function openEditForm(product: ProductAdminItem) {
  editingProductId.value = product.id;
  form.categoryId = product.categoryId;
  form.sku = product.sku;
  form.name = product.name;
  form.description = product.description ?? "";
  form.imageUrl = product.imageUrl ?? "";
  form.price = product.price.toFixed(2);
  isFormOpen.value = true;
}

async function submitForm() {
  formError.value = "";

  if (!form.categoryId) {
    formError.value = "請選擇商品分類。";
    return;
  }

  if (!form.sku.trim() || !form.name.trim()) {
    formError.value = "SKU 與商品名稱為必填欄位。";
    return;
  }

  if (Number(form.price) <= 0) {
    formError.value = "商品價格必須大於 0。";
    return;
  }

  const payload = {
    categoryId: form.categoryId,
    sku: form.sku.trim(),
    name: form.name.trim(),
    description: form.description.trim(),
    imageUrl: form.imageUrl.trim(),
    price: Number(form.price),
  };

  const success = editingProductId.value
    ? await productStore.updateProduct(editingProductId.value, payload)
    : await productStore.createProduct(payload);

  if (success) {
    resetForm();
    isFormOpen.value = false;
  }
}

async function deactivate(product: ProductAdminItem) {
  const confirmed = window.confirm(`確認要停用商品「${product.name}」嗎？`);
  if (!confirmed) {
    return;
  }

  await productStore.deactivateProduct(product.id);
}

onMounted(async () => {
  await productStore.loadCatalog();
  resetForm();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.4fr_0.8fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Catalog</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">商品管理</h3>
          <p class="mt-2 text-sm text-slate-400">管理商品資料、價格、狀態與圖片，資料來源為後端管理 API。</p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button
            class="rounded-2xl border px-4 py-3 text-sm text-slate-200 transition"
            :class="productStore.activeFilter === 'all' ? 'border-brand-aqua/30 bg-brand-aqua/10 text-white' : 'border-white/10 bg-white/5'"
            @click="productStore.loadProducts('all')"
          >
            全部
          </button>
          <button
            class="rounded-2xl border px-4 py-3 text-sm text-slate-200 transition"
            :class="productStore.activeFilter === 'active' ? 'border-brand-aqua/30 bg-brand-aqua/10 text-white' : 'border-white/10 bg-white/5'"
            @click="productStore.loadProducts('active')"
          >
            啟用中
          </button>
          <button
            class="rounded-2xl border px-4 py-3 text-sm text-slate-200 transition"
            :class="productStore.activeFilter === 'inactive' ? 'border-brand-aqua/30 bg-brand-aqua/10 text-white' : 'border-white/10 bg-white/5'"
            @click="productStore.loadProducts('inactive')"
          >
            已停用
          </button>
          <button class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110" @click="openCreateForm">
            新增商品
          </button>
        </div>
      </div>

      <p v-if="productStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ productStore.errorMessage }}</p>

      <div class="mt-6 flex flex-col gap-3 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm font-semibold text-white">商品分類篩選</p>
          <p class="mt-1 text-sm text-slate-400">目前顯示 {{ selectedCategoryName }}，共 {{ filteredProducts.length }} 筆商品。</p>
        </div>
        <label class="flex flex-col gap-2 text-sm text-slate-300 lg:min-w-64">
          <span>篩選分類</span>
          <select v-model="selectedCategoryId" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option value="all">全部分類</option>
            <option v-for="category in productStore.categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </label>
      </div>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">SKU</th>
              <th class="px-4 py-3 font-medium">商品名稱</th>
              <th class="px-4 py-3 font-medium">分類</th>
              <th class="px-4 py-3 font-medium">價格</th>
              <th class="px-4 py-3 font-medium">狀態</th>
              <th class="px-4 py-3 font-medium">商品圖片</th>
              <th class="px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="productStore.loading">
              <td colspan="7" class="px-4 py-12 text-center text-slate-400">商品資料載入中...</td>
            </tr>
            <tr v-for="product in filteredProducts" :key="product.id" class="bg-slate-950/25">
              <td class="px-4 py-4 text-slate-300">{{ product.sku }}</td>
              <td class="px-4 py-4 font-medium text-white">{{ product.name }}</td>
              <td class="px-4 py-4 text-slate-300">{{ product.categoryName }}</td>
              <td class="px-4 py-4 text-white">NT$ {{ product.price.toFixed(2) }}</td>
              <td class="px-4 py-4">
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="product.active ? 'bg-brand-aqua/15 text-brand-aqua' : 'bg-amber-200/10 text-brand-amber'"
                >
                  {{ product.active ? "啟用中" : "已停用" }}
                </span>
              </td>
              <td class="px-4 py-4">
                <div v-if="product.imageUrl" class="h-16 w-16 overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70">
                  <img :src="product.imageUrl" :alt="product.name" class="h-full w-full object-cover" />
                </div>
                <div v-else class="flex h-16 w-16 items-center justify-center rounded-2xl border border-dashed border-white/10 bg-slate-900/50 text-[11px] text-slate-500">
                  No Image
                </div>
              </td>
              <td class="px-4 py-4">
                <div class="flex gap-2">
                  <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-brand-aqua/30 hover:text-white" @click="openEditForm(product)">
                    編輯
                  </button>
                  <button
                    class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral transition hover:bg-brand-coral/10 disabled:opacity-40"
                    :disabled="!product.active"
                    @click="deactivate(product)"
                  >
                    停用
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!productStore.loading && filteredProducts.length === 0">
              <td colspan="7" class="px-4 py-12 text-center text-slate-400">目前沒有符合此分類條件的商品。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Editor</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">{{ titleText }}</h3>
        </div>
        <button
          class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200 transition hover:border-white/20 hover:text-white"
          @click="isFormOpen = !isFormOpen"
        >
          {{ isFormOpen ? "收合" : "展開" }}
        </button>
      </div>

      <div v-if="isFormOpen" class="mt-6 space-y-4">
        <div v-if="formError" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
          {{ formError }}
        </div>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">商品分類</span>
          <select v-model="form.categoryId" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
            <option v-for="category in productStore.categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">SKU</span>
          <input v-model="form.sku" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">商品名稱</span>
          <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">商品價格</span>
          <input v-model="form.price" type="number" min="0.01" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">商品描述</span>
          <textarea v-model="form.description" rows="4" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm text-slate-300">圖片網址</span>
          <input v-model="form.imageUrl" type="url" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="https://example.com/product.jpg" />
        </label>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm font-semibold text-white">圖片預覽</p>
          <div class="mt-4 flex h-40 items-center justify-center overflow-hidden rounded-[1.5rem] border border-white/10 bg-slate-900/70">
            <img v-if="form.imageUrl" :src="form.imageUrl" alt="Product preview" class="h-full w-full object-cover" />
            <span v-else class="text-sm text-slate-500">輸入圖片網址後，這裡會顯示預覽。</span>
          </div>
        </div>

        <div class="flex gap-3">
          <button
            class="flex-1 rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:opacity-60"
            :disabled="productStore.saving"
            @click="submitForm"
          >
            {{ productStore.saving ? "儲存中..." : editingProductId ? "更新商品" : "建立商品" }}
          </button>
          <button class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200" @click="resetForm">
            重設
          </button>
        </div>
      </div>

      <div v-else class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
        展開編輯器後可新增或修改商品，會呼叫後端的商品管理 API。
      </div>

      <div class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
        <p class="text-sm font-semibold text-white">分類總數</p>
        <p class="mt-2 text-sm text-slate-300">{{ productStore.categories.length }} 個已載入分類</p>
      </div>
    </aside>
  </section>
</template>
