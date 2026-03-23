<script setup lang="ts">
type BarItem = {
  label: string;
  value: number;
  displayValue?: string;
  hint?: string;
  color?: string;
};

defineProps<{
  items: BarItem[];
  max?: number;
  emptyText?: string;
}>();

function widthPercent(value: number, maxValue?: number) {
  const safeMax = maxValue && maxValue > 0 ? maxValue : value;
  if (!safeMax || safeMax <= 0) {
    return 0;
  }

  return Math.max(6, Math.round((value / safeMax) * 100));
}
</script>

<template>
  <div class="space-y-4">
    <div
      v-for="item in items"
      :key="item.label"
      class="rounded-2xl border border-white/8 bg-white/4 px-4 py-3"
    >
      <div class="mb-2 flex items-center justify-between gap-3">
        <div>
          <p class="text-sm font-medium text-white">{{ item.label }}</p>
          <p v-if="item.hint" class="mt-1 text-xs text-slate-400">{{ item.hint }}</p>
        </div>
        <p class="text-sm font-semibold text-white">{{ item.displayValue ?? item.value }}</p>
      </div>

      <div class="h-3 rounded-full bg-white/8">
        <div
          class="h-3 rounded-full transition-all duration-500"
          :style="{
            width: `${widthPercent(item.value, max)}%`,
            background: item.color ?? 'linear-gradient(90deg, rgba(34,211,238,0.95), rgba(250,204,21,0.9))',
          }"
        />
      </div>
    </div>

    <p v-if="items.length === 0" class="rounded-2xl border border-dashed border-white/10 px-4 py-6 text-sm text-slate-400">
      {{ emptyText ?? "目前沒有可顯示的圖表資料。" }}
    </p>
  </div>
</template>
