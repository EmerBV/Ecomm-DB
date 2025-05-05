package com.emerbv.ecommdb.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Configuración del sistema de notificaciones
 */
@Configuration
@EnableAsync
public class NotificationConfig {

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String mailAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String mailStartTls;

    /**
     * Configuración del executor para procesar notificaciones en background
     */
    @Bean(name = "notificationTaskExecutor")
    public TaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Número base de hilos
        executor.setMaxPoolSize(10); // Número máximo de hilos
        executor.setQueueCapacity(25); // Tamaño de la cola de tareas
        executor.setThreadNamePrefix("Notification-");
        executor.initialize();
        return executor;
    }

    /**
     * Configuración del Executor para tareas programadas
     */
    @Bean(name = "scheduledTaskExecutor")
    public Executor scheduledTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("Scheduled-");
        executor.initialize();
        return executor;
    }

    /**
     * Configuración del JavaMailSender para envío de emails
     */
    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);

        // Configurar usuario y contraseña si están disponibles
        if (mailUsername != null && !mailUsername.isEmpty()) {
            mailSender.setUsername(mailUsername);
        }
        if (mailPassword != null && !mailPassword.isEmpty()) {
            mailSender.setPassword(mailPassword);
        }

        // Propiedades adicionales
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", mailAuth);
        props.put("mail.smtp.starttls.enable", mailStartTls);
        props.put("mail.debug", "false"); // Cambiar a true para depuración

        return mailSender;
    }

    /**
     * Configuración del resolver de plantillas de Thymeleaf
     */
    @Bean
    public ITemplateResolver templateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/notifications/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(true);
        return templateResolver;
    }

    /**
     * Configuración del motor de plantillas de Thymeleaf
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }
}
