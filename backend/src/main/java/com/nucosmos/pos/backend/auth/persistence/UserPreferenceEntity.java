package com.nucosmos.pos.backend.auth.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferenceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 100)
    private String preferenceKey;

    @Column(nullable = false, columnDefinition = "text")
    private String preferenceValue;

    public static UserPreferenceEntity create(UserEntity user, String preferenceKey, String preferenceValue) {
        UserPreferenceEntity entity = new UserPreferenceEntity();
        entity.user = user;
        entity.preferenceKey = preferenceKey;
        entity.preferenceValue = preferenceValue;
        return entity;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getPreferenceKey() {
        return preferenceKey;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }

    public void updateValue(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }
}
