package com.example.uploads

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.data.repository.AuthorizedUploadAttemptResult
import com.example.data.repository.UploadApiReadinessReport
import com.example.data.repository.WebsiteVideoLibraryRepository
import com.example.network.NetworkClient
import com.example.ui.common.DevFixtureNoticeBanner
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.ShareDabbaVideoCard
import com.example.ui.common.StateModeSelectorRow
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWarning
import com.example.ui.theme.ShareDabbaWhite

/**
 * Screen 11: My Videos & Website Uploads Screen (Stage 6)
 * - Displays the user's authoritative website videos ("My Videos" / "User Uploads") under the same account (`uploaderId`).
 * - Retains Original Video ID, Original Uploader ID, Original Share URL, and Ownership Record.
 * - Connects Android Photo Picker (`ActivityResultContracts.PickVisualMedia.VideoOnly`) to the Authorized Website Upload API check.
 * - Clearly reports missing backend upload endpoints instead of inventing a second upload system or storing production videos only on the phone.
 */
@Composable
fun UploadsScreen(
    environmentConfig: EnvironmentConfig,
    websiteVideoLibraryRepository: WebsiteVideoLibraryRepository = remember(environmentConfig) {
        WebsiteVideoLibraryRepository(environmentConfig, NetworkClient.createApiService(environmentConfig))
    },
    authenticatedUserId: String? = "usr_sharedabba_creator",
    authenticatedUserDisplayName: String? = "ShareDabba Creator",
    onOpenVideoDetails: (String) -> Unit = {},
    onPlayStream: (String) -> Unit = {},
    onOpenDashboard: () -> Unit = {}
) {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var draftTitle by rememberSaveable { mutableStateOf("") }
    var draftCategory by rememberSaveable { mutableStateOf("Creator Originals") }
    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }
    var showMissingEndpointsReport by rememberSaveable { mutableStateOf(true) }
    var uploadAttemptFeedback by rememberSaveable { mutableStateOf<String?>(null) }

    val readinessReport: UploadApiReadinessReport = remember(websiteVideoLibraryRepository) {
        websiteVideoLibraryRepository.evaluateUploadApiReadiness()
    }

    val myWebsiteVideos = remember(authenticatedUserId, authenticatedUserDisplayName) {
        websiteVideoLibraryRepository.getMyVideosForAccount(
            currentUserId = authenticatedUserId,
            currentUserDisplayName = authenticatedUserDisplayName
        )
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedVideoUri = uri
        if (uri != null) {
            AppLogger.i("UploadsModule", "Selected video URI via Android Photo Picker: $uri")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("uploads_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "My Videos & Uploads",
                subtitle = "Stage 6 • Website Authoritative Storage & Same-Account Sync"
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DevFixtureNoticeBanner(
                    message = "Website remains the authoritative source for videos and storage. Production videos are never stored only on the phone."
                )
                StateModeSelectorRow(
                    currentMode = stateMode,
                    onModeChange = { stateMode = it }
                )

                // Upload API Client & Missing Endpoint Guard Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoFile,
                                    contentDescription = null,
                                    tint = ShareDabbaCyan
                                )
                                Text(
                                    text = "Website Authorized Upload Client",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ShareDabbaWhite
                                )
                            }
                            ShareDabbaGradientBadge(text = "Same-Account Sync")
                        }

                        Text(
                            text = "Authenticated Creator UID: ${authenticatedUserId ?: "Not signed in"} • Storage: Website Authoritative Only",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan
                        )

                        OutlinedTextField(
                            value = draftTitle,
                            onValueChange = { draftTitle = it },
                            label = { Text("Video Title (for Website Upload)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_title_input")
                        )

                        OutlinedTextField(
                            value = draftCategory,
                            onValueChange = { draftCategory = it },
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_category_input")
                        )

                        Text(
                            text = if (selectedVideoUri != null) {
                                "Selected Media URI: $selectedVideoUri (Ready for Website Presigned Upload)"
                            } else {
                                "Select a video via the zero-permission Android Photo Picker to verify authorized website upload readiness."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShareDabbaTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_video_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShareDabbaCyan,
                                    contentColor = ShareDabbaBlack
                                )
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Pick Video")
                            }

                            OutlinedButton(
                                onClick = {
                                    val result = websiteVideoLibraryRepository.attemptAuthorizedWebsiteUpload(
                                        authenticatedUserId = authenticatedUserId ?: "usr_sharedabba_creator",
                                        targetUploaderId = authenticatedUserId ?: "usr_sharedabba_creator",
                                        title = draftTitle.trim().ifEmpty { "Creator Upload Test" },
                                        category = draftCategory.trim().ifEmpty { "Creator Originals" },
                                        selectedMediaUriString = selectedVideoUri?.toString()
                                    )
                                    when (result) {
                                        is AuthorizedUploadAttemptResult.BlockedMissingBackendEndpoints -> {
                                            showMissingEndpointsReport = true
                                            uploadAttemptFeedback =
                                                "Upload blocked as required by Stage 6: Phone-only production video storage is prohibited and a second upload system is not invented. See Missing Backend Upload Endpoints below."
                                        }
                                        is AuthorizedUploadAttemptResult.UploadedToWebsite -> {
                                            uploadAttemptFeedback =
                                                "Uploaded to website under videoId=${result.syncedVideo.id}, uploaderId=${result.syncedVideo.uploaderId}"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stage_upload_fixture_button")
                            ) {
                                Text("Verify Upload API", color = ShareDabbaWhite)
                            }
                        }

                        if (!uploadAttemptFeedback.isNullOrBlank()) {
                            Text(
                                text = uploadAttemptFeedback.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaWarning,
                                modifier = Modifier.testTag("upload_attempt_feedback_text")
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenDashboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("uploads_open_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                tint = ShareDabbaPurple
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Open Creator Dashboard & View Metrics", color = ShareDabbaWhite)
                        }
                    }
                }

                // Missing Backend Upload Endpoints Report Card (Stage 6 Requirement)
                if (showMissingEndpointsReport && !readinessReport.isUploadApiConfiguredOnBackend) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ShareDabbaWarning.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .testTag("missing_upload_endpoints_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = "Missing Upload Endpoints Report",
                                    tint = ShareDabbaWarning
                                )
                                Text(
                                    text = readinessReport.statusHeadline,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ShareDabbaWhite
                                )
                            }

                            Text(
                                text = readinessReport.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaTextSecondary
                            )

                            readinessReport.missingBackendEndpoints.forEach { spec ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ShareDabbaBlack, RoundedCornerShape(10.dp))
                                        .border(1.dp, ShareDabbaOutline, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${spec.capability} • ${spec.httpMethod}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ShareDabbaCyan
                                    )
                                    Text(
                                        text = spec.requiredPurpose,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ShareDabbaTextSecondary
                                    )
                                    Text(
                                        text = "Auth: ${spec.requiredAuth} • Status: ${spec.status}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ShareDabbaWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        when (stateMode) {
            PreviewStateMode.LOADING -> {
                item {
                    ShareDabbaLoadingState(message = "Loading your website videos & ownership records...")
                }
            }

            PreviewStateMode.EMPTY -> {
                item {
                    ShareDabbaEmptyState(
                        title = "No Uploaded Videos Found",
                        subtitle = "Videos uploaded on the ShareDabba website under your account automatically appear here.",
                        actionLabel = "Show My Website Videos",
                        onActionClick = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.ERROR -> {
                item {
                    ShareDabbaErrorState(
                        message = "Failed to fetch creator videos list from ShareDabba website backend.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.DATA -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "My Website Videos (${myWebsiteVideos.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = ShareDabbaWhite
                            )
                        }
                        ShareDabbaGradientBadge(text = "Provenance Verified")
                    }
                }

                items(myWebsiteVideos, key = { it.id }) { video ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ShareDabbaVideoCard(
                            video = video,
                            onCardClick = { onOpenVideoDetails(video.id) },
                            onWatchClick = { onPlayStream(video.streamUrl) },
                            testTag = "my_upload_item_${video.id}"
                        )
                    }
                }
            }
        }
    }
}
