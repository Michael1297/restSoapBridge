package io.github.connellite.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getStatusCode().toString());
        body.put("message", exception.getReason());
        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }

    @ExceptionHandler(SoapFaultException.class)
    public ResponseEntity<Map<String, Object>> handleSoapFault(SoapFaultException exception) {
        log.warn("SOAP fault: {} - {}", exception.getFaultName(), exception.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getFaultName());
        body.put("message", exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus()).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Not Found");
        body.put("message", exception.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception exception) {
        log.error("Bridge request failed", exception);
        Throwable root = NestedExceptionUtils.getMostSpecificCause(exception);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", root.getClass().getSimpleName());
        body.put("message", root.getMessage() != null ? root.getMessage() : root.toString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
