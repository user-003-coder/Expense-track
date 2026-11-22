package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionWithDetails
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTooltipDialog
import com.example.ui.components.MonthlyComparisonBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingDonutChart
import com.example.ui.components.TransactionRow
import com.example.ui.components.VerdantCard
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.viewmodel.FinanceUiState
import kotlin.math.abs

@Composable
fun HomeScreen(
    uiState: FinanceUiState,
    onToggleBalanceVisibility: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onOpenTransactionDetail: (TransactionWithDetails) -> Unit,
    onSelectAnalyticsPeriod: (AnalyticsPeriod) -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenBudgets: () -> Unit,
    onAddMoneyToGoal: (SavingsGoalEntity, Double) -> Unit,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val currency = uiState.currency
    val hideBalance = uiState.preferences.hideBalance
    var tooltipPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var contributeGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ================= ABOVE THE FOLD: CLEAN FIRST IMPRESSION =================

            // 1. Minimal Header
            item(key = "home_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_verdant_icon_1795332177129),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = DateFormatter.greetingForCurrentHour(),
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(
                                onClick = onOpenNotifications,
                                modifier = Modifier.testTag("home_notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsNone,
                                    contentDescription = stringResource(R.string.cd_notifications),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            if (uiState.alerts.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 10.dp, end = 10.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(extended.expense)
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_profile_avatar_1795332176663),
                            contentDescription = stringResource(R.string.cd_profile_avatar),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenProfile)
                                .testTag("home_profile_avatar"),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 2. Total Balance Card (Prominent, Calm, High Hierarchy)
            item(key = "home_balance_card") {
                VerdantCard(
                    contentPadding = PaddingValues(24.dp),
                    modifier = Modifier.testTag("home_balance_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.total_balance),
                            style = MaterialTheme.typography.labelLarge,
                            color = extended.mutedText
                        )
                        IconButton(
                            onClick = onToggleBalanceVisibility,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toggle_balance_visibility")
                        ) {
                            Icon(
                                imageVector = if (hideBalance) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = stringResource(R.string.cd_toggle_balance),
                                tint = extended.mutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (hideBalance) {
                            "${currency.symbol} ••••••"
                        } else {
                            CurrencyFormatter.format(uiState.totalBalance, currency)
                        },
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(14.dp))
                    val momPositive = uiState.monthOverMonthPercent >= 0.0
                    val momFormatted = String.format(
                        java.util.Locale.US,
                        "%s%.1f%% vs last month",
                        if (momPositive) "+" else "-",
                        abs(uiState.monthOverMonthPercent)
                    )
                    Surface(
                        color = if (momPositive) extended.incomeContainer else extended.expenseContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (momPositive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                contentDescription = null,
                                tint = if (momPositive) extended.income else extended.expense,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = momFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (momPositive) extended.income else extended.expense
                            )
                        }
                    }
                }
            }

            // 3. Income & Expenses Summary (Side-by-side compact cards)
            item(key = "home_income_expense_summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryMetricCard(
                        label = stringResource(R.string.income),
                        amount = if (hideBalance) {
                            "${currency.symbol} ••••"
                        } else {
                            CurrencyFormatter.format(uiState.currentMonthIncome, currency)
                        },
                        isIncome = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_income_card")
                    )
                    SummaryMetricCard(
                        label = stringResource(R.string.expenses),
                        amount = if (hideBalance) {
                            "${currency.symbol} ••••"
                        } else {
                            CurrencyFormatter.format(uiState.currentMonthExpense, currency)
                        },
                        isIncome = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_expense_card")
                    )
                }
            }

            // Generous breathing space separating above-the-fold clarity from scrollable sections
            item(key = "fold_spacer") {
                Spacer(Modifier.height(4.dp))
            }

            // ================= BELOW THE FOLD: PROGRESSIVE SECTIONS =================

            // SECTION A — Recent Transactions (Latest 5)
            item(key = "section_recent_transactions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.recent_transactions),
                        actionLabel = stringResource(R.string.view_all),
                        actionTestTag = "home_view_all_transactions",
                        onActionClick = onViewAllTransactions
                    )
                    if (uiState.recentTransactions.isEmpty()) {
                        EmptyStateCard(
                            title = "No transactions yet",
                            message = "Record your first income or expense to see real-time balances and insights.",
                            actionLabel = "Add Transaction",
                            onActionClick = onAddTransactionClick
                        )
                    } else {
                        VerdantCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                            uiState.recentTransactions.forEachIndexed { idx, item ->
                                TransactionRow(
                                    item = item,
                                    currency = currency,
                                    hideAmounts = hideBalance,
                                    onClick = { onOpenTransactionDetail(item) }
                                )
                                if (idx < uiState.recentTransactions.lastIndex) {
                                    HorizontalDivider(
                                        color = extended.border,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION B — Spending Analytics
            item(key = "section_spending_analytics") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.spending_analytics),
                        actionLabel = "Full Report",
                        actionTestTag = "home_full_analytics",
                        tooltipText = "Interactive breakdown of your spending by category and monthly income vs expense trends.",
                        onTooltipClick = { t, m -> tooltipPair = t to m },
                        onActionClick = onOpenAnalytics
                    )
                    VerdantCard {
                        // Weekly / Monthly / Yearly Segmented Pills
                        val homePeriods = listOf(
                            AnalyticsPeriod.WEEKLY,
                            AnalyticsPeriod.MONTHLY,
                            AnalyticsPeriod.YEARLY
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            homePeriods.forEach { period ->
                                val selected = uiState.analyticsPeriod == period
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { onSelectAnalyticsPeriod(period) },
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(9.dp)
                                ) {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else extended.mutedText,
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        SpendingDonutChart(
                            categories = uiState.categoryBreakdown,
                            totalSpent = uiState.periodExpense,
                            currency = currency,
                            hideAmounts = hideBalance
                        )
                        Spacer(Modifier.height(22.dp))
                        HorizontalDivider(color = extended.border)
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "Income vs Expense",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        MonthlyComparisonBarChart(
                            comparisons = uiState.monthlyComparisons,
                            currency = currency
                        )
                    }
                }
            }

            // SECTION C — Budget Management
            item(key = "section_budget_management") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.budget_management),
                        actionLabel = "Manage",
                        actionTestTag = "home_manage_budgets",
                        tooltipText = "Track overall and category spending limits for the current month.",
                        onTooltipClick = { t, m -> tooltipPair = t to m },
                        onActionClick = onOpenBudgets
                    )
                    val overall = uiState.overallBudget
                    val categoryBudgets = uiState.currentMonthBudgets.filter { it.budget.categoryId != null }

                    if (overall == null && categoryBudgets.isEmpty()) {
                        EmptyStateCard(
                            title = "No active budgets",
                            message = "Set monthly or weekly spending caps to stay ahead of expenses.",
                            actionLabel = "Create Budget",
                            onActionClick = onOpenBudgets,
                            showIllustration = false
                        )
                    } else {
                        VerdantCard {
                            if (overall != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Monthly Budget Overview",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (overall.isExceeded) {
                                                "Exceeded by ${CurrencyFormatter.format(abs(overall.remainingAmount), currency)}"
                                            } else {
                                                "${CurrencyFormatter.format(overall.remainingAmount, currency)} remaining"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (overall.isExceeded) extended.expense else extended.income
                                        )
                                    }
                                    Text(
                                        text = "${CurrencyFormatter.format(overall.spentAmount, currency, compact = true)} / ${CurrencyFormatter.format(overall.budget.limitAmount, currency, compact = true)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { overall.progressFraction.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (overall.isExceeded) extended.expense else MaterialTheme.colorScheme.primary,
                                    trackColor = extended.sageTint,
                                    strokeCap = StrokeCap.Round
                                )
                            }

                            if (categoryBudgets.isNotEmpty()) {
                                if (overall != null) {
                                    Spacer(Modifier.height(18.dp))
                                    HorizontalDivider(color = extended.border)
                                    Spacer(Modifier.height(14.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    categoryBudgets.take(3).forEach { bp ->
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CategoryIconBadge(
                                                        iconKey = bp.iconKey,
                                                        size = 32.dp,
                                                        tintOverride = if (bp.isExceeded) extended.expense else null,
                                                        bgOverride = if (bp.isExceeded) extended.expenseContainer else null
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = bp.categoryName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = "${CurrencyFormatter.format(bp.spentAmount, currency, compact = true)} / ${CurrencyFormatter.format(bp.budget.limitAmount, currency, compact = true)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (bp.isExceeded) extended.expense else extended.mutedText
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { bp.progressFraction.coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = if (bp.isExceeded) extended.expense else MaterialTheme.colorScheme.primary,
                                                trackColor = extended.sageTint,
                                                strokeCap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION D — Savings Goals
            item(key = "section_savings_goals") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.savings_goals),
                        actionLabel = "All Goals",
                        actionTestTag = "home_all_goals",
                        onActionClick = onOpenBudgets
                    )
                    if (uiState.savingsGoals.isEmpty()) {
                        EmptyStateCard(
                            title = "Start a savings goal",
                            message = "Build an emergency fund, travel reserve, or milestone target.",
                            actionLabel = "Create Goal",
                            onActionClick = onOpenBudgets,
                            showIllustration = false
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.savingsGoals.take(3).forEach { goal ->
                                val progress = if (goal.targetAmount > 0) {
                                    (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                                } else 0f
                                VerdantCard(contentPadding = PaddingValues(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CategoryIconBadge(iconKey = goal.iconKey, isIncome = true, size = 40.dp)
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = goal.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Target ${DateFormatter.formatDate(goal.targetDateMillis)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = extended.mutedText
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { contributeGoal = goal },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, extended.border)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Add", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = extended.income,
                                        trackColor = extended.sageTint,
                                        strokeCap = StrokeCap.Round
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${CurrencyFormatter.format(goal.currentAmount, currency)} saved",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}% of ${CurrencyFormatter.format(goal.targetAmount, currency, compact = true)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = extended.mutedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION E — Financial Insights
            item(key = "section_financial_insights") {
                val insights = uiState.insights
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.financial_insights),
                        tooltipText = "Calculated dynamically from your current month spending behavior and cash flow.",
                        onTooltipClick = { t, m -> tooltipPair = t to m }
                    )
                    VerdantCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InsightTile(
                                label = "Daily Average",
                                value = if (hideBalance) "${currency.symbol} ••••" else CurrencyFormatter.format(insights.dailyAverageSpend, currency),
                                modifier = Modifier.weight(1f)
                            )
                            InsightTile(
                                label = "Monthly Savings",
                                value = if (hideBalance) "${currency.symbol} ••••" else CurrencyFormatter.format(insights.monthlySavings, currency, showSign = true, isPositive = insights.monthlySavings >= 0),
                                valueColor = if (insights.monthlySavings >= 0) extended.income else extended.expense,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InsightTile(
                                label = "Highest Category",
                                value = insights.highestCategory?.categoryName ?: "—",
                                subtext = insights.highestCategory?.let {
                                    CurrencyFormatter.format(it.totalAmount, currency, compact = true)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            InsightTile(
                                label = "Lowest Category",
                                value = insights.lowestCategory?.categoryName ?: "—",
                                subtext = insights.lowestCategory?.let {
                                    CurrencyFormatter.format(it.totalAmount, currency, compact = true)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (insights.suggestions.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = extended.border)
                            Spacer(Modifier.height(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                insights.suggestions.take(3).forEach { tip ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = tip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    tooltipPair?.let { (title, desc) ->
        MetricTooltipDialog(
            title = title,
            description = desc,
            onDismiss = { tooltipPair = null }
        )
    }

    contributeGoal?.let { goal ->
        QuickContributeGoalDialog(
            goal = goal,
            currencySymbol = currency.symbol,
            onConfirm = { amount ->
                onAddMoneyToGoal(goal, amount)
                contributeGoal = null
            },
            onDismiss = { contributeGoal = null }
        )
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    amount: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val accent = if (isIncome) extended.income else extended.expense
    val badgeBg = if (isIncome) extended.incomeContainer else extended.expenseContainer

    VerdantCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = extended.mutedText
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "This month",
            style = MaterialTheme.typography.labelSmall,
            color = extended.mutedText
        )
    }
}

@Composable
private fun InsightTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val extended = VerdantThemeTokens.colors
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, extended.border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = extended.mutedText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.mutedText
                )
            }
        }
    }
}

@Composable
fun QuickContributeGoalDialog(
    goal: SavingsGoalEntity,
    currencySymbol: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${goal.name}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        error = null
                    },
                    label = { Text("Amount ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amountText.toDoubleOrNull()
                    if (value == null || value <= 0.0) {
                        error = "Enter a valid positive amount"
                    } else {
                        onConfirm(value)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Money")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
