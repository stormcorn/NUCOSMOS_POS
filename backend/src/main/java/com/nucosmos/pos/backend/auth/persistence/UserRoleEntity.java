package com.nucosmos.pos.backend.auth.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    public UserEntity getUser() {
        return user;
    }

    public RoleEntity getRole() {
        return role;
    }
}
