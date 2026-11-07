package com.kchada.KchadaApp.chat;

import com.google.cloud.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record ChatDocument(

        String id,
        String postId,
        String buyerId,
        String sellerId,
        String lastMessage,
        String lastSenderId,
        long messageCount,
        long buyerLastReadSequence,
        long sellerLastReadSequence,
        long unreadBuyerCount,
        long unreadSellerCount,
        Date createdAt,
        Date updatedAt

) {

    public Map<String, Object> toMap() {

        Map<String, Object> data = new HashMap<>();

        data.put("postId", postId);
        data.put("buyerId", buyerId);
        data.put("sellerId", sellerId);
        data.put("lastMessage", lastMessage);
        data.put("lastSenderId", lastSenderId);
        data.put("messageCount", messageCount);
        data.put("buyerLastReadSequence", buyerLastReadSequence);
        data.put("sellerLastReadSequence", sellerLastReadSequence);
        data.put("unreadBuyerCount", unreadBuyerCount);
        data.put("unreadSellerCount", unreadSellerCount);
        data.put("createdAt", createdAt);
        data.put("updatedAt", updatedAt);

        return data;
    }

    public static ChatDocument fromSnapshot(
            DocumentSnapshot snapshot) {

        if (!snapshot.exists()) {
            return null;
        }

        return new ChatDocument(
                snapshot.getId(),
                snapshot.getString("postId"),
                snapshot.getString("buyerId"),
                snapshot.getString("sellerId"),
                snapshot.getString("lastMessage"),
                snapshot.getString("lastSenderId"),
                snapshot.getLong("messageCount"),
                snapshot.getLong("buyerLastReadSequence"),
                snapshot.getLong("sellerLastReadSequence"),
                snapshot.getLong("unreadBuyerCount"),
                snapshot.getLong("unreadSellerCount"),
                snapshot.getDate("createdAt"),
                snapshot.getDate("updatedAt")
        );
    }

    public boolean hasParticipant(String userId) {

        return buyerId.equals(userId)
                || sellerId.equals(userId);
    }

    public String partnerOf(String userId) {

        return buyerId.equals(userId)
                ? sellerId
                : buyerId;
    }

    public ChatResponse responseFor(String userId) {

        long unread = buyerId.equals(userId)
                ? unreadBuyerCount
                : unreadSellerCount;

        return new ChatResponse(
                id,
                postId,
                buyerId,
                sellerId,
                partnerOf(userId),
                lastMessage,
                lastSenderId,
                messageCount,
                unread,
                createdAt,
                updatedAt
        );
    }

    public ChatDocument withMessage(
            String senderId,
            String text,
            Date now) {

        boolean fromBuyer = buyerId.equals(senderId);

        return new ChatDocument(
                id,
                postId,
                buyerId,
                sellerId,
                text,
                senderId,
                messageCount + 1,
                fromBuyer
                        ? messageCount + 1
                        : buyerLastReadSequence,
                fromBuyer
                        ? sellerLastReadSequence
                        : messageCount + 1,
                unreadBuyerCount + (fromBuyer ? 0 : 1),
                unreadSellerCount + (fromBuyer ? 1 : 0),
                createdAt,
                now
        );
    }

    public ChatDocument markRead(String userId) {

        boolean buyer = buyerId.equals(userId);

        return new ChatDocument(
                id,
                postId,
                buyerId,
                sellerId,
                lastMessage,
                lastSenderId,
                messageCount,
                buyer ? messageCount : buyerLastReadSequence,
                buyer ? sellerLastReadSequence : messageCount,
                buyer ? 0 : unreadBuyerCount,
                buyer ? unreadSellerCount : 0,
                createdAt,
                updatedAt
        );
    }

    public boolean isReadByRecipient(
            String senderId,
            long sequence) {

        if (buyerId.equals(senderId)) {
            return sequence <= sellerLastReadSequence;
        }

        return sequence <= buyerLastReadSequence;
    }
}
