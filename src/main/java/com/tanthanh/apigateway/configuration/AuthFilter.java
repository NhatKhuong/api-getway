package com.tanthanh.apigateway.configuration;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements WebFilter {
	private final WebClient webClient;
    @Value("${auth.service.url}")
    private String authServiceUrl;
	public AuthFilter(WebClient.Builder webClientBuilder,
					  @Value("${auth.service.url}") String authServiceUrl) {
		this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String token = headers.getFirst(HttpHeaders.AUTHORIZATION).substring("Bearer ".length());
		return authServiceFilter(exchange, chain, token);

	}

	private Mono<Void> authServiceFilter(ServerWebExchange exchange, WebFilterChain chain, String token) {
        return webClient.get()
                .uri(authServiceUrl + "/api/v1/auth/validate?token={token}", token)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    // Xử lý kết quả từ dịch vụ xác thực
                    // Nếu kết quả hợp lệ, tiếp tục chuỗi filter
                    // Nếu kết quả không hợp lệ, có thể trả về lỗi hoặc thực hiện các bước khác
                    return chain.filter(exchange);
                })
                .onErrorResume(error -> {
                    // Xử lý lỗi nếu cần
                    return Mono.error(new Exception("Error while validating token"));
                });
	}

}
