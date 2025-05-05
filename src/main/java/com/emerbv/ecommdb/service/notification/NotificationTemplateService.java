package com.emerbv.ecommdb.service.notification;

import com.emerbv.ecommdb.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio para gestionar y procesar plantillas para las notificaciones
 */
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationTemplateService.class);

    private final TemplateEngine templateEngine;
    private final ResourceLoader resourceLoader;

    @Value("${notification.templates.path:classpath:templates/notifications/}")
    private String templatesPath;

    @Value("${notification.default-language:es}")
    private String defaultLanguage;

    /**
     * Procesa una plantilla para generar contenido HTML
     * @param type Tipo de notificación
     * @param language Idioma para la plantilla
     * @param variables Variables para rellenar la plantilla
     * @return HTML generado
     */
    @Cacheable(value = "notificationTemplates", key = "#type + '_' + #language")
    public String processTemplate(NotificationType type, String language, Map<String, Object> variables) {
        String templateName = getTemplateNameByType(type);

        if (language == null || language.isEmpty()) {
            language = defaultLanguage;
        }

        // Crear contexto de Thymeleaf con las variables
        Context context = new Context(new Locale(language));
        if (variables != null) {
            variables.forEach(context::setVariable);
        }

        // Añadir variables comunes para todas las plantillas
        addCommonVariables(context);

        // Procesar la plantilla
        try {
            String result = templateEngine.process(templateName, context);
            if (result == null || result.isEmpty()) {
                logger.error("Plantilla vacía o no encontrada: {} ({})", templateName, language);
                return getDefaultTemplate(type, variables);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error procesando plantilla {}: {}", templateName, e.getMessage(), e);
            return getDefaultTemplate(type, variables);
        }
    }

    /**
     * Obtiene una versión resumida para SMS de una notificación
     * @param type Tipo de notificación
     * @param language Idioma
     * @param variables Variables
     * @return Texto plano para SMS
     */
    @Cacheable(value = "smsTemplates", key = "#type + '_' + #language")
    public String getSmsSummary(NotificationType type, String language, Map<String, Object> variables) {
        String smsTemplatePath = templatesPath + (language != null ? language : defaultLanguage) +
                "/templates/notifications/es/sms/" + getTemplateNameByType(type) + ".txt";

        try {
            Resource resource = resourceLoader.getResource(smsTemplatePath);
            if (!resource.exists()) {
                logger.warn("Plantilla SMS no encontrada: {}", smsTemplatePath);
                return getDefaultSmsContent(type, variables);
            }

            String template = new String(Files.readAllBytes(Path.of(resource.getURI())), StandardCharsets.UTF_8);

            // Reemplazar variables en la plantilla (formato simple para SMS)
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    template = template.replace("${" + entry.getKey() + "}",
                            entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }

            return template;
        } catch (IOException e) {
            logger.error("Error leyendo plantilla SMS: {}", e.getMessage(), e);
            return getDefaultSmsContent(type, variables);
        }
    }

    /**
     * Determina el nombre de la plantilla según el tipo de notificación
     */
    private String getTemplateNameByType(NotificationType type) {
        return switch (type) {
            case ORDER_CONFIRMATION -> "order-confirmation";
            case ORDER_SHIPPED -> "order-shipped";
            case ORDER_DELIVERED -> "order-delivered";
            case ORDER_CANCELLED -> "order-cancelled";
            case PAYMENT_CONFIRMATION -> "payment-confirmation";
            case PAYMENT_FAILED -> "payment-failed";
            case CART_ABANDONED -> "cart-abandoned";
            case PASSWORD_RESET -> "password-reset";
            case WELCOME -> "welcome";
            case PRODUCT_BACK_IN_STOCK -> "product-back-in-stock";
            case SPECIAL_OFFER -> "special-offer";
            default -> "generic";
        };
    }

    /**
     * Añade variables comunes a todas las plantillas
     */
    private void addCommonVariables(Context context) {
        context.setVariable("storeName", "EmerBV Store");
        context.setVariable("storeUrl", "https://emerbv-ecommerce.com");
        context.setVariable("storeEmail", "support@emerbv-ecommerce.com");
        context.setVariable("storePhone", "+34 123 456 789");
        context.setVariable("year", java.time.Year.now().getValue());
        // Añadir URLs de redes sociales y otros datos comunes
    }

    /**
     * Retorna una plantilla genérica para casos donde no se encuentra la plantilla específica
     */
    private String getDefaultTemplate(NotificationType type, Map<String, Object> variables) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>");
        html.append("<h1>").append(getNotificationTitle(type)).append("</h1>");

        if (variables != null && variables.containsKey("message")) {
            html.append("<p>").append(variables.get("message")).append("</p>");
        } else {
            html.append("<p>").append(getDefaultMessage(type)).append("</p>");
        }

        html.append("<p>Equipo de EmerBV Store</p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Retorna un contenido SMS genérico
     */
    private String getDefaultSmsContent(NotificationType type, Map<String, Object> variables) {
        StringBuilder sms = new StringBuilder();
        sms.append("EmerBV Store: ").append(getNotificationTitle(type));

        if (variables != null && variables.containsKey("orderId")) {
            sms.append(" #").append(variables.get("orderId"));
        }

        sms.append(". ").append(getDefaultMessage(type));

        return sms.toString();
    }

    /**
     * Retorna un título según el tipo de notificación
     */
    private String getNotificationTitle(NotificationType type) {
        return switch (type) {
            case ORDER_CONFIRMATION -> "Confirmación de pedido";
            case ORDER_SHIPPED -> "Tu pedido ha sido enviado";
            case ORDER_DELIVERED -> "Tu pedido ha sido entregado";
            case ORDER_CANCELLED -> "Pedido cancelado";
            case PAYMENT_CONFIRMATION -> "Pago confirmado";
            case PAYMENT_FAILED -> "Problema con tu pago";
            case CART_ABANDONED -> "¿Olvidaste algo en tu carrito?";
            case PASSWORD_RESET -> "Restablecimiento de contraseña";
            case WELCOME -> "Bienvenido a EmerBV Store";
            case PRODUCT_BACK_IN_STOCK -> "Producto disponible nuevamente";
            case SPECIAL_OFFER -> "Oferta especial para ti";
            default -> "Notificación";
        };
    }

    /**
     * Retorna un mensaje predeterminado según el tipo de notificación
     */
    private String getDefaultMessage(NotificationType type) {
        return switch (type) {
            case ORDER_CONFIRMATION -> "Gracias por tu compra. Hemos recibido tu pedido y lo estamos procesando.";
            case ORDER_SHIPPED -> "Tu pedido ha sido enviado y está en camino.";
            case ORDER_DELIVERED -> "Tu pedido ha sido entregado. ¡Esperamos que disfrutes de tus productos!";
            case ORDER_CANCELLED -> "Tu pedido ha sido cancelado. Si tienes alguna pregunta, contáctanos.";
            case PAYMENT_CONFIRMATION -> "Hemos recibido tu pago correctamente.";
            case PAYMENT_FAILED -> "Ha habido un problema con tu pago. Por favor, verifica tus datos.";
            case CART_ABANDONED -> "Has dejado productos en tu carrito. ¿Deseas completar tu compra?";
            case PASSWORD_RESET -> "Has solicitado restablecer tu contraseña.";
            case WELCOME -> "Gracias por registrarte en nuestra tienda.";
            case PRODUCT_BACK_IN_STOCK -> "Un producto que te interesaba está disponible nuevamente.";
            case SPECIAL_OFFER -> "Tenemos una oferta especial para ti.";
            default -> "Gracias por tu interés en EmerBV Store.";
        };
    }
}
