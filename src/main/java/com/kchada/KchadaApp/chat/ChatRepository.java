package com.kchada.KchadaApp.chat;

import com.google.cloud.firestore.*;
import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.FirestoreOperations;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ChatRepository {

    private final Firestore firestore;
    private final FirestoreOperations operations;

    public ChatRepository(
            Firestore firestore,
            FirestoreOperations operations) {

        this.firestore = firestore;
        this.operations = operations;
    }

    public Optional<ChatDocument> findExisting(
            String postId,
            String buyerId) {

        return findById(chatId(postId, buyerId));
    }

    public Optional<ChatDocument> findById(String id) {

        DocumentSnapshot snapshot = operations.await(
                firestore.collection("chats")
                        .document(id)
                        .get()
        );

        return Optional.ofNullable(
                ChatDocument.fromSnapshot(snapshot)
        );
    }

    public ChatDocument createOrGet(
            String postId,
            String buyerId,
            String sellerId) {

        String id = chatId(postId, buyerId);

        DocumentReference chatRef =
                firestore.collection("chats").document(id);

        return operations.await(
                firestore.runTransaction(transaction -> {

                    DocumentSnapshot existing =
                            transaction.get(chatRef).get();

                    if (existing.exists()) {
                        return ChatDocument.fromSnapshot(existing);
                    }

                    Date now = new Date();

                    ChatDocument chat = new ChatDocument(
                            id,
                            postId,
                            buyerId,
                            sellerId,
                            "",
                            null,
                            0L,
                            0L,
                            0L,
                            0L,
                            0L,
                            now,
                            now
                    );

                    DocumentReference buyerRef =
                            userChatRef(buyerId, id);

                    DocumentReference sellerRef =
                            userChatRef(sellerId, id);

                    transaction.create(
                            chatRef,
                            chat.toMap()
                    );

                    transaction.create(
                            buyerRef,
                            chat.responseFor(buyerId).toMap()
                    );

                    transaction.create(
                            sellerRef,
                            chat.responseFor(sellerId).toMap()
                    );

                    return chat;
                })
        );
    }

    public List<ChatResponse> findByUser(
            String userId,
            int limit) {

        QuerySnapshot snapshot = operations.await(
                firestore.collection("userChats")
                        .document(userId)
                        .collection("items")
                        .orderBy(
                                "updatedAt",
                                Query.Direction.DESCENDING
                        )
                        .limit(limit)
                        .get()
        );

        List<ChatResponse> chats = new ArrayList<>();

        for (QueryDocumentSnapshot document
                : snapshot.getDocuments()) {

            chats.add(
                    ChatResponse.fromSnapshot(document)
            );
        }

        return chats;
    }

    public MessageResult sendMessage(
            String chatId,
            String senderId,
            String text) {

        DocumentReference chatRef =
                firestore.collection("chats")
                        .document(chatId);

        String messageId = UUID.randomUUID().toString();

        DocumentReference messageRef =
                chatRef.collection("messages")
                        .document(messageId);

        return operations.await(
                firestore.runTransaction(transaction -> {

                    DocumentSnapshot snapshot =
                            transaction.get(chatRef).get();

                    if (!snapshot.exists()) {
                        throw new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Conversación no encontrada"
                        );
                    }

                    ChatDocument current =
                            ChatDocument.fromSnapshot(snapshot);

                    if (!current.hasParticipant(senderId)) {
                        throw new ApiException(
                                HttpStatus.FORBIDDEN,
                                "No perteneces a esta conversación"
                        );
                    }

                    Date now = new Date();

                    ChatDocument updated =
                            current.withMessage(
                                    senderId,
                                    text,
                                    now
                            );

                    long sequence = updated.messageCount();

                    MessageResponse message =
                            new MessageResponse(
                                    messageId,
                                    chatId,
                                    senderId,
                                    text,
                                    sequence,
                                    false,
                                    now
                            );

                    transaction.create(
                            messageRef,
                            java.util.Map.of(
                                    "senderId", senderId,
                                    "text", text,
                                    "sequence", sequence,
                                    "createdAt", now
                            )
                    );

                    transaction.set(
                            chatRef,
                            updated.toMap()
                    );

                    transaction.set(
                            userChatRef(
                                    updated.buyerId(),
                                    chatId
                            ),
                            updated.responseFor(
                                    updated.buyerId()
                            ).toMap()
                    );

                    transaction.set(
                            userChatRef(
                                    updated.sellerId(),
                                    chatId
                            ),
                            updated.responseFor(
                                    updated.sellerId()
                            ).toMap()
                    );

                    return new MessageResult(
                            message,
                            updated.postId(),
                            updated.partnerOf(senderId)
                    );
                })
        );
    }

    public List<MessageResponse> findMessages(
            String chatId,
            String userId,
            int limit,
            Long before) {

        ChatDocument chat = findById(chatId)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Conversación no encontrada"
                        )
                );

        requireParticipant(chat, userId);

        Query query = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy(
                        "sequence",
                        Query.Direction.DESCENDING
                )
                .limit(limit);

        if (before != null) {
            query = query.whereLessThan(
                    "sequence",
                    before
            );
        }

        QuerySnapshot snapshot =
                operations.await(query.get());

        List<MessageResponse> messages =
                new ArrayList<>();

        for (QueryDocumentSnapshot document
                : snapshot.getDocuments()) {

            String senderId =
                    document.getString("senderId");

            long sequence =
                    document.getLong("sequence");

            messages.add(
                    new MessageResponse(
                            document.getId(),
                            chatId,
                            senderId,
                            document.getString("text"),
                            sequence,
                            chat.isReadByRecipient(
                                    senderId,
                                    sequence
                            ),
                            document.getDate("createdAt")
                    )
            );
        }

        Collections.reverse(messages);

        return messages;
    }

    public ChatResponse markRead(
            String chatId,
            String userId) {

        DocumentReference chatRef =
                firestore.collection("chats")
                        .document(chatId);

        return operations.await(
                firestore.runTransaction(transaction -> {

                    DocumentSnapshot snapshot =
                            transaction.get(chatRef).get();

                    if (!snapshot.exists()) {
                        throw new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Conversación no encontrada"
                        );
                    }

                    ChatDocument current =
                            ChatDocument.fromSnapshot(snapshot);

                    requireParticipant(current, userId);

                    ChatDocument updated =
                            current.markRead(userId);

                    transaction.set(
                            chatRef,
                            updated.toMap()
                    );

                    transaction.set(
                            userChatRef(userId, chatId),
                            updated.responseFor(userId).toMap()
                    );

                    return updated.responseFor(userId);
                })
        );
    }

    private DocumentReference userChatRef(
            String userId,
            String chatId) {

        return firestore.collection("userChats")
                .document(userId)
                .collection("items")
                .document(chatId);
    }

    private void requireParticipant(
            ChatDocument chat,
            String userId) {

        if (!chat.hasParticipant(userId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "No perteneces a esta conversación"
            );
        }
    }

    private String chatId(
            String postId,
            String buyerId) {

        String value = postId + ":" + buyerId;

        return UUID.nameUUIDFromBytes(
                value.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    public record MessageResult(
            MessageResponse message,
            String postId,
            String recipientId
    ) {
    }
}