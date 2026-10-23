package com.portfolio.identity_service.security;

import com.portfolio.identity_service.entities.Account;
import com.portfolio.identity_service.entities.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AccountUserDetails implements UserDetails {

	private final UUID accountId;
	private final String email;
	private final String password;
	private final boolean enabled;

	public AccountUserDetails(Account account) {
		this.accountId = account.getId();
		this.email = account.getEmail();
		this.password = account.getPasswordHash();
		this.enabled = account.getStatus() == AccountStatus.ACTIVE;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
