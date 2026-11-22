package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.CurrencyOption
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {

    fun format(
        amountInInr: Double,
        currency: CurrencyOption = CurrencyOption.INR,
        showSign: Boolean = false,
        isPositive: Boolean = true,
        compact: Boolean = false
    ): String {
        val converted = amountInInr * currency.rateFromInr
        val absVal = abs(converted)
        val prefix = when {
            showSign && isPositive -> "+${currency.symbol}"
            showSign && !isPositive -> "-${currency.symbol}"
            converted < 0 -> "-${currency.symbol}"
            else -> currency.symbol
        }

        if (compact) {
            return when {
                currency == CurrencyOption.INR && absVal >= 10000000 ->
                    "$prefix${String.format(Locale.US, "%.1fCr", absVal / 10000000.0)}"
                currency == CurrencyOption.INR && absVal >= 100000 ->
                    "$prefix${String.format(Locale.US, "%.1fL", absVal / 100000.0)}"
                absVal >= 1000000 ->
                    "$prefix${String.format(Locale.US, "%.1fM", absVal / 1000000.0)}"
                absVal >= 1000 ->
                    "$prefix${String.format(Locale.US, "%.1fk", absVal / 1000.0)}"
                else -> "$prefix${absVal.roundToLong()}"
            }
        }

        val formattedNumber = if (currency == CurrencyOption.INR) {
            formatIndianNumber(absVal)
        } else {
            val df = if (absVal % 1.0 == 0.0) DecimalFormat("#,##0") else DecimalFormat("#,##0.00")
            df.format(absVal)
        }
        return "$prefix$formattedNumber"
    }

    private fun formatIndianNumber(value: Double): String {
        val rounded = (value * 100.0).roundToLong()
        val whole = rounded / 100
        val fraction = rounded % 100
        val wholeStr = whole.toString()
        val grouped = if (wholeStr.length <= 3) {
            wholeStr
        } else {
            val lastThree = wholeStr.takeLast(3)
            val rest = wholeStr.dropLast(3)
            val restGrouped = rest.reversed().chunked(2).joinToString(",").reversed()
            "$restGrouped,$lastThree"
        }
        return if (fraction == 0L) {
            grouped
        } else {
            "$grouped.${fraction.toString().padStart(2, '0')}"
        }
    }
}

object DateFormatter {
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthShortFormat = SimpleDateFormat("MMM", Locale.US)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val dayTimeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    private val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val shortDayFormat = SimpleDateFormat("EEE", Locale.US)
    private val timeOnlyFormat = SimpleDateFormat("hh:mm a", Locale.US)

    fun monthKey(millis: Long): String = monthKeyFormat.format(Date(millis))

    fun monthShortLabel(monthKey: String): String {
        return try {
            val parsed = monthKeyFormat.parse(monthKey) ?: return monthKey
            monthShortFormat.format(parsed)
        } catch (_: Exception) {
            monthKey
        }
    }

    fun monthYearLabel(monthKey: String): String {
        return try {
            val parsed = monthKeyFormat.parse(monthKey) ?: return monthKey
            monthYearFormat.format(parsed)
        } catch (_: Exception) {
            monthKey
        }
    }

    fun formatDateTime(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val dayDiff = now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR)
        return when {
            sameYear && dayDiff == 0 -> "Today, ${timeOnlyFormat.format(Date(millis))}"
            sameYear && dayDiff == 1 -> "Yesterday, ${timeOnlyFormat.format(Date(millis))}"
            else -> dayTimeFormat.format(Date(millis))
        }
    }

    fun formatDate(millis: Long): String = dateOnlyFormat.format(Date(millis))

    fun formatDayHeader(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val dayDiff = now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR)
        return when {
            sameYear && dayDiff == 0 -> "Today"
            sameYear && dayDiff == 1 -> "Yesterday"
            else -> dateOnlyFormat.format(Date(millis))
        }
    }

    fun shortWeekday(millis: Long): String = shortDayFormat.format(Date(millis))

    fun greetingForCurrentHour(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    fun startOfCurrentWeekMillis(): Long {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfCurrentYearMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

object CategoryIcons {
    val selectableIconKeys: List<Pair<String, String>> = listOf(
        "restaurant" to "Dining",
        "shopping_bag" to "Shopping",
        "directions_car" to "Transit",
        "bolt" to "Utilities",
        "movie" to "Leisure",
        "medical_services" to "Health",
        "school" to "Learning",
        "flight" to "Travel",
        "home" to "Home",
        "autorenew" to "Recurring",
        "work" to "Salary",
        "laptop_mac" to "Freelance",
        "trending_up" to "Invest",
        "savings" to "Savings",
        "shield" to "Reserve",
        "local_cafe" to "Coffee",
        "pets" to "Pets",
        "payments" to "Cash"
    )

    fun resolve(key: String): ImageVector = when (key) {
        "restaurant" -> Icons.Outlined.Restaurant
        "shopping_bag" -> Icons.Outlined.ShoppingBag
        "directions_car" -> Icons.Outlined.DirectionsCar
        "bolt" -> Icons.Outlined.Bolt
        "movie" -> Icons.Outlined.Movie
        "medical_services" -> Icons.Outlined.MedicalServices
        "school" -> Icons.Outlined.School
        "flight" -> Icons.Outlined.Flight
        "home" -> Icons.Outlined.Home
        "autorenew" -> Icons.Outlined.Autorenew
        "work" -> Icons.Outlined.Work
        "laptop_mac" -> Icons.Outlined.LaptopMac
        "trending_up" -> Icons.Outlined.TrendingUp
        "payments" -> Icons.Outlined.Payments
        "account_balance" -> Icons.Outlined.AccountBalance
        "qr_code_2" -> Icons.Outlined.QrCode2
        "credit_card" -> Icons.Outlined.CreditCard
        "wallet" -> Icons.Outlined.AccountBalanceWallet
        "savings" -> Icons.Outlined.Savings
        "shield" -> Icons.Outlined.Shield
        "local_cafe" -> Icons.Outlined.LocalCafe
        "pets" -> Icons.Outlined.Pets
        "more_horiz" -> Icons.Outlined.MoreHoriz
        else -> Icons.Outlined.Category
    }
}

object SecurityHash {
    fun sha256(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("verdant_salt_$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
