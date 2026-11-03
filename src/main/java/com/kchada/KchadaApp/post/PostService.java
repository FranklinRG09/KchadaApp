package com.kchada.KchadaApp.post;

import com.kchada.KchadaApp.common.ApiException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PostService(
            PostRepository postRepository,
            ApplicationEventPublisher eventPublisher) {

        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
    }

    public PostResponse create(
            String ownerId,
            PostRequest request) {

        validateRequest(request);

        Date now = new Date();

        PostDocument post = new PostDocument(
                UUID.randomUUID().toString(),
                ownerId,
                request.title().trim(),
                request.description().trim(),
                toCents(request),
                request.category().trim(),
                request.condition(),
                request.department().trim(),
                normalizeImageUrl(request.imageUrl()),
                request.latitude(),
                request.longitude(),
                PostStatus.ACTIVE,
                1L,
                0L,
                0L,
                0L,
                now,
                now
        );

        postRepository.create(post);

        publish(post, PostChangedEvent.Action.CREATED);

        return PostResponse.fromDocument(post);
    }

    public List<PostResponse> findActive(
            int limit,
            String after) {

        validateLimit(limit);

        try {

            return postRepository.findActive(limit, after)
                    .stream()
                    .map(PostResponse::fromDocument)
                    .toList();

        } catch (IllegalArgumentException exception) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Cursor de paginación inválido"
            );
        }
    }

    public PostResponse findById(String id) {

        PostDocument post = postRepository.findById(id)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Publicación no encontrada"
                        )
                );

        if (post.status() == PostStatus.DELETED) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Publicación no encontrada"
            );
        }

        return PostResponse.fromDocument(post);
    }

    public List<PostResponse> findByOwner(
            String ownerId,
            int limit) {

        validateLimit(limit);

        return postRepository.findByOwner(ownerId, limit)
                .stream()
                .map(PostResponse::fromDocument)
                .toList();
    }

    public PostResponse update(
            String id,
            String ownerId,
            PostRequest request) {

        validateRequest(request);

        long priceCents = toCents(request);

        PostRepository.MutationResult result =
                postRepository.mutate(
                        id,
                        ownerId,
                        request.baseVersion(),
                        current -> current.withDetails(
                                new PostRequest(
                                        request.title(),
                                        request.description(),
                                        request.price(),
                                        request.category(),
                                        request.condition(),
                                        request.department(),
                                        normalizeImageUrl(
                                                request.imageUrl()
                                        ),
                                        request.latitude(),
                                        request.longitude(),
                                        request.baseVersion()
                                ),
                                priceCents
                        )
                );

        PostDocument updated = requireSuccess(result);

        publish(updated, PostChangedEvent.Action.UPDATED);

        return PostResponse.fromDocument(updated);
    }

    public PostResponse changeStatus(
            String id,
            String ownerId,
            PostStatusRequest request) {

        if (request.status() == PostStatus.DELETED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Utiliza DELETE para eliminar la publicación"
            );
        }

        PostRepository.MutationResult result =
                postRepository.mutate(
                        id,
                        ownerId,
                        request.baseVersion(),
                        current -> current.withStatus(
                                request.status()
                        )
                );

        PostDocument updated = requireSuccess(result);

        PostChangedEvent.Action action =
                updated.status() == PostStatus.SOLD
                        ? PostChangedEvent.Action.SOLD
                        : PostChangedEvent.Action.REACTIVATED;

        publish(updated, action);

        return PostResponse.fromDocument(updated);
    }

    public void delete(
            String id,
            String ownerId,
            Long baseVersion) {

        PostRepository.MutationResult result =
                postRepository.mutate(
                        id,
                        ownerId,
                        baseVersion,
                        current -> current.withStatus(
                                PostStatus.DELETED
                        )
                );

        PostDocument deleted = requireSuccess(result);

        publish(deleted, PostChangedEvent.Action.DELETED);
    }

    private PostDocument requireSuccess(
            PostRepository.MutationResult result) {

        return switch (result.code()) {

            case SUCCESS -> result.post();

            case NOT_FOUND -> throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Publicación no encontrada"
            );

            case FORBIDDEN -> throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "No puedes modificar esta publicación"
            );

            case CONFLICT -> throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La publicación fue modificada. Actualiza los datos"
            );
        };
    }

    private void validateRequest(PostRequest request) {

        validateCoordinates(
                request.latitude(),
                request.longitude()
        );

        normalizeImageUrl(request.imageUrl());
    }

    private void validateCoordinates(
            Double latitude,
            Double longitude) {

        if (latitude == null && longitude == null) {
            return;
        }

        if (latitude == null || longitude == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Debes proporcionar latitud y longitud"
            );
        }

        if (!Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90
                || latitude > 90
                || longitude < -180
                || longitude > 180) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Las coordenadas no son válidas"
            );
        }
    }

    private String normalizeImageUrl(String imageUrl) {

        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String value = imageUrl.trim();

        try {

            URI uri = URI.create(value);

            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {

                throw new IllegalArgumentException();
            }

            return value;

        } catch (IllegalArgumentException exception) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La imagen debe utilizar una URL HTTPS válida"
            );
        }
    }

    private long toCents(PostRequest request) {

        return request.price()
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact();
    }

    private void validateLimit(int limit) {

        if (limit < 1 || limit > 50) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El límite debe estar entre 1 y 50"
            );
        }
    }

    private void publish(
            PostDocument post,
            PostChangedEvent.Action action) {

        eventPublisher.publishEvent(
                new PostChangedEvent(
                        post.id(),
                        post.ownerId(),
                        action
                )
        );
    }
}
