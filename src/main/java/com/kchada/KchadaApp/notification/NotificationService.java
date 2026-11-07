package com.kchada.KchadaApp.notification;

import com.kchada.KchadaApp.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationPushService pushService;

    public NotificationService(
            NotificationRepository repository,
            NotificationPushService pushService) {

        this.repository = repository;
        this.pushService = pushService;
    }

    public NotificationDocument create(
            String recipientId,
            String actorId,
            NotificationType type,
            String title,
            String body,
            String postId,
            String chatId) {

        if (recipientId == null
                || recipientId.equals(actorId)) {

            return null;
        }

        NotificationDocument notification =
                new NotificationDocument(
                        UUID.randomUUID().toString(),
                        recipientId,
                        actorId,
                        type,
                        title,
                        body,
                        postId,
                        chatId,
                        false,
                        new Date(),
                        null
                );

        repository.create(notification);

        pushService.send(notification);

        return notification;
    }

    public List<NotificationDocument> findMine(
            String userId,
            int limit,
            String after) {

        if (limit < 1 || limit > 50) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El límite debe estar entre 1 y 50"
            );
        }

        return repository.findByUser(
                userId,
                limit,
                after
        );
    }

    public Map<String, Long> unreadCount(String userId) {

        return Map.of(
                "unreadCount",
                repository.unreadCount(userId)
        );
    }

    public void markRead(
            String userId,
            String notificationId) {

        repository.markRead(userId, notificationId);
    }

    public Map<String, Integer> markAllRead(
            String userId) {

        return Map.of(
                "updated",
                repository.markAllRead(userId)
        );
    }

    public void delete(
            String userId,
            String notificationId) {

        repository.delete(userId, notificationId);
    }

    public void registerDevice(
            String userId,
            DeviceTokenRequest request) {

        repository.saveDeviceToken(
                userId,
                request.token().trim()
        );
    }

    public void unregisterDevice(
            String userId,
            DeviceTokenRequest request) {

        repository.deleteDeviceToken(
                userId,
                request.token().trim()
        );
    }
}