package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AccountWithBalance
import com.example.data.model.CategoryEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringFrequency
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.VerdantCard
import com.example.ui.theme.VerdantThemeTokens
import com.example.util.CategoryIcons
import com.example.util.DateFormatter
import com.example.util.ReceiptStorage
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    existingTransaction: TransactionEntity?,
    categories: List<CategoryEntity>,
    accounts: List<AccountWithBalance>,
    currency: CurrencyOption,
    onSave: (TransactionEntity) -> Unit,
    onCreateCustomCategory: (String, TransactionType, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extended = VerdantThemeTokens.colors

    var type by remember {
        mutableStateOf(
            if (existingTransaction?.type == TransactionType.INCOME.name) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }
        )
    }

    val initialAmountDisplay = remember(existingTransaction, currency) {
        if (existingTransaction != null) {
            val converted = existingTransaction.amount * currency.rateFromInr
            if (converted % 1.0 == 0.0) converted.toLong().toString()
            else String.format(java.util.Locale.US, "%.2f", converted)
        } else ""
    }

    var amountText by remember { mutableStateOf(initialAmountDisplay) }
    var titleText by remember { mutableStateOf(existingTransaction?.title ?: "") }
    var notesText by remember { mutableStateOf(existingTransaction?.notes ?: "") }
    var selectedCategoryId by remember { mutableLongStateOf(existingTransaction?.categoryId ?: -1L) }
    var selectedAccountId by remember {
        mutableLongStateOf(
            existingTransaction?.accountId ?: accounts.firstOrNull()?.account?.id ?: 1L
        )
    }
    var selectedPaymentMethod by remember {
        mutableStateOf(
            PaymentMethod.entries.firstOrNull { it.name == existingTransaction?.paymentMethod }
                ?: PaymentMethod.UPI
        )
    }
    var selectedTimestamp by remember {
        mutableLongStateOf(existingTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var isRecurring by remember { mutableStateOf(existingTransaction?.isRecurring ?: false) }
    var recurringFrequency by remember {
        mutableStateOf(
            RecurringFrequency.entries.firstOrNull { it.name == existingTransaction?.recurringFrequency }
                ?.takeIf { it != RecurringFrequency.NONE } ?: RecurringFrequency.MONTHLY
        )
    }
    var receiptPath by remember { mutableStateOf(existingTransaction?.receiptUri) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val filteredCategories = remember(categories, type) {
        categories.filter { it.type == type.name }
    }
    val effectiveCategoryId = remember(filteredCategories, selectedCategoryId) {
        if (filteredCategories.any { it.id == selectedCategoryId }) {
            selectedCategoryId
        } else {
            filteredCategories.firstOrNull()?.id ?: 1L
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = ReceiptStorage.copyReceiptToInternalStorage(context, uri)
                receiptPath = savedPath ?: uri.toString()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingTransaction == null) {
                            stringResource(R.string.add_transaction)
                        } else {
                            stringResource(R.string.edit_transaction)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_tx_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Transaction Type Segmented Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { option ->
                        val selected = type == option
                        val selectedBg = if (option == TransactionType.INCOME) extended.income else extended.expense
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    type = option
                                    validationError = null
                                }
                                .testTag("tx_type_${option.name.lowercase()}"),
                            color = if (selected) selectedBg else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else extended.mutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                // Large Numeric Amount Input
                VerdantCard(contentPadding = PaddingValues(vertical = 22.dp, horizontal = 18.dp)) {
                    Text(
                        text = "Amount (${currency.code})",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.mutedText,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.displayMedium,
                            color = if (type == TransactionType.INCOME) extended.income else extended.expense
                        )
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(
                            value = amountText,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1 && filtered.length <= 12) {
                                    amountText = filtered
                                    validationError = null
                                }
                            },
                            textStyle = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 38.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (amountText.isEmpty()) {
                                        Text(
                                            text = "0",
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                fontSize = 38.sp,
                                                color = extended.mutedText.copy(alpha = 0.45f)
                                            )
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .widthIn(min = 80.dp, max = 260.dp)
                                .testTag("tx_amount_input")
                        )
                    }
                    if (validationError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.expense,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        validationError = null
                    },
                    label = { Text("Title / Merchant") },
                    placeholder = {
                        Text(if (type == TransactionType.INCOME) "e.g., Monthly Salary, Client Invoice" else "e.g., Groceries, Coffee, Rent")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                // Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Custom")
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCategories.forEach { cat ->
                            val selected = effectiveCategoryId == cat.id
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategoryId = cat.id },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIcons.resolve(cat.iconKey),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(cat.name) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Account / Wallet Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account / Wallet", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            val selected = selectedAccountId == acc.account.id
                            FilterChip(
                                selected = selected,
                                onClick = { selectedAccountId = acc.account.id },
                                label = { Text(acc.account.name) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Payment Method Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment Method", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method },
                                label = { Text(method.label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Date Selector + Recurring Toggle
                VerdantCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showDatePicker = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Date", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = DateFormatter.formatDate(selectedTimestamp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recurring Transaction", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Repeat automatically on schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                    }
                    if (isRecurring) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                RecurringFrequency.WEEKLY,
                                RecurringFrequency.MONTHLY,
                                RecurringFrequency.YEARLY
                            ).forEach { freq ->
                                FilterChip(
                                    selected = recurringFrequency == freq,
                                    onClick = { recurringFrequency = freq },
                                    label = { Text(freq.label) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Description (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Receipt Attachment (Zero-Permission Android Photo Picker)
                VerdantCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Receipt Image", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (receiptPath != null) "Attached securely on device" else "Optional photo attachment",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.mutedText
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, extended.border)
                        ) {
                            Text(if (receiptPath == null) "Attach" else "Replace")
                        }
                    }
                    if (!receiptPath.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val imgModel: Any = if (receiptPath!!.startsWith("/")) File(receiptPath!!) else receiptPath!!
                            AsyncImage(
                                model = imgModel,
                                contentDescription = stringResource(R.string.cd_receipt_preview),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { receiptPath = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove receipt")
                            }
                        }
                    }
                }

                // Clear Save Transaction Button
                Button(
                    onClick = {
                        val enteredAmount = amountText.toDoubleOrNull()
                        val amountInInr = if (enteredAmount != null && currency.rateFromInr > 0) {
                            enteredAmount / currency.rateFromInr
                        } else null

                        when {
                            amountInInr == null || amountInInr <= 0.0 -> {
                                validationError = "Please enter an amount greater than 0"
                            }
                            else -> {
                                val chosenCategory = filteredCategories.firstOrNull { it.id == effectiveCategoryId }
                                val finalTitle = titleText.trim().ifEmpty {
                                    chosenCategory?.name ?: type.label
                                }
                                onSave(
                                    TransactionEntity(
                                        id = existingTransaction?.id ?: 0L,
                                        type = type.name,
                                        amount = amountInInr,
                                        title = finalTitle,
                                        categoryId = effectiveCategoryId,
                                        accountId = selectedAccountId,
                                        paymentMethod = selectedPaymentMethod.name,
                                        notes = notesText.trim(),
                                        timestamp = selectedTimestamp,
                                        isRecurring = isRecurring,
                                        recurringFrequency = if (isRecurring) recurringFrequency.name else RecurringFrequency.NONE.name,
                                        receiptUri = receiptPath,
                                        createdAt = existingTransaction?.createdAt ?: System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save_transaction),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedTimestamp = it }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddCategoryDialog) {
        CreateCustomCategoryDialog(
            initialType = type,
            onConfirm = { name, catType, iconKey ->
                onCreateCustomCategory(name, catType, iconKey)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateCustomCategoryDialog(
    initialType: TransactionType,
    onConfirm: (String, TransactionType, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedIcon by remember { mutableStateOf("category") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Custom Category", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Category Name") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.label) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIcons.selectableIconKeys.take(12).forEach { (key, _) ->
                        val isChosen = selectedIcon == key
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedIcon = key },
                            color = if (isChosen) MaterialTheme.colorScheme.primary else VerdantThemeTokens.colors.sageTint,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CategoryIcons.resolve(key),
                                    contentDescription = key,
                                    tint = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        error = "Enter a category name"
                    } else {
                        onConfirm(name.trim(), selectedType, selectedIcon)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
