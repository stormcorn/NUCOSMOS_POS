package com.nucosmos.pos.backend.device;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.UnauthorizedException;
import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import com.nucosmos.pos.backend.device.repository.DeviceRepository;
import com.nucosmos.pos.backend.store.StoreReceiptSettingsRequest;
import com.nucosmos.pos.backend.store.StoreReceiptSettingsResponse;
import com.nucosmos.pos.backend.store.StoreSummaryResponse;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class StoreDeviceService {

    private final StoreRepository storeRepository;
    private final DeviceRepository deviceRepository;

    public StoreDeviceService(StoreRepository storeRepository, DeviceRepository deviceRepository) {
        this.storeRepository = storeRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    public List<StoreSummaryResponse> listStores() {
        return storeRepository.findAllByOrderByCodeAsc()
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

    @Transactional(readOnly = true)
    public StoreReceiptSettingsResponse getReceiptSettings(java.util.UUID storeId) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BadRequestException("Store not found"));
        return toReceiptSettingsResponse(store);
    }

    @Transactional
    public StoreReceiptSettingsResponse updateReceiptSettings(
            java.util.UUID storeId,
            StoreReceiptSettingsRequest request
    ) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BadRequestException("Store not found"));
        store.setReceiptFooterText(normalizeReceiptFooterText(request.receiptFooterText()));
        return toReceiptSettingsResponse(store);
    }

    @Transactional(readOnly = true)
    public List<DeviceAdminResponse> listDevices(String storeCode, String status) {
        List<DeviceEntity> devices;
        if (StringUtils.hasText(storeCode) && StringUtils.hasText(status)) {
            devices = deviceRepository.findAllByStore_CodeAndStatusOrderByNameAsc(storeCode.trim(), status.trim().toUpperCase());
        } else if (StringUtils.hasText(storeCode)) {
            devices = deviceRepository.findAllByStore_CodeOrderByNameAsc(storeCode.trim());
        } else if (StringUtils.hasText(status)) {
            devices = deviceRepository.findAllByStatusOrderByStore_CodeAscNameAsc(status.trim().toUpperCase());
        } else {
            devices = deviceRepository.findAllByOrderByStore_CodeAscNameAsc();
        }

        return devices.stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    @Transactional
    public DeviceHeartbeatResponse recordHeartbeat(Authentication authentication, DeviceHeartbeatRequest request) {
        AuthenticatedUser user = requireAuthenticatedUser(authentication);
        String deviceCode = resolveDeviceCode(user, request);
        OffsetDateTime now = OffsetDateTime.now();

        DeviceEntity device = deviceRepository.findByStore_CodeAndDeviceCodeAndStatus(user.storeCode(), deviceCode, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated device is not available"));

        device.markHeartbeat(now);

        return new DeviceHeartbeatResponse(
                device.getId(),
                device.getStore().getCode(),
                device.getDeviceCode(),
                device.getStatus(),
                device.getLastSeenAt(),
                now
        );
    }

    private AuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    private String resolveDeviceCode(AuthenticatedUser user, DeviceHeartbeatRequest request) {
        if (request != null && StringUtils.hasText(request.deviceCode())) {
            return request.deviceCode().trim();
        }
        if (StringUtils.hasText(user.deviceCode())) {
            return user.deviceCode().trim();
        }
        throw new BadRequestException("Device code is required");
    }

    private DeviceAdminResponse toDeviceResponse(DeviceEntity device) {
        return new DeviceAdminResponse(
                device.getId(),
                device.getStore().getId(),
                device.getStore().getCode(),
                device.getDeviceCode(),
                device.getName(),
                device.getPlatform(),
                device.getStatus(),
                device.getLastSeenAt()
        );
    }

    private StoreReceiptSettingsResponse toReceiptSettingsResponse(StoreEntity store) {
        return new StoreReceiptSettingsResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                defaultString(store.getReceiptFooterText())
        );
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
