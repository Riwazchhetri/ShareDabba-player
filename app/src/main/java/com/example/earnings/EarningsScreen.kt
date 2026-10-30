package com.example.earnings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.EnvironmentConfig
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.EarningsRecordFixture
import com.example.ui.common.DevFixtureNoticeBanner
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.StateModeSelectorRow
import com.example.ui.navigation.ShareDabbaDestination
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite

/**
 * Screen 12: User Dashboard Screen
 * Required metrics:
 * - Total uploads
 * - Recorded views
 * - Eligible views
 * - Earnings
 * - Pending balance
 * - Available balance
 */
@Composable
fun UserDashboardScreen(
    environmentConfig: EnvironmentConfig,
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }
    val metrics = DevFixturesCatalog.dashboardMetrics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("user_dashboard_screen")
    ) {
        ShareDabbaTopBar(
            title = "User Dashboard",
            subtitle = "Creator Telemetry • Development Fixture Preview",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DevFixtureNoticeBanner()
            StateModeSelectorRow(
                currentMode = stateMode,
                onModeChange = { stateMode = it }
            )

            when (stateMode) {
                PreviewStateMode.LOADING -> {
                    ShareDabbaLoadingState(message = "Loading creator dashboard telemetry...")
                }

                PreviewStateMode.EMPTY -> {
                    ShareDabbaEmptyState(
                        title = "No Creator Telemetry Recorded Yet",
                        subtitle = "Upload your first video on ShareDabba to begin tracking recorded and eligible views.",
                        actionLabel = "Go to My Uploads",
                        onActionClick = { onNavigate(ShareDabbaDestination.Uploads.route) }
                    )
                }

                PreviewStateMode.ERROR -> {
                    ShareDabbaErrorState(
                        message = "Could not load creator dashboard from ${environmentConfig.apiBaseUrl}creator/dashboard.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }

                PreviewStateMode.DATA -> {
                    // 6 Required Dashboard Metrics Grid (3 rows of 2 cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            label = "Total Uploads",
                            value = metrics.totalUploads.toString(),
                            subtitle = "Videos on ShareDabba",
                            icon = Icons.Default.CloudUpload,
                            modifier = Modifier.weight(1f),
                            testTag = "dashboard_metric_total_uploads"
                        )
                        DashboardMetricCard(
                            label = "Recorded Views",
                            value = "%,d".format(metrics.recordedViews),
                            subtitle = "Total stream starts",
                            icon = Icons.Default.Visibility,
                            modifier = Modifier.weight(1f),
                            testTag = "dashboard_metric_recorded_views"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            label = "Eligible Views",
                            value = "%,d".format(metrics.eligibleViews),
                            subtitle = "Monetization verified",
                            icon = Icons.Default.VerifiedUser,
                            modifier = Modifier.weight(1f),
                            testTag = "dashboard_metric_eligible_views"
                        )
                        DashboardMetricCard(
                            label = "Total Earnings",
                            value = metrics.earningsDisplay,
                            subtitle = "Fixture preview",
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f),
                            testTag = "dashboard_metric_earnings"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            label = "Pending Balance",
                            value = metrics.pendingBalanceDisplay,
                            subtitle = "In verification hold",
                            icon = Icons.Default.MonetizationOn,
                            modifier = Modifier.weight(1f),
                            testTag = "dashboard_metric_pending_balance"
                        )
                        DashboardMetricCard(
                            label = "Available Balance",
                            value = metrics.availableBalanceDisplay,
                            subtitle = "Ready in web wallet",
                            icon = Icons.Default.AccountBalanceWallet,
                            modifier = Modifier.weight(1f),
                            testTag = "dashboard_metric_available_balance"
                        )
                    }

                    // Quick Action Navigation Buttons
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Creator Financial & Content Actions",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onNavigate(ShareDabbaDestination.Earnings.route) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dashboard_open_earnings_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ShareDabbaCyan,
                                        contentColor = ShareDabbaBlack
                                    )
                                ) {
                                    Text("Earnings Report")
                                }
                                Button(
                                    onClick = { onNavigate(ShareDabbaDestination.Wallet.route) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dashboard_open_wallet_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ShareDabbaPurple,
                                        contentColor = ShareDabbaWhite
                                    )
                                ) {
                                    Text("Open Wallet")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onNavigate(ShareDabbaDestination.WithdrawalHistory.route) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dashboard_open_withdrawals_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = ShareDabbaCyan
                                    )
                                    Spacer(modifier = Modifier.padding(3.dp))
                                    Text("Withdrawals", color = ShareDabbaWhite)
                                }
                                OutlinedButton(
                                    onClick = { onNavigate(ShareDabbaDestination.Uploads.route) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dashboard_open_uploads_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = ShareDabbaCyan
                                    )
                                    Spacer(modifier = Modifier.padding(3.dp))
                                    Text("My Uploads", color = ShareDabbaWhite)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    label: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaTextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ShareDabbaCyan
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = ShareDabbaWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ShareDabbaCyan
            )
        }
    }
}

/**
 * Screen 13: Earnings Screen
 * Displays per-video recorded vs. eligible views and earnings breakdown from `DevFixturesCatalog`,
 * clearly labelled as a development fixture separated from production code.
 */
@Composable
fun EarningsScreen(
    environmentConfig: EnvironmentConfig,
    onOpenWallet: () -> Unit = {},
    onOpenWithdrawals: () -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }
    val records = DevFixturesCatalog.earningsBreakdown

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("earnings_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "Creator Earnings",
                subtitle = "com.example.earnings • Per-Video Revenue Breakdown",
                onBackClick = onBackClick
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DevFixtureNoticeBanner()
                StateModeSelectorRow(
                    currentMode = stateMode,
                    onModeChange = { stateMode = it }
                )

                ShareDabbaSectionCard(
                    title = "Authoritative Web Backend Source of Truth",
                    packagePath = "${environmentConfig.apiBaseUrl}creator/earnings",
                    description = stringResource(id = R.string.no_fake_financial_notice),
                    icon = Icons.Default.MonetizationOn,
                    testTag = "earnings_integrity_card"
                )
            }
        }

        when (stateMode) {
            PreviewStateMode.LOADING -> {
                item {
                    ShareDabbaLoadingState(message = "Loading creator earnings breakdown...")
                }
            }

            PreviewStateMode.EMPTY -> {
                item {
                    ShareDabbaEmptyState(
                        title = "No Earnings Statements Available",
                        subtitle = "Once your uploaded videos accumulate eligible views on ShareDabba, revenue statements appear here.",
                        actionLabel = "Show Fixture Breakdown",
                        onActionClick = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.ERROR -> {
                item {
                    ShareDabbaErrorState(
                        message = "Unable to fetch creator earnings from ${environmentConfig.apiBaseUrl}creator/earnings.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.DATA -> {
                items(records, key = { it.id }) { record ->
                    EarningsRecordCard(record = record)
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenWallet,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("earnings_to_wallet_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaCyan,
                                contentColor = ShareDabbaBlack
                            )
                        ) {
                            Text("Open Wallet")
                        }
                        OutlinedButton(
                            onClick = onOpenWithdrawals,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("earnings_to_withdrawals_button")
                        ) {
                            Text("Withdrawal History", color = ShareDabbaWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsRecordCard(record: EarningsRecordFixture) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag("earnings_record_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShareDabbaGradientBadge(text = record.statusLabel)
                Text(
                    text = record.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaTextSecondary
                )
            }
            Text(
                text = record.videoTitle,
                style = MaterialTheme.typography.titleMedium,
                color = ShareDabbaWhite
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recorded: %,d".format(record.recordedViews),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShareDabbaTextSecondary
                )
                Text(
                    text = "Eligible: %,d".format(record.eligibleViews),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShareDabbaCyan
                )
                Text(
                    text = record.earnedAmountDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    color = ShareDabbaWhite
                )
            }
        }
    }
}
