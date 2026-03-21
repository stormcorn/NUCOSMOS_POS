import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { fetchDevices } from "@/api/devices";
import { ApiError } from "@/api/http";
import type { DeviceItem } from "@/types/device";

export const useDeviceStore = defineStore("devices", () => {
  const devices = ref<DeviceItem[]>([]);
  const loading = ref(false);
  const errorMessage = ref("");
  const statusFilter = ref("");

  const onlineCount = computed(() => devices.value.filter((device) => device.status === "ACTIVE").length);
  const offlineCount = computed(() => devices.value.filter((device) => device.status !== "ACTIVE").length);

  async function loadDevices(storeCode?: string, status = statusFilter.value) {
    loading.value = true;
    errorMessage.value = "";
    statusFilter.value = status;

    try {
      devices.value = await fetchDevices({ storeCode, status: status || undefined });
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法取得裝置資料";
    } finally {
      loading.value = false;
    }
  }

  return {
    devices,
    errorMessage,
    loadDevices,
    loading,
    offlineCount,
    onlineCount,
    statusFilter,
  };
});
