package com.nucosmos.pos.backend.space.persistence;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "space_blockouts")
public class SpaceBlockoutEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_resource_id", nullable = false)
    private SpaceResourceEntity spaceResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private UserEntity createdByUser;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    protected SpaceBlockoutEntity() {
    }

    public static SpaceBlockoutEntity create(
            SpaceResourceEntity spaceResource,
            UserEntity createdByUser,
            String title,
            String reason,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        SpaceBlockoutEntity entity = new SpaceBlockoutEntity();
        entity.spaceResource = spaceResource;
        entity.createdByUser = createdByUser;
        entity.title = title;
        entity.reason = reason;
        entity.startAt = startAt;
        entity.endAt = endAt;
        return entity;
    }

    public SpaceResourceEntity getSpaceResource() {
        return spaceResource;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public String getTitle() {
        return title;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }
}
