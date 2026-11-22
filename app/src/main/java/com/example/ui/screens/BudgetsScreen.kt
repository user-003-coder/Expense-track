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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.BudgetPeriod
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringFrequency
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerdantCard
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.viewmodel.FinanceUiState
import java.util.Calendar
import kotlin.math.abs

@Composable
fun BudgetsScreen(
    uiState: FinanceUiState,
    onSaveBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onSaveGoal: (SavingsGoalEntity) -> Unit,
    onAddMoneyToGoal: (SavingsGoalEntity, Double) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onSaveBill: (BillReminderEntity) -> Unit,
    onToggleBillPaid: (BillReminderEntity) -> Unit,
    onDeleteBill: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = VerdantThemeTokens.colors
    val currency = uiState.currency
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Budgets, 1 = Goals, 2 = Bills
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var contributeGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showBillDialog by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<BillReminderEntity?>(null) }
    var pendingDeleteAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val expenseCategories = remember(uiState.categories) {
        uiState.categories.filter { it.type == TransactionType.EXPENSE.name }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "budgets_header") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Planning",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Budgets, savings goals & recurring bills",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )
                        }
                        Button(
                            onClick = {
                                when (activeTab) {
                                    0 -> {
                                        editingBudget = null
                                        showBudgetDialog = true
                                    }
                                    1 -> {
                                        editingGoal = null
                                        showGoalDialog = true
                                    }
                                    else -> {
                                        editingBill = null
                                        showBillDialog = true
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("budgets_primary_add_button")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when (activeTab) {
                                    0 -> "Budget"
                                    1 -> "Goal"
                                    else -> "Bill"
                                }
                            )
                        }
                    }

                    // 3-way Segmented Pill
                    val tabTitles = listOf("Budgets", "Goals", "Bills")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabTitles.forEachIndexed { idx, label ->
                            val selected = activeTab == idx
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { activeTab = idx }
                                    .testTag("planning_tab_${label.lowercase()}"),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(9.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else extended.mutedText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            when (activeTab) {
                // ================= TAB 0: BUDGETS =================
                0 -> {
                    if (uiState.currentMonthBudgets.isEmpty()) {
                        item(key = "budgets_empty") {
                            EmptyStateCard(
                                title = "No budgets configured",
                                message = "Create an overall monthly cap or category-specific spending limits.",
                                actionLabel = "Create Budget",
                                onActionClick = {
                                    editingBudget = null
                                    showBudgetDialog = true
                                }
                            )
                        }
                    } else {
                        items(uiState.currentMonthBudgets, key = { it.budget.id }) { bp ->
                            VerdantCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CategoryIconBadge(
                                            iconKey = bp.iconKey,
                                            tintOverride = if (bp.isExceeded) extended.expense else null,
                                            bgOverride = if (bp.isExceeded) extended.expenseContainer else null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = bp.categoryName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${bp.budget.period.lowercase().replaceFirstChar { it.uppercase() }}  •  ${DateFormatter.monthYearLabel(bp.budget.monthKey)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extended.mutedText
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingBudget = bp.budget
                                            showBudgetDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "Edit budget",
                                                tint = extended.mutedText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = {
                                            pendingDeleteAction = "Delete ${bp.categoryName} budget?" to {
                                                onDeleteBudget(bp.budget.id)
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Delete budget",
                                                tint = extended.expense,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    progress = { bp.progressFraction.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = when {
                                        bp.isExceeded -> extended.expense
                                        bp.isNearLimit -> extended.warning
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    trackColor = extended.sageTint,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spent ${CurrencyFormatter.format(bp.spentAmount, currency)} of ${CurrencyFormatter.format(bp.budget.limitAmount, currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (bp.isExceeded) {
                                            "Over by ${CurrencyFormatter.format(abs(bp.remainingAmount), currency)}"
                                        } else {
                                            "${CurrencyFormatter.format(bp.remainingAmount, currency)} left"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (bp.isExceeded) extended.expense else extended.income
                                    )
                                }
                            }
                        }
                    }

                    // Budget History Section
                    if (uiState.historicalBudgets.isNotEmpty()) {
                        item(key = "budget_history_header") {
                            SectionHeader(
                                title = "Budget History",
                                subtitle = "Previous month performance"
                            )
                        }
                        items(uiState.historicalBudgets, key = { "hist_${it.budget.id}" }) { bp ->
                            VerdantCard(contentPadding = PaddingValues(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${bp.categoryName} (${DateFormatter.monthYearLabel(bp.budget.monthKey)})",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Spent ${CurrencyFormatter.format(bp.spentAmount, currency)} / ${CurrencyFormatter.format(bp.budget.limitAmount, currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.mutedText
                                        )
                                    }
                                    Surface(
                                        color = if (bp.isExceeded) extended.expenseContainer else extended.incomeContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (bp.isExceeded) "Exceeded" else "On Track",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (bp.isExceeded) extended.expense else extended.income,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= TAB 1: SAVINGS GOALS =================
                1 -> {
                    if (uiState.savingsGoals.isEmpty()) {
                        item(key = "goals_empty") {
                            EmptyStateCard(
                                title = "No savings goals yet",
                                message = "Create dedicated goals to visualize progress toward milestones.",
                                actionLabel = "Create Goal",
                                onActionClick = {
                                    editingGoal = null
                                    showGoalDialog = true
                                }
                            )
                        }
                    } else {
                        items(uiState.savingsGoals, key = { it.id }) { goal ->
                            val progress = if (goal.targetAmount > 0) {
                                (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                            } else 0f
                            val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                            VerdantCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CategoryIconBadge(iconKey = goal.iconKey, isIncome = true)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(goal.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "Target: ${DateFormatter.formatDate(goal.targetDateMillis)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extended.mutedText
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingGoal = goal
                                            showGoalDialog = true
                                        }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit goal", tint = extended.mutedText, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = {
                                            pendingDeleteAction = "Delete ${goal.name} goal?" to {
                                                onDeleteGoal(goal.id)
                                            }
                                        }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete goal", tint = extended.expense, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = extended.income,
                                    trackColor = extended.sageTint,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${CurrencyFormatter.format(goal.currentAmount, currency)} / ${CurrencyFormatter.format(goal.targetAmount, currency)}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}% reached  •  ${CurrencyFormatter.format(remaining, currency)} to go",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.mutedText
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { contributeGoal = goal },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, extended.border)
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Money")
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= TAB 2: BILLS & SUBSCRIPTIONS =================
                else -> {
                    val totalBills = uiState.billReminders.sumOf { it.amount }
                    val unpaidCount = uiState.billReminders.count { !it.isPaidThisCycle }
                    item(key = "bills_summary") {
                        VerdantCard(contentPadding = PaddingValues(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Monthly Recurring Total",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = extended.mutedText
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(totalBills, currency),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Surface(
                                    color = if (unpaidCount > 0) extended.warningContainer else extended.incomeContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (unpaidCount > 0) "$unpaidCount due" else "All settled",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (unpaidCount > 0) extended.warning else extended.income,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.billReminders.isEmpty()) {
                        item(key = "bills_empty") {
                            EmptyStateCard(
                                title = "No bills or subscriptions",
                                message = "Track recurring rent, utilities, and subscriptions so you never miss a due date.",
                                actionLabel = "Add Reminder",
                                onActionClick = {
                                    editingBill = null
                                    showBillDialog = true
                                },
                                showIllustration = false
                            )
                        }
                    } else {
                        items(uiState.billReminders, key = { it.id }) { bill ->
                            VerdantCard(contentPadding = PaddingValues(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onToggleBillPaid(bill) },
                                        modifier = Modifier.testTag("bill_toggle_${bill.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (bill.isPaidThisCycle) {
                                                Icons.Outlined.CheckCircle
                                            } else {
                                                Icons.Outlined.RadioButtonUnchecked
                                            },
                                            contentDescription = "Mark paid",
                                            tint = if (bill.isPaidThisCycle) extended.income else extended.mutedText
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = bill.title,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            if (bill.isSubscription) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    color = extended.sageTint,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Sub",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Due day ${bill.dueDayOfMonth}  •  ${bill.paymentMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.mutedText
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyFormatter.format(bill.amount, currency),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (bill.isPaidThisCycle) extended.mutedText else extended.expense
                                        )
                                        Text(
                                            text = if (bill.isPaidThisCycle) "Paid" else "Tap circle to pay",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (bill.isPaidThisCycle) extended.income else extended.mutedText
                                        )
                                    }
                                    IconButton(onClick = {
                                        pendingDeleteAction = "Delete ${bill.title}?" to {
                                            onDeleteBill(bill.id)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete bill",
                                            tint = extended.mutedText,
                                            modifier = Modifier.size(18.dp)
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

    if (showBudgetDialog) {
        BudgetEditorDialog(
            existing = editingBudget,
            expenseCategories = expenseCategories,
            currencySymbol = currency.symbol,
            onSave = { budget ->
                onSaveBudget(budget)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }

    if (showGoalDialog) {
        GoalEditorDialog(
            existing = editingGoal,
            currencySymbol = currency.symbol,
            onSave = { goal ->
                onSaveGoal(goal)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }

    if (showBillDialog) {
        BillEditorDialog(
            existing = editingBill,
            expenseCategories = expenseCategories,
            currencySymbol = currency.symbol,
            onSave = { bill ->
                onSaveBill(bill)
                showBillDialog = false
            },
            onDismiss = { showBillDialog = false }
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

    pendingDeleteAction?.let { (title, action) ->
        DeleteConfirmDialog(
            title = title,
            message = "This item will be permanently removed.",
            onConfirm = {
                action()
                pendingDeleteAction = null
            },
            onDismiss = { pendingDeleteAction = null }
        )
    }
}

@Composable
private fun BudgetEditorDialog(
    existing: BudgetEntity?,
    expenseCategories: List<CategoryEntity>,
    currencySymbol: String,
    onSave: (BudgetEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(existing?.categoryId) }
    var limitText by remember { mutableStateOf(existing?.limitAmount?.toLong()?.toString() ?: "") }
    var period by remember {
        mutableStateOf(
            BudgetPeriod.entries.firstOrNull { it.name == existing?.period } ?: BudgetPeriod.MONTHLY
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create Budget" else "Edit Budget", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Scope", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Overall") }
                        )
                    }
                    items(expenseCategories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        error = null
                    },
                    label = { Text("Limit Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
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
                    val limit = limitText.toDoubleOrNull()
                    if (limit == null || limit <= 0.0) {
                        error = "Enter a valid budget limit"
                    } else {
                        val catName = expenseCategories.firstOrNull { it.id == selectedCategoryId }?.name
                            ?: "Overall ${period.label} Budget"
                        onSave(
                            BudgetEntity(
                                id = existing?.id ?: 0L,
                                categoryId = selectedCategoryId,
                                name = catName,
                                limitAmount = limit,
                                period = period.name,
                                monthKey = existing?.monthKey ?: DateFormatter.monthKey(System.currentTimeMillis())
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun GoalEditorDialog(
    existing: SavingsGoalEntity?,
    currencySymbol: String,
    onSave: (SavingsGoalEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var targetText by remember { mutableStateOf(existing?.targetAmount?.toLong()?.toString() ?: "") }
    var savedText by remember { mutableStateOf(existing?.currentAmount?.toLong()?.toString() ?: "0") }
    var monthsToTarget by remember { mutableStateOf("6") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Savings Goal" else "Edit Goal", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g., Emergency Fund, Japan Trip") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    label = { Text("Target Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = savedText,
                    onValueChange = { savedText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Already Saved ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (existing == null) {
                    OutlinedTextField(
                        value = monthsToTarget,
                        onValueChange = { monthsToTarget = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Timeline (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = VerdantThemeTokens.colors.expense)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull()
                    val saved = savedText.toDoubleOrNull() ?: 0.0
                    if (name.trim().isEmpty() || target == null || target <= 0.0) {
                        error = "Enter a valid goal name and target amount"
                    } else {
                        val targetMillis = existing?.targetDateMillis ?: Calendar.getInstance().apply {
                            add(Calendar.MONTH, (monthsToTarget.toIntOrNull() ?: 6).coerceIn(1, 120))
                        }.timeInMillis
                        onSave(
                            SavingsGoalEntity(
                                id = existing?.id ?: 0L,
                                name = name.trim(),
                                targetAmount = target,
                                currentAmount = saved.coerceAtLeast(0.0),
                                targetDateMillis = targetMillis,
                                iconKey = existing?.iconKey ?: "savings"
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun BillEditorDialog(
    existing: BillReminderEntity?,
    expenseCategories: List<CategoryEntity>,
    currencySymbol: String,
    onSave: (BillReminderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toLong()?.toString() ?: "") }
    var dueDayText by remember { mutableStateOf(existing?.dueDayOfMonth?.toString() ?: "10") }
    var isSubscription by remember { mutableStateOf(existing?.isSubscription ?: false) }
    var method by remember {
        mutableStateOf(
            PaymentMethod.entries.firstOrNull { it.name == existing?.paymentMethod } ?: PaymentMethod.UPI
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Bill / Subscription" else "Edit Reminder", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., Internet Bill, Netflix") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it.filter { c -> c.isDigit() } },
                    label = { Text("Due Day of Month (1–31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as Subscription", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isSubscription, onCheckedChange = { isSubscription = it })
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PaymentMethod.entries) { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.label) }
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = VerdantThemeTokens.colors.expense)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    val day = (dueDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                    if (title.trim().isEmpty() || amt == null || amt <= 0.0) {
                        error = "Enter a valid title and amount"
                    } else {
                        val defaultCatId = expenseCategories.firstOrNull {
                            it.name.contains(if (isSubscription) "Sub" else "Bill", ignoreCase = true)
                        }?.id ?: expenseCategories.firstOrNull()?.id ?: 4L
                        onSave(
                            BillReminderEntity(
                                id = existing?.id ?: 0L,
                                title = title.trim(),
                                amount = amt,
                                categoryId = existing?.categoryId ?: defaultCatId,
                                dueDayOfMonth = day,
                                isSubscription = isSubscription,
                                frequency = RecurringFrequency.MONTHLY.name,
                                isPaidThisCycle = existing?.isPaidThisCycle ?: false,
                                paymentMethod = method.name
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
