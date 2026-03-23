<script setup lang="ts">
import { computed } from "vue";

type Point = {
  label: string;
  value: number;
  displayValue?: string;
};

const props = defineProps<{
  points: Point[];
  height?: number;
  strokeColor?: string;
  fillColor?: string;
  emptyText?: string;
}>();

const chartHeight = computed(() => props.height ?? 240);
const chartWidth = 100;

const normalizedPoints = computed(() => props.points.map((point) => ({ ...point, value: Math.max(0, point.value) })));
const maxValue = computed(() => Math.max(...normalizedPoints.value.map((point) => point.value), 0));

const coordinates = computed(() => {
  if (normalizedPoints.value.length === 0) {
    return [];
  }

  if (normalizedPoints.value.length === 1) {
    return [{ x: chartWidth / 2, y: 12, ...normalizedPoints.value[0] }];
  }

  return normalizedPoints.value.map((point, index) => {
    const x = (index / (normalizedPoints.value.length - 1)) * chartWidth;
    const ratio = maxValue.value > 0 ? point.value / maxValue.value : 0;
    const y = chartHeight.value - ratio * (chartHeight.value - 24) - 12;

    return { ...point, x, y };
  });
});

const polylinePoints = computed(() => coordinates.value.map((point) => `${point.x},${point.y}`).join(" "));
const areaPoints = computed(() => {
  if (coordinates.value.length === 0) {
    return "";
  }

  const first = coordinates.value[0];
  const last = coordinates.value[coordinates.value.length - 1];
  const inner = coordinates.value.map((point) => `${point.x},${point.y}`).join(" ");
  return `${first.x},${chartHeight.value} ${inner} ${last.x},${chartHeight.value}`;
});
</script>

<template>
  <div>
    <div
      v-if="coordinates.length === 0"
      class="rounded-2xl border border-dashed border-white/10 px-4 py-10 text-sm text-slate-400"
    >
      {{ emptyText ?? "目前沒有趨勢資料。" }}
    </div>

    <div v-else class="space-y-4">
      <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" class="h-[240px] w-full overflow-visible">
        <defs>
          <linearGradient id="trend-fill" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" :stop-color="fillColor ?? 'rgba(34,211,238,0.32)'" />
            <stop offset="100%" stop-color="rgba(15,23,42,0)" />
          </linearGradient>
        </defs>

        <g v-for="gridLine in 4" :key="gridLine">
          <line
            x1="0"
            :y1="(chartHeight / 4) * gridLine"
            x2="100"
            :y2="(chartHeight / 4) * gridLine"
            stroke="rgba(148,163,184,0.16)"
            stroke-dasharray="2 3"
          />
        </g>

        <polygon :points="areaPoints" fill="url(#trend-fill)" />
        <polyline
          :points="polylinePoints"
          fill="none"
          :stroke="strokeColor ?? '#22d3ee'"
          stroke-width="2.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        />

        <g v-for="point in coordinates" :key="point.label">
          <circle :cx="point.x" :cy="point.y" r="2.5" :fill="strokeColor ?? '#22d3ee'" />
        </g>
      </svg>

      <div class="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
        <div
          v-for="point in coordinates"
          :key="point.label"
          class="rounded-2xl border border-white/8 bg-white/4 px-3 py-3"
        >
          <p class="text-xs uppercase tracking-[0.18em] text-slate-500">{{ point.label }}</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ point.displayValue ?? point.value }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
