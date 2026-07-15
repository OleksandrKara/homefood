package com.homefood.admin.phone;

/**
 * Digits-only phone comparison so "+1 555-123-4567", "(555) 123-4567" and "5551234567" are all
 * recognized as the same number - customers (and staff) type phone numbers in whatever format
 * they like, and strict string equality would miss most real matches.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        // Drop a leading US country code (1) so "+1 555..." matches "555...".
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    /**
     * The single format every stored phone should end up in (see Client's @PrePersist/@PreUpdate
     * hook and the V13 migration that backfilled existing rows): "XXX-XXX-XXXX" for a clean
     * 10-digit US number. Anything else (blank, too short/long, non-US) is left as digits-only
     * rather than guessed at, so we never silently corrupt a number we can't confidently format.
     */
    public static String canonicalize(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String digits = normalize(trimmed);
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return digits.isEmpty() ? trimmed : digits;
    }
}
