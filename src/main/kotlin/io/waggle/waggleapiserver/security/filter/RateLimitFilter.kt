package io.waggle.waggleapiserver.security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    // TODO: 다중 서버 환경 시 bucket4j-redis로 전환 필요
    private val buckets = ConcurrentHashMap<String, Bucket>()

    @Scheduled(fixedRate = 600_000)
    fun cleanUpExpiredBuckets() {
        buckets.entries.removeIf { it.value.availableTokens == 100L }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val clientIp = resolveClientIp(request)
        val bucket = buckets.computeIfAbsent(clientIp) { createBucket() }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write(
                objectMapper.writeValueAsString(
                    ErrorResponse(
                        status = HttpStatus.TOO_MANY_REQUESTS.value(),
                        code = "TOO_MANY_REQUESTS",
                        message = "Too many requests. Please try again later.",
                    ),
                ),
            )
        }
    }

    private fun createBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(
                Bandwidth
                    .builder()
                    .capacity(100)
                    .refillGreedy(100, Duration.ofMinutes(1))
                    .build(),
            ).build()

    private fun resolveClientIp(request: HttpServletRequest): String =
        request
            .getHeader("X-Forwarded-For")
            ?.split(",")
            ?.first()
            ?.trim()
            ?: request.remoteAddr
}
