package com.mohaseb.soft.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.mohaseb.soft.ui.*

fun toast(ctx: Context, m: String) {
    android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
fun SimpleListDialog(items: List<String>, onPick: (String) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).background(Color.White, RoundedCornerShape(4.dp))
            .verticalScroll(rememberScrollState())) {
            if (items.isEmpty()) Text("لا توجد عناصر", fontSize = 15.sp, color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
            items.forEach { n ->
                Text(n, fontSize = 15.sp, color = Color.Black,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(n) }.padding(vertical = 14.dp, horizontal = 16.dp),
                    textAlign = TextAlign.Right)
                Divider(color = Color(0xFFE0E0E0))
            }
        }
    }
}

@Composable
fun ConfirmDialog(text: String, onYes: () -> Unit, onNo: () -> Unit) {
    Dialog(onDismissRequest = onNo) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).padding(16.dp)) {
            Text(text, color = Color.Black, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onYes) { Text("نعم", color = Color.Black, fontSize = 15.sp) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onNo) { Text("لا", color = Color.Black, fontSize = 15.sp) }
            }
        }
    }
}

data class FormField(val label: String, val numeric: Boolean = false, val options: List<String>? = null, val initial: String = "")

/** نافذة إدخال عامة. onSubmit تعيد رسالة خطأ أو null عند النجاح. */
@Composable
fun FormDialog(title: String, fields: List<FormField>, onSubmit: (List<String>) -> String?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val values = remember { mutableStateListOf<String>().apply { fields.forEach { add(it.initial) } } }
    var pick by remember { mutableStateOf(-1) }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).padding(16.dp)
            .verticalScroll(rememberScrollState())) {
            Text(title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            fields.forEachIndexed { i, f ->
                if (f.options != null) {
                    Row(Modifier.fillMaxWidth().clickable { pick = i }.padding(top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color.DarkGray)
                        Text(if (values[i].isEmpty()) f.label else values[i], fontSize = 15.sp,
                            color = if (values[i].isEmpty()) Color(0xFF9AA0A6) else Color.Black,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                    }
                    Divider(color = Pink, thickness = 2.dp)
                } else {
                    LineField(values[i], { values[i] = it }, f.label, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        keyboard = if (f.numeric) KeyboardType.Number else KeyboardType.Text)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                BlueButton("حفظ", onClick = {
                    val err = onSubmit(values.toList())
                    if (err == null) onClose() else toast(ctx, err)
                })
                Spacer(Modifier.width(12.dp))
                BlueButton("إغلاق", onClick = onClose)
            }
        }
    }
    if (pick >= 0) SimpleListDialog(fields[pick].options!!, onPick = { values[pick] = it; pick = -1 }, onClose = { pick = -1 })
}

@Composable
fun Chips(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(6.dp)) {
        options.forEach { t ->
            Box(Modifier.padding(horizontal = 4.dp).clickable { onPick(t) }
                .background(if (selected == t) Blue else Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text(t, color = if (selected == t) Color.White else Color.Black, fontSize = 13.sp)
            }
        }
    }
}

/** جدول عام: الأعمدة بترتيب العرض من اليسار لليمين (آخر عمود هو الأول بالقراءة العربية). */
@Composable
fun TableScreen(nav: NavHostController, title: String, cols: List<String>, weights: List<Float>, rows: List<List<String>>,
                onAdd: (() -> Unit)? = null, onRow: ((Int) -> Unit)? = null, footer: String = ".........",
                header: @Composable () -> Unit = {}) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(false) }
    val idx = rows.indices.filter { i -> query.isEmpty() || rows[i].any { it.contains(query) } }
    fun pdf() = PdfShare(ctx, title, cols.reversed(), idx.map { rows[it].reversed() })
    Scaffold(
        topBar = {
            if (searchMode) SearchTopBar(query, { query = it }, { searchMode = false; query = "" }, ::pdf)
            else TopBar(title, onBack = { nav.popBackStack() }, right = {
                IconButton(onClick = { searchMode = true }) { Icon(Icons.Filled.Search, null, tint = Color.White) }
                PdfIcon(onClick = ::pdf)
            })
        },
        bottomBar = { if (onAdd != null) ListBottomBar(center = footer, onAdd = onAdd) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg)) {
            header()
            HeaderRow(cols, weights)
            if (idx.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth()) { EmptyState() }
            else LazyColumn(Modifier.weight(1f)) {
                items(idx) { i ->
                    Row(Modifier.fillMaxWidth().background(Color.White)
                        .clickable(enabled = onRow != null) { onRow?.invoke(i) }
                        .padding(vertical = 12.dp, horizontal = 6.dp)) {
                        rows[i].forEachIndexed { k, cell ->
                            Text(cell, fontSize = 13.sp, modifier = Modifier.weight(weights.getOrElse(k) { 1f }), textAlign = TextAlign.Center)
                        }
                    }
                    Divider(color = Color(0xFFDDDDDD))
                }
            }
        }
    }
}

@Composable
fun KeyValueScreen(nav: NavHostController, title: String, rows: List<Pair<String, String>>) {
    Scaffold(topBar = { TopBar(title, onBack = { nav.popBackStack() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg).verticalScroll(rememberScrollState())) {
            rows.forEach { r ->
                val bold = r.first.startsWith("*")
                Row(Modifier.fillMaxWidth().background(if (bold) Color(0xFFE3F2FD) else Color.White)
                    .padding(vertical = 13.dp, horizontal = 16.dp)) {
                    Text(r.second, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(r.first.removePrefix("*"), fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(2f), textAlign = TextAlign.Right)
                }
                Divider(color = Color(0xFFDDDDDD))
            }
        }
    }
}
