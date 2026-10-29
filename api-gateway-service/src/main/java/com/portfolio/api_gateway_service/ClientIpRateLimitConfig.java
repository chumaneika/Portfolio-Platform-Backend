package com.portfolio.api_gateway_service;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class ClientIpRateLimitConfig {

	@Bean
	KeyResolver clientIpKeyResolver(@Value("${rate-limit.trust-proxy-client-ip:false}") boolean trustProxyClientIp) {
		return exchange -> {
			if (trustProxyClientIp) {
				var forwardedIp = exchange.getRequest().getHeaders().getFirst("X-Client-IP");
				if (forwardedIp != null && !forwardedIp.isBlank()) {
					return Mono.just(forwardedIp);
				}
			}
			return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
					.map(address -> address.getAddress() != null
							? address.getAddress().getHostAddress() : address.getHostString())
					.defaultIfEmpty("unknown");
		};
	}
}
