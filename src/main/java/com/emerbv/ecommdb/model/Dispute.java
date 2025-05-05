package com.emerbv.ecommdb.model;

import com.emerbv.ecommdb.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String stripeDisputeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String paymentIntentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String reason; // Razón de la disputa: duplicate, fraudulent, product_not_received, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime evidenceSubmittedAt;

    private LocalDateTime dueBy; // Fecha límite para responder

    // Referencias a archivos de evidencia en Stripe
    private String receiptFileId;
    private String invoiceFileId;
    private String shippingDocumentationFileId;
    private String serviceDocumentationFileId;
    private String additionalFileId;
}
