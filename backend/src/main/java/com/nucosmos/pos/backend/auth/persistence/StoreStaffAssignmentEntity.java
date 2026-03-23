package com.nucosmos.pos.backend.auth.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_staff_assignments")
public class StoreStaffAssignmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private boolean active;

    public StoreEntity getStore() {
        return store;
    }

    public UserEntity getUser() {
        return user;
    }

    public boolean isActive() {
        return active;
    }

    public void assign(StoreEntity store, UserEntity user, boolean active) {
        this.store = store;
        this.user = user;
        this.active = active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
