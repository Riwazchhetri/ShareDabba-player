package com.example.wallet

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.advertisements.MonetizationCoordinator
import com.example.advertisements.WithdrawalSubmissionRecord
import com.example.core.config.EnvironmentConfig
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.WithdrawalRecordFixture
import com.example.remoteconfig.RemoteConfigManager
import com.example.ui.common.DevFixtureNoticeBanner
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.StateModeSelectorRow
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite

/**
 * Screen 14: Wallet Screen
 * Displays unified website wallet overview (Pending Earnings, Confirmed Earnings, Available Balance),
 * missing backend payout integration report, and authenticated duplicate-safe withdrawal request submission.
 */
@Composable
fun WalletScreen(
    environmentConfig: EnvironmentConfig,
    monetizationCoordinator: MonetizationCoordinator? = null,
    authenticatedUserId: String? = "usr-aarav-mehta",
    onOpenWithdrawalHistory: () -> Unit = {},
    onOpenEarnings: () -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }
    val coordinator = remember(monetizationCoordinator, environmentConfig) {
        monetizationCoordinator ?: MonetizationCoordinator(
            environmentConfig = environmentConfig,
            remoteConfigManager = RemoteConfigManager(environmentConfig)
        )
    }
    val ledger by coordinator.ledgerSnapshot.collectAsStateWithLifecycle()

    var amountInput by rememberSaveable { mutableStateOf("45.00") }
    var destinationInput by rememberSaveable { mutableStateOf("UPI • aarav.creator@okaxis") }
    var idempotencySequence by rememberSaveable { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("wallet_screen")
    ) {
        ShareDabbaTopBar(
            title = "ShareDabba Wallet",
            subtitle = "com.example.wallet • Unified Website Ledger",
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
                    ShareDabbaLoadingState(message = "Syncing wallet ledger from ShareDabba web backend...")
                }

                PreviewStateMode.EMPTY -> {
                    ShareDabbaEmptyState(
                        title = "No Active Wallet Ledger",
                        subtitle = "Complete your creator payout profile on the ShareDabba website to view balances.",
                        actionLabel = "Restore Fixture View",
                        onActionClick = { stateMode = PreviewStateMode.DATA }
                    )
                }

                PreviewStateMode.ERROR -> {
                    ShareDabbaErrorState(
                        message = "Failed to query ${environmentConfig.apiBaseUrl}wallet. Check Stage 2 Integration Contract.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }

                PreviewStateMode.DATA -> {
                    // Balances Overview Card (Pending, Confirmed, Available)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ShareDabbaGradientBadge(text = "DEVELOPMENT FIXTURE PREVIEW")
                            Text(
                                text = "Available Balance",
                                style = MaterialTheme.typography.labelLarge,
                                color = ShareDabbaTextSecondary
                            )
                            Text(
                                text = ledger.availableBalanceDisplay,
                                style = MaterialTheme.typography.headlineMedium,
                                color = ShareDabbaCyan,
                                modifier = Modifier.testTag("wallet_available_balance_value")
                            )
                            Text(
                                text = "Pending Verification Balance: ${ledger.pendingEarningsDisplay}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaWhite,
                                modifier = Modifier.testTag("wallet_pending_balance_value")
                            )
                            Text(
                                text = "Confirmed Total Earnings: ${ledger.confirmedEarningsDisplay}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaCyan,
                                modifier = Modifier.testTag("wallet_confirmed_earnings_value")
                            )
                            Text(
                                text = "All balances and payout requests are governed by the authoritative ShareDabba website ledger (${environmentConfig.websiteBaseUrl}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShareDabbaTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenWithdrawalHistory,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wallet_open_withdrawal_history_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ShareDabbaCyan,
                                        contentColor = ShareDabbaBlack
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.History, contentDescription = null)
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("Withdrawal History")
                                }

                                OutlinedButton(
                                    onClick = onOpenEarnings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wallet_open_earnings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = ShareDabbaPurple
                                    )
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("Earnings", color = ShareDabbaWhite)
                                }
                            }
                        }
                    }

                    // Stage 9 Authenticated Withdrawal Request Card (Duplicate Guard + Non-Client-Paid Guarantee)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ShareDabbaCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .testTag("wallet_withdrawal_submission_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ShareDabbaGradientBadge(text = "STAGE 9 AUTHENTICATED PAYOUT REQUEST")
                            Text(
                                text = "Submit Authenticated Withdrawal Request",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                            Text(
                                text = "Payouts require server verification. Client responses NEVER mark a withdrawal as paid/completed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShareDabbaTextSecondary
                            )

                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                label = { Text("Withdrawal Amount (USD)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_withdrawal_amount_input")
                            )

                            OutlinedTextField(
                                value = destinationInput,
                                onValueChange = { destinationInput = it },
                                label = { Text("Verified Payout Destination") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_withdrawal_destination_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                                        val ownerUid = authenticatedUserId ?: "usr-aarav-mehta"
                                        coordinator.submitAuthenticatedWithdrawalRequest(
                                            authenticatedUid = ownerUid,
                                            requestedCreatorUid = ownerUid,
                                            amountUsd = parsedAmount,
                                            destinationSummary = destinationInput,
                                            idempotencyKey = "wd-key-${idempotencySequence++}"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wallet_submit_withdrawal_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ShareDabbaCyan,
                                        contentColor = ShareDabbaBlack
                                    )
                                ) {
                                    Text("Submit Request")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val parsedAmount = amountInput.toDoubleOrNull() ?: 45.0
                                        val ownerUid = authenticatedUserId ?: "usr-aarav-mehta"
                                        coordinator.submitAuthenticatedWithdrawalRequest(
                                            authenticatedUid = ownerUid,
                                            requestedCreatorUid = ownerUid,
                                            amountUsd = parsedAmount,
                                            destinationSummary = destinationInput,
                                            idempotencyKey = "wd-duplicate-fixed-key"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wallet_test_duplicate_withdrawal_button")
                                ) {
                                    Text("Test Duplicate Guard", color = ShareDabbaWhite)
                                }
                            }

                            ledger.sessionSubmittedWithdrawals.firstOrNull()?.let { latest ->
                                WithdrawalSubmissionStatusBanner(record = latest)
                            }
                        }
                    }

                    // Missing Backend Payout Integration Report
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                            .testTag("wallet_missing_backend_report_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Missing Backend Monetization & Payout Integration Report",
                                style = MaterialTheme.typography.titleSmall,
                                color = ShareDabbaWhite
                            )
                            ledger.missingBackendIntegrationReport.forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ShareDabbaTextSecondary
                                )
                            }
                        }
                    }

                    ShareDabbaSectionCard(
                        title = "Unified Website Wallet Ledger",
                        packagePath = "${environmentConfig.apiBaseUrl}wallet",
                        description = stringResource(id = R.string.no_fake_financial_notice),
                        icon = Icons.Default.AccountBalanceWallet,
                        testTag = "wallet_ledger_card"
                    )

                    ShareDabbaSectionCard(
                        title = "Zero-Trust Ledger Protection",
                        packagePath = "com.example.wallet",
                        description = "Financial mutations require server-side verification on the ShareDabba web platform.",
                        icon = Icons.Default.Lock,
                        testTag = "wallet_security_card"
                    )
                }
            }
        }
    }
}

@Composable
private fun WithdrawalSubmissionStatusBanner(record: WithdrawalSubmissionRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(12.dp))
            .testTag("wallet_latest_submission_status_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaBlack)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShareDabbaGradientBadge(text = record.status.badgeLabel)
            Text(
                text = "${record.amountDisplay} → ${record.destinationSummary}",
                style = MaterialTheme.typography.labelLarge,
                color = ShareDabbaWhite
            )
            Text(
                text = "Marked Paid by Client Alone: ${record.isMarkedPaidByClientAlone} (Always false)",
                style = MaterialTheme.typography.labelSmall,
                color = ShareDabbaCyan
            )
            Text(
                text = record.serverStatusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ShareDabbaTextSecondary
            )
        }
    }
}

/**
 * Screen 15: Withdrawal History Screen
 * Displays session-submitted withdrawal requests (always awaiting server verification, never client-finalized)
 * alongside past and pending withdrawal entries from `DevFixturesCatalog.withdrawalHistory`.
 */
@Composable
fun WithdrawalHistoryScreen(
    monetizationCoordinator: MonetizationCoordinator? = null,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    val fallbackEnv = remember { EnvironmentConfig() }
    val coordinator = remember(monetizationCoordinator) {
        monetizationCoordinator ?: MonetizationCoordinator(fallbackEnv, RemoteConfigManager(fallbackEnv))
    }
    val ledger by coordinator.ledgerSnapshot.collectAsStateWithLifecycle()

    val filteredWithdrawals = ledger.withdrawalHistory.filter { item ->
        when (selectedFilter) {
            "Completed" -> item.status.contains("Completed", ignoreCase = true)
            "Pending" -> item.status.contains("Pending", ignoreCase = true)
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("withdrawal_history_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "Withdrawal History",
                subtitle = "com.example.wallet • Payout Ledger History",
                onBackClick = onBackClick
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DevFixtureNoticeBanner()
                StateModeSelectorRow(
                    currentMode = stateMode,
                    onModeChange = { stateMode = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Completed", "Pending").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShareDabbaCyan,
                                selectedLabelColor = ShareDabbaBlack,
                                containerColor = ShareDabbaSurface,
                                labelColor = ShareDabbaWhite
                            ),
                            modifier = Modifier.testTag("withdrawal_filter_$filter")
                        )
                    }
                }
            }
        }

        when (stateMode) {
            PreviewStateMode.LOADING -> {
                item {
                    ShareDabbaLoadingState(message = "Loading withdrawal history...")
                }
            }

            PreviewStateMode.EMPTY -> {
                item {
                    ShareDabbaEmptyState(
                        title = "No Withdrawal Requests Found",
                        subtitle = "You have no withdrawal records matching the current filter.",
                        actionLabel = "Show All Fixtures",
                        onActionClick = {
                            selectedFilter = "All"
                            stateMode = PreviewStateMode.DATA
                        }
                    )
                }
            }

            PreviewStateMode.ERROR -> {
                item {
                    ShareDabbaErrorState(
                        message = "Unable to retrieve withdrawal history from ShareDabba payout endpoint.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.DATA -> {
                items(ledger.sessionSubmittedWithdrawals, key = { it.requestId }) { submitted ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WithdrawalSubmissionStatusBanner(record = submitted)
                    }
                }

                items(filteredWithdrawals, key = { it.id }) { item ->
                    WithdrawalRecordCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun WithdrawalRecordCard(item: WithdrawalRecordFixture) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag("withdrawal_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.amountDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    color = ShareDabbaCyan
                )
                ShareDabbaGradientBadge(text = item.status)
            }
            Text(
                text = item.destinationLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = ShareDabbaWhite
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.requestedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaTextSecondary
                )
                Text(
                    text = item.referenceCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaTextSecondary
                )
            }
        }
    }
}
