package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AnalyticsPeriod
import com.example.ui.components.CashFlowTrendChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTooltipDialog
import com.example.ui.components.MonthlyComparisonBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingDonutChart
import com.example.ui.components.VerdantCard
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CurrencyFormatter
import com.example.viewmodel.FinanceUiState

@Composable
fun AnalyticsScreen(
    uiState: FinanceUiState,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val currency = uiState.currency
    val hideAmounts = uiState.preferences.hideBalance
    val insights = uiState.insights
    var tooltipPair by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "analytics_header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Analytics",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Cash flow, spending distribution & reports",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )
                        }
                    }

                    // Daily / Weekly / Monthly / Yearly Segmented Control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnalyticsPeriod.entries.forEach { period ->
                            val selected = uiState.analyticsPeriod == period
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { onSelectPeriod(period) }
                                    .testTag("analytics_period_${period.name.lowercase()}"),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(9.dp)
                            ) {
                                Text(
                                    text = period.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else extended.mutedText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Export Actions (PDF & CSV)
            item(key = "analytics_exports") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, extended.border)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export PDF")
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, extended.border)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export CSV")
                    }
                }
            }

            // Key Report KPIs: Income, Expense, Net Cash Flow, Savings Rate
            item(key = "analytics_kpis") {
                VerdantCard {
                    SectionHeader(
                        title = "${uiState.analyticsPeriod.label} Overview",
                        tooltipText = "Savings rate equals (Income - Expenses) / Income for the current month. Cash flow shows net balance change for the chosen period.",
                        onTooltipClick = { t, m -> tooltipPair = t to m }
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsStatCell(
                            label = "Income",
                            value = if (hideAmounts) "${currency.symbol} ••••" else CurrencyFormatter.format(uiState.periodIncome, currency),
                            color = extended.income,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsStatCell(
                            label = "Expenses",
                            value = if (hideAmounts) "${currency.symbol} ••••" else CurrencyFormatter.format(uiState.periodExpense, currency),
                            color = extended.expense,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val cashFlow = uiState.periodIncome - uiState.periodExpense
                        AnalyticsStatCell(
                            label = "Cash Flow",
                            value = if (hideAmounts) "${currency.symbol} ••••" else CurrencyFormatter.format(cashFlow, currency, showSign = true, isPositive = cashFlow >= 0),
                            color = if (cashFlow >= 0) extended.income else extended.expense,
                            modifier = Modifier.weight(1f)
                        )
                        val periodSavingsRate = if (uiState.periodIncome > 0) {
                            ((uiState.periodIncome - uiState.periodExpense) / uiState.periodIncome) * 100.0
                        } else 0.0
                        AnalyticsStatCell(
                            label = "Savings Rate",
                            value = String.format(java.util.Locale.US, "%.1f%%", periodSavingsRate),
                            color = if (periodSavingsRate >= 20.0) extended.income else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Category Breakdown Donut Chart
            item(key = "analytics_donut") {
                if (uiState.categoryBreakdown.isEmpty()) {
                    EmptyStateCard(
                        title = "No spending in this period",
                        message = "Switch to another timeframe or add expenses to view your category breakdown.",
                        showIllustration = false
                    )
                } else {
                    VerdantCard {
                        SectionHeader(
                            title = "Category Breakdown",
                            subtitle = "Tap any category below to inspect its share"
                        )
                        Spacer(Modifier.height(14.dp))
                        SpendingDonutChart(
                            categories = uiState.categoryBreakdown,
                            totalSpent = uiState.periodExpense,
                            currency = currency,
                            hideAmounts = hideAmounts
                        )
                    }
                }
            }

            // Monthly Comparison Bar Chart
            item(key = "analytics_bar") {
                VerdantCard {
                    SectionHeader(
                        title = "Monthly Income vs Expense",
                        subtitle = "Last 6 months comparison"
                    )
                    Spacer(Modifier.height(12.dp))
                    MonthlyComparisonBarChart(
                        comparisons = uiState.monthlyComparisons,
                        currency = currency
                    )
                }
            }

            // Cash Flow Trend Chart
            item(key = "analytics_cashflow_trend") {
                VerdantCard {
                    SectionHeader(
                        title = "Net Cash Flow Trend",
                        subtitle = "Monthly surplus or deficit trajectory"
                    )
                    Spacer(Modifier.height(14.dp))
                    CashFlowTrendChart(comparisons = uiState.monthlyComparisons)
                }
            }

            // Financial Insights & Spending Patterns
            item(key = "analytics_insights") {
                VerdantCard {
                    SectionHeader(
                        title = "Spending Patterns & Insights",
                        subtitle = "Based on your recorded activity"
                    )
                    Spacer(Modifier.height(14.dp))
                    InsightRowItem(
                        label = "Highest Spending Category",
                        value = insights.highestCategory?.let {
                            "${it.categoryName} (${CurrencyFormatter.format(it.totalAmount, currency)})"
                        } ?: "None yet"
                    )
                    HorizontalDivider(color = extended.border, modifier = Modifier.padding(vertical = 10.dp))
                    InsightRowItem(
                        label = "Lowest Spending Category",
                        value = insights.lowestCategory?.let {
                            "${it.categoryName} (${CurrencyFormatter.format(it.totalAmount, currency)})"
                        } ?: "None yet"
                    )
                    HorizontalDivider(color = extended.border, modifier = Modifier.padding(vertical = 10.dp))
                    InsightRowItem(
                        label = "Daily Average Spending",
                        value = CurrencyFormatter.format(insights.dailyAverageSpend, currency)
                    )
                    HorizontalDivider(color = extended.border, modifier = Modifier.padding(vertical = 10.dp))
                    InsightRowItem(
                        label = "Primary Payment Method",
                        value = insights.topPaymentMethod
                    )
                    HorizontalDivider(color = extended.border, modifier = Modifier.padding(vertical = 10.dp))
                    InsightRowItem(
                        label = "Recurring Monthly Bills",
                        value = CurrencyFormatter.format(insights.recurringMonthlyCommitments, currency)
                    )

                    if (insights.suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = extended.sageTint,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Personalized Suggestions",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                insights.suggestions.forEach { tip ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
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
}

@Composable
private fun AnalyticsStatCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, extended.border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = extended.mutedText)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun InsightRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = VerdantThemeTokens.colors.mutedText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
