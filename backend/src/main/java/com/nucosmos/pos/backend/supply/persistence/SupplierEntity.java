package com.nucosmos.pos.backend.supply.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class SupplierEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String contactName;

    @Column(length = 50)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private boolean active;

    protected SupplierEntity() {
    }

    public static SupplierEntity create(
            StoreEntity store,
            String code,
            String name,
            String contactName,
            String phone,
            String email,
            String note
    ) {
        SupplierEntity entity = new SupplierEntity();
        entity.store = store;
        entity.code = code;
        entity.name = name;
        entity.contactName = normalize(contactName);
        entity.phone = normalize(phone);
        entity.email = normalize(email);
        entity.note = normalize(note);
        entity.active = true;
        return entity;
    }

    public void update(
            String code,
            String name,
            String contactName,
            String phone,
            String email,
            String note
    ) {
        this.code = code;
        this.name = name;
        this.contactName = normalize(contactName);
        this.phone = normalize(phone);
        this.email = normalize(email);
        this.note = normalize(note);
    }

    public void deactivate() {
        this.active = false;
    }

    public StoreEntity getStore() {
        return store;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getNote() {
        return note;
    }

    public boolean isActive() {
        return active;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
