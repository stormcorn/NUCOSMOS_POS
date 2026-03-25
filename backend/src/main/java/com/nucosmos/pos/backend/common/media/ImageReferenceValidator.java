package com.nucosmos.pos.backend.common.media;

import com.nucosmos.pos.backend.common.exception.BadRequestException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageReferenceValidator {

    public static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    private static final Pattern DATA_URL_PATTERN = Pattern.compile(
            "^data:(image/[a-zA-Z0-9.+-]+);base64,([A-Za-z0-9+/=\\r\\n]+)$"
    );

    private ImageReferenceValidator() {
    }

    public static String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("data:")) {
            validateDataUrl(trimmed);
            return trimmed;
        }

        validateHttpUrl(trimmed);
        return trimmed;
    }

    private static void validateHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new BadRequestException("Image must be an http/https URL or an uploaded image under 2MB");
            }

            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
                throw new BadRequestException("Image must be an http/https URL or an uploaded image under 2MB");
            }
        } catch (URISyntaxException exception) {
            throw new BadRequestException("Image must be an http/https URL or an uploaded image under 2MB");
        }
    }

    private static void validateDataUrl(String value) {
        Matcher matcher = DATA_URL_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new BadRequestException("Uploaded image must be a JPG, PNG, GIF, or WebP file under 2MB");
        }

        String mimeType = matcher.group(1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BadRequestException("Uploaded image must be a JPG, PNG, GIF, or WebP file under 2MB");
        }

        String encodedPayload = matcher.group(2).replaceAll("\\s+", "");
        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(encodedPayload);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Uploaded image data is invalid");
        }

        if (decodedBytes.length > MAX_UPLOAD_BYTES) {
            throw new BadRequestException("Uploaded image must be 2MB or smaller");
        }
    }
}
