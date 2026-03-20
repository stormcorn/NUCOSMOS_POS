package com.nucosmos.pos.backend.device.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "devices")
public class DeviceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(nullable = false, unique = true, length = 50)
    private String deviceCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    private String platform;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private OffsetDateTime lastSeenAt;

    public StoreEntity getStore() {
        return store;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getName() {
        return name;
    }

    public String getPlatform() {
        return platform;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }
}
