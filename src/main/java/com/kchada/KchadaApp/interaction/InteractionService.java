package com.kchada.KchadaApp.interaction;

import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.SocialEvent;
import com.kchada.KchadaApp.post.PostDocument;
import com.kchada.KchadaApp.post.PostRepository;
import com.kchada.KchadaApp.post.PostResponse;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InteractionService {

    private final InteractionRepository repository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher publisher;

    public InteractionService(
            InteractionRepository repository,
            PostRepository postRepository,
            ApplicationEventPublisher publisher) {

        this.repository = repository;
        this.postRepository = postRepository;
        this.publisher = publisher;
    }

    public Map<String, Boolean> like(
            String postId,
            String userId,
            boolean enabled) {

        boolean wasLiked =
                repository.hasLike(postId, userId);

        boolean liked = repository.setLike(
                postId,
                userId,
                enabled
        );

        if (liked && !wasLiked) {

            PostDocument post = requirePost(postId);

            if (!post.ownerId().equals(userId)) {
                publisher.publishEvent(
                        new SocialEvent(
                                SocialEvent.Type.POST_LIKED,
                                userId,
                                post.ownerId(),
                                postId
                        )
                );
            }
        }

        return Map.of("liked", liked);
    }

    public Map<String, Boolean> save(
            String postId,
            String userId,
            boolean enabled) {

        boolean saved = repository.setSaved(
                postId,
                userId,
                enabled
        );

        return Map.of("saved", saved);
    }

    public Map<String, Boolean> myState(
            String postId,
            String userId) {

        requirePost(postId);

        return Map.of(
                "liked", repository.hasLike(postId, userId),
                "saved", repository.hasSaved(postId, userId)
        );
    }

    public List<PostResponse> savedPosts(
            String userId,
            int limit) {

        validateLimit(limit);

        return repository.findSaved(userId, limit);
    }

    public CommentResponse comment(
            String postId,
            String userId,
            CommentRequest request) {

        String text = request.text().trim();

        CommentResponse response =
                repository.createComment(
                        postId,
                        userId,
                        text
                );

        PostDocument post = requirePost(postId);

        if (!post.ownerId().equals(userId)) {
            publisher.publishEvent(
                    new SocialEvent(
                            SocialEvent.Type.POST_COMMENTED,
                            userId,
                            post.ownerId(),
                            postId
                    )
            );
        }

        return response;
    }

    public List<CommentResponse> comments(
            String postId,
            int limit) {

        validateLimit(limit);

        return repository.findComments(postId, limit);
    }

    public void deleteComment(
            String postId,
            String commentId,
            String userId) {

        repository.deleteComment(
                postId,
                commentId,
                userId
        );
    }

    private PostDocument requirePost(String postId) {

        return postRepository.findById(postId)
                .filter(post ->
                        post.status()
                                != com.kchada.KchadaApp.post.PostStatus.DELETED
                )
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Publicación no encontrada"
                        )
                );
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
