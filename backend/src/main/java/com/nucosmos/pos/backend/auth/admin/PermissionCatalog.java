package com.nucosmos.pos.backend.auth.admin;

import com.nucosmos.pos.backend.common.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PermissionCatalog {

    private static final Map<String, PermissionDefinitionResponse> DEFINITIONS = new LinkedHashMap<>();

    static {
        register("DASHBOARD_VIEW", "營運總覽", "營運總覽", "可查看營運摘要、提醒與總覽卡片");
        register("REPORTS_VIEW", "報表分析", "營運總覽", "可查看營收、毛利與庫存分析報表");

        register("PRODUCTS_VIEW", "商品查看", "商品管理", "可查看商品與商品分類資料");
        register("PRODUCTS_EDIT", "商品編輯", "商品管理", "可新增、編輯、停用商品與維護配方成本");

        register("INVENTORY_VIEW", "庫存查看", "庫存管理", "可查看商品、原料、包裝、瑕疵與盤點資料");
        register("INVENTORY_EDIT", "庫存編輯", "庫存管理", "可建立庫存異動、盤點、瑕疵處理與維護原料包裝");

        register("SUPPLIERS_VIEW", "供應商查看", "採購管理", "可查看供應商資料");
        register("SUPPLIERS_EDIT", "供應商編輯", "採購管理", "可新增、編輯、停用供應商");
        register("PROCUREMENT_VIEW", "採購查看", "採購管理", "可查看採購單、收貨與補貨建議");
        register("PROCUREMENT_EDIT", "採購編輯", "採購管理", "可建立採購單與執行收貨入庫");

        register("ORDERS_VIEW", "訂單查看", "訂單管理", "可查看訂單、付款與退款資料");
        register("ORDERS_REFUND", "訂單退款", "訂單管理", "可執行退款與回補庫存相關操作");

        register("SHIFTS_VIEW", "班次查看", "班次管理", "可查看開班、交班與班次紀錄");
        register("SHIFTS_EDIT", "班次操作", "班次管理", "可執行開班與交班");

        register("DEVICES_VIEW", "裝置查看", "裝置管理", "可查看 POS 裝置與連線狀態");

        register("USERS_VIEW", "使用者查看", "帳號權限", "可查看使用者清單與門市角色指派");
        register("USERS_EDIT", "使用者編輯", "帳號權限", "可新增、編輯、停用使用者與重設 PIN");
        register("ROLES_VIEW", "角色查看", "帳號權限", "可查看角色與權限矩陣");
        register("ROLES_EDIT", "角色編輯", "帳號權限", "可新增、編輯角色與調整權限矩陣");

        register("SETTINGS_VIEW", "系統設定查看", "系統設定", "可查看系統設定");
        register("SETTINGS_EDIT", "系統設定編輯", "系統設定", "可修改系統設定");
    }

    private PermissionCatalog() {
    }

    private static void register(String key, String label, String groupName, String description) {
        DEFINITIONS.put(key, new PermissionDefinitionResponse(key, label, groupName, description));
    }

    public static List<PermissionDefinitionResponse> list() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static Set<String> keys() {
        return DEFINITIONS.keySet();
    }

    public static void validateKeys(List<String> permissionKeys) {
        List<String> invalidKeys = permissionKeys.stream()
                .filter(key -> !DEFINITIONS.containsKey(key))
                .toList();

        if (!invalidKeys.isEmpty()) {
            throw new BadRequestException("Unknown permission keys: " + String.join(", ", invalidKeys));
        }
    }
}
