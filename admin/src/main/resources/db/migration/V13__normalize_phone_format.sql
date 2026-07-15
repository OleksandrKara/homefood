-- One-time backfill to a single consistent phone format (XXX-XXX-XXXX for US numbers).
-- Going forward, Client's @PrePersist/@PreUpdate hook (see PhoneNumbers.canonicalize) keeps every
-- save in this same format automatically - this migration only catches data that predates it.
UPDATE clients
SET phone = CASE
    WHEN length(regexp_replace(phone, '[^0-9]', '', 'g')) = 10
        THEN substring(regexp_replace(phone, '[^0-9]', '', 'g') from 1 for 3) || '-' ||
             substring(regexp_replace(phone, '[^0-9]', '', 'g') from 4 for 3) || '-' ||
             substring(regexp_replace(phone, '[^0-9]', '', 'g') from 7 for 4)
    WHEN length(regexp_replace(phone, '[^0-9]', '', 'g')) = 11
         AND regexp_replace(phone, '[^0-9]', '', 'g') LIKE '1%'
        THEN substring(regexp_replace(phone, '[^0-9]', '', 'g') from 2 for 3) || '-' ||
             substring(regexp_replace(phone, '[^0-9]', '', 'g') from 5 for 3) || '-' ||
             substring(regexp_replace(phone, '[^0-9]', '', 'g') from 8 for 4)
    ELSE regexp_replace(phone, '[^0-9]', '', 'g')
END
WHERE phone IS NOT NULL AND phone <> '';
