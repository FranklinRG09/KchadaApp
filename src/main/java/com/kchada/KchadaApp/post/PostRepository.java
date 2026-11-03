package com.kchada.KchadaApp.post;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

@Repository
public class PostRepository {

    private final Firestore firestore;

    public PostRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public void create(PostDocument post) {

        DocumentReference reference =
                firestore.collection("posts")
                        .document(post.id());

        await(reference.create(post.toMap()));
    }

    public Optional<PostDocument> findById(String id) {

        DocumentSnapshot snapshot = await(
                firestore.collection("posts")
                        .document(id)
                        .get()
        );

        return Optional.ofNullable(
                PostDocument.fromSnapshot(snapshot)
        );
    }

    public List<PostDocument> findActive(
            int limit,
            String after) {

        Query query = firestore.collection("posts")
                .whereEqualTo("status", "ACTIVE")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(limit);

        if (after != null && !after.isBlank()) {

            DocumentSnapshot cursor = await(
                    firestore.collection("posts")
                            .document(after)
                            .get()
            );

            if (!cursor.exists()) {
                throw new IllegalArgumentException(
                        "Cursor de paginación inválido"
                );
            }

            query = query.startAfter(cursor);
        }

        return executeQuery(query);
    }

    public List<PostDocument> findByOwner(
            String ownerId,
            int limit) {

        Query query = firestore.collection("posts")
                .whereEqualTo("ownerId", ownerId)
                .whereIn(
                        "status",
                        List.of("ACTIVE", "SOLD")
                )
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(limit);

        return executeQuery(query);
    }

    public MutationResult mutate(
            String id,
            String ownerId,
            Long baseVersion,
            UnaryOperator<PostDocument> operation) {

        DocumentReference reference =
                firestore.collection("posts")
                        .document(id);

        return await(firestore.runTransaction(transaction -> {

            DocumentSnapshot snapshot =
                    transaction.get(reference).get();

            if (!snapshot.exists()) {
                return new MutationResult(
                        MutationCode.NOT_FOUND,
                        null
                );
            }

            PostDocument current =
                    PostDocument.fromSnapshot(snapshot);

            if (current.status() == PostStatus.DELETED) {
                return new MutationResult(
                        MutationCode.NOT_FOUND,
                        null
                );
            }

            if (!current.ownerId().equals(ownerId)) {
                return new MutationResult(
                        MutationCode.FORBIDDEN,
                        null
                );
            }

            if (baseVersion != null
                    && current.version() != baseVersion) {

                return new MutationResult(
                        MutationCode.CONFLICT,
                        null
                );
            }

            PostDocument updated =
                    operation.apply(current);

            transaction.set(
                    reference,
                    updated.toMap()
            );

            return new MutationResult(
                    MutationCode.SUCCESS,
                    updated
            );
        }));
    }

    private List<PostDocument> executeQuery(Query query) {

        QuerySnapshot snapshot = await(query.get());

        List<PostDocument> posts = new ArrayList<>();

        for (QueryDocumentSnapshot document
                : snapshot.getDocuments()) {

            posts.add(
                    PostDocument.fromSnapshot(document)
            );
        }

        return posts;
    }

    private <T> T await(ApiFuture<T> future) {

        try {
            return future.get();

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Operación interrumpida",
                    exception
            );

        } catch (ExecutionException exception) {

            throw new IllegalStateException(
                    "Error de comunicación con Firestore",
                    exception
            );
        }
    }

    public enum MutationCode {
        SUCCESS,
        NOT_FOUND,
        FORBIDDEN,
        CONFLICT
    }

    public record MutationResult(
            MutationCode code,
            PostDocument post
    ) {
    }
}