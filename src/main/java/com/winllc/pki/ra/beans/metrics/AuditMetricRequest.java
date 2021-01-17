package com.winllc.pki.ra.beans.metrics;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class AuditMetricRequest {
    private String auditRecordType;
    private String dateFrom;
    private String dateTo;
    private Boolean returnFullAuditRecords = false;
    private ChronoUnit timeUnit;

    public String getAuditRecordType() {
        return auditRecordType;
    }

    public void setAuditRecordType(String auditRecordType) {
        this.auditRecordType = auditRecordType;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public Boolean getReturnFullAuditRecords() {
        return returnFullAuditRecords;
    }

    public void setReturnFullAuditRecords(Boolean returnFullAuditRecords) {
        this.returnFullAuditRecords = returnFullAuditRecords;
    }

    public ChronoUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(ChronoUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
