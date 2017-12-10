package com.example.demoosbapi;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootApplication
public class DemoOsbapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoOsbapiApplication.class, args);
	}

	@Bean
	public RouterFunction<ServerResponse> route(
			@Value("${osb.catalog:classpath:catalog.yml}") Resource catalog)
			throws IOException {
		return new ServiceBrokerHandler(catalog).routes();
	}

	@Bean
	public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
		return http.authorizeExchange() //
				.pathMatchers("/v2/**").authenticated() //
				.pathMatchers("/application/**").authenticated() //
				.and() //
				.httpBasic() //
				.and() //
				.csrf().disable() //
				.build();
	}

	@Bean
	public ReactiveUserDetailsService userDetailsService() {
		UserDetails user = User.withDefaultPasswordEncoder().username("username")
				.password("password").roles("USER").build();
		return new MapReactiveUserDetailsService(user);
	}

}
