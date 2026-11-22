package com.example.data.model

import com.squareup.moshi.JsonClass

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    val categoryName: String,
    val categoryIconKey: String,
    val accountName: String
) {
    val isIncome: Boolean get() = transaction.type == TransactionType.INCOME.name
    val paymentMethodEnum: PaymentMethod
        get() = PaymentMethod.entries.firstOrNull { it.name == transaction.paymentMethod }
            ?: PaymentMethod.UPI
}

data class AccountWithBalance(
    val account: AccountEntity,
    val currentBalance: Double,
    val transactionCount: Int
)

data class CategorySpend(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class MonthlyComparison(
    val monthKey: String,
    val label: String,
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
}

data class BudgetProgress(
    val budget: BudgetEntity,
    val categoryName: String,
    val iconKey: String,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progressFraction: Float,
    val isExceeded: Boolean,
    val isNearLimit: Boolean
)

data class FinancialInsights(
    val highestCategory: CategorySpend?,
    val lowestCategory: CategorySpend?,
    val dailyAverageSpend: Double,
    val monthlySavings: Double,
    val savingsRatePercent: Double,
    val cashFlowNet: Double,
    val topPaymentMethod: String,
    val recurringMonthlyCommitments: Double,
    val suggestions: List<String>
)

data class AlertNotification(
    val id: String,
    val title: String,
    val message: String,
    val isWarning: Boolean,
    val tag: String
)

data class UserPreferences(
    val userName: String = "Aarav Sharma",
    val userEmail: String = "aarav.sharma@verdant.app",
    val currencyCode: String = CurrencyOption.INR.code,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hideBalance: Boolean = false,
    val appLockEnabled: Boolean = false,
    val pinHash: String? = null,
    val budgetAlertsEnabled: Boolean = true,
    val privacyScreenEnabled: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<SavingsGoalEntity> = emptyList(),
    val bills: List<BillReminderEntity> = emptyList()
)
