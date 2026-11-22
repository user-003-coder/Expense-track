package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}

enum class PaymentMethod(val label: String) {
    UPI("UPI"),
    BANK("Bank Transfer"),
    CARD("Card"),
    CASH("Cash")
}

enum class AccountType(val label: String) {
    BANK("Bank Account"),
    UPI("UPI Wallet"),
    CARD("Credit / Debit Card"),
    CASH("Cash Wallet")
}

enum class BudgetPeriod(val label: String) {
    MONTHLY("Monthly"),
    WEEKLY("Weekly")
}

enum class RecurringFrequency(val label: String) {
    NONE("One-time"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class AnalyticsPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
    val rateFromInr: Double
) {
    INR("INR", "₹", "Indian Rupee (₹)", 1.0),
    USD("USD", "$", "US Dollar ($)", 0.0119),
    EUR("EUR", "€", "Euro (€)", 0.0110),
    GBP("GBP", "£", "British Pound (£)", 0.0094),
    AED("AED", "د.إ", "UAE Dirham (AED)", 0.0437),
    SGD("SGD", "S$", "Singapore Dollar (S$)", 0.0159);

    companion object {
        fun fromCode(code: String): CurrencyOption =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
    }
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String = AccountType.BANK.name,
    val initialBalance: Double = 0.0,
    val currencyCode: String = "INR",
    val iconKey: String = "account_balance",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String = TransactionType.EXPENSE.name,
    val iconKey: String = "category",
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double,
    val title: String,
    val categoryId: Long,
    val accountId: Long,
    val paymentMethod: String = PaymentMethod.UPI.name,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurringFrequency: String = RecurringFrequency.NONE.name,
    val receiptUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long? = null,
    val name: String,
    val limitAmount: Double,
    val period: String = BudgetPeriod.MONTHLY.name,
    val monthKey: String,
    val alertThresholdPercent: Int = 80,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long,
    val iconKey: String = "savings",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "bill_reminders")
data class BillReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val dueDayOfMonth: Int,
    val isSubscription: Boolean = false,
    val frequency: String = RecurringFrequency.MONTHLY.name,
    val isPaidThisCycle: Boolean = false,
    val lastPaidTimestamp: Long? = null,
    val paymentMethod: String = PaymentMethod.UPI.name,
    val notes: String = ""
)
