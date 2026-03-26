package com.nucosmos.pos.backend.auth.admin;

import com.nucosmos.pos.backend.auth.persistence.RoleEntity;
import com.nucosmos.pos.backend.auth.persistence.RolePermissionEntity;
import com.nucosmos.pos.backend.auth.persistence.StoreStaffAssignmentEntity;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.persistence.UserRoleEntity;
import com.nucosmos.pos.backend.auth.repository.RolePermissionRepository;
import com.nucosmos.pos.backend.auth.repository.RoleRepository;
import com.nucosmos.pos.backend.auth.repository.StoreStaffAssignmentRepository;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.auth.repository.UserRoleRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AccessAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffAssignmentRepository storeStaffAssignmentRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;

    public AccessAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            StoreRepository storeRepository,
            StoreStaffAssignmentRepository storeStaffAssignmentRepository,
            RolePermissionRepository rolePermissionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.storeRepository = storeRepository;
        this.storeStaffAssignmentRepository = storeStaffAssignmentRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> listUsers(String status, String storeCode) {
        List<UserEntity> users = userRepository.findAllByOrderByEmployeeCodeAsc();
        if (users.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<UserRoleEntity>> roleMap = userRoleRepository.findAllByUserIdIn(users.stream().map(UserEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(entry -> entry.getUser().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
        Map<UUID, List<StoreStaffAssignmentEntity>> assignmentMap = storeStaffAssignmentRepository.findAllByUserIdIn(users.stream().map(UserEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(entry -> entry.getUser().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));

        return users.stream()
                .map(user -> toUserResponse(user, roleMap.getOrDefault(user.getId(), List.of()), assignmentMap.getOrDefault(user.getId(), List.of())))
                .filter(response -> !StringUtils.hasText(status) || response.status().equalsIgnoreCase(status))
                .filter(response -> !StringUtils.hasText(storeCode) || response.storeCodes().contains(storeCode))
                .toList();
    }

    @Transactional
    public UserAdminResponse createUser(UserAdminRequest request) {
        String pin = normalizePin(request.pin(), true);
        if (userRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new BadRequestException("Employee code already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmployeeCode(request.employeeCode().trim().toUpperCase());
        user.setDisplayName(request.displayName().trim());
        user.setStatus(request.status().trim().toUpperCase());
        user.setPinHash(passwordEncoder.encode(pin));
        userRepository.save(user);
        syncUserAssignments(user, request);
        return loadUserResponse(user.getId());
    }

    @Transactional
    public UserAdminResponse updateUser(UUID userId, UserAdminRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (userRepository.existsByEmployeeCodeAndIdNot(request.employeeCode(), userId)) {
            throw new BadRequestException("Employee code already exists");
        }

        user.setEmployeeCode(request.employeeCode().trim().toUpperCase());
        user.setDisplayName(request.displayName().trim());
        user.setStatus(request.status().trim().toUpperCase());
        String pin = normalizePin(request.pin(), false);
        if (pin != null) {
            user.setPinHash(passwordEncoder.encode(pin));
        }
        userRepository.save(user);
        syncUserAssignments(user, request);
        return loadUserResponse(userId);
    }

    @Transactional(readOnly = true)
    public List<RoleAdminResponse> listRoles() {
        List<RoleEntity> roles = roleRepository.findAllByOrderByCodeAsc();
        if (roles.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<RolePermissionEntity>> permissionsByRole = rolePermissionRepository.findAllByRoleIdIn(roles.stream().map(RoleEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(entry -> entry.getRole().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));

        return roles.stream()
                .map(role -> toRoleResponse(role, permissionsByRole.getOrDefault(role.getId(), List.of())))
                .toList();
    }

    @Transactional
    public RoleAdminResponse createRole(RoleAdminRequest request) {
        if (roleRepository.existsByCode(request.code().trim().toUpperCase())) {
            throw new BadRequestException("Role code already exists");
        }

        RoleEntity role = new RoleEntity();
        applyRole(role, request);
        roleRepository.save(role);
        replaceRolePermissions(role, request.permissionKeys());
        return loadRoleResponse(role.getId());
    }

    @Transactional
    public RoleAdminResponse updateRole(UUID roleId, RoleAdminRequest request) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BadRequestException("Role not found"));
        if (roleRepository.existsByCodeAndIdNot(request.code().trim().toUpperCase(), roleId)) {
            throw new BadRequestException("Role code already exists");
        }

        applyRole(role, request);
        roleRepository.save(role);
        replaceRolePermissions(role, request.permissionKeys());
        return loadRoleResponse(roleId);
    }

    public List<PermissionDefinitionResponse> listPermissionCatalog() {
        return PermissionCatalog.list();
    }

    private void syncUserAssignments(UserEntity user, UserAdminRequest request) {
        List<RoleEntity> roles = resolveRoles(request.roleCodes());
        List<StoreEntity> stores = resolveStores(request.storeCodes());

        userRoleRepository.deleteAllByUserId(user.getId());
        userRoleRepository.flush();
        storeStaffAssignmentRepository.deleteAllByUserId(user.getId());
        storeStaffAssignmentRepository.flush();

        List<UserRoleEntity> userRoles = new ArrayList<>();
        for (RoleEntity role : roles) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.assign(user, role);
            userRoles.add(userRole);
        }
        userRoleRepository.saveAll(userRoles);

        List<StoreStaffAssignmentEntity> assignments = new ArrayList<>();
        for (StoreEntity store : stores) {
            StoreStaffAssignmentEntity assignment = new StoreStaffAssignmentEntity();
            assignment.assign(store, user, true);
            assignments.add(assignment);
        }
        storeStaffAssignmentRepository.saveAll(assignments);
    }

    private void applyRole(RoleEntity role, RoleAdminRequest request) {
        role.setCode(request.code().trim().toUpperCase());
        role.setName(request.name().trim());
        role.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        role.setActive(Boolean.TRUE.equals(request.active()));
    }

    private List<RoleEntity> resolveRoles(List<String> roleCodes) {
        List<String> normalizedCodes = roleCodes.stream()
                .map(code -> code.trim().toUpperCase())
                .distinct()
                .toList();

        List<RoleEntity> roles = roleRepository.findAllByCodeInAndActiveTrue(normalizedCodes);
        if (roles.size() != normalizedCodes.size()) {
            Set<String> foundCodes = roles.stream().map(RoleEntity::getCode).collect(java.util.stream.Collectors.toSet());
            List<String> missingCodes = normalizedCodes.stream().filter(code -> !foundCodes.contains(code)).toList();
            throw new BadRequestException("Unknown active role codes: " + String.join(", ", missingCodes));
        }
        return roles;
    }

    private List<StoreEntity> resolveStores(List<String> storeCodes) {
        List<String> normalizedCodes = storeCodes.stream()
                .map(String::trim)
                .distinct()
                .toList();

        List<StoreEntity> stores = storeRepository.findAllByCodeIn(normalizedCodes);
        if (stores.size() != normalizedCodes.size()) {
            Set<String> foundCodes = stores.stream().map(StoreEntity::getCode).collect(java.util.stream.Collectors.toSet());
            List<String> missingCodes = normalizedCodes.stream().filter(code -> !foundCodes.contains(code)).toList();
            throw new BadRequestException("Unknown store codes: " + String.join(", ", missingCodes));
        }
        return stores;
    }

    private void replaceRolePermissions(RoleEntity role, List<String> permissionKeys) {
        List<String> normalizedKeys = (permissionKeys == null ? List.<String>of() : permissionKeys).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .distinct()
                .toList();
        PermissionCatalog.validateKeys(normalizedKeys);

        rolePermissionRepository.deleteAllByRoleId(role.getId());
        rolePermissionRepository.flush();
        if (normalizedKeys.isEmpty()) {
            return;
        }

        List<RolePermissionEntity> permissions = new ArrayList<>();
        for (String key : normalizedKeys) {
            RolePermissionEntity permission = new RolePermissionEntity();
            permission.assign(role, key);
            permissions.add(permission);
        }
        rolePermissionRepository.saveAll(permissions);
    }

    private UserAdminResponse loadUserResponse(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        List<UserRoleEntity> roles = userRoleRepository.findAllByUserIdIn(List.of(userId));
        List<StoreStaffAssignmentEntity> assignments = storeStaffAssignmentRepository.findAllByUserIdIn(List.of(userId));
        return toUserResponse(user, roles, assignments);
    }

    private RoleAdminResponse loadRoleResponse(UUID roleId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BadRequestException("Role not found"));
        List<RolePermissionEntity> permissions = rolePermissionRepository.findAllByRoleIdIn(List.of(roleId));
        return toRoleResponse(role, permissions);
    }

    private UserAdminResponse toUserResponse(
            UserEntity user,
            List<UserRoleEntity> roles,
            List<StoreStaffAssignmentEntity> assignments
    ) {
        List<String> roleCodes = roles.stream()
                .map(userRole -> userRole.getRole().getCode())
                .distinct()
                .sorted()
                .toList();

        List<StoreStaffAssignmentEntity> activeAssignments = assignments.stream()
                .filter(StoreStaffAssignmentEntity::isActive)
                .sorted(Comparator.comparing(entry -> entry.getStore().getCode()))
                .toList();

        return new UserAdminResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getDisplayName(),
                user.getStatus(),
                user.getLastLoginAt(),
                roleCodes,
                activeAssignments.stream().map(entry -> entry.getStore().getCode()).toList(),
                activeAssignments.stream().map(entry -> entry.getStore().getName()).toList()
        );
    }

    private RoleAdminResponse toRoleResponse(RoleEntity role, List<RolePermissionEntity> permissions) {
        List<String> permissionKeys = permissions.stream()
                .map(RolePermissionEntity::getPermissionKey)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));

        permissionKeys.sort(String::compareTo);

        return new RoleAdminResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                permissionKeys
        );
    }

    private String normalizePin(String pin, boolean required) {
        if (!StringUtils.hasText(pin)) {
            if (required) {
                throw new BadRequestException("PIN is required when creating a user");
            }
            return null;
        }

        String normalizedPin = pin.trim();
        if (!normalizedPin.matches("\\d{6}")) {
            throw new BadRequestException("PIN must contain exactly 6 digits");
        }
        return normalizedPin;
    }
}
