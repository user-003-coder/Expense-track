package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AlertNotification
import com.example.data.model.CurrencyOption
import com.example.data.model.TransactionWithDetails
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CategoryIcons
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter

@Composable
fun VerdantCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = VerdantThemeTokens.colors
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, extended.border),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    tooltipText: String? = null,
    onTooltipClick: ((String, String) -> Unit)? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (tooltipText != null && onTooltipClick != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { onTooltipClick(title, tooltipText) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info about $title",
                            tint = VerdantThemeTokens.colors.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VerdantThemeTokens.colors.mutedText
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CategoryIconBadge(
    iconKey: String,
    isIncome: Boolean = false,
    size: Dp = 42.dp,
    tintOverride: Color? = null,
    bgOverride: Color? = null
) {
    val extended = VerdantThemeTokens.colors
    val bgColor = bgOverride ?: if (isIncome) extended.incomeContainer else extended.sageTint
    val tintColor = tintOverride ?: if (isIncome) extended.income else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryIcons.resolve(iconKey),
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun TransactionRow(
    item: TransactionWithDetails,
    currency: CurrencyOption,
    hideAmounts: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val tx = item.transaction

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(
            iconKey = item.categoryIconKey,
            isIncome = item.isIncome
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (tx.isRecurring) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Autorenew,
                        contentDescription = "Recurring",
                        tint = extended.mutedText,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (!tx.receiptUri.isNullOrBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Has receipt",
                        tint = extended.mutedText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.categoryName}  •  ${DateFormatter.formatDateTime(tx.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = extended.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (hideAmounts) {
                    "${currency.symbol} ••••"
                } else {
                    CurrencyFormatter.format(
                        amountInInr = tx.amount,
                        currency = currency,
                        showSign = true,
                        isPositive = item.isIncome
                    )
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (item.isIncome) extended.income else extended.expense
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.paymentMethodEnum.label,
                style = MaterialTheme.typography.labelSmall,
                color = extended.mutedText
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    showIllustration: Boolean = true,
    modifier: Modifier = Modifier
) {
    VerdantCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showIllustration) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_ledger_1795332177223),
                    contentDescription = stringResource(R.string.cd_empty_state),
                    modifier = Modifier
                        .size(width = 160.dp, height = 116.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(14.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = VerdantThemeTokens.colors.mutedText
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun MetricTooltipDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = VerdantThemeTokens.colors.mutedText
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DeleteConfirmDialog(
    title: String = stringResource(R.string.delete_confirm_title),
    message: String = stringResource(R.string.delete_confirm_message),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val extended = VerdantThemeTokens.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.mutedText
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = extended.expense),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsBottomSheet(
    alerts: List<AlertNotification>,
    onDismiss: () -> Unit,
    onOpenBudgets: () -> Unit
) {
    val extended = VerdantThemeTokens.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alerts & Reminders",
                    style = MaterialTheme.typography.headlineMedium
                )
                TextButton(onClick = {
                    onDismiss()
                    onOpenBudgets()
                }) {
                    Text("Manage")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (alerts.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = extended.mutedText
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "You're all caught up. No pending alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.mutedText
                    )
                }
            } else {
                alerts.forEachIndexed { index, alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (alert.isWarning) extended.expenseContainer else extended.sageTint
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (alert.isWarning) {
                                    Icons.Outlined.WarningAmber
                                } else {
                                    Icons.Outlined.NotificationsNone
                                },
                                contentDescription = null,
                                tint = if (alert.isWarning) extended.expense else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = if (alert.isWarning) extended.expenseContainer else extended.sageTint,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = alert.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (alert.isWarning) extended.expense else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )
                        }
                    }
                    if (index < alerts.lastIndex) {
                        HorizontalDivider(color = extended.border)
                    }
                }
            }
        }
    }
}
