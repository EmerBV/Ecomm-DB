package com.emerbv.ecommdb.exceptions;

import com.emerbv.ecommdb.response.ApiResponse;
import com.stripe.exception.CardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class )
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class )
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
        String message = "You do not have permission to this action";
        return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(com.stripe.exception.StripeException.class)
    public ResponseEntity<ApiResponse> handleStripeException(com.stripe.exception.StripeException ex) {
        logger.error("Stripe API error: {}", ex.getMessage());

        String message = "Error procesando el pago: ";
        Map<String, Object> errorDetails = new HashMap<>();

        // Manejo específico para diferentes tipos de excepciones de Stripe
        if (ex instanceof com.stripe.exception.CardException) {
            com.stripe.exception.CardException cardException = (com.stripe.exception.CardException) ex;
            message += "Problema con la tarjeta";

            // Añadir detalles específicos de la tarjeta
            if (cardException.getDeclineCode() != null) {
                errorDetails.put("declineCode", cardException.getDeclineCode());

                switch (cardException.getDeclineCode()) {
                    case "insufficient_funds":
                        message += " - Fondos insuficientes";
                        break;
                    case "lost_card":
                    case "stolen_card":
                        message += " - Tarjeta reportada como perdida o robada";
                        break;
                    case "expired_card":
                        message += " - Tarjeta expirada";
                        break;
                    case "incorrect_cvc":
                        message += " - Código CVC incorrecto";
                        break;
                    default:
                        message += " - " + cardException.getMessage();
                }
            } else {
                message += " - " + cardException.getMessage();
            }
        } else {
            // Para otros tipos de excepciones de Stripe
            message += ex.getMessage();
        }

        // Agregamos los detalles al objeto de respuesta
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("type", ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(message, errorDetails));
    }

    @ExceptionHandler(com.emerbv.ecommdb.exceptions.StripeException.class)
    public ResponseEntity<ApiResponse> handleCustomStripeException(com.emerbv.ecommdb.exceptions.StripeException ex) {
        logger.error("Custom Stripe error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse("Error en el procesamiento del pago: " + ex.getMessage(), null));
    }

}
