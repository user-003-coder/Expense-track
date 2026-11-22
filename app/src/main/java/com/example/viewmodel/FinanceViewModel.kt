package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VerdantDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountWithBalance
import com.example.data.model.AlertNotification
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.BudgetPeriod
import com.example.data.model.BudgetProgress
import com.example.data.model.CategoryEntity
import com.example.data.model.CategorySpend
import com.example.data.model.CurrencyOption
import com.example.data.model.FinancialInsights
import com.example.data.model.MonthlyComparison
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithDetails
import com.example.data.model.UserPreferences
import com.example.data.repository.FinanceRepository
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.SecurityHash
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

data class TransactionFilters(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val paymentMethod: PaymentMethod? = null
)

data class FinanceUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val currency: CurrencyOption = CurrencyOption.INR,
    val accounts: List<AccountWithBalance> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val allTransactions: List<TransactionWithDetails> = emptyList(),
    val filteredTransactions: List<TransactionWithDetails> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val totalBalance: Double = 0.0,
    val monthOverMonthPercent: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val currentMonthExpense: Double = 0.0,
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.MONTHLY,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val monthlyComparisons: List<MonthlyComparison> = emptyList(),
    val currentMonthBudgets: List<BudgetProgress> = emptyList(),
    val historicalBudgets: List<BudgetProgress> = emptyList(),
    val overallBudget: BudgetProgress? = null,
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val billReminders: List<BillReminderEntity> = emptyList(),
    val insights: FinancialInsights = FinancialInsights(
        highestCategory = null,
        lowestCategory = null,
        dailyAverageSpend = 0.0,
        monthlySavings = 0.0,
        savingsRatePercent = 0.0,
        cashFlowNet = 0.0,
        topPaymentMethod = "UPI",
        recurringMonthlyCommitments = 0.0,
        suggestions = emptyList()
    ),
    val alerts: List<AlertNotification> = emptyList(),
    val filters: TransactionFilters = TransactionFilters()
)

private data class RawDatabaseBundle(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<SavingsGoalEntity>,
    val bills: List<BillReminderEntity>
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VerdantDatabase.getInstance(application)
    private val repository = FinanceRepository(database.verdantDao())
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.MONTHLY)
    private val _filters = MutableStateFlow(TransactionFilters())
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    private val rawDataFlow = combine(
        repository.accounts,
        repository.categories,
        repository.transactions,
        repository.budgets,
        repository.savingsGoals
    ) { accounts, categories, transactions, budgets, goals ->
        RawDatabaseBundle(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            bills = emptyList()
        )
    }.combine(repository.billReminders) { bundle, bills ->
        bundle.copy(bills = bills)
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        rawDataFlow,
        preferencesRepository.preferencesFlow,
        _selectedPeriod,
        _filters
    ) { raw, prefs, period, filters ->
        computeUiState(raw, prefs, period, filters)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedHash = uiState.value.preferences.pinHash ?: return true
        val matched = SecurityHash.sha256(enteredPin) == storedHash
        if (matched) _isUnlocked.value = true
        return matched
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _selectedPeriod.value = period
    }

    fun updateSearchQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun updateTypeFilter(type: TransactionType?) {
        _filters.value = _filters.value.copy(type = type)
    }

    fun updateCategoryFilter(categoryId: Long?) {
        _filters.value = _filters.value.copy(categoryId = categoryId)
    }

    fun updateAccountFilter(accountId: Long?) {
        _filters.value = _filters.value.copy(accountId = accountId)
    }

    fun updatePaymentMethodFilter(method: PaymentMethod?) {
        _filters.value = _filters.value.copy(paymentMethod = method)
    }

    fun clearFilters() {
        _filters.value = TransactionFilters()
    }

    // Preferences Actions
    fun toggleHideBalance() {
        viewModelScope.launch {
            preferencesRepository.setHideBalance(!uiState.value.preferences.hideBalance)
        }
    }

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            preferencesRepository.updateProfile(name, email)
            _messages.emit("Profile updated")
        }
    }

    fun setCurrency(option: CurrencyOption) {
        viewModelScope.launch {
            preferencesRepository.setCurrency(option.code)
            _messages.emit("Currency switched to ${option.code}")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun enableAppLock(pin: String) {
        viewModelScope.launch {
            preferencesRepository.setAppLock(enabled = true, pinHash = SecurityHash.sha256(pin))
            _isUnlocked.value = true
            _messages.emit("App security lock enabled")
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            preferencesRepository.setAppLock(enabled = false)
            _isUnlocked.value = true
            _messages.emit("App lock disabled")
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBudgetAlertsEnabled(enabled)
        }
    }

    fun setPrivacyScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPrivacyScreenEnabled(enabled)
            _messages.emit(if (enabled) "Screen privacy protection enabled" else "Screen privacy disabled")
        }
    }

    // CRUD Operations
    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.saveTransaction(transaction)
            _messages.emit(if (transaction.id == 0L) "Transaction added" else "Transaction updated")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _messages.emit("Transaction deleted")
        }
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.saveAccount(account)
            _messages.emit(if (account.id == 0L) "Account created" else "Account updated")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            if (uiState.value.accounts.size <= 1) {
                _messages.emit("Keep at least one account active")
                return@launch
            }
            repository.deleteAccount(account)
            _messages.emit("Account removed")
        }
    }

    fun addCustomCategory(name: String, type: TransactionType, iconKey: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@launch
            repository.addCategory(
                CategoryEntity(
                    name = trimmed,
                    type = type.name,
                    iconKey = iconKey,
                    isCustom = true,
                    sortOrder = 99
                )
            )
            _messages.emit("Category \"$trimmed\" added")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (!category.isCustom) {
                _messages.emit("Predefined categories cannot be deleted")
                return@launch
            }
            repository.deleteCategory(category)
            _messages.emit("Category removed")
        }
    }

    fun saveBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.saveBudget(budget)
            _messages.emit(if (budget.id == 0L) "Budget created" else "Budget updated")
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            _messages.emit("Budget deleted")
        }
    }

    fun saveGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.saveGoal(goal)
            _messages.emit(if (goal.id == 0L) "Savings goal created" else "Goal updated")
        }
    }

    fun addMoneyToGoal(goal: SavingsGoalEntity, amountToAdd: Double) {
        if (amountToAdd <= 0.0) return
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + amountToAdd)
            repository.saveGoal(updated)
            val currency = uiState.value.currency
            _messages.emit(
                "Added ${CurrencyFormatter.format(amountToAdd, currency)} to ${goal.name}"
            )
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            _messages.emit("Savings goal removed")
        }
    }

    fun saveBillReminder(bill: BillReminderEntity) {
        viewModelScope.launch {
            repository.saveBillReminder(bill)
            _messages.emit(if (bill.id == 0L) "Reminder saved" else "Reminder updated")
        }
    }

    fun markBillPaid(bill: BillReminderEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = bill.copy(
                isPaidThisCycle = !bill.isPaidThisCycle,
                lastPaidTimestamp = if (!bill.isPaidThisCycle) now else bill.lastPaidTimestamp
            )
            repository.saveBillReminder(updated)
            if (!bill.isPaidThisCycle) {
                val defaultAccId = uiState.value.accounts.firstOrNull()?.account?.id ?: 1L
                repository.saveTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE.name,
                        amount = bill.amount,
                        title = bill.title,
                        categoryId = bill.categoryId,
                        accountId = defaultAccId,
                        paymentMethod = bill.paymentMethod,
                        notes = if (bill.isSubscription) "Subscription payment" else "Scheduled bill payment",
                        timestamp = now,
                        isRecurring = true,
                        recurringFrequency = bill.frequency
                    )
                )
                _messages.emit("Marked \"${bill.title}\" paid & logged expense")
            } else {
                _messages.emit("Marked \"${bill.title}\" as unpaid")
            }
        }
    }

    fun deleteBillReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteBillReminder(id)
            _messages.emit("Reminder deleted")
        }
    }

    // Export, Backup & Data Controls
    fun exportCsvToUri(uri: Uri) {
        viewModelScope.launch {
            val state = uiState.value
            val result = repository.exportCsv(
                context = getApplication(),
                uri = uri,
                items = state.allTransactions,
                currency = state.currency
            )
            result.onSuccess { count ->
                _messages.emit("Exported $count transactions as CSV")
            }.onFailure {
                _messages.emit("CSV export failed: ${it.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun exportPdfToUri(uri: Uri) {
        viewModelScope.launch {
            val state = uiState.value
            val result = repository.exportPdfReport(
                context = getApplication(),
                uri = uri,
                userName = state.preferences.userName,
                periodLabel = state.analyticsPeriod.label,
                totalBalance = state.totalBalance,
                periodIncome = state.periodIncome,
                periodExpense = state.periodExpense,
                items = state.allTransactions,
                currency = state.currency
            )
            result.onSuccess {
                _messages.emit("PDF financial report exported")
            }.onFailure {
                _messages.emit("PDF export failed: ${it.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val result = repository.exportBackupJson(getApplication(), uri)
            result.onSuccess {
                _messages.emit("Encrypted-ready JSON backup saved")
            }.onFailure {
                _messages.emit("Backup failed: ${it.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = repository.restoreBackupJson(getApplication(), uri)
            result.onSuccess { count ->
                _messages.emit("Restored backup ($count transactions)")
            }.onFailure {
                _messages.emit("Restore failed: Invalid backup file")
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
            _messages.emit("Sample financial data loaded")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _messages.emit("All records cleared")
        }
    }

    private fun computeUiState(
        raw: RawDatabaseBundle,
        prefs: UserPreferences,
        period: AnalyticsPeriod,
        filters: TransactionFilters
    ): FinanceUiState {
        val currency = CurrencyOption.fromCode(prefs.currencyCode)
        val categoryMap = raw.categories.associateBy { it.id }
        val accountMap = raw.accounts.associateBy { it.id }

        val detailedTransactions = raw.transactions.map { tx ->
            val cat = categoryMap[tx.categoryId]
            val acc = accountMap[tx.accountId]
            TransactionWithDetails(
                transaction = tx,
                categoryName = cat?.name ?: "Other",
                categoryIconKey = cat?.iconKey ?: "category",
                accountName = acc?.name ?: "Primary"
            )
        }

        // Per-account balances & total balance
        val accountsWithBalance = raw.accounts.map { acc ->
            val accTxs = raw.transactions.filter { it.accountId == acc.id }
            val incomeSum = accTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val expenseSum = accTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            AccountWithBalance(
                account = acc,
                currentBalance = acc.initialBalance + incomeSum - expenseSum,
                transactionCount = accTxs.size
            )
        }
        val totalBalance = accountsWithBalance.sumOf { it.currentBalance }

        // Current & Previous Month figures
        val nowCal = Calendar.getInstance()
        val currentMonthKey = DateFormatter.monthKey(nowCal.timeInMillis)
        val prevMonthCal = (nowCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMonthKey = DateFormatter.monthKey(prevMonthCal.timeInMillis)

        val currentMonthTxs = detailedTransactions.filter {
            DateFormatter.monthKey(it.transaction.timestamp) == currentMonthKey
        }
        val currentMonthIncome = currentMonthTxs.filter { it.isIncome }.sumOf { it.transaction.amount }
        val currentMonthExpense = currentMonthTxs.filter { !it.isIncome }.sumOf { it.transaction.amount }

        val prevMonthTxs = detailedTransactions.filter {
            DateFormatter.monthKey(it.transaction.timestamp) == prevMonthKey
        }
        val prevMonthNet = prevMonthTxs.filter { it.isIncome }.sumOf { it.transaction.amount } -
            prevMonthTxs.filter { !it.isIncome }.sumOf { it.transaction.amount }
        val currentMonthNet = currentMonthIncome - currentMonthExpense

        val momPercent = when {
            abs(prevMonthNet) > 1.0 -> ((currentMonthNet - prevMonthNet) / abs(prevMonthNet)) * 100.0
            currentMonthNet > 0.0 -> 100.0
            else -> 0.0
        }

        // Analytics Period Filter
        val periodStartMillis = when (period) {
            AnalyticsPeriod.DAILY -> DateFormatter.startOfTodayMillis()
            AnalyticsPeriod.WEEKLY -> DateFormatter.startOfCurrentWeekMillis()
            AnalyticsPeriod.MONTHLY -> (nowCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            AnalyticsPeriod.YEARLY -> DateFormatter.startOfCurrentYearMillis()
        }

        val periodTxs = detailedTransactions.filter { it.transaction.timestamp >= periodStartMillis }
        val periodIncome = periodTxs.filter { it.isIncome }.sumOf { it.transaction.amount }
        val periodExpense = periodTxs.filter { !it.isIncome }.sumOf { it.transaction.amount }

        // Category breakdown (Expenses in selected period)
        val expenseByCat = periodTxs.filter { !it.isIncome }.groupBy { it.transaction.categoryId }
        val categoryBreakdown = expenseByCat.map { (catId, list) ->
            val sum = list.sumOf { it.transaction.amount }
            val first = list.first()
            CategorySpend(
                categoryId = catId,
                categoryName = first.categoryName,
                iconKey = first.categoryIconKey,
                totalAmount = sum,
                percentage = if (periodExpense > 0.0) (sum / periodExpense).toFloat() else 0f,
                transactionCount = list.size
            )
        }.sortedByDescending { it.totalAmount }

        // Monthly Comparisons (last 6 months)
        val monthlyComparisons = (5 downTo 0).map { offset ->
            val cal = (nowCal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
            val mKey = DateFormatter.monthKey(cal.timeInMillis)
            val mTxs = detailedTransactions.filter { DateFormatter.monthKey(it.transaction.timestamp) == mKey }
            MonthlyComparison(
                monthKey = mKey,
                label = DateFormatter.monthShortLabel(mKey),
                income = mTxs.filter { it.isIncome }.sumOf { it.transaction.amount },
                expense = mTxs.filter { !it.isIncome }.sumOf { it.transaction.amount }
            )
        }

        // Budgets Progress
        val weekStart = DateFormatter.startOfCurrentWeekMillis()
        val allBudgetProgress = raw.budgets.map { budget ->
            val relevantExpenses = detailedTransactions.filter { item ->
                if (item.isIncome) return@filter false
                val matchesPeriod = if (budget.period == BudgetPeriod.WEEKLY.name) {
                    item.transaction.timestamp >= weekStart
                } else {
                    DateFormatter.monthKey(item.transaction.timestamp) == budget.monthKey
                }
                val matchesCategory = budget.categoryId == null || item.transaction.categoryId == budget.categoryId
                matchesPeriod && matchesCategory
            }
            val spent = relevantExpenses.sumOf { it.transaction.amount }
            val remaining = budget.limitAmount - spent
            val fraction = if (budget.limitAmount > 0.0) (spent / budget.limitAmount).toFloat() else 0f
            val cat = budget.categoryId?.let { categoryMap[it] }
            BudgetProgress(
                budget = budget,
                categoryName = cat?.name ?: budget.name,
                iconKey = cat?.iconKey ?: "account_balance",
                spentAmount = spent,
                remainingAmount = remaining,
                progressFraction = fraction,
                isExceeded = spent > budget.limitAmount && budget.limitAmount > 0.0,
                isNearLimit = !((spent > budget.limitAmount)) &&
                    fraction >= (budget.alertThresholdPercent / 100f)
            )
        }

        val currentBudgets = allBudgetProgress.filter {
            it.budget.monthKey == currentMonthKey || it.budget.period == BudgetPeriod.WEEKLY.name
        }
        val historicalBudgets = allBudgetProgress.filter {
            it.budget.monthKey != currentMonthKey && it.budget.period == BudgetPeriod.MONTHLY.name
        }
        val overallBudget = currentBudgets.firstOrNull { it.budget.categoryId == null }

        // Financial Insights
        val monthlyExpenseByCat = currentMonthTxs.filter { !it.isIncome }
            .groupBy { it.transaction.categoryId }
            .map { (catId, list) ->
                val sum = list.sumOf { it.transaction.amount }
                val first = list.first()
                CategorySpend(
                    categoryId = catId,
                    categoryName = first.categoryName,
                    iconKey = first.categoryIconKey,
                    totalAmount = sum,
                    percentage = if (currentMonthExpense > 0.0) (sum / currentMonthExpense).toFloat() else 0f,
                    transactionCount = list.size
                )
            }.sortedByDescending { it.totalAmount }

        val daysElapsed = max(1, nowCal.get(Calendar.DAY_OF_MONTH))
        val dailyAvg = currentMonthExpense / daysElapsed
        val monthlySavings = currentMonthIncome - currentMonthExpense
        val savingsRate = if (currentMonthIncome > 0.0) {
            (monthlySavings / currentMonthIncome) * 100.0
        } else 0.0

        val topMethod = currentMonthTxs.filter { !it.isIncome }
            .groupingBy { it.paymentMethodEnum.label }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "UPI"

        val recurringCommitments = raw.bills.sumOf { it.amount }

        val suggestions = buildList {
            if (currentMonthTxs.isEmpty()) {
                add("Log your first income and daily expenses to unlock personalized savings guidance.")
            } else {
                if (savingsRate >= 30.0) {
                    add("Strong discipline — you are saving ${savingsRate.toInt()}% of this month's income. Consider allocating surplus funds toward your top savings goal.")
                } else if (savingsRate > 0.0) {
                    add("Your current savings rate is ${savingsRate.toInt()}%. Aiming for 25–30% by trimming discretionary spend can accelerate your reserve.")
                } else if (currentMonthIncome > 0.0) {
                    add("Expenses exceed income this month. Review high-impact categories before adding new recurring bills.")
                }

                val highest = monthlyExpenseByCat.firstOrNull()
                if (highest != null && highest.percentage > 0.35f) {
                    add("${highest.categoryName} accounts for ${(highest.percentage * 100).toInt()}% of your monthly spending (${CurrencyFormatter.format(highest.totalAmount, currency)}).")
                }

                val exceeded = currentBudgets.firstOrNull { it.isExceeded }
                if (exceeded != null) {
                    add("Your ${exceeded.categoryName} budget is over limit by ${CurrencyFormatter.format(abs(exceeded.remainingAmount), currency)}.")
                }

                val unpaidBills = raw.bills.filter { !it.isPaidThisCycle }
                if (unpaidBills.isNotEmpty()) {
                    val totalDue = unpaidBills.sumOf { it.amount }
                    add("${unpaidBills.size} upcoming bill${if (unpaidBills.size > 1) "s" else ""} totaling ${CurrencyFormatter.format(totalDue, currency)} remain this cycle.")
                }
            }
        }

        val insights = FinancialInsights(
            highestCategory = monthlyExpenseByCat.firstOrNull(),
            lowestCategory = monthlyExpenseByCat.lastOrNull(),
            dailyAverageSpend = dailyAvg,
            monthlySavings = monthlySavings,
            savingsRatePercent = savingsRate,
            cashFlowNet = periodIncome - periodExpense,
            topPaymentMethod = topMethod,
            recurringMonthlyCommitments = recurringCommitments,
            suggestions = suggestions
        )

        // Alerts for Bell Icon
        val alerts = buildList {
            if (prefs.budgetAlertsEnabled) {
                currentBudgets.filter { it.isExceeded }.forEach { bp ->
                    add(
                        AlertNotification(
                            id = "budget_over_${bp.budget.id}",
                            title = "${bp.categoryName} Budget Exceeded",
                            message = "Spent ${CurrencyFormatter.format(bp.spentAmount, currency)} of ${CurrencyFormatter.format(bp.budget.limitAmount, currency)} limit.",
                            isWarning = true,
                            tag = "Budget"
                        )
                    )
                }
                currentBudgets.filter { it.isNearLimit }.forEach { bp ->
                    add(
                        AlertNotification(
                            id = "budget_near_${bp.budget.id}",
                            title = "${bp.categoryName} Near Limit",
                            message = "${(bp.progressFraction * 100).toInt()}% used — ${CurrencyFormatter.format(bp.remainingAmount, currency)} remaining.",
                            isWarning = false,
                            tag = "Budget"
                        )
                    )
                }
            }
            raw.bills.filter { !it.isPaidThisCycle }.forEach { bill ->
                add(
                    AlertNotification(
                        id = "bill_${bill.id}",
                        title = "${bill.title} Due on Day ${bill.dueDayOfMonth}",
                        message = "${CurrencyFormatter.format(bill.amount, currency)} via ${bill.paymentMethod}",
                        isWarning = false,
                        tag = if (bill.isSubscription) "Subscription" else "Bill"
                    )
                )
            }
        }

        // Apply Transaction Filters
        val query = filters.query.trim().lowercase()
        val filtered = detailedTransactions.filter { item ->
            val tx = item.transaction
            val matchesQuery = query.isEmpty() ||
                tx.title.lowercase().contains(query) ||
                item.categoryName.lowercase().contains(query) ||
                tx.notes.lowercase().contains(query) ||
                item.accountName.lowercase().contains(query)
            val matchesType = filters.type == null || tx.type == filters.type.name
            val matchesCat = filters.categoryId == null || tx.categoryId == filters.categoryId
            val matchesAcc = filters.accountId == null || tx.accountId == filters.accountId
            val matchesPay = filters.paymentMethod == null || tx.paymentMethod == filters.paymentMethod.name
            matchesQuery && matchesType && matchesCat && matchesAcc && matchesPay
        }

        return FinanceUiState(
            isLoading = false,
            preferences = prefs,
            currency = currency,
            accounts = accountsWithBalance,
            categories = raw.categories,
            allTransactions = detailedTransactions,
            filteredTransactions = filtered,
            recentTransactions = detailedTransactions.take(5),
            totalBalance = totalBalance,
            monthOverMonthPercent = momPercent,
            currentMonthIncome = currentMonthIncome,
            currentMonthExpense = currentMonthExpense,
            analyticsPeriod = period,
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            categoryBreakdown = categoryBreakdown,
            monthlyComparisons = monthlyComparisons,
            currentMonthBudgets = currentBudgets,
            historicalBudgets = historicalBudgets,
            overallBudget = overallBudget,
            savingsGoals = raw.goals,
            billReminders = raw.bills,
            insights = insights,
            alerts = alerts,
            filters = filters
        )
    }
}
