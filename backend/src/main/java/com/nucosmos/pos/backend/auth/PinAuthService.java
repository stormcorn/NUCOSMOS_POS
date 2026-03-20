package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.auth.persistence.RoleEntity;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.RoleRepository;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.UnauthorizedException;
import com.nucosmos.pos.backend.device.repository.DeviceRepository;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PinAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PinAuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StoreRepository storeRepository,
            DeviceRepository deviceRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.storeRepository = storeRepository;
        this.deviceRepository = deviceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public PinLoginResponse login(PinLoginRequest request) {
        StoreEntity store = storeRepository.findByCodeAndStatus(request.storeCode(), "ACTIVE")
                .orElseThrow(() -> new UnauthorizedException("Invalid store, role, or PIN"));

        RoleEntity selectedRole = roleRepository.findByCodeAndActiveTrue(request.roleCode())
                .orElseThrow(() -> new UnauthorizedException("Invalid store, role, or PIN"));

        validateDevice(request);

        UserEntity matchedUser = userRepository.findCandidatesForPinLogin(request.storeCode(), request.roleCode())
                .stream()
                .filter(user -> passwordEncoder.matches(request.pin(), user.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Invalid store, role, or PIN"));

        matchedUser.markLoggedIn();

        List<String> roleCodes = roleRepository.findActiveRoleCodesByUserId(matchedUser.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("employeeCode", matchedUser.getEmployeeCode());
        claims.put("displayName", matchedUser.getDisplayName());
        claims.put("storeCode", store.getCode());
        claims.put("roleCodes", roleCodes);
        claims.put("activeRole", selectedRole.getCode());
        if (StringUtils.hasText(request.deviceCode())) {
            claims.put("deviceCode", request.deviceCode());
        }

        return new PinLoginResponse(
                "Bearer",
                jwtService.generateToken(matchedUser.getId().toString(), claims),
                jwtService.calculateExpiresAt(),
                request.deviceCode(),
                new AuthStoreResponse(store.getCode(), store.getName()),
                new AuthStaffResponse(
                        matchedUser.getId(),
                        matchedUser.getEmployeeCode(),
                        matchedUser.getDisplayName(),
                        roleCodes,
                        selectedRole.getCode()
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
                user.deviceCode()
        );
    }

    private void validateDevice(PinLoginRequest request) {
        if (!StringUtils.hasText(request.deviceCode())) {
            return;
        }

        deviceRepository.findByStore_CodeAndDeviceCodeAndStatus(request.storeCode(), request.deviceCode(), "ACTIVE")
                .orElseThrow(() -> new UnauthorizedException("Invalid store, role, or PIN"));
    }
}
