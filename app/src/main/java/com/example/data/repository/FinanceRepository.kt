package com.example.data.repository

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.SampleDataSeeder
import com.example.data.local.VerdantDao
import com.example.data.model.AccountEntity
import com.example.data.model.BackupPayload
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithDetails
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class FinanceRepository(private val dao: VerdantDao) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val accounts: Flow<List<AccountEntity>> = dao.observeAccounts()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val budgets: Flow<List<BudgetEntity>> = dao.observeBudgets()
    val savingsGoals: Flow<List<SavingsGoalEntity>> = dao.observeSavingsGoals()
    val billReminders: Flow<List<BillReminderEntity>> = dao.observeBillReminders()

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        SampleDataSeeder.seedInitialDataIfNeeded(dao)
    }

    suspend fun loadSampleData() = withContext(Dispatchers.IO) {
        SampleDataSeeder.seedSampleData(dao)
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        SampleDataSeeder.seedEmptyDefaults(dao)
    }

    // Transactions
    suspend fun saveTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        if (transaction.id == 0L) {
            dao.insertTransaction(transaction)
        } else {
            dao.updateTransaction(transaction)
            transaction.id
        }
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteTransactionById(id)
    }

    // Accounts
    suspend fun saveAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        if (account.id == 0L) dao.insertAccount(account) else {
            dao.updateAccount(account)
            account.id
        }
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        dao.deleteAccount(account)
    }

    // Categories
    suspend fun addCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        dao.deleteCategory(category)
    }

    // Budgets
    suspend fun saveBudget(budget: BudgetEntity): Long = withContext(Dispatchers.IO) {
        if (budget.id == 0L) dao.insertBudget(budget) else {
            dao.updateBudget(budget)
            budget.id
        }
    }

    suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteBudgetById(id)
    }

    // Savings Goals
    suspend fun saveGoal(goal: SavingsGoalEntity): Long = withContext(Dispatchers.IO) {
        if (goal.id == 0L) dao.insertSavingsGoal(goal) else {
            dao.updateSavingsGoal(goal)
            goal.id
        }
    }

    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteSavingsGoalById(id)
    }

    // Bill Reminders & Subscriptions
    suspend fun saveBillReminder(bill: BillReminderEntity): Long = withContext(Dispatchers.IO) {
        if (bill.id == 0L) dao.insertBillReminder(bill) else {
            dao.updateBillReminder(bill)
            bill.id
        }
    }

    suspend fun deleteBillReminder(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteBillReminderById(id)
    }

    // Export CSV via SAF Uri
    suspend fun exportCsv(
        context: Context,
        uri: Uri,
        items: List<TransactionWithDetails>,
        currency: CurrencyOption
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val sb = StringBuilder()
            sb.appendLine("Date,Title,Type,Category,Account,Payment Method,Amount (${currency.code}),Recurring,Notes")
            items.forEach { item ->
                val tx = item.transaction
                val escapedTitle = tx.title.replace("\"", "\"\"")
                val escapedNotes = tx.notes.replace("\"", "\"\"")
                val converted = String.format(java.util.Locale.US, "%.2f", tx.amount * currency.rateFromInr)
                sb.appendLine(
                    "\"${DateFormatter.formatDate(tx.timestamp)}\",\"$escapedTitle\",\"${tx.type}\",\"${item.categoryName}\",\"${item.accountName}\",\"${tx.paymentMethod}\",$converted,\"${tx.recurringFrequency}\",\"$escapedNotes\""
                )
            }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream")
            items.size
        }
    }

    // Export PDF Statement via Android Native PdfDocument + SAF Uri
    suspend fun exportPdfReport(
        context: Context,
        uri: Uri,
        userName: String,
        periodLabel: String,
        totalBalance: Double,
        periodIncome: Double,
        periodExpense: Double,
        items: List<TransactionWithDetails>,
        currency: CurrencyOption
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val emerald = AndroidColor.parseColor("#146C55")
            val charcoal = AndroidColor.parseColor("#202824")
            val muted = AndroidColor.parseColor("#6E7A74")
            val green = AndroidColor.parseColor("#23865B")
            val red = AndroidColor.parseColor("#D65C5C")
            val border = AndroidColor.parseColor("#E6ECE8")
            val tint = AndroidColor.parseColor("#F4F8F6")

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emerald
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = muted
                textSize = 11f
            }
            val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = charcoal
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = charcoal
                textSize = 10.5f
            }
            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = border
                strokeWidth = 1f
            }

            var y = 52f
            canvas.drawText("Verdant Financial Statement", 40f, y, titlePaint)
            y += 18f
            canvas.drawText(
                "Prepared for $userName  •  Period: $periodLabel  •  Generated ${DateFormatter.formatDate(System.currentTimeMillis())}",
                40f,
                y,
                subPaint
            )
            y += 22f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            // Summary Box
            canvas.drawRoundRect(40f, y, 555f, y + 78f, 12f, 12f, cardBgPaint)
            val metricLabelPaint = Paint(subPaint).apply { textSize = 10f }
            val metricValuePaint = Paint(headingPaint).apply { textSize = 15f }

            canvas.drawText("TOTAL BALANCE", 56f, y + 26f, metricLabelPaint)
            canvas.drawText(CurrencyFormatter.format(totalBalance, currency), 56f, y + 50f, metricValuePaint)

            canvas.drawText("INCOME ($periodLabel)", 230f, y + 26f, metricLabelPaint)
            metricValuePaint.color = green
            canvas.drawText(CurrencyFormatter.format(periodIncome, currency, showSign = true, isPositive = true), 230f, y + 50f, metricValuePaint)

            canvas.drawText("EXPENSES ($periodLabel)", 400f, y + 26f, metricLabelPaint)
            metricValuePaint.color = red
            canvas.drawText(CurrencyFormatter.format(periodExpense, currency, showSign = true, isPositive = false), 400f, y + 50f, metricValuePaint)

            y += 108f
            canvas.drawText("Transactions (${items.size})", 40f, y, headingPaint)
            y += 16f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 18f

            val amountPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }

            items.take(24).forEach { item ->
                if (y > 790f) return@forEach
                val tx = item.transaction
                canvas.drawText(DateFormatter.formatDate(tx.timestamp), 40f, y, subPaint)
                canvas.drawText(tx.title.take(30), 125f, y, bodyPaint)
                canvas.drawText(item.categoryName.take(18), 320f, y, subPaint)
                amountPaint.color = if (tx.type == TransactionType.INCOME.name) green else red
                canvas.drawText(
                    CurrencyFormatter.format(
                        tx.amount,
                        currency,
                        showSign = true,
                        isPositive = tx.type == TransactionType.INCOME.name
                    ),
                    555f,
                    y,
                    amountPaint
                )
                y += 22f
                canvas.drawLine(40f, y - 8f, 555f, y - 8f, linePaint)
            }

            document.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
            } ?: error("Unable to open output stream")
            document.close()
        }
    }

    // JSON Full Backup Export
    suspend fun exportBackupJson(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupPayload(
                accounts = dao.getAccountsOnce(),
                categories = dao.getCategoriesOnce(),
                transactions = dao.getTransactionsOnce(),
                budgets = dao.getBudgetsOnce(),
                goals = dao.getSavingsGoalsOnce(),
                bills = dao.getBillRemindersOnce()
            )
            val adapter = moshi.adapter(BackupPayload::class.java).indent("  ")
            val json = adapter.toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream")
        }
    }

    // JSON Full Backup Restore
    suspend fun restoreBackupJson(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            } ?: error("Unable to read backup file")
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(json) ?: error("Invalid backup format")

            dao.clearTransactions()
            dao.clearBudgets()
            dao.clearSavingsGoals()
            dao.clearBillReminders()
            dao.clearAccounts()
            dao.clearCategories()

            dao.insertCategories(payload.categories.ifEmpty { SampleDataSeeder.defaultCategories() })
            dao.insertAccounts(payload.accounts.ifEmpty { SampleDataSeeder.defaultAccounts() })
            dao.insertTransactions(payload.transactions)
            dao.insertBudgets(payload.budgets)
            dao.insertSavingsGoals(payload.goals)
            dao.insertBillReminders(payload.bills)
            payload.transactions.size
        }
    }
}
