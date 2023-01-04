package com.premiumminds.dbeaver.vault;

import java.util.Objects;

public class DynamicSecretKey {

	private final String address;
	private final String secret;

	public DynamicSecretKey(String address, String secret) {
		this.secret = secret;
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public String getSecret() {
		return secret;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, secret);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DynamicSecretKey other = (DynamicSecretKey) obj;
		return Objects.equals(address, other.address) && Objects.equals(secret, other.secret);
	}
}
