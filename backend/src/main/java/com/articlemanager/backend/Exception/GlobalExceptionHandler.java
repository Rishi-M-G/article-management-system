package com.articlemanager.backend.Exception;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import org.jboss.logging.MDC;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        pd.setType(URI.create("https://api.articlemanager.com/errors/not-found"));
        pd.setProperty("resourceType", ex.getResourceType());
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        pd.setType(URI.create("https://api.articlemanager.com/errors/duplicate-resource"));
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        pd.setType(URI.create("https://api.articlemanager.com/errors/data-integrity-violation"));
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessViolation(BusinessRuleViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        pd.setType(URI.create("https://api.articlemanager.com/errors/business-rule-violation"));
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage())).toList();

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Validation Failed");
        pd.setProperty("errors", fieldErrors);
        pd.setProperty("trace", MDC.get("traceId"));

        return ResponseEntity.unprocessableEntity().body(pd);
    }

}
