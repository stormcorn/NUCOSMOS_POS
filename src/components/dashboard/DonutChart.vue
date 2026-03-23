<script setup lang="ts">
import { computed } from "vue";

type Segment = {
  label: string;
  value: number;
  color: string;
};

const props = defineProps<{
  segments: Segment[];
  size?: number;
  strokeWidth?: number;
  centerTitle: string;
  centerValue: string;
}>();

const normalizedSegments = computed(() => props.segments.filter((segment) => segment.value > 0));
const total = computed(() => normalizedSegments.value.reduce((sum, segment) => sum + segment.value, 0));
const size = computed(() => props.size ?? 220);
const strokeWidth = computed(() => props.strokeWidth ?? 18);
const radius = computed(() => (size.value - strokeWidth.value) / 2);
const circumference = computed(() => 2 * Math.PI * radius.value);

const segmentArcs = computed(() => {
  if (total.value <= 0) {
    return [];
  }

  let offset = 0;

  return normalizedSegments.value.map((segment) => {
    const ratio = segment.value / total.value;
    const dash = circumference.value * ratio;
    const segmentOffset = circumference.value - offset;
    offset += dash;

    return {
      ...segment,
      dashArray: `${dash} ${circumference.value - dash}`,
      dashOffset: segmentOffset,
      percent: Math.round(ratio * 100),
    };
  });
});
</script>

<template>
  <div class="flex flex-col items-center gap-6 lg:flex-row lg:items-start">
    <div class="relative shrink-0">
      <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`" class="-rotate-90">
        <circle
          :cx="size / 2"
          :cy="size / 2"
          :r="radius"
          :stroke-width="strokeWidth"
          class="fill-none stroke-white/10"
        />
        <circle
          v-for="segment in segmentArcs"
          :key="segment.label"
          :cx="size / 2"
          :cy="size / 2"
          :r="radius"
          :stroke="segment.color"
          :stroke-width="strokeWidth"
          :stroke-dasharray="segment.dashArray"
          :stroke-dashoffset="segment.dashOffset"
          stroke-linecap="round"
          class="fill-none transition-all duration-500"
        />
      </svg>

      <div class="pointer-events-none absolute inset-0 flex flex-col items-center justify-center text-center">
        <p class="text-xs uppercase tracking-[0.24em] text-slate-500">{{ centerTitle }}</p>
        <p class="mt-2 text-2xl font-semibold text-white">{{ centerValue }}</p>
      </div>
    </div>

    <div class="grid flex-1 gap-3 self-stretch">
      <div
        v-for="segment in segmentArcs"
        :key="segment.label"
        class="rounded-2xl border border-white/8 bg-white/4 px-4 py-3"
      >
        <div class="flex items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <span class="h-3 w-3 rounded-full" :style="{ backgroundColor: segment.color }" />
            <span class="text-sm text-slate-200">{{ segment.label }}</span>
          </div>
          <span class="text-sm font-semibold text-white">{{ segment.percent }}%</span>
        </div>
        <p class="mt-2 text-xs text-slate-400">數值 {{ segment.value }}</p>
      </div>

      <p v-if="segmentArcs.length === 0" class="rounded-2xl border border-dashed border-white/10 px-4 py-6 text-sm text-slate-400">
        目前沒有可視化資料。
      </p>
    </div>
  </div>
</template>
