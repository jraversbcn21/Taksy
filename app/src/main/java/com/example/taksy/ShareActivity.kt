package com.example.taksy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.taksy.data.Category
import com.example.taksy.data.preferences.PreferencesRepository
import com.example.taksy.domain.SharedText
import com.example.taksy.ui.components.DialogButtons
import com.example.taksy.ui.components.DialogTitle
import com.example.taksy.ui.components.HomeDialogSurface
import com.example.taksy.ui.components.OptionPills
import com.example.taksy.ui.components.PillOption
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans
import com.example.taksy.ui.theme.TicksyTheme
import com.example.taksy.ui.theme.accentColor
import com.example.taksy.ui.theme.isDark
import com.example.taksy.utils.CategoryUtils
import com.example.taksy.utils.LocaleHelper
import com.example.taksy.viewmodel.CategoryViewModel
import com.example.taksy.viewmodel.TaskInput
import com.example.taksy.viewmodel.TaskViewModel
import com.example.taksy.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Destino de "Compartir" de otras apps: un diálogo sobre la app de origen para guardar el texto
 * como tarea en una lista, sin abrir Taksy. Se queda en su propia tarea (taskAffinity vacío) y
 * fuera de recientes, así que al terminar se vuelve a donde se estaba.
 */
@AndroidEntryPoint
class ShareActivity : AppCompatActivity() {

    @Inject lateinit var preferences: PreferencesRepository

    private val taskViewModel: TaskViewModel by viewModels()
    private val categoryViewModel: CategoryViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shared = if (intent?.action == Intent.ACTION_SEND) {
            SharedText.parse(
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            )
        } else null
        if (shared == null) {
            finish()
            return
        }

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = themeMode.isDark()
            val categories by categoryViewModel.getAllCategories().collectAsStateWithLifecycle(initialValue = emptyList())
            TicksyTheme(darkTheme = isDark) {
                ShareDialog(
                    initialTitle = shared.title,
                    description = shared.description,
                    categories = categories,
                    initialCategoryId = preferences.getLastShareCategoryId(),
                    palette = if (isDark) HomePalette.Dark else HomePalette.Light,
                    onAdd = { title, category, name -> add(title, shared.description, category, name) },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun add(title: String, description: String?, category: Category, categoryName: String) {
        preferences.setLastShareCategoryId(category.id)
        val saved = taskViewModel.addTask(TaskInput(title, categoriaId = category.id, descripcion = description))
        // addTask corre en el ViewModel: hasta que no termine no se cierra la pantalla
        lifecycleScope.launch {
            saved.join()
            Toast.makeText(
                this@ShareActivity,
                getString(R.string.share_added, categoryName),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}

@Composable
private fun ShareDialog(
    initialTitle: String,
    description: String?,
    categories: List<Category>,
    initialCategoryId: Long?,
    palette: HomePalette,
    onAdd: (title: String, category: Category, categoryName: String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var selectedId by remember { mutableStateOf(initialCategoryId) }
    // La última lista usada, o la primera si no hay o ya no existe
    val selected = categories.firstOrNull { it.id == selectedId } ?: categories.firstOrNull()
    val accent = selected?.accentColor() ?: palette.brand
    val selectedName = selected?.let { CategoryUtils.getCategoryName(it) }.orEmpty()
    val focusRequester = remember { FocusRequester() }
    var submitted by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // El contenido del Dialog se compone aparte: el foco se pide aquí, cuando el campo ya existe
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        HomeDialogSurface(palette) {
            DialogTitle(stringResource(R.string.share_title), palette)

            val fieldShape = RoundedCornerShape(14.dp)
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                cursorBrush = SolidColor(accent),
                textStyle = TextStyle(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = palette.ink
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(fieldShape)
                    .background(palette.background)
                    .border(1.dp, palette.border, fieldShape)
                    .padding(14.dp)
                    .focusRequester(focusRequester)
            )
            if (description != null) {
                Text(
                    text = description,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = palette.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (categories.isNotEmpty()) {
                OptionPills(
                    label = stringResource(R.string.share_list),
                    options = categories.map {
                        PillOption(it.id, 0, it.accentColor(), text = CategoryUtils.getCategoryName(it))
                    },
                    selected = selected?.id,
                    onSelect = { selectedId = it },
                    palette = palette
                )
            }

            DialogButtons(
                confirmText = stringResource(R.string.add_task),
                confirmColor = accent,
                palette = palette,
                onDismiss = onCancel,
                confirmEnabled = title.isNotBlank() && selected != null && !submitted,
                onConfirm = {
                    if (selected != null && !submitted) {
                        submitted = true
                        onAdd(title.trim(), selected, selectedName)
                    }
                }
            )
        }
    }
}
