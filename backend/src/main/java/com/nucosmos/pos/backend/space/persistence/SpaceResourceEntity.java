package com.nucosmos.pos.backend.space.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "space_resources")
public class SpaceResourceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 160)
    private String locationLabel;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private boolean active;

    @OneToOne(mappedBy = "spaceResource", fetch = FetchType.LAZY)
    private SpaceBookingPolicyEntity bookingPolicy;

    protected SpaceResourceEntity() {
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

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return active;
    }

    public SpaceBookingPolicyEntity getBookingPolicy() {
        return bookingPolicy;
    }
}
