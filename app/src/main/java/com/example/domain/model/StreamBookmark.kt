package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StreamBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val streamUrl: String = "",
    val category: String = "Live",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(authenticatedUid: String): Map<String, Any> {
        return mapOf(
            "userId" to authenticatedUid,
            "title" to title.trim(),
            "streamUrl" to streamUrl.trim(),
            "category" to category.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}

data class ArchitectureModuleInfo(
    val id: String,
    val title: String,
    val packageName: String,
    val summary: String,
    val route: String
)
