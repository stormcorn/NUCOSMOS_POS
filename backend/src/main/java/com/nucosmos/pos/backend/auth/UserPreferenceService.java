package com.nucosmos.pos.backend.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.persistence.UserPreferenceEntity;
import com.nucosmos.pos.backend.auth.repository.UserPreferenceRepository;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserPreferenceService {

    private static final String NAVIGATION_PREFERENCE_KEY = "admin.navigation.order";

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ObjectMapper objectMapper;

    public UserPreferenceService(
            UserRepository userRepository,
            UserPreferenceRepository userPreferenceRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public NavigationPreferenceResponse getNavigationPreference(AuthenticatedUser authenticatedUser) {
        return userPreferenceRepository
                .findByUser_IdAndPreferenceKey(authenticatedUser.userId(), NAVIGATION_PREFERENCE_KEY)
                .map(this::toResponse)
                .orElseGet(() -> new NavigationPreferenceResponse(List.of(), Map.of(), null));
    }

    @Transactional
    public NavigationPreferenceResponse saveNavigationPreference(
            AuthenticatedUser authenticatedUser,
            NavigationPreferenceRequest request
    ) {
        UserEntity user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        StoredNavigationPreference sanitized = sanitize(request);
        String serialized = serialize(sanitized);

        UserPreferenceEntity preference = userPreferenceRepository
                .findByUser_IdAndPreferenceKey(authenticatedUser.userId(), NAVIGATION_PREFERENCE_KEY)
                .map(existing -> {
                    existing.updateValue(serialized);
                    return existing;
                })
                .orElseGet(() -> UserPreferenceEntity.create(user, NAVIGATION_PREFERENCE_KEY, serialized));

        UserPreferenceEntity saved = userPreferenceRepository.save(preference);
        return toResponse(saved);
    }

    private NavigationPreferenceResponse toResponse(UserPreferenceEntity entity) {
        StoredNavigationPreference stored = deserialize(entity.getPreferenceValue());
        return new NavigationPreferenceResponse(
                stored.rootOrder(),
                stored.childOrders(),
                entity.getUpdatedAt()
        );
    }

    private StoredNavigationPreference sanitize(NavigationPreferenceRequest request) {
        List<String> rootOrder = request.rootOrder() == null
                ? List.of()
                : request.rootOrder().stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, List<String>> childOrders = new LinkedHashMap<>();
        if (request.childOrders() != null) {
            request.childOrders().forEach((key, value) -> {
                if (!StringUtils.hasText(key)) {
                    return;
                }

                List<String> sanitizedChildren = value == null
                        ? List.of()
                        : value.stream()
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();

                childOrders.put(key, sanitizedChildren);
            });
        }

        return new StoredNavigationPreference(rootOrder, childOrders);
    }

    private String serialize(StoredNavigationPreference preference) {
        try {
            return objectMapper.writeValueAsString(preference);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Failed to save navigation preference");
        }
    }

    private StoredNavigationPreference deserialize(String rawValue) {
        try {
            return objectMapper.readValue(rawValue, StoredNavigationPreference.class);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Failed to read navigation preference");
        }
    }

    private record StoredNavigationPreference(
            List<String> rootOrder,
            Map<String, List<String>> childOrders
    ) {
    }
}
