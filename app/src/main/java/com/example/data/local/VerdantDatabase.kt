package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.BudgetPeriod
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringFrequency
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.DateFormatter
import java.util.Calendar

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        BillReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VerdantDatabase : RoomDatabase() {
    abstract fun verdantDao(): VerdantDao

    companion object {
        @Volatile
        private var INSTANCE: VerdantDatabase? = null

        fun getInstance(context: Context): VerdantDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VerdantDatabase::class.java,
                    "verdant_finance.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object SampleDataSeeder {

    fun defaultCategories(): List<CategoryEntity> = listOf(
        // Predefined Expense Categories
        CategoryEntity(id = 1L, name = "Food & Dining", type = TransactionType.EXPENSE.name, iconKey = "restaurant", sortOrder = 1),
        CategoryEntity(id = 2L, name = "Shopping", type = TransactionType.EXPENSE.name, iconKey = "shopping_bag", sortOrder = 2),
        CategoryEntity(id = 3L, name = "Transportation", type = TransactionType.EXPENSE.name, iconKey = "directions_car", sortOrder = 3),
        CategoryEntity(id = 4L, name = "Bills & Utilities", type = TransactionType.EXPENSE.name, iconKey = "bolt", sortOrder = 4),
        CategoryEntity(id = 5L, name = "Entertainment", type = TransactionType.EXPENSE.name, iconKey = "movie", sortOrder = 5),
        CategoryEntity(id = 6L, name = "Healthcare", type = TransactionType.EXPENSE.name, iconKey = "medical_services", sortOrder = 6),
        CategoryEntity(id = 7L, name = "Education", type = TransactionType.EXPENSE.name, iconKey = "school", sortOrder = 7),
        CategoryEntity(id = 8L, name = "Travel", type = TransactionType.EXPENSE.name, iconKey = "flight", sortOrder = 8),
        CategoryEntity(id = 9L, name = "Rent", type = TransactionType.EXPENSE.name, iconKey = "home", sortOrder = 9),
        CategoryEntity(id = 10L, name = "Subscriptions", type = TransactionType.EXPENSE.name, iconKey = "autorenew", sortOrder = 10),
        CategoryEntity(id = 11L, name = "Other", type = TransactionType.EXPENSE.name, iconKey = "more_horiz", sortOrder = 11),
        // Predefined Income Categories
        CategoryEntity(id = 12L, name = "Salary", type = TransactionType.INCOME.name, iconKey = "work", sortOrder = 1),
        CategoryEntity(id = 13L, name = "Freelance", type = TransactionType.INCOME.name, iconKey = "laptop_mac", sortOrder = 2),
        CategoryEntity(id = 14L, name = "Investments", type = TransactionType.INCOME.name, iconKey = "trending_up", sortOrder = 3),
        CategoryEntity(id = 15L, name = "Other", type = TransactionType.INCOME.name, iconKey = "payments", sortOrder = 4)
    )

    fun defaultAccounts(): List<AccountEntity> = listOf(
        AccountEntity(
            id = 1L,
            name = "HDFC Savings",
            type = AccountType.BANK.name,
            initialBalance = 98500.0,
            currencyCode = "INR",
            iconKey = "account_balance",
            isDefault = true
        ),
        AccountEntity(
            id = 2L,
            name = "UPI Primary",
            type = AccountType.UPI.name,
            initialBalance = 18200.0,
            currencyCode = "INR",
            iconKey = "qr_code_2"
        ),
        AccountEntity(
            id = 3L,
            name = "Sapphire Card",
            type = AccountType.CARD.name,
            initialBalance = 24000.0,
            currencyCode = "INR",
            iconKey = "credit_card"
        ),
        AccountEntity(
            id = 4L,
            name = "Cash Wallet",
            type = AccountType.CASH.name,
            initialBalance = 6500.0,
            currencyCode = "INR",
            iconKey = "wallet"
        )
    )

    suspend fun seedInitialDataIfNeeded(dao: VerdantDao) {
        if (dao.getCategoryCount() > 0) return
        seedSampleData(dao)
    }

    suspend fun seedEmptyDefaults(dao: VerdantDao) {
        dao.clearTransactions()
        dao.clearBudgets()
        dao.clearSavingsGoals()
        dao.clearBillReminders()
        dao.clearAccounts()
        dao.clearCategories()
        dao.insertCategories(defaultCategories())
        dao.insertAccounts(
            listOf(
                AccountEntity(
                    id = 1L,
                    name = "Primary Bank",
                    type = AccountType.BANK.name,
                    initialBalance = 0.0,
                    currencyCode = "INR",
                    iconKey = "account_balance",
                    isDefault = true
                ),
                AccountEntity(
                    id = 2L,
                    name = "Cash Wallet",
                    type = AccountType.CASH.name,
                    initialBalance = 0.0,
                    currencyCode = "INR",
                    iconKey = "wallet"
                )
            )
        )
    }

    suspend fun seedSampleData(dao: VerdantDao) {
        dao.clearTransactions()
        dao.clearBudgets()
        dao.clearSavingsGoals()
        dao.clearBillReminders()
        dao.clearAccounts()
        dao.clearCategories()

        dao.insertCategories(defaultCategories())
        dao.insertAccounts(defaultAccounts())

        val now = Calendar.getInstance()
        val currentMonthKey = DateFormatter.monthKey(now.timeInMillis)
        val prevMonthCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMonthKey = DateFormatter.monthKey(prevMonthCal.timeInMillis)

        fun millisAgo(daysAgo: Int, hour: Int = 11, minute: Int = 30): Long {
            return (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        }

        fun millisInMonth(monthOffset: Int, dayOfMonth: Int, hour: Int = 10): Long {
            return (now.clone() as Calendar).apply {
                add(Calendar.MONTH, monthOffset)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(maxDay))
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 15)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        }

        val sampleTransactions = listOf(
            // Current Month — Income
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 115000.0,
                title = "Monthly Salary",
                categoryId = 12L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                notes = "Direct salary credit",
                timestamp = millisInMonth(0, 1, 9),
                isRecurring = true,
                recurringFrequency = RecurringFrequency.MONTHLY.name
            ),
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 22500.0,
                title = "UI Design Consultation",
                categoryId = 13L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                notes = "Product audit milestone",
                timestamp = millisAgo(5, 16, 40)
            ),
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 6400.0,
                title = "Index Fund Dividend",
                categoryId = 14L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                notes = "Quarterly equity payout",
                timestamp = millisAgo(9, 10, 20)
            ),

            // Current Month — Recent Expenses
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 840.0,
                title = "Third Wave Coffee & Brunch",
                categoryId = 1L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Weekend catch-up",
                timestamp = millisAgo(0, 9, 45)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 2450.0,
                title = "Fresh Organic Groceries",
                categoryId = 1L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Weekly produce and pantry essentials",
                timestamp = millisAgo(1, 18, 20)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 499.0,
                title = "Spotify & Cloud Storage",
                categoryId = 10L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Monthly digital bundle",
                timestamp = millisAgo(2, 12, 0),
                isRecurring = true,
                recurringFrequency = RecurringFrequency.MONTHLY.name
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 1180.0,
                title = "Metro & Airport Cab",
                categoryId = 3L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Client meeting commute",
                timestamp = millisAgo(3, 19, 10)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 3650.0,
                title = "Electricity & Fiber Broadband",
                categoryId = 4L,
                accountId = 1L,
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Monthly utility bills",
                timestamp = millisAgo(4, 14, 5),
                isRecurring = true,
                recurringFrequency = RecurringFrequency.MONTHLY.name
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 24000.0,
                title = "Apartment Rent",
                categoryId = 9L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                notes = "2BHK Indiranagar lease",
                timestamp = millisInMonth(0, 2, 10),
                isRecurring = true,
                recurringFrequency = RecurringFrequency.MONTHLY.name
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 5490.0,
                title = "Minimal Linen Workwear",
                categoryId = 2L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Two shirts and everyday tote",
                timestamp = millisAgo(6, 17, 30)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 1850.0,
                title = "Pharmacy & Dental Checkup",
                categoryId = 6L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Routine preventive visit",
                timestamp = millisAgo(8, 11, 15)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 1420.0,
                title = "Artisan Dinner",
                categoryId = 1L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Family dinner",
                timestamp = millisAgo(10, 20, 45)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 1250.0,
                title = "IMAX Cinema & Books",
                categoryId = 5L,
                accountId = 4L,
                paymentMethod = PaymentMethod.CASH.name,
                notes = "Evening film and paperback",
                timestamp = millisAgo(11, 19, 30)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 2800.0,
                title = "Design Systems Course",
                categoryId = 7L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Annual learning module",
                timestamp = millisAgo(12, 15, 0)
            ),

            // 1 Month Ago (Previous Month for comparisons & charts)
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 115000.0,
                title = "Monthly Salary",
                categoryId = 12L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-1, 1, 9),
                isRecurring = true,
                recurringFrequency = RecurringFrequency.MONTHLY.name
            ),
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 15000.0,
                title = "Brand Advisory Sprint",
                categoryId = 13L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-1, 18, 15)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 24000.0,
                title = "Apartment Rent",
                categoryId = 9L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-1, 2, 10)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 9600.0,
                title = "Groceries & Dining Out",
                categoryId = 1L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                timestamp = millisInMonth(-1, 14, 19)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 11400.0,
                title = "Coorg Weekend Retreat",
                categoryId = 8L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                timestamp = millisInMonth(-1, 21, 12)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 4200.0,
                title = "Home Utilities & Mobile",
                categoryId = 4L,
                accountId = 1L,
                paymentMethod = PaymentMethod.UPI.name,
                timestamp = millisInMonth(-1, 7, 11)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 6100.0,
                title = "Running Shoes & Gear",
                categoryId = 2L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                timestamp = millisInMonth(-1, 11, 16)
            ),

            // 2 Months Ago
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 115000.0,
                title = "Monthly Salary",
                categoryId = 12L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-2, 1, 9)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 24000.0,
                title = "Apartment Rent",
                categoryId = 9L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-2, 2, 10)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 10800.0,
                title = "Monthly Food & Cafes",
                categoryId = 1L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                timestamp = millisInMonth(-2, 16, 13)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 8500.0,
                title = "Desk Lamp & Workspace Setup",
                categoryId = 2L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                timestamp = millisInMonth(-2, 12, 18)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 4900.0,
                title = "Fuel & Intercity Travel",
                categoryId = 3L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                timestamp = millisInMonth(-2, 20, 9)
            ),

            // 3 Months Ago
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 112000.0,
                title = "Monthly Salary",
                categoryId = 12L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-3, 1, 9)
            ),
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 9200.0,
                title = "Mutual Fund Redemption",
                categoryId = 14L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-3, 15, 11)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 24000.0,
                title = "Apartment Rent",
                categoryId = 9L,
                accountId = 1L,
                paymentMethod = PaymentMethod.BANK.name,
                timestamp = millisInMonth(-3, 2, 10)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 11200.0,
                title = "Dining & Groceries",
                categoryId = 1L,
                accountId = 2L,
                paymentMethod = PaymentMethod.UPI.name,
                timestamp = millisInMonth(-3, 19, 14)
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 6400.0,
                title = "Annual Health Checkup",
                categoryId = 6L,
                accountId = 3L,
                paymentMethod = PaymentMethod.CARD.name,
                timestamp = millisInMonth(-3, 10, 10)
            )
        )
        dao.insertTransactions(sampleTransactions)

        // Budgets (Current Month + Previous Month history)
        val sampleBudgets = listOf(
            BudgetEntity(
                categoryId = null,
                name = "Overall Monthly Budget",
                limitAmount = 55000.0,
                period = BudgetPeriod.MONTHLY.name,
                monthKey = currentMonthKey,
                alertThresholdPercent = 80
            ),
            BudgetEntity(
                categoryId = 1L,
                name = "Food & Dining",
                limitAmount = 10000.0,
                period = BudgetPeriod.MONTHLY.name,
                monthKey = currentMonthKey,
                alertThresholdPercent = 80
            ),
            BudgetEntity(
                categoryId = 2L,
                name = "Shopping",
                limitAmount = 5000.0,
                period = BudgetPeriod.MONTHLY.name,
                monthKey = currentMonthKey,
                alertThresholdPercent = 85
            ),
            BudgetEntity(
                categoryId = 3L,
                name = "Transportation",
                limitAmount = 4000.0,
                period = BudgetPeriod.MONTHLY.name,
                monthKey = currentMonthKey,
                alertThresholdPercent = 80
            ),
            BudgetEntity(
                categoryId = 4L,
                name = "Bills & Utilities",
                limitAmount = 5000.0,
                period = BudgetPeriod.MONTHLY.name,
                monthKey = currentMonthKey,
                alertThresholdPercent = 85
            ),
            BudgetEntity(
                categoryId = null,
                name = "Overall Monthly Budget",
                limitAmount = 55000.0,
                period = BudgetPeriod.MONTHLY.name,
                monthKey = prevMonthKey,
                alertThresholdPercent = 80
            )
        )
        dao.insertBudgets(sampleBudgets)

        // Savings Goals
        fun monthsAhead(months: Int): Long =
            (now.clone() as Calendar).apply { add(Calendar.MONTH, months) }.timeInMillis

        val sampleGoals = listOf(
            SavingsGoalEntity(
                name = "Emergency Reserve",
                targetAmount = 300000.0,
                currentAmount = 215000.0,
                targetDateMillis = monthsAhead(6),
                iconKey = "shield",
                notes = "6 months of essential living expenses"
            ),
            SavingsGoalEntity(
                name = "Kyoto Autumn Trip",
                targetAmount = 160000.0,
                currentAmount = 94000.0,
                targetDateMillis = monthsAhead(9),
                iconKey = "flight",
                notes = "Flights, ryokan stay, and rail pass"
            ),
            SavingsGoalEntity(
                name = "Studio Workstation",
                targetAmount = 120000.0,
                currentAmount = 48000.0,
                targetDateMillis = monthsAhead(4),
                iconKey = "laptop_mac",
                notes = "Display and ergonomic setup upgrade"
            )
        )
        dao.insertSavingsGoals(sampleGoals)

        // Bill Reminders & Subscriptions
        val sampleBills = listOf(
            BillReminderEntity(
                title = "Apartment Rent",
                amount = 24000.0,
                categoryId = 9L,
                dueDayOfMonth = 2,
                isSubscription = false,
                frequency = RecurringFrequency.MONTHLY.name,
                isPaidThisCycle = true,
                lastPaidTimestamp = millisInMonth(0, 2, 10),
                paymentMethod = PaymentMethod.BANK.name,
                notes = "NEFT auto-reminder"
            ),
            BillReminderEntity(
                title = "Fiber Broadband & Power",
                amount = 3650.0,
                categoryId = 4L,
                dueDayOfMonth = 25,
                isSubscription = false,
                frequency = RecurringFrequency.MONTHLY.name,
                isPaidThisCycle = false,
                paymentMethod = PaymentMethod.UPI.name,
                notes = "BESCOM + ACT Fibernet"
            ),
            BillReminderEntity(
                title = "Spotify Duo & Cloud 200GB",
                amount = 499.0,
                categoryId = 10L,
                dueDayOfMonth = 28,
                isSubscription = true,
                frequency = RecurringFrequency.MONTHLY.name,
                isPaidThisCycle = false,
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Auto-billed on Sapphire Card"
            ),
            BillReminderEntity(
                title = "Fitness Studio Membership",
                amount = 2200.0,
                categoryId = 6L,
                dueDayOfMonth = 27,
                isSubscription = true,
                frequency = RecurringFrequency.MONTHLY.name,
                isPaidThisCycle = false,
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Monthly pilates & strength pass"
            )
        )
        dao.insertBillReminders(sampleBills)
    }
}
