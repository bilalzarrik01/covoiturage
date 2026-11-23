package ma.youcode.covoiturage.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Object> handleBusinessValidation(RuntimeException exception, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintValidation(WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Les donnees fournies sont invalides.", request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNotFound(NoSuchElementException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException exception, WebRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataConflict(WebRequest request) {
        // Never return SQL, constraint names or database values to the client.
        return problem(HttpStatus.CONFLICT, "Les donnees sont en conflit avec les donnees existantes.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedError(WebRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.", request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Spring handles invalid JSON, @Valid, missing parameters and type conversion here.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "La requete ne peut pas etre traitee.");
        return super.handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setType(URI.create("about:blank"));
            problem.setProperty("timestamp", Instant.now().toString());
            problem.setProperty("path", ((ServletWebRequest) request).getRequest().getRequestURI());
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String detail, WebRequest request) {
        return createResponseEntity(ProblemDetail.forStatusAndDetail(status, detail),
                new HttpHeaders(), status, request);
    }
}
