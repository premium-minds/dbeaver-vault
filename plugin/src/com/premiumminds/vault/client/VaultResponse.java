package com.premiumminds.vault.client;

import java.util.Map;

class VaultResponse {

    private String requestId;
    private String leaseId;
    private Boolean renewable;
    private Long leaseDuration;
    private Map<String, Object> data;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }

    public Boolean getRenewable() {
        return renewable;
    }

    public void setRenewable(Boolean renewable) {
        this.renewable = renewable;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Long getLeaseDuration() {
        return leaseDuration;
    }

    public void setLeaseDuration(Long leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

}
