package com.premiumminds.dbeaver.vault;

public class LeaseResponse {

    public static class Data {	
        private String expireTime;
        private String id;
        private String issueTime;
        private String lastRenewal;
        private Boolean renewable;
        private Long ttl;
        
		public String getExpireTime() {
			return expireTime;
		}
		
		public void setExpireTime(String expireTime) {
			this.expireTime = expireTime;
		}
		
		public String getId() {
			return id;
		}
		
		public void setId(String id) {
			this.id = id;
		}
		
		public String getIssueTime() {
			return issueTime;
		}
		
		public void setIssueTime(String issueTime) {
			this.issueTime = issueTime;
		}
		
		public String getLastRenewal() {
			return lastRenewal;
		}
		
		public void setLastRenewal(String lastRenewal) {
			this.lastRenewal = lastRenewal;
		}
		
		public Boolean getRenewable() {
			return renewable;
		}
		
		public void setRenewable(Boolean renewable) {
			this.renewable = renewable;
		}
		
		public Long getTtl() {
			return ttl;
		}
		
		public void setTtl(Long ttl) {
			this.ttl = ttl;
		}
    }
	
    private String requestId;
    private String leaseId;
    private Boolean renewable;
    private Long leaseDuration;
    private Data data;
    
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
	
	public Long getLeaseDuration() {
		return leaseDuration;
	}
	
	public void setLeaseDuration(Long leaseDuration) {
		this.leaseDuration = leaseDuration;
	}
	
	public Data getData() {
		return data;
	}
	
	public void setData(Data data) {
		this.data = data;
	}
}
