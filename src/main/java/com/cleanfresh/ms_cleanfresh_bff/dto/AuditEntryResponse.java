package com.cleanfresh.ms_cleanfresh_bff.dto;

/**
 * Espejo del recurso que expone ms-cleanfresh-auditoria.
 */
public record AuditEntryResponse(String time, String actor, String action, String level) {
}
