package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createBookmark_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = BookmarkRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBookmark(
                title = "ShareDabba Live Stream",
                streamUrl = "https://sharedabba.com/watch/live1",
                category = "Live"
            )
        }
        assertTrue(createResult.isSuccess)
        val bookmarkId = createResult.getOrThrow()
        assertTrue(bookmarkId.isNotEmpty())
    }

    @Test
    fun getUserBookmarks_authenticatedOwner_returnsMatchingBookmarks() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = BookmarkRepository(firestore)
        val bookmarkId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBookmark(
                title = "Creator Studio Stream",
                streamUrl = "https://sharedabba.com/watch/studio",
                category = "Creator"
            ).getOrThrow()
        }

        val listResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserBookmarks()
        }
        assertTrue(listResult.isSuccess)
        assertTrue(listResult.getOrThrow().any { it.id == bookmarkId })
    }

    @Test
    fun observeBookmarks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = BookmarkRepository(firestore)
        val bookmarkId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBookmark(
                title = "Realtime Stream Test",
                streamUrl = "https://sharedabba.com/watch/rt",
                category = "Tech"
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBookmarks(uid).first { list -> list.any { it.id == bookmarkId } }
        }
        assertTrue(emitted.any { it.id == bookmarkId })
    }

    @Test
    fun getBookmarkById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BookmarkRepository(firestore)
        val bookmarkId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createBookmark(
                title = "Alice Private Bookmark",
                streamUrl = "https://sharedabba.com/watch/alice",
                category = "Live"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BookmarkRepository(firestore)
        var caughtException: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getBookmarkById(aliceUid, bookmarkId)
            }
        } catch (e: Exception) {
            caughtException = e
        }
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeBookmarks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = BookmarkRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBookmarks(aliceUid).first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@sharedabba.test"
        const val BOB_EMAIL = "bob@sharedabba.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
