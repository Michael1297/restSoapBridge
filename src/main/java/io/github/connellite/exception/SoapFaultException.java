package io.github.connellite.exception;

import lombok.Getter;
import org.springframework.core.NestedExceptionUtils;

@Getter
public class SoapFaultException extends RuntimeException {

    private final String faultName;
    private final int httpStatus;

    public SoapFaultException(String faultName, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.faultName = faultName;
        this.httpStatus = httpStatus;
    }

    public static SoapFaultException from(Throwable throwable) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(throwable);
        String className = root.getClass().getSimpleName();
        String faultName = className.endsWith("_Exception")
                ? className.substring(0, className.length() - "_Exception".length())
                : className;
        int status = switch (faultName) {
            case "AccessDeniedException" -> 401;
            case "RepositoryException", "DatabaseException" -> 500;
            default -> 502;
        };
        String message = root.getMessage() != null ? root.getMessage() : faultName;
        return new SoapFaultException(faultName, message, status, throwable);
    }
}
