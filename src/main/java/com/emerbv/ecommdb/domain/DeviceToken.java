package com.emerbv.ecommdb.domain;

import com.emerbv.ecommdb.model.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class DeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum DeviceType {
        IOS,
        ANDROID
    }
} 