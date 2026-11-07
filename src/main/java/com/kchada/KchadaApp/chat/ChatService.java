package com.kchada.KchadaApp.chat;

import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.post.PostDocument;
import com.kchada.KchadaApp.post.PostRepository;
import com.kchada.KchadaApp.post.PostStatus;
import com.kchada.KchadaApp.user.UserDocument;
import com.kchada.KchadaApp.user.UserRepository;
import com.kchada.KchadaApp.user.UserStatus;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public ChatService(
            ChatRepository chatRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            ApplicationEventPublisher publisher) {

        this.chatRepository = chatRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.publisher = publisher;
    }

    public ChatResponse openChat(
            String buyerId,
            ChatRequest request) {

        PostDocument post = postRepository
                .findById(request.postId())
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Publicación no encontrada"
                        )
                );

        String sellerId = post.ownerId();

        if (buyerId.equals(sellerId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "No puedes iniciar un chat contigo mismo"
            );
        }

        ChatDocument existing = chatRepository
                .findExisting(post.id(), buyerId)
                .orElse(null);

        if (existing != null) {
            return existing.responseFor(buyerId);
        }

        if (post.status() != PostStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La publicación no está disponible"
            );
        }

        UserDocument seller = userRepository
                .findById(sellerId)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Vendedor no encontrado"
                        )
                );

        if (seller.status() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El vendedor no está disponible"
            );
        }

        ChatDocument chat = chatRepository.createOrGet(
                post.id(),
                buyerId,
                sellerId
        );

        return chat.responseFor(buyerId);
    }

    public List<ChatResponse> myChats(
            String userId,
            int limit) {

        validateLimit(limit);

        return chatRepository.findByUser(
                userId,
                limit
        );
    }

    public ChatResponse getChat(
            String chatId,
            String userId) {

        ChatDocument chat = requireChat(
                chatId,
                userId
        );

        return chat.responseFor(userId);
    }

    public List<MessageResponse> messages(
            String chatId,
            String userId,
            int limit,
            Long before) {

        validateLimit(limit);

        if (before != null && before < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La secuencia debe ser positiva"
            );
        }

        return chatRepository.findMessages(
                chatId,
                userId,
                limit,
                before
        );
    }

    public MessageResponse sendMessage(
            String chatId,
            String senderId,
            MessageRequest request) {

        String text = request.text().trim();

        ChatRepository.MessageResult result =
                chatRepository.sendMessage(
                        chatId,
                        senderId,
                        text
                );

        publisher.publishEvent(
                new ChatMessageSentEvent(
                        chatId,
                        result.message().id(),
                        result.postId(),
                        senderId,
                        result.recipientId()
                )
        );

        return result.message();
    }

    public ChatResponse markRead(
            String chatId,
            String userId) {

        return chatRepository.markRead(
                chatId,
                userId
        );
    }

    private ChatDocument requireChat(
            String chatId,
            String userId) {

        ChatDocument chat = chatRepository
                .findById(chatId)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Conversación no encontrada"
                        )
                );

        if (!chat.hasParticipant(userId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "No perteneces a esta conversación"
            );
        }

        return chat;
    }

    private void validateLimit(int limit) {

        if (limit < 1 || limit > 50) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El límite debe estar entre 1 y 50"
            );
        }
    }
}