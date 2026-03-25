package com.nucosmos.pos.backend.supply.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLineEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @Column(nullable = false, length = 20)
    private String itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_item_id")
    private MaterialItemEntity materialItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_item_id")
    private PackagingItemEntity packagingItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufactured_item_id")
    private ManufacturedItemEntity manufacturedItem;

    @Column(nullable = false, length = 50)
    private String itemSku;

    @Column(nullable = false, length = 120)
    private String itemName;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false)
    private int orderedQuantity;

    @Column(nullable = false)
    private int receivedQuantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(length = 80)
    private String batchCode;

    @Column
    private OffsetDateTime expiryDate;

    @Column
    private OffsetDateTime manufacturedAt;

    @Column(length = 255)
    private String note;

    protected PurchaseOrderLineEntity() {
    }

    public static PurchaseOrderLineEntity createForMaterial(
            PurchaseOrderEntity purchaseOrder,
            MaterialItemEntity materialItem,
            int orderedQuantity,
            BigDecimal unitCost,
            String batchCode,
            OffsetDateTime expiryDate,
            OffsetDateTime manufacturedAt,
            String note
    ) {
        PurchaseOrderLineEntity entity = new PurchaseOrderLineEntity();
        entity.purchaseOrder = purchaseOrder;
        entity.itemType = "MATERIAL";
        entity.materialItem = materialItem;
        entity.itemSku = materialItem.getSku();
        entity.itemName = materialItem.getName();
        entity.unit = materialItem.getPurchaseUnit();
        entity.orderedQuantity = orderedQuantity;
        entity.receivedQuantity = 0;
        entity.unitCost = unitCost;
        entity.batchCode = normalize(batchCode);
        entity.expiryDate = expiryDate;
        entity.manufacturedAt = manufacturedAt;
        entity.note = normalize(note);
        return entity;
    }

    public static PurchaseOrderLineEntity createForPackaging(
            PurchaseOrderEntity purchaseOrder,
            PackagingItemEntity packagingItem,
            int orderedQuantity,
            BigDecimal unitCost,
            String batchCode,
            OffsetDateTime expiryDate,
            OffsetDateTime manufacturedAt,
            String note
    ) {
        PurchaseOrderLineEntity entity = new PurchaseOrderLineEntity();
        entity.purchaseOrder = purchaseOrder;
        entity.itemType = "PACKAGING";
        entity.packagingItem = packagingItem;
        entity.itemSku = packagingItem.getSku();
        entity.itemName = packagingItem.getName();
        entity.unit = packagingItem.getPurchaseUnit();
        entity.orderedQuantity = orderedQuantity;
        entity.receivedQuantity = 0;
        entity.unitCost = unitCost;
        entity.batchCode = normalize(batchCode);
        entity.expiryDate = expiryDate;
        entity.manufacturedAt = manufacturedAt;
        entity.note = normalize(note);
        return entity;
    }

    public static PurchaseOrderLineEntity createForManufactured(
            PurchaseOrderEntity purchaseOrder,
            ManufacturedItemEntity manufacturedItem,
            int orderedQuantity,
            BigDecimal unitCost,
            String batchCode,
            OffsetDateTime expiryDate,
            OffsetDateTime manufacturedAt,
            String note
    ) {
        PurchaseOrderLineEntity entity = new PurchaseOrderLineEntity();
        entity.purchaseOrder = purchaseOrder;
        entity.itemType = "MANUFACTURED";
        entity.manufacturedItem = manufacturedItem;
        entity.itemSku = manufacturedItem.getSku();
        entity.itemName = manufacturedItem.getName();
        entity.unit = manufacturedItem.getPurchaseUnit();
        entity.orderedQuantity = orderedQuantity;
        entity.receivedQuantity = 0;
        entity.unitCost = unitCost;
        entity.batchCode = normalize(batchCode);
        entity.expiryDate = expiryDate;
        entity.manufacturedAt = manufacturedAt;
        entity.note = normalize(note);
        return entity;
    }

    public void markReceived() {
        this.receivedQuantity = orderedQuantity;
    }

    public PurchaseOrderEntity getPurchaseOrder() {
        return purchaseOrder;
    }

    public String getItemType() {
        return itemType;
    }

    public MaterialItemEntity getMaterialItem() {
        return materialItem;
    }

    public PackagingItemEntity getPackagingItem() {
        return packagingItem;
    }

    public ManufacturedItemEntity getManufacturedItem() {
        return manufacturedItem;
    }

    public String getItemSku() {
        return itemSku;
    }

    public String getItemName() {
        return itemName;
    }

    public String getUnit() {
        return unit;
    }

    public int getOrderedQuantity() {
        return orderedQuantity;
    }

    public int getReceivedQuantity() {
        return receivedQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public OffsetDateTime getExpiryDate() {
        return expiryDate;
    }

    public OffsetDateTime getManufacturedAt() {
        return manufacturedAt;
    }

    public String getNote() {
        return note;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
