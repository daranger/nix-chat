package uz.nchat.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.nchat.server.auth.AuthDtos.ErrorResponse;
import uz.nchat.server.auth.AuthExceptions.InvalidCredentialsException;
import uz.nchat.server.auth.AuthExceptions.UsernameTakenException;
import uz.nchat.server.phone.PhoneExceptions.DeliveryFailedException;
import uz.nchat.server.phone.PhoneExceptions.PhoneTakenException;
import uz.nchat.server.phone.PhoneExceptions.TooManyRequestsException;

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

    @ExceptionHandler(PhoneTakenException.class)
    public ResponseEntity<ErrorResponse> phoneTaken(PhoneTakenException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> tooManyRequests(TooManyRequestsException e) {
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(DeliveryFailedException.class)
    public ResponseEntity<ErrorResponse> deliveryFailed(DeliveryFailedException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
