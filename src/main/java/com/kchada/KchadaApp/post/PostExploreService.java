package com.kchada.KchadaApp.post;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.FirestoreOperations;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PostExploreService {

    private final Firestore firestore;
    private final FirestoreOperations operations;

    public PostExploreService(
            Firestore firestore,
            FirestoreOperations operations) {

        this.firestore = firestore;
        this.operations = operations;
    }

    public List<PostResponse> explore(
            String category,
            String department,
            int limit) {

        if (limit < 1 || limit > 50) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El límite debe estar entre 1 y 50"
            );
        }

        Query query = firestore.collection("posts")
                .whereEqualTo("status", "ACTIVE");

        if (category != null && !category.isBlank()) {
            query = query.whereEqualTo(
                    "category",
                    category.trim()
            );
        }

        if (department != null && !department.isBlank()) {
            query = query.whereEqualTo(
                    "department",
                    department.trim()
            );
        }

        query = query
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .limit(limit);

        QuerySnapshot snapshot =
                operations.await(query.get());

        List<PostResponse> results = new ArrayList<>();

        for (QueryDocumentSnapshot document
                : snapshot.getDocuments()) {

            PostDocument post =
                    PostDocument.fromSnapshot(document);

            results.add(
                    PostResponse.fromDocument(post)
            );
        }

        return results;
    }
}