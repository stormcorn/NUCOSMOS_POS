package com.nucosmos.pos.backend.supply.persistence;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrderEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdByUser;

    @Column(nullable = false, unique = true, length = 60)
    private String orderNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String note;

    @Column
    private OffsetDateTime expectedAt;

    @Column
    private OffsetDateTime receivedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<PurchaseOrderLineEntity> lines = new ArrayList<>();

    protected PurchaseOrderEntity() {
    }

    public static PurchaseOrderEntity create(
            StoreEntity store,
            SupplierEntity supplier,
            UserEntity createdByUser,
            String orderNumber,
            String note,
            OffsetDateTime expectedAt
    ) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.store = store;
        entity.supplier = supplier;
        entity.createdByUser = createdByUser;
        entity.orderNumber = orderNumber;
        entity.status = "OPEN";
        entity.note = normalize(note);
        entity.expectedAt = expectedAt;
        return entity;
    }

    public void addLine(PurchaseOrderLineEntity line) {
        lines.add(line);
    }

    public void receive(String receiveNote, OffsetDateTime receivedAt) {
        this.status = "RECEIVED";
        this.receivedAt = receivedAt;
        if (receiveNote != null && !receiveNote.isBlank()) {
            this.note = normalize(receiveNote);
        }
    }

    public StoreEntity getStore() {
        return store;
    }

    public SupplierEntity getSupplier() {
        return supplier;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getExpectedAt() {
        return expectedAt;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public List<PurchaseOrderLineEntity> getLines() {
        return lines;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
