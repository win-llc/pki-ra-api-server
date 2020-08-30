package com.winllc.pki.ra.beans.metrics;

import com.winllc.pki.ra.domain.AuditRecord;

import java.util.List;

public class DailyAuditMetric {
    private String date;
    private Integer recordsTotal;
    private List<AuditRecord> auditRecords;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(Integer recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public List<AuditRecord> getAuditRecords() {
        return auditRecords;
    }

    public void setAuditRecords(List<AuditRecord> auditRecords) {
        this.auditRecords = auditRecords;
    }
}
