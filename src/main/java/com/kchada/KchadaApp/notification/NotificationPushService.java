package com.kchada.KchadaApp.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationPushService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificationPushService.class
            );

    private final FirebaseMessaging messaging;
    private final NotificationRepository repository;

    public NotificationPushService(
            FirebaseApp firebaseApp,
            NotificationRepository repository) {

        this.messaging =
                FirebaseMessaging.getInstance(firebaseApp);

        this.repository = repository;
    }

    public void send(NotificationDocument notification) {

        List<String> tokens = repository.findDeviceTokens(
                notification.recipientId()
        );

        for (String token : tokens) {

            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(
                            com.google.firebase.messaging.Notification
                                    .builder()
                                    .setTitle(notification.title())
                                    .setBody(notification.body())
                                    .build()
                    )
                    .putData(
                            "notificationId",
                            notification.id()
                    )
                    .putData(
                            "type",
                            notification.type().name()
                    );

            if (notification.postId() != null) {

                builder.putData(
                        "postId",
                        notification.postId()
                );
            }

            if (notification.chatId() != null) {

                builder.putData(
                        "chatId",
                        notification.chatId()
                );
            }

            try {

                messaging.send(builder.build());

            } catch (FirebaseMessagingException exception) {

                log.warn(
                        "No se pudo enviar una notificación FCM: {}",
                        exception.getMessage()
                );
            }
        }
    }
}
