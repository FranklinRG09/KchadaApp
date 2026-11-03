package com.kchada.KchadaApp.user;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;

import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {

    private final Firestore firestore;

    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<UserDocument> findById(String id) {

        DocumentReference reference =
                firestore.collection("users").document(id);

        DocumentSnapshot snapshot = await(reference.get());

        return Optional.ofNullable(
                UserDocument.fromSnapshot(snapshot)
        );
    }

    public Optional<UserDocument> findByEmail(String email) {

        DocumentReference emailReference =
                firestore.collection("userEmails")
                        .document(emailKey(email));

        DocumentSnapshot emailSnapshot =
                await(emailReference.get());

        if (!emailSnapshot.exists()) {
            return Optional.empty();
        }

        String userId = emailSnapshot.getString("userId");

        if (userId == null) {
            throw new IllegalStateException(
                    "Índice de usuario inconsistente"
            );
        }

        return findById(userId);
    }

    public boolean create(UserDocument user) {

        DocumentReference userReference =
                firestore.collection("users")
                        .document(user.id());

        DocumentReference emailReference =
                firestore.collection("userEmails")
                        .document(emailKey(user.email()));

        DocumentReference profileReference =
                firestore.collection("profiles")
                        .document(user.id());

        return await(firestore.runTransaction(transaction -> {

            DocumentSnapshot existing =
                    transaction.get(emailReference).get();

            if (existing.exists()) {
                return false;
            }

            transaction.create(
                    userReference,
                    user.toMap()
            );

            transaction.create(
                    emailReference,
                    Map.of("userId", user.id())
            );

            transaction.create(
                    profileReference,
                    Map.of(
                            "userId", user.id(),
                            "bio", "",
                            "photoUrl", "",
                            "department", ""
                    )
            );

            return true;
        }));
    }

    public void revokeSessions(String userId) {

        DocumentReference reference =
                firestore.collection("users")
                        .document(userId);

        await(firestore.runTransaction(transaction -> {

            DocumentSnapshot snapshot =
                    transaction.get(reference).get();

            if (!snapshot.exists()) {
                throw new IllegalStateException(
                        "El usuario no existe"
                );
            }

            Long currentVersion =
                    snapshot.getLong("tokenVersion");

            if (currentVersion == null) {
                throw new IllegalStateException(
                        "Versión de sesión no encontrada"
                );
            }

            transaction.update(
                    reference,
                    Map.of(
                            "tokenVersion", currentVersion + 1,
                            "updatedAt", new Date()
                    )
            );

            return null;
        }));
    }

    private String emailKey(String email) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    email.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "No se pudo procesar el correo",
                    exception
            );
        }
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
}