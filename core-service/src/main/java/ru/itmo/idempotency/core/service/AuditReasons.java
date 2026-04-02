package ru.itmo.idempotency.core.service;

public final class AuditReasons {

    public static final String ORPHAN_REPLY = "ORPHAN_REPLY";
    public static final String INVALID_RECEIVER_REPLY = "INVALID_RECEIVER_REPLY";

    private AuditReasons() {
    }
}
