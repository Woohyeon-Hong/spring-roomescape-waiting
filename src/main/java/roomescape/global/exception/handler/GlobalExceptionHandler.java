package roomescape.global.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import roomescape.auth.exception.AuthenticationException;
import roomescape.auth.exception.AuthorizationException;
import roomescape.global.exception.BusinessException;
import roomescape.global.exception.DeleteFailedException;
import roomescape.global.exception.DuplicateException;
import roomescape.global.exception.InvalidRequestValueException;
import roomescape.global.exception.NotFoundException;
import roomescape.global.exception.response.ErrorResponse;
import roomescape.payment.exception.PaymentAlreadyProcessedException;
import roomescape.payment.exception.PaymentBadRequestException;
import roomescape.payment.exception.PaymentNotFoundException;
import roomescape.payment.exception.PaymentReadTimeoutException;
import roomescape.payment.exception.PaymentRejectedException;
import roomescape.payment.exception.PaymentServerErrorException;
import roomescape.payment.exception.PaymentTimeoutException;
import roomescape.payment.exception.PaymentUnauthorizedException;
import roomescape.payment.exception.UnknownPaymentErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse("서버 내부 예외가 발생했습니다."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("필수 요청 파라미터가 누락되었습니다."));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return makeResponse(e, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> makeResponse(
            BusinessException e,
            HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(e));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleMethodArgumentTypeMismatchException(
            MissingServletRequestParameterException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("요청 파라미터 형식이 유효하지 않습니다."));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateException e) {
        return makeResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DeleteFailedException.class)
    public ResponseEntity<ErrorResponse> handleDeleteFailedException(DeleteFailedException e) {
        return makeResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidRequestValueException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestValueException(InvalidRequestValueException e) {
        return makeResponse(e, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
        return makeResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        return makeResponse(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationException(AuthorizationException e) {
        return makeResponse(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("요청 값이 유효하지 않습니다.");

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(PaymentAlreadyProcessedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyProcessedException(PaymentAlreadyProcessedException e) {
        return makeResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentBadRequestException.class)
    public ResponseEntity<ErrorResponse> handlePaymentBadRequestException(PaymentBadRequestException e) {
        return makeResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentUnauthorizedException(PaymentUnauthorizedException e) {
        return makeResponse(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PaymentRejectedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentRejectedException(PaymentRejectedException e) {
        return makeResponse(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException e) {
        return makeResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentServerErrorException.class)
    public ResponseEntity<ErrorResponse> handlePaymentServerErrorException(PaymentServerErrorException e) {
        return makeResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UnknownPaymentErrorException.class)
    public ResponseEntity<ErrorResponse> handleUnknownPaymentErrorException(UnknownPaymentErrorException e) {
        return makeResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(PaymentTimeoutException.class)
    public ResponseEntity<ErrorResponse> handlePaymentTimeoutException(PaymentTimeoutException e) {
        return makeResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // read timeout은 승인 성공 여부를 알 수 없는 상태이므로, 확실한 실패(500)와 구분되는
    // 504로 응답해 클라이언트가 "확인 필요"로 다르게 처리하도록 한다.
    @ExceptionHandler(PaymentReadTimeoutException.class)
    public ResponseEntity<ErrorResponse> handlePaymentReadTimeoutException(PaymentReadTimeoutException e) {
        return makeResponse(e, HttpStatus.GATEWAY_TIMEOUT);
    }
}
