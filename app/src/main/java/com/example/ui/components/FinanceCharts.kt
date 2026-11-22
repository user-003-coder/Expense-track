package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategorySpend
import com.example.data.model.CurrencyOption
import com.example.data.model.MonthlyComparison
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CurrencyFormatter
import kotlin.math.max
import kotlin.math.min

@Composable
fun SpendingDonutChart(
    categories: List<CategorySpend>,
    totalSpent: Double,
    currency: CurrencyOption,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val palette = extended.chartPalette
    var selectedIndex by remember(categories) { mutableIntStateOf(-1) }
    val displayList = categories.take(6)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(172.dp)
            ) {
                val strokeWidth = 22.dp.toPx()
                val diameter = min(size.width, size.height) - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                if (displayList.isEmpty() || totalSpent <= 0.0) {
                    drawArc(
                        color = extended.border,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                } else {
                    var startAngle = -90f
                    displayList.forEachIndexed { idx, slice ->
                        val sweep = (slice.percentage * 360f).coerceAtLeast(3f)
                        val isSelected = idx == selectedIndex
                        drawArc(
                            color = palette[idx % palette.size],
                            startAngle = startAngle,
                            sweepAngle = (sweep - 2.5f).coerceAtLeast(1.5f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) strokeWidth + 5.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += sweep
                    }
                }
            }

            val focused = displayList.getOrNull(selectedIndex)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = focused?.categoryName ?: "Total Spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (hideAmounts) {
                        "${currency.symbol} ••••"
                    } else {
                        CurrencyFormatter.format(
                            amountInInr = focused?.totalAmount ?: totalSpent,
                            currency = currency,
                            compact = true
                        )
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (focused != null) {
                    Text(
                        text = "${(focused.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (displayList.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayList.forEachIndexed { idx, cat ->
                    val isSelected = idx == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) extended.sageTint else MaterialTheme.colorScheme.surface)
                            .clickable {
                                selectedIndex = if (selectedIndex == idx) -1 else idx
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(palette[idx % palette.size])
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = cat.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${(cat.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = extended.mutedText
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (hideAmounts) "${currency.symbol} ••••" else CurrencyFormatter.format(cat.totalAmount, currency),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyComparisonBarChart(
    comparisons: List<MonthlyComparison>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val maxVal = max(1.0, comparisons.maxOfOrNull { max(it.income, it.expense) } ?: 1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = extended.income, label = "Income")
            Spacer(Modifier.width(16.dp))
            LegendDot(color = extended.expense, label = "Expense")
        }
        Spacer(Modifier.height(14.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
        ) {
            val count = comparisons.size.coerceAtLeast(1)
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceIn(8.dp.toPx(), 18.dp.toPx())
            val barGap = 4.dp.toPx()
            val chartHeight = size.height - 8.dp.toPx()

            // Subtle horizontal guide lines
            for (step in 0..3) {
                val y = chartHeight * (step / 3f) + 4.dp.toPx()
                drawLine(
                    color = extended.border,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            comparisons.forEachIndexed { idx, month ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val incomeHeight = ((month.income / maxVal) * chartHeight).toFloat().coerceAtLeast(
                    if (month.income > 0) 4.dp.toPx() else 0f
                )
                val expenseHeight = ((month.expense / maxVal) * chartHeight).toFloat().coerceAtLeast(
                    if (month.expense > 0) 4.dp.toPx() else 0f
                )

                // Income Bar
                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = extended.income,
                        topLeft = Offset(centerX - barWidth - barGap / 2f, size.height - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }

                // Expense Bar
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = extended.expense,
                        topLeft = Offset(centerX + barGap / 2f, size.height - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            comparisons.forEach { month ->
                Text(
                    text = month.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.mutedText
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val latest = comparisons.lastOrNull()
        if (latest != null) {
            Text(
                text = "Latest (${latest.label}): Net ${CurrencyFormatter.format(latest.net, currency, showSign = true, isPositive = latest.net >= 0)}",
                style = MaterialTheme.typography.bodySmall,
                color = extended.mutedText
            )
        }
    }
}

@Composable
fun CashFlowTrendChart(
    comparisons: List<MonthlyComparison>,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val values = comparisons.map { it.net }
    val minVal = min(0.0, values.minOrNull() ?: 0.0)
    val maxVal = max(1.0, values.maxOrNull() ?: 1.0)
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            if (values.size < 2) return@Canvas
            val stepX = size.width / (values.size - 1)
            val padY = 12.dp.toPx()
            val usableH = size.height - padY * 2

            val zeroY = size.height - padY - (((0.0 - minVal) / range) * usableH).toFloat()
            drawLine(
                color = extended.border,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx()
            )

            val path = Path()
            val points = values.mapIndexed { i, v ->
                val x = i * stepX
                val y = size.height - padY - (((v - minVal) / range) * usableH).toFloat()
                Offset(x, y)
            }

            points.forEachIndexed { i, pt ->
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { idx, pt ->
                val dotColor = if (values[idx] >= 0) extended.income else extended.expense
                drawCircle(color = dotColor, radius = 4.dp.toPx(), center = pt)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            comparisons.forEach { m ->
                Text(
                    text = m.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.mutedText
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = VerdantThemeTokens.colors.mutedText
        )
    }
}
