package com.kchada.KchadaApp.follow;

import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.SocialEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FollowService {

    private final FollowRepository repository;
    private final ApplicationEventPublisher publisher;

    public FollowService(
            FollowRepository repository,
            ApplicationEventPublisher publisher) {

        this.repository = repository;
        this.publisher = publisher;
    }

    public Map<String, Boolean> follow(
            String actorId,
            String targetId,
            boolean enabled) {

        if (actorId.equals(targetId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "No puedes seguirte a ti mismo"
            );
        }

        FollowRepository.FollowResult result =
                repository.setFollow(
                        actorId,
                        targetId,
                        enabled
                );

        if (result.changed() && result.following()) {

            publisher.publishEvent(
                    new SocialEvent(
                            SocialEvent.Type.USER_FOLLOWED,
                            actorId,
                            targetId,
                            null
                    )
            );
        }

        return Map.of(
                "following",
                result.following()
        );
    }

    public Map<String, Boolean> myState(
            String actorId,
            String targetId) {

        return Map.of(
                "following",
                repository.isFollowing(actorId, targetId)
        );
    }

    public FollowRepository.FollowStats stats(
            String userId) {

        return repository.stats(userId);
    }

    public List<String> followers(
            String userId,
            int limit) {

        validateLimit(limit);

        return repository.followers(userId, limit);
    }

    public List<String> following(
            String userId,
            int limit) {

        validateLimit(limit);

        return repository.following(userId, limit);
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
