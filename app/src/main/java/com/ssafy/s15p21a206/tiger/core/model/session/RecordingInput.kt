package com.ssafy.s15p21a206.tiger.core.model.session

sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(
        val error: ValidationError,
    ) : ValidationResult
}

enum class ValidationError {
    EMPTY_TASK,
    EMPTY_OBJECT,
    INVALID_TASK,
    UNSUPPORTED_CAMERA_CONFIG,
}
