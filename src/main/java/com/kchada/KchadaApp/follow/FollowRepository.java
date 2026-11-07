package com.kchada.KchadaApp.follow;

import com.google.cloud.firestore.*;
import com.kchada.KchadaApp.common.ApiException;
import com.kchada.KchadaApp.common.FirestoreOperations;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class FollowRepository {

	private final Firestore firestore;
	private final FirestoreOperations operations;

	public FollowRepository(
			Firestore firestore,
			FirestoreOperations operations) {

		this.firestore = firestore;
		this.operations = operations;
	}

	public FollowResult setFollow(
			String actorId,
			String targetId,
			boolean enabled) {

		DocumentReference targetUser = firestore.collection("users")
				.document(targetId);

		DocumentReference followingRef = firestore.collection("users")
				.document(actorId)
				.collection("following")
				.document(targetId);

		DocumentReference followerRef = targetUser.collection("followers")
				.document(actorId);

		DocumentReference actorStats = firestore.collection("followStats")
				.document(actorId);

		DocumentReference targetStats = firestore.collection("followStats")
				.document(targetId);

		return operations.await(
				firestore.runTransaction(transaction -> {

					DocumentSnapshot target = transaction.get(targetUser).get();

					DocumentSnapshot relationship = transaction.get(followingRef).get();

					DocumentSnapshot actorCounters = transaction.get(actorStats).get();

					DocumentSnapshot targetCounters = transaction.get(targetStats).get();

					if (!target.exists()
							|| !"ACTIVE".equals(
									target.getString("status"))) {

						throw new ApiException(
								HttpStatus.NOT_FOUND,
								"Usuario no encontrado");
					}

					if (relationship.exists() == enabled) {
						return new FollowResult(
								enabled,
								false);
					}

					long difference = enabled ? 1L : -1L;

					if (enabled) {

						transaction.create(
								followingRef,
								Map.of(
										"userId", targetId,
										"createdAt", new Date()));

						transaction.create(
								followerRef,
								Map.of(
										"userId", actorId,
										"createdAt", new Date()));

					} else {

						transaction.delete(followingRef);
						transaction.delete(followerRef);
					}

					if (actorCounters.exists()) {

						transaction.update(
								actorStats,
								"followingCount",
								FieldValue.increment(difference));

					} else {

						transaction.set(
								actorStats,
								Map.of(
										"followersCount", 0L,
										"followingCount", 1L));
					}

					if (targetCounters.exists()) {

						transaction.update(
								targetStats,
								"followersCount",
								FieldValue.increment(difference));

					} else {

						transaction.set(
								targetStats,
								Map.of(
										"followersCount", 1L,
										"followingCount", 0L));
					}

					return new FollowResult(
							enabled,
							true);
				}));
	}

	public boolean isFollowing(
			String actorId,
			String targetId) {

		return operations.await(
				firestore.collection("users")
						.document(actorId)
						.collection("following")
						.document(targetId)
						.get())
				.exists();
	}

	public FollowStats stats(String userId) {

		DocumentSnapshot snapshot = operations.await(
				firestore.collection("followStats")
						.document(userId)
						.get());

		if (!snapshot.exists()) {
			return new FollowStats(0L, 0L);
		}

		return new FollowStats(
				snapshot.getLong("followersCount"),
				snapshot.getLong("followingCount"));
	}

	public List<String> followers(
			String userId,
			int limit) {

		QuerySnapshot snapshot = operations.await(
				firestore.collection("users")
						.document(userId)
						.collection("followers")
						.limit(limit)
						.get());

		return snapshot.getDocuments()
				.stream()
				.map(DocumentSnapshot::getId)
				.toList();
	}

	public List<String> following(
			String userId,
			int limit) {

		QuerySnapshot snapshot = operations.await(
				firestore.collection("users")
						.document(userId)
						.collection("following")
						.limit(limit)
						.get());

		return snapshot.getDocuments()
				.stream()
				.map(DocumentSnapshot::getId)
				.toList();
	}

	public record FollowResult(
			boolean following,
			boolean changed) {
	}

	public record FollowStats(
			long followersCount,
			long followingCount) {
	}
}
