package com.kchada.KchadaApp.interaction;

import com.google.cloud.firestore.*;
import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.FirestoreOperations;
import com.kchada.KchadaApp.post.PostDocument;
import com.kchada.KchadaApp.post.PostResponse;
import com.kchada.KchadaApp.post.PostStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class InteractionRepository {

	private final Firestore firestore;
	private final FirestoreOperations operations;

	public InteractionRepository(
			Firestore firestore,
			FirestoreOperations operations) {

		this.firestore = firestore;
		this.operations = operations;
	}

	public boolean setLike(
			String postId,
			String userId,
			boolean enabled) {

		return setFlag(
				postId,
				userId,
				enabled,
				true);
	}

	public boolean setSaved(
			String postId,
			String userId,
			boolean enabled) {

		return setFlag(
				postId,
				userId,
				enabled,
				false);
	}

	private boolean setFlag(
			String postId,
			String userId,
			boolean enabled,
			boolean like) {

		DocumentReference postRef = firestore.collection("posts")
				.document(postId);

		DocumentReference flagRef = like
				? postRef.collection("likes").document(userId)
				: firestore.collection("users")
						.document(userId)
						.collection("savedPosts")
						.document(postId);

		String counter = like ? "likeCount" : "saveCount";

		return operations.await(
				firestore.runTransaction(transaction -> {

					DocumentSnapshot post = transaction.get(postRef).get();

					DocumentSnapshot flag = transaction.get(flagRef).get();

					if (!post.exists()) {
						throw new ApiException(
								HttpStatus.NOT_FOUND,
								"Publicación no encontrada");
					}

					PostStatus status = PostStatus.valueOf(
							post.getString("status"));

					if (enabled && status != PostStatus.ACTIVE) {
						throw new ApiException(
								HttpStatus.CONFLICT,
								"La publicación no está disponible");
					}

					if (flag.exists() == enabled) {
						return enabled;
					}

					if (enabled) {

						transaction.create(
								flagRef,
								Map.of(
										"userId", userId,
										"postId", postId,
										"createdAt", new Date()));

					} else {
						transaction.delete(flagRef);
					}

					transaction.update(
							postRef,
							counter,
							FieldValue.increment(
									enabled ? 1L : -1L));

					return enabled;
				}));
	}

	public boolean hasLike(String postId, String userId) {

		return operations.await(
				firestore.collection("posts")
						.document(postId)
						.collection("likes")
						.document(userId)
						.get())
				.exists();
	}

	public boolean hasSaved(String postId, String userId) {

		return operations.await(
				firestore.collection("users")
						.document(userId)
						.collection("savedPosts")
						.document(postId)
						.get())
				.exists();
	}

	public List<PostResponse> findSaved(
			String userId,
			int limit) {

		QuerySnapshot saved = operations.await(
				firestore.collection("users")
						.document(userId)
						.collection("savedPosts")
						.orderBy(
								"createdAt",
								Query.Direction.DESCENDING)
						.limit(limit)
						.get());

		List<PostResponse> results = new ArrayList<>();

		for (QueryDocumentSnapshot item : saved.getDocuments()) {

			DocumentSnapshot snapshot = operations.await(
					firestore.collection("posts")
							.document(item.getId())
							.get());

			if (!snapshot.exists()) {
				continue;
			}

			PostDocument post = PostDocument.fromSnapshot(snapshot);

			if (post.status() != PostStatus.DELETED) {
				results.add(PostResponse.fromDocument(post));
			}
		}

		return results;
	}

	public CommentResponse createComment(
			String postId,
			String authorId,
			String text) {

		DocumentReference postRef = firestore.collection("posts")
				.document(postId);

		String commentId = UUID.randomUUID().toString();

		DocumentReference commentRef = postRef.collection("comments")
				.document(commentId);

		Date now = new Date();

		return operations.await(
				firestore.runTransaction(transaction -> {

					DocumentSnapshot post = transaction.get(postRef).get();

					if (!post.exists()
							|| "DELETED".equals(
									post.getString("status"))) {

						throw new ApiException(
								HttpStatus.NOT_FOUND,
								"Publicación no encontrada");
					}

					transaction.create(
							commentRef,
							Map.of(
									"postId", postId,
									"authorId", authorId,
									"text", text,
									"createdAt", now,
									"deleted", false));

					transaction.update(
							postRef,
							"commentCount",
							FieldValue.increment(1L));

					return new CommentResponse(
							commentId,
							postId,
							authorId,
							text,
							now);
				}));
	}

	public List<CommentResponse> findComments(
			String postId,
			int limit) {

		DocumentSnapshot post = operations.await(
				firestore.collection("posts")
						.document(postId)
						.get());

		if (!post.exists()
				|| "DELETED".equals(post.getString("status"))) {

			throw new ApiException(
					HttpStatus.NOT_FOUND,
					"Publicación no encontrada");
		}

		QuerySnapshot snapshot = operations.await(
				firestore.collection("posts")
						.document(postId)
						.collection("comments")
						.whereEqualTo("deleted", false)
						.orderBy(
								"createdAt",
								Query.Direction.DESCENDING)
						.limit(limit)
						.get());

		List<CommentResponse> comments = new ArrayList<>();

		for (QueryDocumentSnapshot document : snapshot.getDocuments()) {

			comments.add(
					new CommentResponse(
							document.getId(),
							postId,
							document.getString("authorId"),
							document.getString("text"),
							document.getDate("createdAt")));
		}

		return comments;
	}

	public void deleteComment(
			String postId,
			String commentId,
			String userId) {

		DocumentReference postRef = firestore.collection("posts")
				.document(postId);

		DocumentReference commentRef = postRef.collection("comments")
				.document(commentId);

		operations.await(
				firestore.runTransaction(transaction -> {

					DocumentSnapshot post = transaction.get(postRef).get();

					DocumentSnapshot comment = transaction.get(commentRef).get();

					if (!post.exists() || !comment.exists()
							|| "DELETED".equals(
									post.getString("status"))
							|| Boolean.TRUE.equals(
									comment.getBoolean("deleted"))) {

						throw new ApiException(
								HttpStatus.NOT_FOUND,
								"Comentario no encontrado");
					}

					String authorId = comment.getString("authorId");

					String ownerId = post.getString("ownerId");

					if (!userId.equals(authorId)
							&& !userId.equals(ownerId)) {

						throw new ApiException(
								HttpStatus.FORBIDDEN,
								"No puedes eliminar este comentario");
					}

					transaction.update(
							commentRef,
							Map.of(
									"deleted", true,
									"text", "",
									"deletedAt", new Date()));

					transaction.update(
							postRef,
							"commentCount",
							FieldValue.increment(-1L));

					return null;
				}));
	}
}
