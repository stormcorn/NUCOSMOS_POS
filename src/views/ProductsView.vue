<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useProductStore } from "@/stores/products";
import type { MaterialAdminItem } from "@/types/materials";
import type { PackagingAdminItem } from "@/types/packaging";
import type { ProductAdminItem } from "@/types/product";

type EditableMaterialComponent = {
  materialItemId: string;
  quantity: string;
};

type EditablePackagingComponent = {
  packagingItemId: string;
  quantity: string;
};

const authStore = useAuthStore();
const productStore = useProductStore();

const isFormOpen = ref(false);
const editingProductId = ref<string | null>(null);
const formError = ref("");
const selectedCategoryId = ref("all");
const canEditProducts = computed(() => authStore.hasPermission(PERMISSIONS.PRODUCTS_EDIT));

const form = reactive({
  categoryId: "",
  sku: "",
  name: "",
  description: "",
  imageUrl: "",
  price: "0.00",
  recipeNote: "",
  materialComponents: [] as EditableMaterialComponent[],
  packagingComponents: [] as EditablePackagingComponent[],
});

const titleText = computed(() => (editingProductId.value ? "編輯商品" : "新增商品"));

const filteredProducts = computed(() =>
  selectedCategoryId.value === "all"
    ? productStore.products
    : productStore.products.filter((product) => product.categoryId === selectedCategoryId.value),
);

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "--";
  }

  return new Intl.NumberFormat("zh-TW", {
    style: "currency",
    currency: "TWD",
    minimumFractionDigits: 2,
  }).format(value);
}

function resetForm() {
  editingProductId.value = null;
  formError.value = "";
  form.categoryId = productStore.categories[0]?.id ?? "";
  form.sku = "";
  form.name = "";
  form.description = "";
  form.imageUrl = "";
  form.price = "0.00";
  form.recipeNote = "";
  form.materialComponents = [];
  form.packagingComponents = [];
}

function openCreateForm() {
  if (!canEditProducts.value) {
    return;
  }
  resetForm();
  isFormOpen.value = true;
}

function openEditForm(product: ProductAdminItem) {
  if (!canEditProducts.value) {
    return;
  }
  editingProductId.value = product.id;
  formError.value = "";
  form.categoryId = product.categoryId;
  form.sku = product.sku;
  form.name = product.name;
  form.description = product.description ?? "";
  form.imageUrl = product.imageUrl ?? "";
  form.price = product.price.toFixed(2);
  form.recipeNote = "";
  form.materialComponents = product.materialComponents.map((component) => ({
    materialItemId: component.materialItemId,
    quantity: component.quantity.toString(),
  }));
  form.packagingComponents = product.packagingComponents.map((component) => ({
    packagingItemId: component.packagingItemId,
    quantity: component.quantity.toString(),
  }));
  isFormOpen.value = true;
}

function addMaterialComponent() {
  if (!canEditProducts.value) {
    return;
  }
  form.materialComponents.push({
    materialItemId: productStore.materials[0]?.id ?? "",
    quantity: "1",
  });
}

function addPackagingComponent() {
  if (!canEditProducts.value) {
    return;
  }
  form.packagingComponents.push({
    packagingItemId: productStore.packagingItems[0]?.id ?? "",
    quantity: "1",
  });
}

function removeMaterialComponent(index: number) {
  if (!canEditProducts.value) {
    return;
  }
  form.materialComponents.splice(index, 1);
}

function removePackagingComponent(index: number) {
  if (!canEditProducts.value) {
    return;
  }
  form.packagingComponents.splice(index, 1);
}

function materialItemById(materialItemId: string) {
  return productStore.materials.find((item) => item.id === materialItemId) ?? null;
}

function packagingItemById(packagingItemId: string) {
  return productStore.packagingItems.find((item) => item.id === packagingItemId) ?? null;
}

function lineCost(item: MaterialAdminItem | PackagingAdminItem | null, quantityText: string) {
  const quantity = Number(quantityText);
  if (!item || item.latestUnitCost === null || Number.isNaN(quantity) || quantity <= 0) {
    return 0;
  }

  return item.latestUnitCost * quantity;
}

function validateComponents() {
  const materialIds = new Set<string>();
  for (const component of form.materialComponents) {
    if (!component.materialItemId) {
      formError.value = "請選擇原料。";
      return false;
    }

    const quantity = Number(component.quantity);
    if (Number.isNaN(quantity) || quantity <= 0) {
      formError.value = "原料用量必須大於 0。";
      return false;
    }

    if (materialIds.has(component.materialItemId)) {
      formError.value = "同一個原料不能重複加入。";
      return false;
    }
    materialIds.add(component.materialItemId);
  }

  const packagingIds = new Set<string>();
  for (const component of form.packagingComponents) {
    if (!component.packagingItemId) {
      formError.value = "請選擇包裝。";
      return false;
    }

    const quantity = Number(component.quantity);
    if (Number.isNaN(quantity) || quantity <= 0) {
      formError.value = "包裝用量必須大於 0。";
      return false;
    }

    if (packagingIds.has(component.packagingItemId)) {
      formError.value = "同一個包裝不能重複加入。";
      return false;
    }
    packagingIds.add(component.packagingItemId);
  }

  return true;
}

async function submitForm() {
  if (!canEditProducts.value) {
    return;
  }
  formError.value = "";

  if (!form.categoryId) {
    formError.value = "請先選擇商品分類。";
    return;
  }

  if (!form.sku.trim() || !form.name.trim()) {
    formError.value = "SKU 與商品名稱為必填欄位。";
    return;
  }

  if (Number(form.price) <= 0) {
    formError.value = "商品售價必須大於 0。";
    return;
  }

  if (!validateComponents()) {
    return;
  }

  const payload = {
    categoryId: form.categoryId,
    sku: form.sku.trim(),
    name: form.name.trim(),
    description: form.description.trim(),
    imageUrl: form.imageUrl.trim(),
    price: Number(form.price),
    recipeNote: form.recipeNote.trim() || undefined,
    materialComponents: form.materialComponents.map((component) => ({
      materialItemId: component.materialItemId,
      quantity: Number(component.quantity),
    })),
    packagingComponents: form.packagingComponents.map((component) => ({
      packagingItemId: component.packagingItemId,
      quantity: Number(component.quantity),
    })),
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
  if (!canEditProducts.value) {
    return;
  }
  if (!window.confirm(`確定要停用商品「${product.name}」嗎？`)) {
    return;
  }

  await productStore.deactivateProduct(product.id);
}

const materialCost = computed(() =>
  form.materialComponents.reduce((sum, component) => sum + lineCost(materialItemById(component.materialItemId), component.quantity), 0),
);

const packagingCost = computed(() =>
  form.packagingComponents.reduce((sum, component) => sum + lineCost(packagingItemById(component.packagingItemId), component.quantity), 0),
);

const totalCost = computed(() => materialCost.value + packagingCost.value);
const grossProfit = computed(() => Number(form.price || 0) - totalCost.value);

onMounted(async () => {
  await productStore.loadCatalog();
  resetForm();
});
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[1.3fr_0.95fr]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Catalog</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">商品管理</h3>
          <p class="mt-2 text-sm text-slate-400">維護商品資料、圖片、配方與成本，同時保留配方版本歷史。</p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button v-if="canEditProducts" class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" @click="openCreateForm">
            新增商品
          </button>
          <select v-model="selectedCategoryId" class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none">
            <option value="all">全部分類</option>
            <option v-for="category in productStore.categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </div>
      </div>

      <p v-if="productStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ productStore.errorMessage }}</p>

      <div class="mt-6 overflow-hidden rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">SKU</th>
              <th class="px-4 py-3 font-medium">商品</th>
              <th class="px-4 py-3 font-medium">分類</th>
              <th class="px-4 py-3 font-medium">售價</th>
              <th class="px-4 py-3 font-medium">成本</th>
              <th class="px-4 py-3 font-medium">配方版本</th>
              <th class="px-4 py-3 font-medium">圖片</th>
              <th class="px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/8">
            <tr v-if="productStore.loading">
              <td colspan="8" class="px-4 py-12 text-center text-slate-400">商品資料載入中...</td>
            </tr>
            <tr v-for="product in filteredProducts" :key="product.id" class="bg-slate-950/25">
              <td class="px-4 py-4 text-slate-300">{{ product.sku }}</td>
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ product.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ product.description || "無描述" }}</p>
              </td>
              <td class="px-4 py-4 text-slate-300">{{ product.categoryName }}</td>
              <td class="px-4 py-4 text-white">{{ formatCurrency(product.price) }}</td>
              <td class="px-4 py-4 text-white">{{ formatCurrency(product.totalCost) }}</td>
              <td class="px-4 py-4 text-slate-300">
                <div v-if="product.recipeVersions.length > 0">
                  <p class="font-medium text-white">v{{ product.recipeVersions[0].versionNumber }}</p>
                  <p class="mt-1 text-xs text-slate-400">{{ product.recipeVersions[0].status }}</p>
                </div>
                <span v-else>--</span>
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
                  <button v-if="canEditProducts" class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="openEditForm(product)">編輯</button>
                  <button v-if="canEditProducts" class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" :disabled="!product.active" @click="deactivate(product)">停用</button>
                </div>
              </td>
            </tr>
            <tr v-if="!productStore.loading && filteredProducts.length === 0">
              <td colspan="8" class="px-4 py-12 text-center text-slate-400">目前沒有符合條件的商品。</td>
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
        <button class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200" @click="isFormOpen = !isFormOpen">
          {{ isFormOpen ? "收合" : "展開" }}
        </button>
      </div>

      <div v-if="isFormOpen" class="mt-6 space-y-5">
        <div v-if="formError" class="rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
          {{ formError }}
        </div>

        <select v-model="form.categoryId" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none">
          <option v-for="category in productStore.categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>

        <input v-model="form.sku" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="SKU" />
        <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="商品名稱" />
        <input v-model="form.price" type="number" min="0.01" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="售價" />
        <textarea v-model="form.description" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="描述" />
        <input v-model="form.imageUrl" type="url" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="圖片網址" />
        <input v-model="form.recipeNote" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="本次配方版本說明" />

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-white">原料配方</p>
            <button v-if="canEditProducts" class="rounded-xl border border-brand-aqua/30 px-3 py-2 text-xs text-brand-aqua" @click="addMaterialComponent">新增原料</button>
          </div>
          <div v-for="(component, index) in form.materialComponents" :key="`material-${index}`" class="mt-4 rounded-2xl border border-white/8 bg-slate-900/50 p-4">
            <select v-model="component.materialItemId" class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none">
              <option value="" disabled>請選擇原料</option>
              <option v-for="item in productStore.materials" :key="item.id" :value="item.id">
                {{ item.name }} ({{ item.unit }})
              </option>
            </select>
            <input v-model="component.quantity" type="number" min="0.001" step="0.001" class="mt-3 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none" placeholder="用量" />
            <p class="mt-3 text-xs text-slate-400">
              單位成本 {{ formatCurrency(materialItemById(component.materialItemId)?.latestUnitCost) }} / 行成本 {{ formatCurrency(lineCost(materialItemById(component.materialItemId), component.quantity)) }}
            </p>
            <button v-if="canEditProducts" class="mt-3 rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" @click="removeMaterialComponent(index)">移除原料</button>
          </div>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-white">包裝配方</p>
            <button v-if="canEditProducts" class="rounded-xl border border-brand-aqua/30 px-3 py-2 text-xs text-brand-aqua" @click="addPackagingComponent">新增包裝</button>
          </div>
          <div v-for="(component, index) in form.packagingComponents" :key="`packaging-${index}`" class="mt-4 rounded-2xl border border-white/8 bg-slate-900/50 p-4">
            <select v-model="component.packagingItemId" class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none">
              <option value="" disabled>請選擇包裝</option>
              <option v-for="item in productStore.packagingItems" :key="item.id" :value="item.id">
                {{ item.name }} ({{ item.unit }})
              </option>
            </select>
            <input v-model="component.quantity" type="number" min="0.001" step="0.001" class="mt-3 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none" placeholder="用量" />
            <p class="mt-3 text-xs text-slate-400">
              單位成本 {{ formatCurrency(packagingItemById(component.packagingItemId)?.latestUnitCost) }} / 行成本 {{ formatCurrency(lineCost(packagingItemById(component.packagingItemId), component.quantity)) }}
            </p>
            <button v-if="canEditProducts" class="mt-3 rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" @click="removePackagingComponent(index)">移除包裝</button>
          </div>
        </div>

        <div class="rounded-[1.5rem] border border-brand-aqua/20 bg-brand-aqua/5 p-4">
          <p class="text-sm font-semibold text-white">成本摘要</p>
          <div class="mt-4 grid gap-3 sm:grid-cols-2">
            <div class="rounded-2xl border border-white/8 bg-slate-950/55 p-3">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">原料成本</p>
              <p class="mt-2 text-lg font-semibold text-white">{{ formatCurrency(materialCost) }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/55 p-3">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">包裝成本</p>
              <p class="mt-2 text-lg font-semibold text-white">{{ formatCurrency(packagingCost) }}</p>
            </div>
            <div class="rounded-2xl border border-brand-aqua/20 bg-slate-950/55 p-3">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">總成本</p>
              <p class="mt-2 text-lg font-semibold text-brand-aqua">{{ formatCurrency(totalCost) }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/55 p-3">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">預估毛利</p>
              <p class="mt-2 text-lg font-semibold text-white">{{ formatCurrency(grossProfit) }}</p>
            </div>
          </div>
        </div>

        <div v-if="editingProductId" class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-sm font-semibold text-white">配方版本歷史</p>
          <div class="mt-4 space-y-3">
            <div v-for="version in productStore.products.find((item) => item.id === editingProductId)?.recipeVersions ?? []" :key="`${version.versionNumber}-${version.effectiveAt}`" class="rounded-2xl border border-white/8 bg-slate-950/50 p-3 text-sm">
              <div class="flex items-center justify-between">
                <p class="font-medium text-white">v{{ version.versionNumber }} / {{ version.status }}</p>
                <p class="text-xs text-slate-400">{{ new Date(version.effectiveAt).toLocaleString("zh-TW") }}</p>
              </div>
              <p class="mt-2 text-slate-300">{{ version.note || "無版本備註" }}</p>
              <p class="mt-2 text-xs text-slate-400">原料 {{ version.materialComponentCount }} 項 / 包裝 {{ version.packagingComponentCount }} 項 / 成本 {{ formatCurrency(version.totalCost) }}</p>
            </div>
          </div>
        </div>

        <div class="flex gap-3">
          <button v-if="canEditProducts" class="flex-1 rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950" :disabled="productStore.saving" @click="submitForm">
            {{ productStore.saving ? "儲存中..." : editingProductId ? "更新商品" : "建立商品" }}
          </button>
          <button v-if="canEditProducts" class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200" @click="resetForm">清空</button>
        </div>
        <p v-if="!canEditProducts" class="text-sm text-slate-400">你目前只有查看商品資料的權限，不能編輯商品、配方或成本。</p>
      </div>

      <div v-else class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
        展開右側編輯器後，可以維護商品圖片、配方用量、配方版本說明與成本資料。
      </div>
    </aside>
  </section>
</template>
