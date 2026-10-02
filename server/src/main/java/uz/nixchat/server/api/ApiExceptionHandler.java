package uz.nixchat.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.nixchat.server.auth.AuthDtos.ErrorResponse;
import uz.nixchat.server.auth.AuthExceptions.InvalidCredentialsException;
import uz.nixchat.server.auth.AuthExceptions.UsernameTakenException;

/**
 * Turns exceptions into JSON errors: {@code {"error": "..."}} with a matching HTTP status.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<ErrorResponse> usernameTaken(UsernameTakenException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> invalidCredentials(InvalidCredentialsException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
