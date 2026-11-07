package com.kchada.KchadaApp.notification;

import com.google.cloud.firestore.*;
import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.FirestoreOperations;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class NotificationRepository {

    private final Firestore firestore;
    private final FirestoreOperations operations;

    public NotificationRepository(
            Firestore firestore,
            FirestoreOperations operations) {

        this.firestore = firestore;
        this.operations = operations;
    }

    public NotificationDocument create(
            NotificationDocument notification) {

        DocumentReference reference =
                notifications(notification.recipientId())
                        .document(notification.id());

        operations.await(
                reference.create(notification.toMap())
        );

        return notification;
    }

    public List<NotificationDocument> findByUser(
            String userId,
            int limit,
            String after) {

        Query query = notifications(userId)
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(limit);

        if (after != null && !after.isBlank()) {

            DocumentSnapshot cursor = operations.await(
                    notifications(userId)
                            .document(after)
                            .get()
            );

            if (!cursor.exists()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Cursor inválido"
                );
            }

            query = query.startAfter(cursor);
        }

        QuerySnapshot snapshot =
                operations.await(query.get());

        List<NotificationDocument> result =
                new ArrayList<>();

        for (QueryDocumentSnapshot document
                : snapshot.getDocuments()) {

            result.add(
                    NotificationDocument.fromSnapshot(document)
            );
        }

        return result;
    }

    public long unreadCount(String userId) {

        return operations.await(
                notifications(userId)
                        .whereEqualTo("read", false)
                        .count()
                        .get()
        ).getCount();
    }

    public void markRead(
            String userId,
            String notificationId) {

        DocumentReference reference =
                notifications(userId)
                        .document(notificationId);

        operations.await(
                firestore.runTransaction(transaction -> {

                    DocumentSnapshot snapshot =
                            transaction.get(reference).get();

                    if (!snapshot.exists()) {
                        throw new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Notificación no encontrada"
                        );
                    }

                    if (!Boolean.TRUE.equals(
                            snapshot.getBoolean("read"))) {

                        transaction.update(
                                reference,
                                Map.of(
                                        "read", true,
                                        "readAt", new Date()
                                )
                        );
                    }

                    return null;
                })
        );
    }

    public int markAllRead(String userId) {

        int total = 0;

        while (true) {

            QuerySnapshot unread = operations.await(
                    notifications(userId)
                            .whereEqualTo("read", false)
                            .limit(200)
                            .get()
            );

            if (unread.isEmpty()) {
                return total;
            }

            WriteBatch batch = firestore.batch();

            Date now = new Date();

            for (QueryDocumentSnapshot document
                    : unread.getDocuments()) {

                batch.update(
                        document.getReference(),
                        Map.of(
                                "read", true,
                                "readAt", now
                        )
                );

                total++;
            }

            operations.await(batch.commit());
        }
    }

    public void delete(
            String userId,
            String notificationId) {

        DocumentReference reference =
                notifications(userId)
                        .document(notificationId);

        DocumentSnapshot snapshot =
                operations.await(reference.get());

        if (!snapshot.exists()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Notificación no encontrada"
            );
        }

        operations.await(reference.delete());
    }

    public void saveDeviceToken(
            String userId,
            String token) {

        DocumentReference reference =
                deviceTokens(userId)
                        .document(tokenId(token));

        operations.await(
                reference.set(
                        Map.of(
                                "token", token,
                                "updatedAt", new Date()
                        )
                )
        );
    }

    public void deleteDeviceToken(
            String userId,
            String token) {

        operations.await(
                deviceTokens(userId)
                        .document(tokenId(token))
                        .delete()
        );
    }

    public List<String> findDeviceTokens(String userId) {

        QuerySnapshot snapshot = operations.await(
                deviceTokens(userId)
                        .orderBy(
                                "updatedAt",
                                Query.Direction.DESCENDING
                        )
                        .limit(20)
                        .get()
        );

        return snapshot.getDocuments()
                .stream()
                .map(document -> document.getString("token"))
                .filter(token -> token != null && !token.isBlank())
                .toList();
    }

    public Set<String> findBuyersForPost(String postId) {

        QuerySnapshot snapshot = operations.await(
                firestore.collection("chats")
                        .whereEqualTo("postId", postId)
                        .limit(200)
                        .get()
        );

        Set<String> buyers = new HashSet<>();

        for (QueryDocumentSnapshot document
                : snapshot.getDocuments()) {

            String buyerId =
                    document.getString("buyerId");

            if (buyerId != null) {
                buyers.add(buyerId);
            }
        }

        return buyers;
    }

    private CollectionReference notifications(
            String userId) {

        return firestore.collection("notifications")
                .document(userId)
                .collection("items");
    }

    private CollectionReference deviceTokens(
            String userId) {

        return firestore.collection("users")
                .document(userId)
                .collection("deviceTokens");
    }

    private String tokenId(String token) {

        return UUID.nameUUIDFromBytes(
                token.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}
