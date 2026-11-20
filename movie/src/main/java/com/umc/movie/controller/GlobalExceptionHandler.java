package com.umc.movie.controller;

import com.umc.movie.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.umc.movie.exception.ApiException;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException exception
    ) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.failure(
                        exception.getCode(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("COMMON4001", message));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure("COMMON4001", "요청 값의 형식과 범위를 확인해주세요."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException exception) {

        // 사전 검사 뒤 동시 등록이 일어나더라도, DB UNIQUE 오류를 같은 도메인 코드로 반환합니다.
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                String name = violation.getConstraintName().toLowerCase(Locale.ROOT);
                if (name.matches("(?s).*\\buk_favorite_member_movie\\b.*")) {
                    return ResponseEntity.status(409).body(ApiResponse.failure("FAVORITE409_1", "이미 이 영화를 즐겨찾기하셨습니다."));
                }
                if (name.matches("(?s).*\\buk_rating_member_movie\\b.*")) {
                    return ResponseEntity.status(409).body(ApiResponse.failure("RATING409_1", "이미 이 영화에 평점을 남기셨습니다."));
                }
            }
        }
        return ResponseEntity.status(409)
                .body(ApiResponse.failure(
                        "COMMON4091",
                        "이미 존재하는 데이터입니다."
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException exception) {

        return ResponseEntity.status(409)
                .body(ApiResponse.failure(
                        "COMMON4092",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException exception) {

        int status = exception.getStatusCode().value();

        return ResponseEntity.status(status)
                .body(ApiResponse.failure(
                        "COMMON" + status,
                        exception.getReason()
                ));
    }
}
