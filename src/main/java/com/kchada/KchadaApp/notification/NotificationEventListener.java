package com.kchada.KchadaApp.notification;

import com.kchada.KchadaApp.chat.ChatMessageSentEvent;
import com.kchada.KchadaApp.common.SocialEvent;
import com.kchada.KchadaApp.post.PostChangedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificationEventListener.class
            );

    private final NotificationService notificationService;
    private final NotificationRepository repository;

    public NotificationEventListener(
            NotificationService notificationService,
            NotificationRepository repository) {

        this.notificationService = notificationService;
        this.repository = repository;
    }

    @Async("notificationExecutor")
    @EventListener
    public void onSocialEvent(SocialEvent event) {

        safely(() -> {

            switch (event.type()) {

                case POST_LIKED -> notificationService.create(
                        event.recipientId(),
                        event.actorId(),
                        NotificationType.LIKE,
                        "Nuevo me gusta",
                        "A alguien le gustó tu publicación",
                        event.postId(),
                        null
                );

                case POST_COMMENTED -> notificationService.create(
                        event.recipientId(),
                        event.actorId(),
                        NotificationType.COMMENT,
                        "Nuevo comentario",
                        "Alguien comentó tu publicación",
                        event.postId(),
                        null
                );

                case USER_FOLLOWED -> notificationService.create(
                        event.recipientId(),
                        event.actorId(),
                        NotificationType.FOLLOW,
                        "Nuevo seguidor",
                        "Alguien comenzó a seguirte",
                        null,
                        null
                );
            }
        });
    }

    @Async("notificationExecutor")
    @EventListener
    public void onChatMessage(ChatMessageSentEvent event) {

        safely(() -> {

            notificationService.create(
                    event.recipientId(),
                    event.senderId(),
                    NotificationType.MESSAGE,
                    "Nuevo mensaje",
                    "Tienes un nuevo mensaje en Kchada",
                    event.postId(),
                    event.chatId()
            );
        });
    }

    @Async("notificationExecutor")
    @EventListener
    public void onPostChanged(PostChangedEvent event) {

        if (event.action() != PostChangedEvent.Action.SOLD) {
            return;
        }

        safely(() -> {

            for (String buyerId :
                    repository.findBuyersForPost(event.postId())) {

                safely(() -> notificationService.create(
                        buyerId,
                        event.ownerId(),
                        NotificationType.POST_SOLD,
                        "Producto vendido",
                        "Un producto por el que consultaste fue vendido",
                        event.postId(),
                        null
                ));
            }
        });
    }

    private void safely(Runnable action) {

        try {

            action.run();

        } catch (RuntimeException exception) {

            log.error(
                    "No se pudo procesar una notificación",
                    exception
            );
        }
    }
}