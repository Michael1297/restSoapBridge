package io.github.connellite.exception;

import lombok.Getter;
import org.apache.cxf.binding.soap.SoapFault;
import org.springframework.core.NestedExceptionUtils;

@Getter
public class SoapFaultException extends RuntimeException {

    private final String faultName;
    private final int httpStatus;
    private final String faultCode;

    public SoapFaultException(String faultName, String message, int httpStatus, String faultCode, Throwable cause) {
        super(message, cause);
        this.faultName = faultName;
        this.httpStatus = httpStatus;
        this.faultCode = faultCode;
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
        String faultCode = root instanceof SoapFault soapFault && soapFault.getFaultCode() != null
                ? soapFault.getFaultCode().toString()
                : null;
        return new SoapFaultException(faultName, message, status, faultCode, throwable);
    }
}
