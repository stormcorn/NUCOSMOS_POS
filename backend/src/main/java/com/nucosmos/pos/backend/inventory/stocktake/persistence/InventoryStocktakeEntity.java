package com.nucosmos.pos.backend.inventory.stocktake.persistence;

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
@Table(name = "inventory_stocktakes")
public class InventoryStocktakeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdByUser;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private OffsetDateTime countedAt;

    @Column(nullable = false)
    private OffsetDateTime postedAt;

    @OneToMany(mappedBy = "stocktake", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<InventoryStocktakeItemEntity> items = new ArrayList<>();

    protected InventoryStocktakeEntity() {
    }

    public static InventoryStocktakeEntity create(
            StoreEntity store,
            UserEntity createdByUser,
            String note,
            OffsetDateTime countedAt,
            OffsetDateTime postedAt
    ) {
        InventoryStocktakeEntity entity = new InventoryStocktakeEntity();
        entity.store = store;
        entity.createdByUser = createdByUser;
        entity.status = "POSTED";
        entity.note = note;
        entity.countedAt = countedAt;
        entity.postedAt = postedAt;
        return entity;
    }

    public void addItem(InventoryStocktakeItemEntity item) {
        items.add(item);
    }

    public StoreEntity getStore() {
        return store;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getCountedAt() {
        return countedAt;
    }

    public OffsetDateTime getPostedAt() {
        return postedAt;
    }

    public List<InventoryStocktakeItemEntity> getItems() {
        return items;
    }
}
