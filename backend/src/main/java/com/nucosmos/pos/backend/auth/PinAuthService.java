package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.auth.persistence.RoleEntity;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.RolePermissionRepository;
import com.nucosmos.pos.backend.auth.repository.RoleRepository;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.UnauthorizedException;
import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import com.nucosmos.pos.backend.device.repository.DeviceRepository;
import com.nucosmos.pos.backend.store.StoreSummaryResponse;
import com.nucosmos.pos.backend.store.StoreReceiptSettingsRequest;
import com.nucosmos.pos.backend.store.StoreReceiptSettingsResponse;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PinAuthService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "MANAGER", "CASHIER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final DeviceRepository deviceRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PinAuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StoreRepository storeRepository,
            DeviceRepository deviceRepository,
            RolePermissionRepository rolePermissionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.storeRepository = storeRepository;
        this.deviceRepository = deviceRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public List<StoreSummaryResponse> listAvailableStores() {
        return storeRepository.findAllByStatusOrderByCodeAsc(ACTIVE_STATUS)
                .stream()
                .map(store -> new StoreSummaryResponse(
                        store.getId(),
                        store.getCode(),
                        store.getName(),
                        store.getTimezone(),
                        store.getCurrencyCode(),
                        store.getStatus(),
                        defaultString(store.getReceiptFooterText())
                ))
                .toList();
    }

    @Transactional
    public PinLoginResponse login(PinLoginRequest request) {
        StoreEntity store = storeRepository.findByCodeAndStatus(request.storeCode(), ACTIVE_STATUS)
                .orElseThrow(() -> new UnauthorizedException("Invalid store or PIN"));

        UserEntity matchedUser = resolveMatchedUser(request);
        matchedUser.markLoggedIn();

        List<String> roleCodes = roleRepository.findActiveRoleCodesByUserId(matchedUser.getId());
        RoleEntity activeRole = resolveActiveRole(request.roleCode(), roleCodes);
        DeviceEntity device = resolveDevice(store, request);
        List<String> permissionKeys = rolePermissionRepository.findPermissionKeysByRoleCode(activeRole.getCode());

        Map<String, Object> claims = new HashMap<>();
        claims.put("employeeCode", matchedUser.getEmployeeCode());
        claims.put("displayName", matchedUser.getDisplayName());
        claims.put("storeCode", store.getCode());
        claims.put("roleCodes", roleCodes);
        claims.put("activeRole", activeRole.getCode());
        claims.put("permissionKeys", permissionKeys);
        if (device != null) {
            claims.put("deviceCode", device.getDeviceCode());
        }

        return new PinLoginResponse(
                "Bearer",
                jwtService.generateToken(matchedUser.getId().toString(), claims),
                jwtService.calculateExpiresAt(),
                device == null ? "" : device.getDeviceCode(),
                new AuthStoreResponse(store.getCode(), store.getName(), defaultString(store.getReceiptFooterText())),
                new AuthStaffResponse(
                        matchedUser.getId(),
                        matchedUser.getEmployeeCode(),
                        matchedUser.getDisplayName(),
                        roleCodes,
                        activeRole.getCode(),
                        permissionKeys
                )
        );
    }

    public CurrentSessionResponse currentSession(AuthenticatedUser user) {
        return new CurrentSessionResponse(
                user.userId(),
                user.employeeCode(),
                user.displayName(),
                user.storeCode(),
                user.activeRole(),
                user.roleCodes(),
                user.permissionKeys(),
                user.deviceCode()
        );
    }

    @Transactional(readOnly = true)
    public StoreReceiptSettingsResponse currentStoreReceiptSettings(AuthenticatedUser user) {
        StoreEntity store = storeRepository.findByCodeAndStatus(user.storeCode(), ACTIVE_STATUS)
                .orElseThrow(() -> new UnauthorizedException("Store is not available"));
        return new StoreReceiptSettingsResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                defaultString(store.getReceiptFooterText())
        );
    }

    @Transactional
    public StoreReceiptSettingsResponse updateCurrentStoreReceiptSettings(
            AuthenticatedUser user,
            StoreReceiptSettingsRequest request
    ) {
        StoreEntity store = storeRepository.findByCodeAndStatus(user.storeCode(), ACTIVE_STATUS)
                .orElseThrow(() -> new UnauthorizedException("Store is not available"));
        store.setReceiptFooterText(normalizeReceiptFooterText(request.receiptFooterText()));
        return new StoreReceiptSettingsResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                defaultString(store.getReceiptFooterText())
        );
    }

    private UserEntity resolveMatchedUser(PinLoginRequest request) {
        List<UserEntity> candidates = StringUtils.hasText(request.roleCode())
                ? userRepository.findCandidatesForPinLogin(request.storeCode(), request.roleCode().trim().toUpperCase())
                : userRepository.findCandidatesForPinLogin(request.storeCode());

        List<UserEntity> matches = candidates.stream()
                .filter(user -> passwordEncoder.matches(request.pin(), user.getPinHash()))
                .toList();

        if (matches.isEmpty()) {
            throw new UnauthorizedException("Invalid store or PIN");
        }

        if (matches.size() > 1) {
            throw new UnauthorizedException("PIN matches multiple staff accounts; please contact an administrator");
        }

        return matches.get(0);
    }

    private RoleEntity resolveActiveRole(String requestedRoleCode, List<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            throw new UnauthorizedException("No active role is assigned to this account");
        }

        String selectedRoleCode = StringUtils.hasText(requestedRoleCode)
                ? requestedRoleCode.trim().toUpperCase()
                : roleCodes.stream()
                        .sorted(Comparator.comparingInt(this::rolePriority))
                        .findFirst()
                        .orElseThrow(() -> new UnauthorizedException("No active role is assigned to this account"));

        if (!roleCodes.contains(selectedRoleCode)) {
            throw new UnauthorizedException("Invalid store or PIN");
        }

        return roleRepository.findByCodeAndActiveTrue(selectedRoleCode)
                .orElseThrow(() -> new UnauthorizedException("No active role is assigned to this account"));
    }

    private int rolePriority(String roleCode) {
        int index = ROLE_PRIORITY.indexOf(roleCode);
        return index >= 0 ? index : ROLE_PRIORITY.size();
    }

    private DeviceEntity resolveDevice(StoreEntity store, PinLoginRequest request) {
        if (!StringUtils.hasText(request.deviceCode())) {
            return null;
        }

        String deviceCode = request.deviceCode().trim();
        OffsetDateTime now = OffsetDateTime.now();

        DeviceEntity device = deviceRepository
                .findByStore_CodeAndDeviceCode(store.getCode(), deviceCode)
                .orElseGet(() -> {
                    DeviceEntity created = new DeviceEntity();
                    created.setStore(store);
                    created.setDeviceCode(deviceCode);
                    created.setName(resolveDeviceName(request, deviceCode));
                    created.setPlatform(resolveDevicePlatform(request));
                    created.setStatus(ACTIVE_STATUS);
                    created.setLastSeenAt(now);
                    return deviceRepository.save(created);
                });

        device.setName(resolveDeviceName(request, deviceCode));
        device.setPlatform(resolveDevicePlatform(request));
        device.setStatus(ACTIVE_STATUS);
        device.markHeartbeat(now);
        return device;
    }

    private String resolveDeviceName(PinLoginRequest request, String deviceCode) {
        if (StringUtils.hasText(request.deviceName())) {
            return request.deviceName().trim();
        }

        String suffix = deviceCode.length() <= 12
                ? deviceCode
                : deviceCode.substring(deviceCode.length() - 12);
        return "POS Device " + suffix;
    }

    private String resolveDevicePlatform(PinLoginRequest request) {
        if (StringUtils.hasText(request.devicePlatform())) {
            return request.devicePlatform().trim().toUpperCase();
        }
        return "ANDROID";
    }

    private String normalizeReceiptFooterText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace("\r\n", "\n").trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
