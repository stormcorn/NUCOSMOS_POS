package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.auth.persistence.PhoneRegistrationRequestEntity;
import com.nucosmos.pos.backend.auth.persistence.RoleEntity;
import com.nucosmos.pos.backend.auth.persistence.StoreStaffAssignmentEntity;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.persistence.UserRoleEntity;
import com.nucosmos.pos.backend.auth.repository.PhoneRegistrationRequestRepository;
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

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PhoneRegistrationService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String PENDING_STATUS = "PENDING_VERIFICATION";
    private static final String VERIFIED_STATUS = "VERIFIED";
    private static final String FIREBASE_PROVIDER = "FIREBASE_SMS";
    private static final String DEFAULT_ROLE_CODE = "CASHIER";

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final StoreStaffAssignmentRepository storeStaffAssignmentRepository;
    private final PhoneRegistrationRequestRepository phoneRegistrationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationService phoneVerificationService;

    public PhoneRegistrationService(
            StoreRepository storeRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            StoreStaffAssignmentRepository storeStaffAssignmentRepository,
            PhoneRegistrationRequestRepository phoneRegistrationRequestRepository,
            PasswordEncoder passwordEncoder,
            PhoneVerificationService phoneVerificationService
    ) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.storeStaffAssignmentRepository = storeStaffAssignmentRepository;
        this.phoneRegistrationRequestRepository = phoneRegistrationRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneVerificationService = phoneVerificationService;
    }

    @Transactional
    public RegistrationStartResponse startRegistration(RegistrationStartRequest request) {
        StoreEntity store = storeRepository.findByCodeAndStatus(request.storeCode().trim(), ACTIVE_STATUS)
                .orElseThrow(() -> new BadRequestException("Store not found"));
        String normalizedPhoneNumber = normalizePhoneNumber(request.phoneNumber());

        if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
            throw new BadRequestException("This phone number is already registered");
        }

        if (phoneRegistrationRequestRepository.existsByPhoneNumberAndStatusIn(
                normalizedPhoneNumber,
                List.of(PENDING_STATUS, VERIFIED_STATUS)
        )) {
            throw new BadRequestException("A registration request is already in progress for this phone number");
        }

        PhoneRegistrationRequestEntity registration = new PhoneRegistrationRequestEntity();
        registration.setStore(store);
        registration.setPhoneNumber(normalizedPhoneNumber);
        registration.setPinHash(passwordEncoder.encode(request.pin().trim()));
        registration.setStatus(PENDING_STATUS);
        registration.setProvider(FIREBASE_PROVIDER);
        registration.setVerificationSessionId(null);
        registration.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        phoneRegistrationRequestRepository.save(registration);

        return new RegistrationStartResponse(
                registration.getId(),
                store.getCode(),
                normalizedPhoneNumber,
                FIREBASE_PROVIDER,
                registration.getStatus(),
                registration.getExpiresAt()
        );
    }

    @Transactional
    public RegistrationCompleteResponse completeRegistration(RegistrationCompleteRequest request) {
        PhoneRegistrationRequestEntity registration = phoneRegistrationRequestRepository.findDetailedById(request.registrationId())
                .orElseThrow(() -> new BadRequestException("Registration request not found"));

        if (!PENDING_STATUS.equals(registration.getStatus())) {
            throw new BadRequestException("Registration request is no longer pending");
        }

        if (registration.getExpiresAt().isBefore(OffsetDateTime.now())) {
            registration.setStatus("EXPIRED");
            throw new BadRequestException("Registration request has expired");
        }

        PhoneVerificationResult verification = phoneVerificationService.verifyPhoneNumber(
                registration.getPhoneNumber(),
                request.verificationCode().trim(),
                StringUtils.hasText(request.firebaseIdToken()) ? request.firebaseIdToken().trim() : null
        );

        String normalizedPhoneNumber = normalizePhoneNumber(verification.phoneNumber());
        if (!normalizedPhoneNumber.equals(registration.getPhoneNumber())) {
            throw new BadRequestException("Verified phone number does not match the registration request");
        }

        if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
            throw new BadRequestException("This phone number is already registered");
        }

        RoleEntity defaultRole = roleRepository.findByCodeAndActiveTrue(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new BadRequestException("Default cashier role is not available"));

        OffsetDateTime activatedAt = OffsetDateTime.now();
        UserEntity user = new UserEntity();
        user.setEmployeeCode(generateEmployeeCode(normalizedPhoneNumber));
        user.setDisplayName("Phone User " + normalizedPhoneNumber.substring(Math.max(0, normalizedPhoneNumber.length() - 4)));
        user.setPinHash(registration.getPinHash());
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setPhoneVerifiedAt(activatedAt);
        user.setStatus(ACTIVE_STATUS);
        userRepository.save(user);

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.assign(user, defaultRole);
        userRoleRepository.save(userRole);

        StoreStaffAssignmentEntity assignment = new StoreStaffAssignmentEntity();
        assignment.assign(registration.getStore(), user, true);
        storeStaffAssignmentRepository.save(assignment);

        registration.setFirebaseUid(verification.firebaseUid());
        registration.setVerificationCompletedAt(activatedAt);
        registration.setStatus(VERIFIED_STATUS);

        return new RegistrationCompleteResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getDisplayName(),
                registration.getStore().getCode(),
                normalizedPhoneNumber,
                ACTIVE_STATUS,
                activatedAt
        );
    }

    private String normalizePhoneNumber(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.replaceAll("[\\s\\-()]", "").trim();
        if (!normalized.matches("^\\+?\\d{10,15}$")) {
            throw new BadRequestException("Phone number format is invalid");
        }
        return normalized;
    }

    private String generateEmployeeCode(String phoneNumber) {
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        String baseCode = "SELF-" + digitsOnly.substring(Math.max(0, digitsOnly.length() - 8));
        String candidate = baseCode;
        int suffix = 1;
        while (userRepository.existsByEmployeeCode(candidate)) {
            candidate = baseCode + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
