<script setup lang="ts">
defineProps<{
  label: string;
  value: string;
  delta: string;
  tone: "mint" | "amber" | "sky" | "coral";
}>();

const toneClasses = {
  mint: "from-brand-aqua/22 to-brand-aqua/0 text-brand-aqua",
  amber: "from-brand-amber/25 to-brand-amber/0 text-brand-amber",
  sky: "from-sky-300/18 to-sky-300/0 text-sky-200",
  coral: "from-brand-coral/20 to-brand-coral/0 text-brand-coral",
};
</script>

<template>
  <article class="animate-fade-up rounded-[1.75rem] border border-white/10 bg-slate-950/55 p-5 shadow-soft shadow-black/20">
    <div class="flex items-start justify-between gap-4">
      <div>
        <p class="text-xs uppercase tracking-[0.28em] text-slate-500">{{ label }}</p>
        <p class="mt-4 text-3xl font-semibold text-white">{{ value }}</p>
      </div>
      <div class="rounded-2xl border px-3 py-2 text-xs font-medium" :class="['bg-gradient-to-br', toneClasses[tone]]">
        {{ delta }}
      </div>
    </div>
  </article>
</template>
