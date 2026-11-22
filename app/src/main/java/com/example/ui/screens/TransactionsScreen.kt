package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CurrencyOption
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithDetails
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TransactionRow
import com.example.ui.components.VerdantCard
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.viewmodel.FinanceUiState
import java.io.File

@Composable
fun TransactionsScreen(
    uiState: FinanceUiState,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onPaymentMethodFilterChange: (PaymentMethod?) -> Unit,
    onClearFilters: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransaction: (TransactionWithDetails) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val currency = uiState.currency
    val hideAmounts = uiState.preferences.hideBalance
    val filters = uiState.filters
    val hasActiveFilter = filters.query.isNotBlank() ||
        filters.type != null ||
        filters.categoryId != null ||
        filters.paymentMethod != null

    var selectedTransaction by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    val groupedByDay = remember(uiState.filteredTransactions) {
        uiState.filteredTransactions.groupBy { DateFormatter.formatDayHeader(it.transaction.timestamp) }
    }

    val filteredIncome = remember(uiState.filteredTransactions) {
        uiState.filteredTransactions.filter { it.isIncome }.sumOf { it.transaction.amount }
    }
    val filteredExpense = remember(uiState.filteredTransactions) {
        uiState.filteredTransactions.filter { !it.isIncome }.sumOf { it.transaction.amount }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "tx_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${uiState.filteredTransactions.size} entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.mutedText
                        )
                    }
                    if (hasActiveFilter) {
                        TextButton(onClick = onClearFilters) {
                            Text("Reset Filters")
                        }
                    }
                }
            }

            // Search Field
            item(key = "tx_search") {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search title, category, account, notes...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = extended.mutedText
                        )
                    },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = extended.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactions_search_input")
                )
            }

            // Type & Payment Method Filter Chips
            item(key = "tx_filter_chips") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filters.type == null,
                                onClick = { onTypeFilterChange(null) },
                                label = { Text("All") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filters.type == TransactionType.EXPENSE,
                                onClick = {
                                    onTypeFilterChange(
                                        if (filters.type == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                                    )
                                },
                                label = { Text("Expenses") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = extended.expense,
                                    selectedLabelColor = MaterialTheme.colorScheme.onError
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filters.type == TransactionType.INCOME,
                                onClick = {
                                    onTypeFilterChange(
                                        if (filters.type == TransactionType.INCOME) null else TransactionType.INCOME
                                    )
                                },
                                label = { Text("Income") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = extended.income,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        items(PaymentMethod.entries) { method ->
                            FilterChip(
                                selected = filters.paymentMethod == method,
                                onClick = {
                                    onPaymentMethodFilterChange(
                                        if (filters.paymentMethod == method) null else method
                                    )
                                },
                                label = { Text(method.label) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Category quick chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.categories.take(12), key = { it.id }) { cat ->
                            FilterChip(
                                selected = filters.categoryId == cat.id,
                                onClick = {
                                    onCategoryFilterChange(
                                        if (filters.categoryId == cat.id) null else cat.id
                                    )
                                },
                                label = { Text(cat.name) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Compact Filtered Summary Bar
            item(key = "tx_summary_bar") {
                VerdantCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Inflow", style = MaterialTheme.typography.labelSmall, color = extended.mutedText)
                            Text(
                                text = if (hideAmounts) "${currency.symbol} ••••" else CurrencyFormatter.format(filteredIncome, currency, showSign = true, isPositive = true),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = extended.income
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Outflow", style = MaterialTheme.typography.labelSmall, color = extended.mutedText)
                            Text(
                                text = if (hideAmounts) "${currency.symbol} ••••" else CurrencyFormatter.format(filteredExpense, currency, showSign = true, isPositive = false),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = extended.expense
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val net = filteredIncome - filteredExpense
                            Text("Net", style = MaterialTheme.typography.labelSmall, color = extended.mutedText)
                            Text(
                                text = if (hideAmounts) "${currency.symbol} ••••" else CurrencyFormatter.format(net, currency, showSign = true, isPositive = net >= 0),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (net >= 0) extended.income else extended.expense
                            )
                        }
                    }
                }
            }

            // Grouped Transaction List
            if (uiState.filteredTransactions.isEmpty()) {
                item(key = "tx_empty") {
                    EmptyStateCard(
                        title = if (hasActiveFilter) "No matching transactions" else "Your ledger is empty",
                        message = if (hasActiveFilter) {
                            "Try adjusting your search query or clearing active filters."
                        } else {
                            "Tap '+' to record an expense or income entry."
                        },
                        actionLabel = if (hasActiveFilter) "Reset Filters" else "Add Transaction",
                        onActionClick = if (hasActiveFilter) onClearFilters else onAddTransactionClick
                    )
                }
            } else {
                groupedByDay.forEach { (dateLabel, itemsInDay) ->
                    item(key = "header_$dateLabel") {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = extended.mutedText,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    item(key = "group_$dateLabel") {
                        VerdantCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                            itemsInDay.forEachIndexed { idx, item ->
                                TransactionRow(
                                    item = item,
                                    currency = currency,
                                    hideAmounts = hideAmounts,
                                    onClick = { selectedTransaction = item }
                                )
                                if (idx < itemsInDay.lastIndex) {
                                    HorizontalDivider(color = extended.border)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { detail ->
        TransactionDetailBottomSheet(
            item = detail,
            currency = currency,
            onDismiss = { selectedTransaction = null },
            onEdit = {
                selectedTransaction = null
                onEditTransaction(detail)
            },
            onDeleteRequest = {
                selectedTransaction = null
                pendingDeleteId = detail.transaction.id
            }
        )
    }

    pendingDeleteId?.let { idToDelete ->
        DeleteConfirmDialog(
            onConfirm = {
                onDeleteTransaction(idToDelete)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    item: TransactionWithDetails,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val extended = VerdantThemeTokens.colors
    val tx = item.transaction
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CategoryIconBadge(
                    iconKey = item.categoryIconKey,
                    isIncome = item.isIncome,
                    size = 48.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.mutedText
                    )
                }
                Text(
                    text = CurrencyFormatter.format(
                        amountInInr = tx.amount,
                        currency = currency,
                        showSign = true,
                        isPositive = item.isIncome
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (item.isIncome) extended.income else extended.expense
                )
            }

            HorizontalDivider(color = extended.border)

            DetailMetaRow("Date & Time", DateFormatter.formatDateTime(tx.timestamp))
            DetailMetaRow("Account", item.accountName)
            DetailMetaRow("Payment Method", item.paymentMethodEnum.label)
            if (tx.isRecurring) {
                DetailMetaRow("Recurring", tx.recurringFrequency.lowercase().replaceFirstChar { it.uppercase() })
            }
            if (tx.notes.isNotBlank()) {
                DetailMetaRow("Notes", tx.notes)
            }

            if (!tx.receiptUri.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Receipt Attachment",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.mutedText
                    )
                    val model: Any = if (tx.receiptUri.startsWith("/")) File(tx.receiptUri) else tx.receiptUri
                    AsyncImage(
                        model = model,
                        contentDescription = "Receipt image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(extended.sageTint),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_delete_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, extended.expense.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = extended.expense,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", color = extended.expense)
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_edit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun DetailMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = VerdantThemeTokens.colors.mutedText
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
