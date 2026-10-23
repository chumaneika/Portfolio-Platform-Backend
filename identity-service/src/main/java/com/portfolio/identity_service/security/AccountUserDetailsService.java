package com.portfolio.identity_service.security;

import com.portfolio.identity_service.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

	private final AccountRepository accountRepository;

	@Override
	public UserDetails loadUserByUsername(String emailValue) throws UsernameNotFoundException {
		if (emailValue == null || emailValue.isBlank()) {
			throw new UsernameNotFoundException("User not found");
		}

		var email = emailValue.trim().toLowerCase(Locale.ROOT);
		var account = accountRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return new AccountUserDetails(account);
	}
}
