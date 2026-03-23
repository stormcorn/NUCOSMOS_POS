package com.nucosmos.pos.backend.auth.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(nullable = false, length = 100)
    private String permissionKey;

    public RoleEntity getRole() {
        return role;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public void assign(RoleEntity role, String permissionKey) {
        this.role = role;
        this.permissionKey = permissionKey;
    }
}
