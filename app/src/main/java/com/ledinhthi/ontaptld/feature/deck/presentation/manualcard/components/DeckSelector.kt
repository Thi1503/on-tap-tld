package com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.presentation.components.parseDeckColor

/**
 * Ô "Bộ thẻ": hiện bộ đang chọn, bấm vào thì thả xuống danh sách các bộ để đổi.
 * [enabled] = false (khi sửa thẻ): chỉ cho biết thẻ thuộc bộ nào, không bấm được.
 */
@Composable
fun DeckSelector(
    decks: List<Deck>,
    selected: Deck?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = appColors()
    val label = stringResource(R.string.manual_card_deck_label)
    var expanded by remember { mutableStateOf(false) }
    // Bề rộng thật của ô (tính bằng pixel, đo sau khi xếp chỗ) để menu thả xuống rộng đúng bằng ô.
    var fieldWidthPx by remember { mutableIntStateOf(0) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.padding6)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Box {
            Surface(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.btnMedium)
                    .onSizeChanged { fieldWidthPx = it.width }
                    .semantics {
                        role = Role.DropdownList
                        contentDescription = "$label: ${selected?.name.orEmpty()}"
                    },
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                color = colors.inputBackground,
                contentColor = colors.textPrimary,
                border = BorderStroke(1.dp, colors.borderStrong),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.padding10),
                ) {
                    if (selected != null) DeckDot(selected.colorHex)
                    Text(
                        text = selected?.name.orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (enabled) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.sizeIcon),
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(LocalDensity.current) { fieldWidthPx.toDp() }),
                shape = MaterialTheme.shapes.medium,
                containerColor = colors.bottomSheetBackground,
            ) {
                decks.forEach { deck ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = deck.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(deck.id)
                        },
                        leadingIcon = { DeckDot(deck.colorHex) },
                        trailingIcon = if (deck.id == selected?.id) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.sizeIcon),
                                    tint = colors.primaryStrong,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/** Chấm tròn mang màu nhận diện của bộ thẻ. */
@Composable
private fun DeckDot(colorHex: String) {
    Box(Modifier.size(12.dp).background(parseDeckColor(colorHex), CircleShape))
}
