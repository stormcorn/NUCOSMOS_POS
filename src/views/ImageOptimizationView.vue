<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { fetchManufacturedItems, updateManufacturedItem } from "@/api/manufactured";
import { fetchMaterials, updateMaterial } from "@/api/materials";
import { fetchPackagingItems, updatePackagingItem } from "@/api/packaging";
import { fetchProducts, updateProduct } from "@/api/products";
import { ApiError } from "@/api/http";
import { PERMISSIONS } from "@/constants/permissions";
import { useAuthStore } from "@/stores/auth";
import type { ManufacturedAdminItem } from "@/types/manufactured";
import type { MaterialAdminItem } from "@/types/materials";
import type { PackagingAdminItem } from "@/types/packaging";
import type { ProductAdminItem } from "@/types/product";
import {
  estimateEmbeddedImageBytes,
  isEmbeddedImage,
  optimizeEmbeddedImageDataUrl,
} from "@/utils/image-upload";

type OptimizationScope = "商品" | "原料" | "製成品" | "包裝";

type Candidate = {
  key: string;
  scope: OptimizationScope;
  id: string;
  sku: string;
  name: string;
  imageUrl: string;
  originalBytes: number;
  applyOptimizedImage: (optimizedImageUrl: string) => Promise<void>;
};

type OptimizationResult = {
  key: string;
  scope: OptimizationScope;
  sku: string;
  name: string;
  originalBytes: number;
  optimizedBytes: number;
  status: "optimized" | "skipped" | "failed";
  message: string;
};

const authStore = useAuthStore();

const loading = ref(false);
const optimizing = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const progressMessage = ref("");
const lastScannedAt = ref<string | null>(null);
const candidates = ref<Candidate[]>([]);
const results = ref<OptimizationResult[]>([]);

const hasEditPermission = computed(() => authStore.hasPermission(PERMISSIONS.SETTINGS_EDIT));

const totalCandidateBytes = computed(() =>
  candidates.value.reduce((sum, candidate) => sum + candidate.originalBytes, 0),
);

const optimizedCount = computed(() =>
  results.value.filter((result) => result.status === "optimized").length,
);

const savedBytes = computed(() =>
  results.value
    .filter((result) => result.status === "optimized")
    .reduce((sum, result) => sum + Math.max(0, result.originalBytes - result.optimizedBytes), 0),
);

const groupedCounts = computed(() => {
  const summary = {
    商品: 0,
    原料: 0,
    製成品: 0,
    包裝: 0,
  } satisfies Record<OptimizationScope, number>;

  for (const candidate of candidates.value) {
    summary[candidate.scope] += 1;
  }

  return summary;
});

async function scanCatalog() {
  loading.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const [products, materials, manufacturedItems, packagingItems] = await Promise.all([
      fetchProducts(),
      fetchMaterials(),
      fetchManufacturedItems(),
      fetchPackagingItems(),
    ]);

    candidates.value = [
      ...buildProductCandidates(products),
      ...buildMaterialCandidates(materials),
      ...buildManufacturedCandidates(manufacturedItems),
      ...buildPackagingCandidates(packagingItems),
    ].sort((left, right) => right.originalBytes - left.originalBytes);

    lastScannedAt.value = new Date().toLocaleString("zh-TW", {
      dateStyle: "medium",
      timeStyle: "short",
    });

    if (candidates.value.length === 0) {
      successMessage.value = "目前沒有需要批次優化的內嵌圖片。";
    } else {
      successMessage.value = `已掃描到 ${candidates.value.length} 筆可優化圖片。`;
    }
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : "掃描圖片資料失敗。";
  } finally {
    loading.value = false;
  }
}

async function optimizeAllImages() {
  if (!hasEditPermission.value || optimizing.value || candidates.value.length === 0) {
    return;
  }

  optimizing.value = true;
  errorMessage.value = "";
  successMessage.value = "";
  results.value = [];

  try {
    const pendingCandidates = [...candidates.value];
    for (const [index, candidate] of pendingCandidates.entries()) {
      progressMessage.value = `正在處理 ${index + 1} / ${pendingCandidates.length}：${candidate.scope} ${candidate.sku}`;
      try {
        const optimizedImageUrl = await optimizeEmbeddedImageDataUrl(candidate.imageUrl);
        const optimizedBytes = estimateEmbeddedImageBytes(optimizedImageUrl);

        if (optimizedBytes >= candidate.originalBytes - 1024) {
          results.value.unshift({
            key: candidate.key,
            scope: candidate.scope,
            sku: candidate.sku,
            name: candidate.name,
            originalBytes: candidate.originalBytes,
            optimizedBytes,
            status: "skipped",
            message: "壓縮後差異不大，保留原圖。",
          });
          continue;
        }

        await candidate.applyOptimizedImage(optimizedImageUrl);
        results.value.unshift({
          key: candidate.key,
          scope: candidate.scope,
          sku: candidate.sku,
          name: candidate.name,
          originalBytes: candidate.originalBytes,
          optimizedBytes,
          status: "optimized",
          message: "已完成壓縮並回寫。",
        });
      } catch (error) {
        results.value.unshift({
          key: candidate.key,
          scope: candidate.scope,
          sku: candidate.sku,
          name: candidate.name,
          originalBytes: candidate.originalBytes,
          optimizedBytes: candidate.originalBytes,
          status: "failed",
          message: error instanceof Error ? error.message : "圖片優化失敗。",
        });
      }
    }

    successMessage.value = `批次處理完成，成功優化 ${optimizedCount.value} 筆圖片。`;
    await scanCatalog();
  } finally {
    optimizing.value = false;
    progressMessage.value = "";
  }
}

function buildProductCandidates(products: ProductAdminItem[]): Candidate[] {
  return products
    .filter((product) => isEmbeddedImage(product.imageUrl))
    .map((product) => ({
      key: `product-${product.id}`,
      scope: "商品" as const,
      id: product.id,
      sku: product.sku,
      name: product.name,
      imageUrl: product.imageUrl!,
      originalBytes: estimateEmbeddedImageBytes(product.imageUrl!),
      applyOptimizedImage: async (optimizedImageUrl: string) => {
        await updateProduct(product.id, {
          categoryId: product.categoryId,
          sku: product.sku,
          name: product.name,
          description: product.description ?? "",
          imageUrl: optimizedImageUrl,
          price: product.price,
          campaignEnabled: product.campaignEnabled,
          campaignLabel: product.campaignLabel ?? undefined,
          campaignPrice: product.campaignPrice ?? undefined,
          campaignStartsAt: product.campaignStartsAt ?? undefined,
          campaignEndsAt: product.campaignEndsAt ?? undefined,
          manufacturedComponents: product.manufacturedComponents.map((component) => ({
            manufacturedItemId: component.manufacturedItemId,
            quantity: component.quantity,
          })),
          materialComponents: product.materialComponents.map((component) => ({
            materialItemId: component.materialItemId,
            quantity: component.quantity,
          })),
          packagingComponents: product.packagingComponents.map((component) => ({
            packagingItemId: component.packagingItemId,
            quantity: component.quantity,
          })),
          customizationGroups: product.customizationGroups.map((group) => ({
            name: group.name,
            selectionMode: group.selectionMode,
            required: group.required,
            minSelections: group.minSelections,
            maxSelections: group.maxSelections,
            displayOrder: group.displayOrder,
            options: group.options.map((option) => ({
              name: option.name,
              priceDelta: option.priceDelta,
              defaultSelected: option.defaultSelected,
              displayOrder: option.displayOrder,
            })),
          })),
        });
      },
    }));
}

function buildMaterialCandidates(items: MaterialAdminItem[]): Candidate[] {
  return items
    .filter((item) => isEmbeddedImage(item.imageUrl))
    .map((item) => ({
      key: `material-${item.id}`,
      scope: "原料" as const,
      id: item.id,
      sku: item.sku,
      name: item.name,
      imageUrl: item.imageUrl!,
      originalBytes: estimateEmbeddedImageBytes(item.imageUrl!),
      applyOptimizedImage: async (optimizedImageUrl: string) => {
        await updateMaterial(item.id, {
          sku: item.sku,
          name: item.name,
          unit: item.unit,
          purchaseUnit: item.purchaseUnit,
          purchaseToStockRatio: item.purchaseToStockRatio,
          imageUrl: optimizedImageUrl,
          description: item.description ?? "",
          reorderLevel: item.reorderLevel,
          latestUnitCost: item.latestUnitCost,
        });
      },
    }));
}

function buildManufacturedCandidates(items: ManufacturedAdminItem[]): Candidate[] {
  return items
    .filter((item) => isEmbeddedImage(item.imageUrl))
    .map((item) => ({
      key: `manufactured-${item.id}`,
      scope: "製成品" as const,
      id: item.id,
      sku: item.sku,
      name: item.name,
      imageUrl: item.imageUrl!,
      originalBytes: estimateEmbeddedImageBytes(item.imageUrl!),
      applyOptimizedImage: async (optimizedImageUrl: string) => {
        await updateManufacturedItem(item.id, {
          sku: item.sku,
          name: item.name,
          unit: item.unit,
          purchaseUnit: item.purchaseUnit,
          purchaseToStockRatio: item.purchaseToStockRatio,
          imageUrl: optimizedImageUrl,
          description: item.description ?? "",
          reorderLevel: item.reorderLevel,
          latestUnitCost: item.latestUnitCost,
        });
      },
    }));
}

function buildPackagingCandidates(items: PackagingAdminItem[]): Candidate[] {
  return items
    .filter((item) => isEmbeddedImage(item.imageUrl))
    .map((item) => ({
      key: `packaging-${item.id}`,
      scope: "包裝" as const,
      id: item.id,
      sku: item.sku,
      name: item.name,
      imageUrl: item.imageUrl!,
      originalBytes: estimateEmbeddedImageBytes(item.imageUrl!),
      applyOptimizedImage: async (optimizedImageUrl: string) => {
        await updatePackagingItem(item.id, {
          sku: item.sku,
          name: item.name,
          unit: item.unit,
          purchaseUnit: item.purchaseUnit,
          purchaseToStockRatio: item.purchaseToStockRatio,
          specification: item.specification ?? "",
          imageUrl: optimizedImageUrl,
          description: item.description ?? "",
          reorderLevel: item.reorderLevel,
          latestUnitCost: item.latestUnitCost,
        });
      },
    }));
}

function formatBytes(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function resultTone(status: OptimizationResult["status"]) {
  if (status === "optimized") {
    return "border-emerald-400/20 bg-emerald-400/10 text-emerald-200";
  }

  if (status === "failed") {
    return "border-brand-coral/20 bg-brand-coral/10 text-brand-coral";
  }

  return "border-white/10 bg-white/5 text-slate-300";
}

onMounted(() => {
  void scanCatalog();
});
</script>

<template>
  <section class="space-y-6">
    <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">System Maintenance</p>
          <h2 class="mt-2 text-2xl font-semibold text-white">圖片優化</h2>
          <p class="mt-2 max-w-3xl text-sm text-slate-400">
            掃描既有的內嵌 base64 圖片，批次縮圖與壓縮後再回寫，降低商品清單 payload 與 POS 同步 timeout 風險。
          </p>
        </div>

        <div class="flex flex-wrap gap-3">
          <button
            class="rounded-2xl border border-white/10 px-5 py-3 text-sm font-semibold text-white transition hover:border-brand-aqua/30 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loading || optimizing"
            @click="scanCatalog"
          >
            {{ loading ? "掃描中..." : "重新掃描" }}
          </button>
          <button
            class="rounded-2xl bg-brand-aqua px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="!hasEditPermission || optimizing || loading || candidates.length === 0"
            @click="optimizeAllImages"
          >
            {{ optimizing ? "優化中..." : "批次優化全部圖片" }}
          </button>
        </div>
      </div>

      <div class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <article class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">待優化筆數</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ candidates.length }}</p>
          <p class="mt-2 text-xs text-slate-500">
            商品 {{ groupedCounts.商品 }} / 原料 {{ groupedCounts.原料 }} / 製成品 {{ groupedCounts.製成品 }} / 包裝 {{ groupedCounts.包裝 }}
          </p>
        </article>
        <article class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">目前總容量</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ formatBytes(totalCandidateBytes) }}</p>
          <p class="mt-2 text-xs text-slate-500">只計算內嵌 base64 圖片。</p>
        </article>
        <article class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">已優化筆數</p>
          <p class="mt-3 text-2xl font-semibold text-white">{{ optimizedCount }}</p>
          <p class="mt-2 text-xs text-slate-500">本次批次執行結果。</p>
        </article>
        <article class="rounded-[1.5rem] border border-brand-aqua/20 bg-brand-aqua/5 p-4">
          <p class="text-xs uppercase tracking-[0.2em] text-slate-400">已節省容量</p>
          <p class="mt-3 text-2xl font-semibold text-brand-aqua">{{ formatBytes(savedBytes) }}</p>
          <p class="mt-2 text-xs text-slate-500">以本次成功回寫的圖片計算。</p>
        </article>
      </div>

      <p v-if="lastScannedAt" class="mt-4 text-xs text-slate-500">最近掃描：{{ lastScannedAt }}</p>
      <p v-if="progressMessage" class="mt-4 rounded-2xl border border-brand-aqua/20 bg-brand-aqua/10 px-4 py-3 text-sm text-brand-aqua">
        {{ progressMessage }}
      </p>
      <p v-if="successMessage" class="mt-4 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
        {{ successMessage }}
      </p>
      <p v-if="errorMessage" class="mt-4 rounded-2xl border border-brand-coral/20 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
        {{ errorMessage }}
      </p>
    </article>

    <div class="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Candidates</p>
            <h3 class="mt-2 text-xl font-semibold text-white">待優化清單</h3>
          </div>
          <span class="rounded-full border border-white/10 px-3 py-1 text-xs text-slate-400">{{ candidates.length }} items</span>
        </div>

        <div class="mt-6 space-y-3">
          <div
            v-for="candidate in candidates"
            :key="candidate.key"
            class="rounded-[1.5rem] border border-white/8 bg-white/4 p-4"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-sm font-semibold text-white">{{ candidate.name }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ candidate.scope }} / {{ candidate.sku }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold text-white">{{ formatBytes(candidate.originalBytes) }}</p>
                <p class="mt-1 text-xs text-slate-500">目前圖片大小</p>
              </div>
            </div>
          </div>

          <p v-if="candidates.length === 0" class="rounded-[1.5rem] border border-dashed border-white/10 px-4 py-8 text-sm text-slate-400">
            目前沒有偵測到需要批次壓縮的內嵌圖片。
          </p>
        </div>
      </article>

      <article class="rounded-[2rem] border border-white/10 bg-slate-950/55 p-6 shadow-soft shadow-black/20">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-xs uppercase tracking-[0.28em] text-brand-aqua/70">Results</p>
            <h3 class="mt-2 text-xl font-semibold text-white">最近執行結果</h3>
          </div>
          <span class="rounded-full border border-white/10 px-3 py-1 text-xs text-slate-400">{{ results.length }} records</span>
        </div>

        <div class="mt-6 space-y-3">
          <div
            v-for="result in results"
            :key="result.key"
            class="rounded-[1.5rem] border p-4"
            :class="resultTone(result.status)"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-sm font-semibold">{{ result.name }}</p>
                <p class="mt-1 text-xs opacity-80">{{ result.scope }} / {{ result.sku }}</p>
                <p class="mt-3 text-sm">{{ result.message }}</p>
              </div>
              <div class="text-right text-xs opacity-80">
                <p>{{ formatBytes(result.originalBytes) }} → {{ formatBytes(result.optimizedBytes) }}</p>
              </div>
            </div>
          </div>

          <p v-if="results.length === 0" class="rounded-[1.5rem] border border-dashed border-white/10 px-4 py-8 text-sm text-slate-400">
            尚未執行批次優化。掃描後可直接開始整理現有圖片。
          </p>
        </div>
      </article>
    </div>
  </section>
</template>
