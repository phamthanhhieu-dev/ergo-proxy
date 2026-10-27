package com.ergoproxy.platform.exception;

import com.ergoproxy.platform.dto.GlobalApiResponse;
import com.ergoproxy.platform.util.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    private ResponseEntity<GlobalApiResponse<Void>> respond(LogicException exception, HttpStatusCode status) {
        String message = messageSource.getMessage(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey(), LocaleContextHolder.getLocale());
        GlobalApiResponse<Void> payload = GlobalApiResponse.error(exception.getCode(), message);
        ResponseEntity<GlobalApiResponse<Void>> response = ResponseEntity.status(status).body(payload);
        return response;
    }

    @ExceptionHandler({
        org.apache.catalina.connector.ClientAbortException.class,
        org.springframework.web.context.request.async.AsyncRequestNotUsableException.class
    })
    public void handleDisconnectedClient(Exception exception) {
        // Trình duyệt đóng Range cũ khi tua/chuyển tập, không ghi JSON vào response video.
    }

    @ExceptionHandler(LogicException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleLogicException(LogicException exception) {
        ResponseEntity<GlobalApiResponse<Void>> response = respond(exception, HttpStatus.BAD_REQUEST);
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        LogicException error = new LogicException(exception, "HVEGEH_00001", "request.data.invalid");
        String message = Messages.get(error.getMessageKey(), error.getArgs());
        List<FieldError> errors = exception.getBindingResult().getFieldErrors();
        if (!errors.isEmpty()) {
            message = errors.getFirst().getDefaultMessage();
        }
        GlobalApiResponse<Void> payload = GlobalApiResponse.error(error.getCode(), message);
        ResponseEntity<GlobalApiResponse<Void>> response = ResponseEntity.badRequest().body(payload);
        return response;
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<GlobalApiResponse<Void>> handleBadRequest(Exception exception) {
        LogicException error = new LogicException(exception, "HBRGEH_00001", "request.parameters.invalid");
        ResponseEntity<GlobalApiResponse<Void>> response = respond(error, HttpStatus.BAD_REQUEST);
        return response;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleException(Exception exception) {
        if (exception instanceof ErrorResponse upstream) {
            HttpStatusCode status = upstream.getStatusCode();
            LogicException error = new LogicException(exception, "HEGEH_00001", "request.http.failed", status.value());
            ResponseEntity<GlobalApiResponse<Void>> response = respond(error, status);
            return response;
        }
        LogicException error = new LogicException(exception, "HEGEH_00002", "request.unexpected.failed", exception.getClass().getSimpleName());
        ResponseEntity<GlobalApiResponse<Void>> response = respond(error, HttpStatus.INTERNAL_SERVER_ERROR);
        return response;
    }
}
