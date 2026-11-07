package com.kchada.KchadaApp.profile;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.FirestoreOperations;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Map;

@Repository
public class ProfileRepository {

    private final Firestore firestore;
    private final FirestoreOperations operations;

    public ProfileRepository(
            Firestore firestore,
            FirestoreOperations operations) {

        this.firestore = firestore;
        this.operations = operations;
    }

    public ProfileDocument findById(String userId) {

        DocumentSnapshot snapshot = operations.await(
                firestore.collection("profiles")
                        .document(userId)
                        .get()
        );

        return ProfileDocument.fromSnapshot(
                userId,
                snapshot
        );
    }

    public void update(
            String userId,
            String fullName,
            ProfileDocument profile) {

        DocumentReference userRef =
                firestore.collection("users")
                        .document(userId);

        DocumentReference profileRef =
                firestore.collection("profiles")
                        .document(userId);

        operations.await(
                firestore.runTransaction(transaction -> {

                    DocumentSnapshot user =
                            transaction.get(userRef).get();

                    if (!user.exists()
                            || !"ACTIVE".equals(
                                    user.getString("status"))) {

                        throw new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        );
                    }

                    transaction.update(
                            userRef,
                            Map.of(
                                    "fullName", fullName,
                                    "updatedAt", new Date()
                            )
                    );

                    transaction.set(
                            profileRef,
                            profile.toMap()
                    );

                    return null;
                })
        );
    }

    public boolean changePassword(
            String userId,
            String expectedHash,
            String newHash) {

        DocumentReference reference =
                firestore.collection("users")
                        .document(userId);

        return operations.await(
                firestore.runTransaction(transaction -> {

                    DocumentSnapshot snapshot =
                            transaction.get(reference).get();

                    if (!snapshot.exists()) {
                        throw new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        );
                    }

                    String currentHash =
                            snapshot.getString("passwordHash");

                    if (!expectedHash.equals(currentHash)) {
                        return false;
                    }

                    Long currentVersion =
                            snapshot.getLong("tokenVersion");

                    if (currentVersion == null) {
                        throw new IllegalStateException(
                                "Versión de sesión inválida"
                        );
                    }

                    transaction.update(
                            reference,
                            Map.of(
                                    "passwordHash", newHash,
                                    "tokenVersion", currentVersion + 1,
                                    "updatedAt", new Date()
                            )
                    );

                    return true;
                })
        );
    }

    public long countPosts(
            String userId,
            String status) {

        return operations.await(
                firestore.collection("posts")
                        .whereEqualTo("ownerId", userId)
                        .whereEqualTo("status", status)
                        .count()
                        .get()
        ).getCount();
    }

    public long countSaved(String userId) {

        return operations.await(
                firestore.collection("users")
                        .document(userId)
                        .collection("savedPosts")
                        .count()
                        .get()
        ).getCount();
    }
}