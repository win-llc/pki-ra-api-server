package com.winllc.pki.ra.beans.metrics;

import java.util.List;

public class AuditMetricResponse {
    private String dateFormat;
    private List<DailyAuditMetric> auditMetrics;

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public List<DailyAuditMetric> getAuditMetrics() {
        return auditMetrics;
    }

    public void setAuditMetrics(List<DailyAuditMetric> auditMetrics) {
        this.auditMetrics = auditMetrics;
    }
}
