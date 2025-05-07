package com.emerbv.ecommdb.service.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.emerbv.ecommdb.domain.DeviceToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PushNotificationService {

    public void sendPushNotification(DeviceToken deviceToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(deviceToken.getToken())
                    .build();

            String response = FirebaseMessaging.getInstance().sendAsync(message).get();
            log.info("Successfully sent message: {}", response);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error sending push notification", e);
            Thread.currentThread().interrupt();
        }
    }

    public void sendPushNotificationToMultipleDevices(Iterable<DeviceToken> deviceTokens, String title, String body) {
        deviceTokens.forEach(token -> sendPushNotification(token, title, body));
    }
} 