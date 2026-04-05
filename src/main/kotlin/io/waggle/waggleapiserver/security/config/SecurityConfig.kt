package io.waggle.waggleapiserver.security.config

import io.waggle.waggleapiserver.security.filter.RateLimitFilter
import io.waggle.waggleapiserver.security.jwt.JwtAuthenticationFilter
import io.waggle.waggleapiserver.security.oauth2.CustomOAuth2UserService
import io.waggle.waggleapiserver.security.oauth2.OAuth2LoginFailureHandler
import io.waggle.waggleapiserver.security.oauth2.OAuth2LoginSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val environment: Environment,
    private val rateLimitFilter: RateLimitFilter,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val oAuth2LoginFailureHandler: OAuth2LoginFailureHandler,
) {
    private val isLocal: Boolean
        get() = environment.activeProfiles.contains("local")

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

        if (isLocal) {
            http.headers { it.frameOptions { frame -> frame.sameOrigin() } }
        }

        http.authorizeHttpRequests { authorize ->
            authorize
                .requestMatchers(
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/auth/refresh",
                    "/ws",
                    "/ws-sockjs/**",
                ).permitAll()

            if (isLocal) {
                authorize.requestMatchers("/h2-console/**", "/ws-test.html").permitAll()
            }

            authorize
                .requestMatchers(HttpMethod.GET, "/posts/**", "/teams/**", "/users/**")
                .permitAll()
                .anyRequest()
                .authenticated()
        }.oauth2Login { oauth2 ->
            oauth2
                .userInfoEndpoint { it.userService(customOAuth2UserService) }
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2LoginFailureHandler)
        }.addFilterBefore(
            rateLimitFilter,
            UsernamePasswordAuthenticationFilter::class.java,
        ).addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter::class.java,
        )

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:3000", "http://localhost:5173", "https://waggle.lol")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
