package com.example.data.fixtures

/**
 * Authoritative ownership and provenance record attached to every ShareDabba video.
 * Stage 6 requirement: Every video must retain:
 * - The original video ID (`videoId`)
 * - The original uploader ID (`uploaderId`)
 * - The original share URL (`shareUrl`)
 * - The original ownership record (`VideoOwnershipRecord`)
 */
data class VideoOwnershipRecord(
    val videoId: String,
    val uploaderId: String,
    val uploaderHandle: String,
    val canonicalShareUrl: String,
    val storageAuthority: String = "SHAREDABBA_WEBSITE_AUTHORITATIVE_STORAGE",
    val originPlatform: String = "WEBSITE_UPLOAD_WATCHABLE_ON_ANDROID",
    val isPhoneOnlyStorage: Boolean = false
) {
    val isValidProvenance: Boolean
        get() = videoId.isNotBlank() &&
            uploaderId.isNotBlank() &&
            canonicalShareUrl.startsWith("https://") &&
            !isPhoneOnlyStorage
}

/**
 * DEVELOPMENT FIXTURES ONLY — SEPARATED FROM PRODUCTION REPOSITORIES.
 * Used exclusively to preview UI states before the live ShareDabba website backend
 * API endpoints are connected. Every model carries an explicit isDevFixture flag
 * alongside complete Stage 6 ownership provenance fields.
 */
data class VideoItemFixture(
    val id: String,
    val uploaderId: String = "usr_web_${id}",
    val title: String,
    val description: String,
    val category: String,
    val durationLabel: String = "04:15",
    val viewsLabel: String = "0 recorded views",
    val uploadedAgo: String = "Just now",
    val uploaderName: String,
    val uploaderHandle: String,
    val uploaderFollowers: String = "10.0K followers",
    val thumbnailUrl: String = "https://sharedabba.com/assets/thumbs/$id.jpg",
    val streamUrl: String,
    val shareUrl: String = "https://sharedabba.com/watch/$id",
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val isRecent: Boolean = false,
    val isMyUpload: Boolean = false,
    val isDevFixture: Boolean = true,
    val ownershipRecord: VideoOwnershipRecord = VideoOwnershipRecord(
        videoId = id,
        uploaderId = uploaderId,
        uploaderHandle = uploaderHandle,
        canonicalShareUrl = shareUrl
    ),
    val viewsFormatted: String = viewsLabel,
    val duration: String = durationLabel,
    val sharePath: String = shareUrl
) {
    fun hasCompleteProvenance(): Boolean {
        return id.isNotBlank() &&
            uploaderId.isNotBlank() &&
            shareUrl.startsWith("https://") &&
            ownershipRecord.videoId == id &&
            ownershipRecord.uploaderId == uploaderId &&
            ownershipRecord.canonicalShareUrl == shareUrl &&
            !ownershipRecord.isPhoneOnlyStorage
    }
}

data class CategoryFixture(
    val id: String,
    val name: String,
    val description: String,
    val videoCount: Int
)

data class DashboardMetricsFixture(
    val totalUploads: Int,
    val recordedViews: Long,
    val eligibleViews: Long,
    val totalEarningsDisplay: String,
    val pendingBalanceDisplay: String,
    val availableBalanceDisplay: String,
    val fixtureLabel: String = "DEVELOPMENT FIXTURE • NOT PRODUCTION FINANCIAL DATA"
) {
    val earningsDisplay: String get() = totalEarningsDisplay
}

data class EarningsRecordFixture(
    val id: String,
    val dateLabel: String,
    val videoTitle: String,
    val recordedViews: Int,
    val eligibleViews: Int,
    val estimatedAmountDisplay: String,
    val statusLabel: String = "ELIGIBLE VIEWS VERIFIED"
) {
    val earnedAmountDisplay: String get() = estimatedAmountDisplay
}

data class WithdrawalRecordFixture(
    val id: String,
    val referenceCode: String,
    val requestedDate: String,
    val methodLabel: String,
    val amountDisplay: String,
    val status: String // "Completed", "Pending"
) {
    val destinationLabel: String get() = methodLabel
    val requestedAt: String get() = requestedDate
}

data class NotificationItemFixture(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val category: String,
    val isUnread: Boolean = true,
    val targetRoute: String = "home"
) {
    val timestampLabel: String get() = timeAgo
    val message: String get() = body
}

data class SupportFaqItem(
    val id: String,
    val question: String,
    val answer: String
)

object DevFixturesCatalog {
    const val FIXTURE_BADGE_TEXT = "DEV FIXTURE • WEBSITE VIDEO CONTRACT READY"

    private const val SAMPLE_MP4_1 =
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    private const val SAMPLE_MP4_2 =
        "https://storage.googleapis.com/exoplayer-test-media-0/jazz_in_paris.mp3"

    val categories: List<CategoryFixture> = listOf(
        CategoryFixture("live", "Live Streams", "Low-latency creator broadcasts and events", 2),
        CategoryFixture("tech", "Tech & Architecture", "Kotlin, Compose, Media3, and cloud engineering", 2),
        CategoryFixture("creator", "Creator Originals", "Exclusive independent productions on ShareDabba", 2),
        CategoryFixture("gaming", "Gaming & Esports", "High-framerate tournament and gameplay streams", 1),
        CategoryFixture("education", "Tutorials & Deep Dives", "Step-by-step workshops and masterclasses", 1)
    )

    val videos: List<VideoItemFixture> = listOf(
        VideoItemFixture(
            id = "vid_featured_1",
            uploaderId = "usr_sharedabba_eng_01",
            title = "ShareDabba Next-Gen Streaming Architecture Keynote",
            description = "Deep dive into adaptive HLS/DASH delivery, Media3 ExoPlayer customization, and creator-first monetization on the ShareDabba platform.",
            category = "Tech & Architecture",
            durationLabel = "14:28",
            viewsLabel = "18.4K views",
            uploadedAgo = "2 hours ago",
            uploaderName = "ShareDabba Engineering",
            uploaderHandle = "@sharedabba_eng",
            uploaderFollowers = "42.5K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_featured_1.jpg",
            streamUrl = SAMPLE_MP4_1,
            shareUrl = "https://sharedabba.com/watch/vid_featured_1",
            isFeatured = true,
            isPopular = true,
            isRecent = true,
            isMyUpload = true
        ),
        VideoItemFixture(
            id = "vid_featured_2",
            uploaderId = "usr_aarav_studio_02",
            title = "Creator Studio Masterclass: Maximizing Eligible Views",
            description = "Understand how ShareDabba validates eligible watch sessions, optimizes ad slots, and synchronizes creator analytics.",
            category = "Creator Originals",
            durationLabel = "09:45",
            viewsLabel = "11.2K views",
            uploadedAgo = "5 hours ago",
            uploaderName = "Aarav Studio",
            uploaderHandle = "@aarav_creates",
            uploaderFollowers = "19.8K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_featured_2.jpg",
            streamUrl = SAMPLE_MP4_1,
            shareUrl = "https://sharedabba.com/watch/vid_featured_2",
            isFeatured = true,
            isPopular = true,
            isRecent = false,
            isMyUpload = false
        ),
        VideoItemFixture(
            id = "vid_recent_1",
            uploaderId = "usr_kotlin_media_03",
            title = "Building Custom Media3 Renderers on Android 16",
            description = "Walkthrough of hardware-accelerated video decoding, audio focus management, and smooth aspect-ratio transitions in Jetpack Compose.",
            category = "Tech & Architecture",
            durationLabel = "21:10",
            viewsLabel = "4.7K views",
            uploadedAgo = "1 day ago",
            uploaderName = "Kotlin Media Lab",
            uploaderHandle = "@kotlin_media",
            uploaderFollowers = "15.1K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_recent_1.jpg",
            streamUrl = SAMPLE_MP4_1,
            shareUrl = "https://sharedabba.com/watch/vid_recent_1",
            isFeatured = false,
            isPopular = false,
            isRecent = true,
            isMyUpload = true
        ),
        VideoItemFixture(
            id = "vid_recent_2",
            uploaderId = "usr_neonforge_04",
            title = "Live Indie Game Dev Session: Shader Pipelines",
            description = "Real-time procedural neon shader design and community Q&A streamed in 60fps.",
            category = "Live Streams",
            durationLabel = "48:12",
            viewsLabel = "9.3K views",
            uploadedAgo = "1 day ago",
            uploaderName = "NeonForge Live",
            uploaderHandle = "@neonforge",
            uploaderFollowers = "28.0K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_recent_2.jpg",
            streamUrl = SAMPLE_MP4_1,
            shareUrl = "https://sharedabba.com/watch/vid_recent_2",
            isFeatured = false,
            isPopular = true,
            isRecent = true,
            isMyUpload = false
        ),
        VideoItemFixture(
            id = "vid_pop_1",
            uploaderId = "usr_apex_arena_05",
            title = "Esports Regional Finals — Highlight Reel",
            description = "Top plays and caster commentary from the weekend tournament finals.",
            category = "Gaming & Esports",
            durationLabel = "12:04",
            viewsLabel = "31.9K views",
            uploadedAgo = "3 days ago",
            uploaderName = "Apex Arena India",
            uploaderHandle = "@apex_arena",
            uploaderFollowers = "64.2K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_pop_1.jpg",
            streamUrl = SAMPLE_MP4_1,
            shareUrl = "https://sharedabba.com/watch/vid_pop_1",
            isFeatured = false,
            isPopular = true,
            isRecent = false,
            isMyUpload = false
        ),
        VideoItemFixture(
            id = "vid_edu_1",
            uploaderId = "usr_lumina_film_06",
            title = "Color Grading Dark Neon Aesthetics for Creators",
            description = "Learn how to balance deep blacks, cyan highlights, and electric purple accents in your video workflow.",
            category = "Tutorials & Deep Dives",
            durationLabel = "16:50",
            viewsLabel = "7.6K views",
            uploadedAgo = "4 days ago",
            uploaderName = "Lumina Film School",
            uploaderHandle = "@lumina_film",
            uploaderFollowers = "12.4K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_edu_1.jpg",
            streamUrl = SAMPLE_MP4_2,
            shareUrl = "https://sharedabba.com/watch/vid_edu_1",
            isFeatured = false,
            isPopular = false,
            isRecent = true,
            isMyUpload = false
        ),
        VideoItemFixture(
            id = "vid_live_2",
            uploaderId = "usr_sharedabba_live_07",
            title = "Late Night Creator Roundtable & Channel Reviews",
            description = "Interactive panel reviewing community uploads and thumbnail strategies.",
            category = "Live Streams",
            durationLabel = "54:00",
            viewsLabel = "6.1K views",
            uploadedAgo = "5 days ago",
            uploaderName = "ShareDabba Community",
            uploaderHandle = "@sharedabba_live",
            uploaderFollowers = "33.0K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_live_2.jpg",
            streamUrl = SAMPLE_MP4_1,
            shareUrl = "https://sharedabba.com/watch/vid_live_2",
            isFeatured = false,
            isPopular = false,
            isRecent = false,
            isMyUpload = true
        ),
        VideoItemFixture(
            id = "vid_creator_2",
            uploaderId = "usr_himalayan_lens_08",
            title = "Behind the Scenes: High-Altitude Documentary",
            description = "Field production diary filming in Sikkim with compact cinema rigs.",
            category = "Creator Originals",
            durationLabel = "19:33",
            viewsLabel = "14.5K views",
            uploadedAgo = "6 days ago",
            uploaderName = "Himalayan Lens",
            uploaderHandle = "@himalayan_lens",
            uploaderFollowers = "22.9K followers",
            thumbnailUrl = "https://sharedabba.com/assets/thumbs/vid_creator_2.jpg",
            streamUrl = SAMPLE_MP4_1,
            shareUrl = "https://sharedabba.com/watch/vid_creator_2",
            isFeatured = false,
            isPopular = true,
            isRecent = false,
            isMyUpload = false
        )
    )

    val dashboardMetrics = DashboardMetricsFixture(
        totalUploads = 3,
        recordedViews = 29_200L,
        eligibleViews = 24_850L,
        totalEarningsDisplay = "₹14,910.00 (Fixture)",
        pendingBalanceDisplay = "₹3,450.00 (Fixture)",
        availableBalanceDisplay = "₹11,460.00 (Fixture)"
    )

    val earningsBreakdown: List<EarningsRecordFixture> = listOf(
        EarningsRecordFixture(
            id = "earn_1",
            dateLabel = "Oct 29, 2026",
            videoTitle = "ShareDabba Next-Gen Streaming Architecture Keynote",
            recordedViews = 18_400,
            eligibleViews = 15_900,
            estimatedAmountDisplay = "₹9,540.00 (Fixture)"
        ),
        EarningsRecordFixture(
            id = "earn_2",
            dateLabel = "Oct 28, 2026",
            videoTitle = "Building Custom Media3 Renderers on Android 16",
            recordedViews = 4_700,
            eligibleViews = 4_100,
            estimatedAmountDisplay = "₹2,460.00 (Fixture)"
        ),
        EarningsRecordFixture(
            id = "earn_3",
            dateLabel = "Oct 25, 2026",
            videoTitle = "Late Night Creator Roundtable & Channel Reviews",
            recordedViews = 6_100,
            eligibleViews = 4_850,
            estimatedAmountDisplay = "₹2,910.00 (Fixture)"
        )
    )

    val withdrawalHistory: List<WithdrawalRecordFixture> = listOf(
        WithdrawalRecordFixture(
            id = "wd_101",
            referenceCode = "SD-FIXTURE-9081",
            requestedDate = "Oct 24, 2026",
            methodLabel = "Website Payout Portal (Bank / UPI)",
            amountDisplay = "₹5,000.00 (Fixture)",
            status = "Completed"
        ),
        WithdrawalRecordFixture(
            id = "wd_102",
            referenceCode = "SD-FIXTURE-9144",
            requestedDate = "Oct 29, 2026",
            methodLabel = "Website Payout Portal (Bank / UPI)",
            amountDisplay = "₹3,450.00 (Fixture)",
            status = "Pending"
        )
    )

    val initialNotifications: List<NotificationItemFixture> = listOf(
        NotificationItemFixture(
            id = "notif_1",
            title = "Transcoding Complete (Fixture)",
            body = "Your video 'ShareDabba Next-Gen Streaming Architecture Keynote' is ready in 1080p HLS.",
            timeAgo = "25m ago",
            category = "Uploads",
            isUnread = true,
            targetRoute = "uploads"
        ),
        NotificationItemFixture(
            id = "notif_2",
            title = "Eligible Views Updated (Fixture)",
            body = "Your creator dashboard recorded +1,200 eligible views in the last 24 hours.",
            timeAgo = "3h ago",
            category = "Earnings",
            isUnread = true,
            targetRoute = "user_dashboard"
        ),
        NotificationItemFixture(
            id = "notif_3",
            title = "Stage 6 Video Library Contract Active",
            body = "Website video provenance (ID, Uploader ID, Share URL, Ownership Record) enforced across all screens.",
            timeAgo = "1d ago",
            category = "System",
            isUnread = false,
            targetRoute = "settings"
        )
    )

    val supportFaqs: List<SupportFaqItem> = listOf(
        SupportFaqItem(
            id = "faq_1",
            question = "How are Recorded Views vs. Eligible Views calculated?",
            answer = "Recorded views count every initiated stream session, while Eligible views represent verified watch sessions meeting ShareDabba's minimum watch-duration and anti-bot checks on the website backend."
        ),
        SupportFaqItem(
            id = "faq_2",
            question = "Where is my production Wallet balance stored?",
            answer = "Your official creator wallet and withdrawal ledger live exclusively on the ShareDabba website backend. Until the website API endpoints are connected, financial screens display clearly labelled Development Fixtures."
        ),
        SupportFaqItem(
            id = "faq_3",
            question = "How does offline/local caching work in the Android app?",
            answer = "The Android app uses Room strictly as a local device cache for recently watched stream playback positions and Cloud Firestore for personal stream bookmarks. Production videos are never stored only on the phone."
        ),
        SupportFaqItem(
            id = "faq_4",
            question = "Which AI models power ShareDabba Search?",
            answer = "Instant stream discovery uses gemini-3.1-flash-lite for low latency, while complex creator strategy queries use gemini-4-argon with ThinkingLevel.HIGH."
        )
    )

    fun findVideoById(videoId: String?): VideoItemFixture {
        return videos.firstOrNull { it.id == videoId } ?: videos.first()
    }

    fun getRelatedVideos(currentVideoId: String): List<VideoItemFixture> {
        return videos.filter { it.id != currentVideoId }.take(4)
    }
}
