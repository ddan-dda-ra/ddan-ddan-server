package notbe.tmtm.ddanddanserver.presentation.config

import jakarta.servlet.http.HttpServletRequest
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationException
import notbe.tmtm.ddanddanserver.domain.exception.AuthorizationException
import notbe.tmtm.ddanddanserver.domain.exception.CustomException
import notbe.tmtm.ddanddanserver.domain.exception.ErrorCode
import notbe.tmtm.ddanddanserver.domain.exception.PermissionDeniedException
import notbe.tmtm.ddanddanserver.infrastructure.util.logger
import notbe.tmtm.ddanddanserver.presentation.dto.response.ErrorResponse
import org.hibernate.exception.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.util.BindErrorUtils
import org.springframework.web.util.ContentCachingRequestWrapper
import java.io.IOException

@RestControllerAdvice
class WebExceptionHandler {
    val logger = logger()

    @ExceptionHandler(value = [CustomException::class])
    fun handleDomainException(
        exception: CustomException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.fromErrorCode(
                    errorCode = exception.errorCode,
                    data = exception.data,
                ),
            )

    @ExceptionHandler(
        value = [
            MethodArgumentNotValidException::class,
            MethodArgumentTypeMismatchException::class,
            HttpMessageNotReadableException::class,
            ConstraintViolationException::class,
        ],
    )
    fun handleInvalidInput(exception: Exception): ResponseEntity<ErrorResponse> {
        val data: Any? =
            when (exception) {
                is MethodArgumentNotValidException -> BindErrorUtils.resolve(exception.allErrors).values
                is MethodArgumentTypeMismatchException -> exception.localizedMessage
                is HttpMessageNotReadableException -> exception.localizedMessage
                else -> null
            }
        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.fromErrorCode(
                    errorCode = ErrorCode.INVALID_INPUT,
                    data = data,
                ),
            )
    }

    @ExceptionHandler(
        value = [
            NoResourceFoundException::class,
            HttpRequestMethodNotSupportedException::class,
        ],
    )
    fun handleUnknownResource(exception: Exception): ResponseEntity<ErrorResponse> {
        val errorCode =
            when (exception) {
                is NoResourceFoundException -> ErrorCode.UNKNOWN_RESOURCE
                is HttpRequestMethodNotSupportedException -> ErrorCode.INVALID_METHOD
                else -> ErrorCode.UNKNOWN_SERVER_ERROR
            }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse.fromErrorCode(
                    errorCode = errorCode,
                ),
            )
    }

    @ExceptionHandler(value = [Throwable::class])
    fun handleUnhandledException(
        exception: Throwable,
        request: HttpServletRequest,
        bodyCache: ContentCachingRequestWrapper,
    ): ResponseEntity<ErrorResponse> {
        if (exception is IOException) {
            return ResponseEntity
                .internalServerError()
                .build()
        }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.fromErrorCode(ErrorCode.UNKNOWN_SERVER_ERROR))
    }

    @ExceptionHandler(value = [AuthenticationException::class])
    fun handleAuthenticationException(
        exception: AuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse.fromErrorCode(
                    errorCode = exception.errorCode,
                    data = exception.data,
                ),
            )

    @ExceptionHandler(value = [AuthorizationException::class])
    fun handleAuthorizationException(exception: AuthorizationException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse.fromErrorCode(
                    errorCode = exception.errorCode,
                    data = exception.data,
                ),
            )

    @ExceptionHandler(value = [AccessDeniedException::class])
    fun handleAccessDeniedException() = (PermissionDeniedException())
}
