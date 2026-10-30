package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.domain.model.StreamBookmark
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing authenticated user stream bookmarks in Cloud Firestore.
 * Uses the provisioned custom database ID (R.string.firestore_database_id).
 */
class BookmarkRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeBookmarks(userId: String = requireUserId()): Flow<List<StreamBookmark>> = flow {
        val path = "users/$userId/bookmarks"
        emitAll(
            db.collection("users").document(userId).collection("bookmarks")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StreamBookmark::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createBookmark(
        title: String,
        streamUrl: String,
        category: String = "Live"
    ): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("bookmarks")
        val docRef = collectionRef.document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim(),
            "streamUrl" to streamUrl.trim(),
            "category" to category.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserBookmarks(): Result<List<StreamBookmark>> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("bookmarks")
        return try {
            val snapshot = collectionRef.whereEqualTo("userId", uid).get().await()
            val items = snapshot.toObjects(
                StreamBookmark::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, collectionRef.path)
            Result.failure(e)
        }
    }

    suspend fun getBookmarkById(ownerUid: String, bookmarkId: String): Result<StreamBookmark?> {
        val docRef = db.collection("users").document(ownerUid)
            .collection("bookmarks").document(bookmarkId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(
                snapshot.toObject(
                    StreamBookmark::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateBookmarkTitle(bookmark: StreamBookmark, newTitle: String): Result<Unit> {
        val uid = requireUserId()
        require(bookmark.id.isNotBlank()) { "Bookmark ID must not be blank for update" }
        requireNotNull(bookmark.createdAt) { "Bookmark createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid)
            .collection("bookmarks").document(bookmark.id)
        return try {
            docRef.set(
                bookmark.copy(
                    title = newTitle.trim(),
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBookmark(bookmarkId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("bookmarks").document(bookmarkId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
