package io.github.connellite.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(exception.getStatusCode());
        problem.setTitle(exception.getStatusCode().toString());
        problem.setDetail(exception.getReason());
        return ResponseEntity.status(exception.getStatusCode()).body(problem);
    }

    @ExceptionHandler(SoapFaultException.class)
    public ResponseEntity<ProblemDetail> handleSoapFault(SoapFaultException exception) {
        log.warn("SOAP fault: {} - {}", exception.getFaultName(), exception.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(exception.httpStatusCode());
        problem.setTitle("SOAP Fault");
        problem.setDetail(exception.getMessage());
        problem.setProperty("faultName", exception.getFaultName());
        if (exception.getFaultCode() != null) {
            problem.setProperty("faultCode", exception.getFaultCode());
        }
        return ResponseEntity.status(exception.httpStatusCode()).body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(NoResourceFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(exception.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception exception) {
        log.error("Bridge request failed", exception);
        Throwable root = NestedExceptionUtils.getMostSpecificCause(exception);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle(root.getClass().getSimpleName());
        problem.setDetail(root.getMessage() != null ? root.getMessage() : root.toString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }
}
