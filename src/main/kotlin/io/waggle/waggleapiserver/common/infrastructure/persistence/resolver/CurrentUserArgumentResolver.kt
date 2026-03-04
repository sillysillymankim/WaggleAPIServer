package io.waggle.waggleapiserver.common.infrastructure.persistence.resolver

import io.swagger.v3.oas.annotations.Parameter
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import org.springframework.core.MethodParameter
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Parameter(hidden = true)
annotation class CurrentUser

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AllowIncompleteProfile

@Component
class CurrentUserArgumentResolver(
    private val userRepository: UserRepository,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java) &&
            parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        val userPrincipal =
            authentication?.principal as? UserPrincipal
                ?: if (parameter.isOptional) return null
                else throw BusinessException(ErrorCode.UNAUTHORIZED)

        val user =
            userRepository.findByIdOrNull(userPrincipal.userId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: ${userPrincipal.userId}")

        if (!parameter.hasMethodAnnotation(AllowIncompleteProfile::class.java)) {
            user.checkProfileComplete()
        }

        return user
    }
}
