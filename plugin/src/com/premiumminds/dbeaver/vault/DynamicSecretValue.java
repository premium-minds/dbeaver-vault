package com.premiumminds.dbeaver.vault;

import java.time.Instant;

public class DynamicSecretValue {

	private final Instant issueTime;
	private final DynamicSecretResponse response;

	public DynamicSecretValue(Instant issueTime, DynamicSecretResponse response) {
		this.issueTime = issueTime;
		this.response = response;
	}

	public Instant getIssueTime() {
		return issueTime;
	}

	public DynamicSecretResponse getResponse() {
		return response;
	}

	public Instant getExpireTime() {
		return issueTime.plusSeconds(response.getLeaseDuration());
	}
}
