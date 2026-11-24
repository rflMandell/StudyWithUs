package com.studywithus.admin.application;

public final class AdminAuditContextHolder {

    private static final ThreadLocal<Boolean> RECORDED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private AdminAuditContextHolder() {
    }

    public static void markRecorded() {
        RECORDED.set(Boolean.TRUE);
    }

    public static boolean wasRecorded() {
        return Boolean.TRUE.equals(RECORDED.get());
    }

    public static void clear() {
        RECORDED.remove();
    }
}
