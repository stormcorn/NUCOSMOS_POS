package com.nucosmos.pos.backend.supply;

public enum SupplyItemType {
    MATERIAL,
    MANUFACTURED,
    PACKAGING;

    public static SupplyItemType from(String value) {
        return SupplyItemType.valueOf(value.trim().toUpperCase());
    }
}
