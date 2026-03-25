<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import { useProductStore } from "@/stores/products";
import type { MaterialAdminItem } from "@/types/materials";
import type { PackagingAdminItem } from "@/types/packaging";
import type { ProductAdminItem } from "@/types/product";
import { isEmbeddedImage, readImageFileAsDataUrl } from "@/utils/image-upload";

type EditableMaterialComponent = {
  materialItemId: string;
  quantity: string;
};

type EditablePackagingComponent = {
  packagingItemId: string;
  quantity: string;
};

type EditableCustomizationOption = {
  name: string;
  priceDelta: string;
  defaultSelected: boolean;
  displayOrder: string;
};

type EditableCustomizationGroup = {
  name: string;
  selectionMode: "SINGLE" | "MULTIPLE";
  required: boolean;
  minSelections: string;
  maxSelections: string;
  displayOrder: string;
  options: EditableCustomizationOption[];
};

const authStore = useAuthStore();
const productStore = useProductStore();

const isFormOpen = ref(false);
const editingProductId = ref<string | null>(null);
const formError = ref("");
const selectedCategoryId = ref("all");
const selectedCampaignFilter = ref<"all" | "campaign" | "regular">("all");
const canEditProducts = computed(() => authStore.hasPermission(PERMISSIONS.PRODUCTS_EDIT));

const form = reactive({
  categoryId: "",
  sku: "",
  name: "",
  description: "",
  imageUrl: "",
  imageUrlInput: "",
  uploadedImageName: "",
  price: "0.00",
  campaignEnabled: false,
  campaignLabel: "",
  campaignPrice: "",
  campaignStartsAt: "",
  campaignEndsAt: "",
  recipeNote: "",
  materialComponents: [] as EditableMaterialComponent[],
  packagingComponents: [] as EditablePackagingComponent[],
  customizationGroups: [] as EditableCustomizationGroup[],
});

const titleText = computed(() => (editingProductId.value ? "編輯商品" : "新增商品"));

const filteredProducts = computed(() =>
  productStore.products.filter((product) => {
    const categoryMatched = selectedCategoryId.value === "all" || product.categoryId === selectedCategoryId.value;
    const campaignMatched =
      selectedCampaignFilter.value === "all"
      || (selectedCampaignFilter.value === "campaign" && product.campaignEnabled)
      || (selectedCampaignFilter.value === "regular" && !product.campaignEnabled);

    return categoryMatched && campaignMatched;
  }),
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

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "--";
  }

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function resetForm() {
  editingProductId.value = null;
  formError.value = "";
  form.categoryId = productStore.categories[0]?.id ?? "";
  form.sku = "";
  form.name = "";
  form.description = "";
  form.imageUrl = "";
  form.imageUrlInput = "";
  form.uploadedImageName = "";
  form.price = "0.00";
  form.campaignEnabled = false;
  form.campaignLabel = "";
  form.campaignPrice = "";
  form.campaignStartsAt = "";
  form.campaignEndsAt = "";
  form.recipeNote = "";
  form.materialComponents = [];
  form.packagingComponents = [];
  form.customizationGroups = [];
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
  form.imageUrlInput = isEmbeddedImage(product.imageUrl) ? "" : (product.imageUrl ?? "");
  form.uploadedImageName = isEmbeddedImage(product.imageUrl) ? "已上傳圖片" : "";
  form.price = product.price.toFixed(2);
  form.campaignEnabled = product.campaignEnabled;
  form.campaignLabel = product.campaignLabel ?? "";
  form.campaignPrice = product.campaignPrice?.toFixed(2) ?? "";
  form.campaignStartsAt = product.campaignStartsAt ? product.campaignStartsAt.slice(0, 16) : "";
  form.campaignEndsAt = product.campaignEndsAt ? product.campaignEndsAt.slice(0, 16) : "";
  form.recipeNote = "";
  form.materialComponents = product.materialComponents.map((component) => ({
    materialItemId: component.materialItemId,
    quantity: component.quantity.toString(),
  }));
  form.packagingComponents = product.packagingComponents.map((component) => ({
    packagingItemId: component.packagingItemId,
    quantity: component.quantity.toString(),
  }));
  form.customizationGroups = product.customizationGroups.map((group) => ({
    name: group.name,
    selectionMode: group.selectionMode,
    required: group.required,
    minSelections: group.minSelections.toString(),
    maxSelections: group.maxSelections.toString(),
    displayOrder: group.displayOrder.toString(),
    options: group.options.map((option) => ({
      name: option.name,
      priceDelta: option.priceDelta.toString(),
      defaultSelected: option.defaultSelected,
      displayOrder: option.displayOrder.toString(),
    })),
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

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

  try {
    form.imageUrl = await readImageFileAsDataUrl(file);
    form.imageUrlInput = "";
    form.uploadedImageName = file.name;
    formError.value = "";
  } catch (error) {
    formError.value = error instanceof Error ? error.message : "圖片上傳失敗。";
  } finally {
    input.value = "";
  }
}

function handleImageUrlInput() {
  form.imageUrl = form.imageUrlInput.trim();
  if (form.imageUrl) {
    form.uploadedImageName = "";
  }
}

function clearImage() {
  form.imageUrl = "";
  form.imageUrlInput = "";
  form.uploadedImageName = "";
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

function addCustomizationGroup() {
  if (!canEditProducts.value) {
    return;
  }

  form.customizationGroups.push({
    name: "",
    selectionMode: "SINGLE",
    required: false,
    minSelections: "0",
    maxSelections: "1",
    displayOrder: String(form.customizationGroups.length),
    options: [
      {
        name: "",
        priceDelta: "0.00",
        defaultSelected: false,
        displayOrder: "0",
      },
    ],
  });
}

function removeCustomizationGroup(index: number) {
  if (!canEditProducts.value) {
    return;
  }

  form.customizationGroups.splice(index, 1);
}

function addCustomizationOption(groupIndex: number) {
  if (!canEditProducts.value) {
    return;
  }

  form.customizationGroups[groupIndex]?.options.push({
    name: "",
    priceDelta: "0.00",
    defaultSelected: false,
    displayOrder: String(form.customizationGroups[groupIndex].options.length),
  });
}

function removeCustomizationOption(groupIndex: number, optionIndex: number) {
  if (!canEditProducts.value) {
    return;
  }

  form.customizationGroups[groupIndex]?.options.splice(optionIndex, 1);
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

function campaignStatus(product: ProductAdminItem) {
  if (!product.campaignEnabled) {
    return "一般商品";
  }

  const now = new Date();
  const startsAt = product.campaignStartsAt ? new Date(product.campaignStartsAt) : null;
  const endsAt = product.campaignEndsAt ? new Date(product.campaignEndsAt) : null;

  if (startsAt && now < startsAt) {
    return "尚未開始";
  }

  if (endsAt && now > endsAt) {
    return "已結束";
  }

  return product.campaignActive ? "活動中" : "活動設定";
}

function validateComponents() {
  const materialIds = new Set<string>();
  for (const component of form.materialComponents) {
    if (!component.materialItemId) {
      formError.value = "請選擇原料項目。";
      return false;
    }

    const quantity = Number(component.quantity);
    if (Number.isNaN(quantity) || quantity <= 0) {
      formError.value = "原料用量必須大於 0。";
      return false;
    }

    if (materialIds.has(component.materialItemId)) {
      formError.value = "原料不可重複加入。";
      return false;
    }
    materialIds.add(component.materialItemId);
  }

  const packagingIds = new Set<string>();
  for (const component of form.packagingComponents) {
    if (!component.packagingItemId) {
      formError.value = "請選擇包裝項目。";
      return false;
    }

    const quantity = Number(component.quantity);
    if (Number.isNaN(quantity) || quantity <= 0) {
      formError.value = "包裝用量必須大於 0。";
      return false;
    }

    if (packagingIds.has(component.packagingItemId)) {
      formError.value = "包裝不可重複加入。";
      return false;
    }
    packagingIds.add(component.packagingItemId);
  }

  return true;
}

function validateCustomizationGroups() {
  const groupNames = new Set<string>();

  for (const group of form.customizationGroups) {
    const name = group.name.trim();
    if (!name) {
      formError.value = "Customization group name is required.";
      return false;
    }

    const normalizedName = name.toLowerCase();
    if (groupNames.has(normalizedName)) {
      formError.value = "Customization group name must be unique.";
      return false;
    }
    groupNames.add(normalizedName);

    const minSelections = Number(group.minSelections);
    const maxSelections = Number(group.maxSelections);
    if (Number.isNaN(minSelections) || minSelections < 0) {
      formError.value = "Customization min selections is invalid.";
      return false;
    }
    if (Number.isNaN(maxSelections) || maxSelections < 1 || maxSelections < minSelections) {
      formError.value = "Customization max selections is invalid.";
      return false;
    }
    if (group.selectionMode === "SINGLE" && maxSelections > 1) {
      formError.value = "Single selection group can only choose one option.";
      return false;
    }
    if (group.required && minSelections < 1) {
      formError.value = "Required customization group must select at least one option.";
      return false;
    }
    if (!group.options.length) {
      formError.value = "Each customization group needs at least one option.";
      return false;
    }

    const optionNames = new Set<string>();
    let defaultSelectedCount = 0;
    for (const option of group.options) {
      const optionName = option.name.trim();
      if (!optionName) {
        formError.value = "Customization option name is required.";
        return false;
      }

      const normalizedOptionName = optionName.toLowerCase();
      if (optionNames.has(normalizedOptionName)) {
        formError.value = "Customization option name must be unique in a group.";
        return false;
      }
      optionNames.add(normalizedOptionName);

      const priceDelta = Number(option.priceDelta);
      if (Number.isNaN(priceDelta) || priceDelta < 0) {
        formError.value = "Customization price delta must be zero or greater.";
        return false;
      }

      if (option.defaultSelected) {
        defaultSelectedCount += 1;
      }
    }

    if (group.selectionMode === "SINGLE" && defaultSelectedCount > 1) {
      formError.value = "Single selection group cannot have multiple default options.";
      return false;
    }
    if (defaultSelectedCount > maxSelections) {
      formError.value = "Default selected options exceed the max selections setting.";
      return false;
    }
  }

  return true;
}

async function submitForm() {
  if (!canEditProducts.value) {
    return;
  }

  formError.value = "";

  if (!form.categoryId) {
    formError.value = "請選擇商品分類。";
    return;
  }

  if (!form.sku.trim() || !form.name.trim()) {
    formError.value = "SKU 與商品名稱為必填。";
    return;
  }

  if (Number(form.price) <= 0) {
    formError.value = "商品售價必須大於 0。";
    return;
  }

  if (form.campaignEnabled) {
    if (!form.campaignPrice || Number(form.campaignPrice) <= 0) {
      formError.value = "活動價必須大於 0。";
      return;
    }

    if (Number(form.campaignPrice) > Number(form.price)) {
      formError.value = "活動價不可高於原價。";
      return;
    }

    if (form.campaignStartsAt && form.campaignEndsAt && new Date(form.campaignStartsAt) > new Date(form.campaignEndsAt)) {
      formError.value = "活動開始時間不可晚於結束時間。";
      return;
    }
  }

  if (!validateComponents()) {
    return;
  }

  if (!validateCustomizationGroups()) {
    return;
  }

  const payload = {
    categoryId: form.categoryId,
    sku: form.sku.trim(),
    name: form.name.trim(),
    description: form.description.trim(),
    imageUrl: form.imageUrlInput.trim() || form.imageUrl.trim(),
    price: Number(form.price),
    campaignEnabled: form.campaignEnabled,
    campaignLabel: form.campaignEnabled ? form.campaignLabel.trim() || undefined : undefined,
    campaignPrice: form.campaignEnabled && form.campaignPrice ? Number(form.campaignPrice) : undefined,
    campaignStartsAt: form.campaignEnabled && form.campaignStartsAt ? new Date(form.campaignStartsAt).toISOString() : undefined,
    campaignEndsAt: form.campaignEnabled && form.campaignEndsAt ? new Date(form.campaignEndsAt).toISOString() : undefined,
    recipeNote: form.recipeNote.trim() || undefined,
    materialComponents: form.materialComponents.map((component) => ({
      materialItemId: component.materialItemId,
      quantity: Number(component.quantity),
    })),
    packagingComponents: form.packagingComponents.map((component) => ({
      packagingItemId: component.packagingItemId,
      quantity: Number(component.quantity),
    })),
    customizationGroups: form.customizationGroups.map((group) => ({
      name: group.name.trim(),
      selectionMode: group.selectionMode,
      required: group.required,
      minSelections: Number(group.minSelections),
      maxSelections: Number(group.maxSelections),
      displayOrder: Number(group.displayOrder || 0),
      options: group.options.map((option) => ({
        name: option.name.trim(),
        priceDelta: Number(option.priceDelta),
        defaultSelected: option.defaultSelected,
        displayOrder: Number(option.displayOrder || 0),
      })),
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
const currentDisplayPrice = computed(() =>
  form.campaignEnabled && form.campaignPrice ? Number(form.campaignPrice) : Number(form.price || 0),
);
const grossProfit = computed(() => currentDisplayPrice.value - totalCost.value);

onMounted(async () => {
  await productStore.loadCatalog();
  resetForm();
});
</script>

<template>
  <section class="grid gap-6 lg:grid-cols-[minmax(0,1.2fr)_minmax(320px,0.9fr)]">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 border-b border-white/8 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Catalog</p>
          <h3 class="mt-2 text-2xl font-semibold text-white">商品管理</h3>
          <p class="mt-2 text-sm text-slate-400">
            維護商品資料、配方、活動價格與成本，方便前台與報表共用同一份商品主檔。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button
            v-if="canEditProducts"
            class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950"
            @click="openCreateForm"
          >
            新增商品
          </button>
          <select
            v-model="selectedCategoryId"
            class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
          >
            <option value="all">全部分類</option>
            <option v-for="category in productStore.categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
          <select
            v-model="selectedCampaignFilter"
            class="rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-white outline-none"
          >
            <option value="all">全部商品</option>
            <option value="campaign">只看活動商品</option>
            <option value="regular">只看一般商品</option>
          </select>
        </div>
      </div>

      <p v-if="productStore.errorMessage" class="mt-4 text-sm text-brand-coral">{{ productStore.errorMessage }}</p>

      <div class="mt-6 overflow-x-auto rounded-[1.5rem] border border-white/8">
        <table class="min-w-full divide-y divide-white/8 text-left text-sm">
          <thead class="bg-white/5 text-slate-400">
            <tr>
              <th class="px-4 py-3 font-medium">SKU</th>
              <th class="px-4 py-3 font-medium">商品</th>
              <th class="px-4 py-3 font-medium">分類</th>
              <th class="px-4 py-3 font-medium">售價</th>
              <th class="px-4 py-3 font-medium">活動</th>
              <th class="px-4 py-3 font-medium">成本</th>
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
                <p class="mt-1 text-xs text-slate-400">{{ product.description || "無商品描述" }}</p>
              </td>
              <td class="px-4 py-4 text-slate-300">{{ product.categoryName }}</td>
              <td class="px-4 py-4">
                <p class="font-medium text-white">{{ formatCurrency(product.displayPrice) }}</p>
                <p
                  v-if="product.campaignEnabled && product.displayPrice !== product.price"
                  class="mt-1 text-xs text-slate-500 line-through"
                >
                  原價 {{ formatCurrency(product.price) }}
                </p>
              </td>
              <td class="px-4 py-4">
                <div class="rounded-2xl border border-white/8 bg-white/4 px-3 py-2">
                  <p class="text-sm font-medium text-white">{{ campaignStatus(product) }}</p>
                  <p v-if="product.campaignEnabled" class="mt-1 text-xs text-slate-400">
                    {{ product.campaignLabel || "未設定標籤" }}
                  </p>
                  <p v-if="product.campaignEnabled && product.campaignStartsAt" class="mt-1 text-xs text-slate-500">
                    起：{{ formatDateTime(product.campaignStartsAt) }}
                  </p>
                  <p v-if="product.campaignEnabled && product.campaignEndsAt" class="mt-1 text-xs text-slate-500">
                    迄：{{ formatDateTime(product.campaignEndsAt) }}
                  </p>
                </div>
              </td>
              <td class="px-4 py-4 text-white">{{ formatCurrency(product.totalCost) }}</td>
              <td class="px-4 py-4">
                <div v-if="product.imageUrl" class="h-16 w-16 overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70">
                  <img :src="product.imageUrl" :alt="product.name" class="h-full w-full object-cover" />
                </div>
                <div
                  v-else
                  class="flex h-16 w-16 items-center justify-center rounded-2xl border border-dashed border-white/10 bg-slate-900/50 text-[11px] text-slate-500"
                >
                  No Image
                </div>
              </td>
              <td class="px-4 py-4">
                <div class="flex gap-2">
                  <button
                    v-if="canEditProducts"
                    class="rounded-xl border border-white/10 px-3 py-2 text-xs text-slate-200"
                    @click="openEditForm(product)"
                  >
                    編輯
                  </button>
                  <button
                    v-if="canEditProducts"
                    class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral"
                    :disabled="!product.active"
                    @click="deactivate(product)"
                  >
                    停用
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!productStore.loading && filteredProducts.length === 0">
              <td colspan="8" class="px-4 py-12 text-center text-slate-400">目前沒有符合篩選條件的商品。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <aside class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20 lg:sticky lg:top-6 lg:self-start">
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
        <input v-model="form.price" type="number" min="0.01" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="原價" />
        <textarea v-model="form.description" rows="3" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="商品描述" />
        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-white">商品圖片</p>
              <p class="mt-1 text-xs text-slate-400">支援 JPG、PNG、GIF、WebP，上傳檔案不可超過 2MB。</p>
            </div>
            <button
              v-if="form.imageUrl"
              type="button"
              class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral"
              @click="clearImage"
            >
              移除圖片
            </button>
          </div>
          <div class="mt-4 flex items-start gap-4">
            <div
              class="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-slate-900/70 text-[11px] text-slate-500"
            >
              <img v-if="form.imageUrl" :src="form.imageUrl" :alt="form.name || '商品圖片'" class="h-full w-full object-cover" />
              <span v-else>尚未上傳</span>
            </div>
            <div class="min-w-0 flex-1 space-y-3">
              <input
                type="file"
                accept="image/png,image/jpeg,image/gif,image/webp"
                class="block w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-slate-200 file:mr-4 file:rounded-xl file:border-0 file:bg-brand-aqua file:px-3 file:py-2 file:text-sm file:font-semibold file:text-slate-950"
                @change="handleImageUpload"
              />
              <p v-if="form.uploadedImageName" class="text-xs text-slate-400">{{ form.uploadedImageName }}</p>
              <input
                v-model="form.imageUrlInput"
                type="url"
                class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none"
                placeholder="圖片網址（可留空，改用上傳）"
                @input="handleImageUrlInput"
              />
            </div>
          </div>
        </div>

        <div class="rounded-[1.5rem] border border-brand-aqua/15 bg-brand-aqua/5 p-4">
          <label class="flex items-center gap-3 text-sm font-medium text-white">
            <input v-model="form.campaignEnabled" type="checkbox" class="h-4 w-4 rounded border-white/20 bg-slate-900/80" />
            設定為活動商品
          </label>

          <div class="mt-4 grid gap-3">
            <input
              v-model="form.campaignLabel"
              :disabled="!form.campaignEnabled"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-40"
              placeholder="活動標籤，例如：春季限定"
            />
            <input
              v-model="form.campaignPrice"
              :disabled="!form.campaignEnabled"
              type="number"
              min="0.01"
              step="0.01"
              class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-40"
              placeholder="活動價"
            />
            <div class="grid gap-3 md:grid-cols-2">
              <label class="block">
                <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-500">Campaign Start</span>
                <input
                  v-model="form.campaignStartsAt"
                  :disabled="!form.campaignEnabled"
                  type="datetime-local"
                  class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-40"
                />
              </label>
              <label class="block">
                <span class="mb-2 block text-xs uppercase tracking-[0.2em] text-slate-500">Campaign End</span>
                <input
                  v-model="form.campaignEndsAt"
                  :disabled="!form.campaignEnabled"
                  type="datetime-local"
                  class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-40"
                />
              </label>
            </div>
          </div>
        </div>

        <input v-model="form.recipeNote" class="w-full rounded-2xl border border-white/10 bg-slate-900/80 px-4 py-3 text-white outline-none" placeholder="本次配方調整備註" />

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-white">商品配方：原料</p>
            <button v-if="canEditProducts" class="rounded-xl border border-brand-aqua/30 px-3 py-2 text-xs text-brand-aqua" @click="addMaterialComponent">新增原料</button>
          </div>
          <div v-for="(component, index) in form.materialComponents" :key="`material-${index}`" class="mt-4 rounded-2xl border border-white/8 bg-slate-900/50 p-4">
            <select v-model="component.materialItemId" class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none">
              <option value="" disabled>選擇原料</option>
              <option v-for="item in productStore.materials" :key="item.id" :value="item.id">
                {{ item.name }} ({{ item.unit }})
              </option>
            </select>
            <input v-model="component.quantity" type="number" min="0.001" step="0.001" class="mt-3 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none" placeholder="用量" />
            <p class="mt-3 text-xs text-slate-400">
              單位成本 {{ formatCurrency(materialItemById(component.materialItemId)?.latestUnitCost) }}
              / 該項成本 {{ formatCurrency(lineCost(materialItemById(component.materialItemId), component.quantity)) }}
            </p>
            <button v-if="canEditProducts" class="mt-3 rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" @click="removeMaterialComponent(index)">移除原料</button>
          </div>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-white">商品配方：包裝</p>
            <button v-if="canEditProducts" class="rounded-xl border border-brand-aqua/30 px-3 py-2 text-xs text-brand-aqua" @click="addPackagingComponent">新增包裝</button>
          </div>
          <div v-for="(component, index) in form.packagingComponents" :key="`packaging-${index}`" class="mt-4 rounded-2xl border border-white/8 bg-slate-900/50 p-4">
            <select v-model="component.packagingItemId" class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none">
              <option value="" disabled>選擇包裝</option>
              <option v-for="item in productStore.packagingItems" :key="item.id" :value="item.id">
                {{ item.name }} ({{ item.unit }})
              </option>
            </select>
            <input v-model="component.quantity" type="number" min="0.001" step="0.001" class="mt-3 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none" placeholder="用量" />
            <p class="mt-3 text-xs text-slate-400">
              單位成本 {{ formatCurrency(packagingItemById(component.packagingItemId)?.latestUnitCost) }}
              / 該項成本 {{ formatCurrency(lineCost(packagingItemById(component.packagingItemId), component.quantity)) }}
            </p>
            <button v-if="canEditProducts" class="mt-3 rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral" @click="removePackagingComponent(index)">移除包裝</button>
          </div>
        </div>

        <div class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-semibold text-white">Customization Options</p>
              <p class="mt-1 text-xs text-slate-400">Configure sugar level, ice level, toppings, or any paid add-on for POS ordering.</p>
            </div>
            <button
              v-if="canEditProducts"
              class="rounded-xl border border-brand-aqua/30 px-3 py-2 text-xs text-brand-aqua"
              @click="addCustomizationGroup"
            >
              Add Group
            </button>
          </div>

          <div
            v-for="(group, groupIndex) in form.customizationGroups"
            :key="`customization-group-${groupIndex}`"
            class="mt-4 rounded-2xl border border-white/8 bg-slate-900/50 p-4"
          >
            <div class="grid gap-3 md:grid-cols-2">
              <input
                v-model="group.name"
                class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
                placeholder="Group name"
              />
              <select
                v-model="group.selectionMode"
                class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
              >
                <option value="SINGLE">Single select</option>
                <option value="MULTIPLE">Multiple select</option>
              </select>
              <label class="flex items-center gap-2 rounded-xl border border-white/10 bg-slate-950/60 px-3 py-2 text-sm text-white">
                <input v-model="group.required" type="checkbox" class="h-4 w-4 rounded border-white/20 bg-slate-900/80" />
                Required group
              </label>
              <input
                v-model="group.displayOrder"
                type="number"
                min="0"
                class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
                placeholder="Display order"
              />
            </div>

            <div class="mt-3 grid gap-3 md:grid-cols-2">
              <input
                v-model="group.minSelections"
                type="number"
                min="0"
                class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
                placeholder="Min selections"
              />
              <input
                v-model="group.maxSelections"
                type="number"
                min="1"
                class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
                placeholder="Max selections"
              />
            </div>

            <div class="mt-4 flex items-center justify-between">
              <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Options</p>
              <div class="flex gap-2">
                <button
                  v-if="canEditProducts"
                  class="rounded-xl border border-brand-aqua/30 px-3 py-2 text-xs text-brand-aqua"
                  @click="addCustomizationOption(groupIndex)"
                >
                  Add Option
                </button>
                <button
                  v-if="canEditProducts"
                  class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral"
                  @click="removeCustomizationGroup(groupIndex)"
                >
                  Remove Group
                </button>
              </div>
            </div>

            <div
              v-for="(option, optionIndex) in group.options"
              :key="`customization-option-${groupIndex}-${optionIndex}`"
              class="mt-3 rounded-2xl border border-white/8 bg-slate-950/55 p-4"
            >
              <div class="grid gap-3 md:grid-cols-[minmax(0,1.3fr)_minmax(0,0.8fr)_minmax(0,0.8fr)]">
                <input
                  v-model="option.name"
                  class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
                  placeholder="Option name"
                />
                <input
                  v-model="option.priceDelta"
                  type="number"
                  min="0"
                  step="0.01"
                  class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
                  placeholder="Price delta"
                />
                <input
                  v-model="option.displayOrder"
                  type="number"
                  min="0"
                  class="w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2 text-sm text-white outline-none"
                  placeholder="Display order"
                />
              </div>

              <div class="mt-3 flex items-center justify-between">
                <label class="flex items-center gap-2 text-sm text-white">
                  <input v-model="option.defaultSelected" type="checkbox" class="h-4 w-4 rounded border-white/20 bg-slate-900/80" />
                  Default selected
                </label>
                <button
                  v-if="canEditProducts"
                  class="rounded-xl border border-brand-coral/20 px-3 py-2 text-xs text-brand-coral"
                  @click="removeCustomizationOption(groupIndex, optionIndex)"
                >
                  Remove Option
                </button>
              </div>
            </div>
          </div>

          <p v-if="form.customizationGroups.length === 0" class="mt-4 text-sm text-slate-400">
            No customization groups yet. Add one here so POS staff can choose sugar, ice, toppings, or add-ons.
          </p>
        </div>

        <div class="rounded-[1.5rem] border border-brand-aqua/20 bg-brand-aqua/5 p-4">
          <p class="text-sm font-semibold text-white">成本與毛利試算</p>
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
          <p class="text-sm font-semibold text-white">配方版本紀錄</p>
          <div class="mt-4 space-y-3">
            <div
              v-for="version in productStore.products.find((item) => item.id === editingProductId)?.recipeVersions ?? []"
              :key="`${version.versionNumber}-${version.effectiveAt}`"
              class="rounded-2xl border border-white/8 bg-slate-950/50 p-3 text-sm"
            >
              <div class="flex items-center justify-between">
                <p class="font-medium text-white">v{{ version.versionNumber }} / {{ version.status }}</p>
                <p class="text-xs text-slate-400">{{ formatDateTime(version.effectiveAt) }}</p>
              </div>
              <p class="mt-2 text-slate-300">{{ version.note || "無備註" }}</p>
              <p class="mt-2 text-xs text-slate-400">
                原料 {{ version.materialComponentCount }} 項 / 包裝 {{ version.packagingComponentCount }} 項 /
                總成本 {{ formatCurrency(version.totalCost) }}
              </p>
            </div>
          </div>
        </div>

        <div class="flex gap-3">
          <button
            v-if="canEditProducts"
            class="flex-1 rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950"
            :disabled="productStore.saving"
            @click="submitForm"
          >
            {{ productStore.saving ? "儲存中..." : editingProductId ? "更新商品" : "建立商品" }}
          </button>
          <button
            v-if="canEditProducts"
            class="rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200"
            @click="resetForm"
          >
            清空
          </button>
        </div>

        <p v-if="!canEditProducts" class="text-sm text-slate-400">
          你目前只有檢視權限，無法修改商品、配方與成本設定。
        </p>
      </div>

      <div v-else class="mt-6 rounded-[1.5rem] border border-white/8 bg-white/4 p-4 text-sm text-slate-300">
        展開右側編輯區後，可以設定商品資訊、活動價格、配方原料、包裝與成本。
      </div>
    </aside>
  </section>
</template>
