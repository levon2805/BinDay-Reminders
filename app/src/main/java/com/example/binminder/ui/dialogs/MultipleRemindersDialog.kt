package com.example.binminder.ui.dialogs

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.neoShadow
import com.example.binminder.util.formatTimeForUser
import java.time.LocalTime

private val defaultAddLambda: (LocalTime) -> Unit = {}
private val defaultEditLambda: (LocalTime, LocalTime) -> Unit = { _, _ -> }
private val defaultDeleteLambda: (LocalTime) -> Unit = {}

private sealed class TimePickerTarget {
    data class Add(val isDayBefore: Boolean) : TimePickerTarget()
    data class Edit(val isDayBefore: Boolean, val originalTime: LocalTime) : TimePickerTarget()
}

/**
 * Material 3 AlertDialog enabling users to view, add, edit, and remove reminder times
 * organized into "Day Before" and "Day Of" sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleRemindersDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    eveningReminderTimes: Set<LocalTime> = emptySet(),
    morningReminderTimes: Set<LocalTime> = emptySet(),
    onAddEveningTime: (LocalTime) -> Unit = defaultAddLambda,
    onEditEveningTime: (oldTime: LocalTime, newTime: LocalTime) -> Unit = defaultEditLambda,
    onDeleteEveningTime: (LocalTime) -> Unit = defaultDeleteLambda,
    onAddMorningTime: (LocalTime) -> Unit = defaultAddLambda,
    onEditMorningTime: (oldTime: LocalTime, newTime: LocalTime) -> Unit = defaultEditLambda,
    onDeleteMorningTime: (LocalTime) -> Unit = defaultDeleteLambda,
    extraTimes: List<LocalTime> = emptyList(),
    onAddExtraTime: (LocalTime) -> Unit = defaultAddLambda,
    onDeleteExtraTime: (LocalTime) -> Unit = defaultDeleteLambda
) {
    val context = LocalContext.current
    var activePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

    val effectiveEveningTimes = remember(eveningReminderTimes, extraTimes) {
        if (eveningReminderTimes.isNotEmpty() || morningReminderTimes.isNotEmpty()) {
            eveningReminderTimes.toList().sorted()
        } else {
            extraTimes.filter { it.hour >= 12 }.sorted()
        }
    }

    val effectiveMorningTimes = remember(morningReminderTimes, extraTimes) {
        if (eveningReminderTimes.isNotEmpty() || morningReminderTimes.isNotEmpty()) {
            morningReminderTimes.toList().sorted()
        } else {
            extraTimes.filter { it.hour < 12 }.sorted()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .fillMaxWidth(0.92f)
            .heightIn(min = 420.dp, max = 620.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Advanced Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Configure notification alerts for your bins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Day Before
                ReminderSection(
                    title = "Day Before",
                    times = effectiveEveningTimes,
                    onAddNewClick = {
                        activePickerTarget = TimePickerTarget.Add(isDayBefore = true)
                    },
                    onEditTimeClick = { time ->
                        activePickerTarget = TimePickerTarget.Edit(isDayBefore = true, originalTime = time)
                    },
                    onDeleteTimeClick = { time ->
                        if (onDeleteEveningTime !== defaultDeleteLambda) {
                            onDeleteEveningTime(time)
                        } else {
                            onDeleteExtraTime(time)
                        }
                    },
                    context = context
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Day Of
                ReminderSection(
                    title = "Day Of",
                    times = effectiveMorningTimes,
                    onAddNewClick = {
                        activePickerTarget = TimePickerTarget.Add(isDayBefore = false)
                    },
                    onEditTimeClick = { time ->
                        activePickerTarget = TimePickerTarget.Edit(isDayBefore = false, originalTime = time)
                    },
                    onDeleteTimeClick = { time ->
                        if (onDeleteMorningTime !== defaultDeleteLambda) {
                            onDeleteMorningTime(time)
                        } else {
                            onDeleteExtraTime(time)
                        }
                    },
                    context = context
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )

    if (activePickerTarget != null) {
        val target = activePickerTarget!!
        val initialTime = when (target) {
            is TimePickerTarget.Add -> if (target.isDayBefore) LocalTime.of(19, 0) else LocalTime.of(7, 0)
            is TimePickerTarget.Edit -> target.originalTime
        }

        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )

        val pickerTitle = when (target) {
            is TimePickerTarget.Add -> if (target.isDayBefore) "Add Day Before Reminder" else "Add Day Of Reminder"
            is TimePickerTarget.Edit -> if (target.isDayBefore) "Edit Day Before Reminder" else "Edit Day Of Reminder"
        }

        val confirmText = when (target) {
            is TimePickerTarget.Add -> "Add Time"
            is TimePickerTarget.Edit -> "Save Time"
        }

        AlertDialog(
            onDismissRequest = { activePickerTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        when (target) {
                            is TimePickerTarget.Add -> {
                                if (target.isDayBefore) {
                                    if (onAddEveningTime !== defaultAddLambda) {
                                        onAddEveningTime(selectedTime)
                                    } else {
                                        onAddExtraTime(selectedTime)
                                    }
                                } else {
                                    if (onAddMorningTime !== defaultAddLambda) {
                                        onAddMorningTime(selectedTime)
                                    } else {
                                        onAddExtraTime(selectedTime)
                                    }
                                }
                            }
                            is TimePickerTarget.Edit -> {
                                if (target.isDayBefore) {
                                    if (onEditEveningTime !== defaultEditLambda) {
                                        onEditEveningTime(target.originalTime, selectedTime)
                                    } else {
                                        onDeleteExtraTime(target.originalTime)
                                        onAddExtraTime(selectedTime)
                                    }
                                } else {
                                    if (onEditMorningTime !== defaultEditLambda) {
                                        onEditMorningTime(target.originalTime, selectedTime)
                                    } else {
                                        onDeleteExtraTime(target.originalTime)
                                        onAddExtraTime(selectedTime)
                                    }
                                }
                            }
                        }
                        activePickerTarget = null
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(confirmText, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { activePickerTarget = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pickerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
private fun ReminderSection(
    title: String,
    times: List<LocalTime>,
    onAddNewClick: () -> Unit,
    onEditTimeClick: (LocalTime) -> Unit,
    onDeleteTimeClick: (LocalTime) -> Unit,
    context: Context
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (times.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                times.forEach { time ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(offset = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = formatTimeForUser(time, context),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onEditTimeClick(time) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit $title reminder ${formatTimeForUser(time, context)}",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteTimeClick(time) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete $title reminder ${formatTimeForUser(time, context)}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onAddNewClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .neoShadow(offset = 1.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Add New",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun MultipleRemindersDialogPreview() {
    BinMinderTheme {
        MultipleRemindersDialog(
            eveningReminderTimes = setOf(LocalTime.of(19, 0), LocalTime.of(20, 30)),
            morningReminderTimes = setOf(LocalTime.of(7, 0)),
            onDismissRequest = {}
        )
    }
}
