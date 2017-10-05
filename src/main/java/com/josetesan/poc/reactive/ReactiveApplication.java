package com.josetesan.poc.reactive;

import io.micrometer.core.annotation.Timed;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapUserDetailsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class ReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveApplication.class, args);
	}
}

@Configuration
class WebConfiguration {

    @Timed("message")
	private Mono<ServerResponse> message(ServerRequest serverRequest) {
		Mono<String> principalPublisher = serverRequest.principal().map(o -> "Hello " + o.getName());
		return ServerResponse.ok().body(principalPublisher, String.class);
	}

    @Timed("username")
    private Mono<ServerResponse> username(ServerRequest serverRequest) {
        Mono<UserDetails> detailsMono = serverRequest.principal()
                .map(o -> UserDetails.class.cast(Authentication.class.cast(o).getPrincipal()));
        return ServerResponse.ok().body(detailsMono, UserDetails.class);
    }

	@Bean
    RouterFunction<?> routes() {
		return route(GET("/hello"), this::message)
                .andRoute(GET("/users/{username}"), this::username);
	}

}

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

	@Bean
    UserDetailsRepository userDetailsRepository() {
		UserDetails josete = User.withUsername("josete").roles("USER").password("password").build();
		UserDetails almu = User.withUsername("almu").roles("USER","ADMIN").password("password").build();

		return new MapUserDetailsRepository(josete,almu);
	}

	@Bean
    SecurityWebFilterChain security(HttpSecurity httpSecurity) {
		return httpSecurity
				.authorizeExchange()
                .pathMatchers("/users/{username}")
                    .access((mono, context) -> mono
                            .map(auth -> auth.getName().equals(context.getVariables().get("username")))
                            .map(AuthorizationDecision::new))
                    .anyExchange().authenticated()
				.and()
				.build();
	}
}