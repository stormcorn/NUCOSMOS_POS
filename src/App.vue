<script setup lang="ts">
import { computed } from "vue";
import { RouterView, useRoute } from "vue-router";

import AdminShell from "@/components/layout/AdminShell.vue";

const route = useRoute();

const isAuthLayout = computed(() => route.meta.layout === "auth");
</script>

<template>
  <RouterView v-if="isAuthLayout" />
  <AdminShell v-else>
    <RouterView />
  </AdminShell>
</template>
