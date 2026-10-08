package com.mohaseb.soft.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.*

@Composable
fun SearchTopBar(query: String, onQuery: (String) -> Unit, onBack: () -> Unit, onPdf: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).background(BarBrush).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
        BasicTextField(value = query, onValueChange = onQuery,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            decorationBox = { inner ->
                Box { if (query.isEmpty()) Text("بحث", color = Color(0xFFBBDEFB), fontSize = 16.sp); inner() }
            })
        Icon(Icons.Filled.Search, null, tint = Color.White)
        PdfIcon(onClick = onPdf)
    }
}

@Composable
fun DocListScreen(nav: NavHostController, kind: String) {
    val cfg = remember(kind) { kindCfg(kind) }
    val bills = remember(kind) { Repo.bills(listOf(cfg.trType)) }
    val rows = bills.map {
        val who = if (kind == "transfer") it.brName + " ← " + it.toBrName else it.party + (if (it.isBack) " (مرتجع)" else "")
        listOf(fmt(it.amount), who, Repo.dmy(it.date), it.number.toString())
    }
    TableScreen(nav, cfg.listTitle, listOf("المبلغ", "الإسم", "التاريخ", "رقم"), listOf(1f, 1.3f, 1f, 0.7f), rows,
        onAdd = { nav.navigate("new/$kind") },
        onRow = { nav.navigate("view/${bills[it].id}/$kind") },
        footer = "الإجمالي: " + fmt(bills.sumOf { it.amount }))
}

@Composable
fun VoucherListScreen(nav: NavHostController) {
    val list = remember { Repo.vouchers() }
    val rows = list.map { listOf(if (it.isPay) "صرف" else "قبض", it.descr, fmt(it.total), it.date, it.number.toString()) }
    TableScreen(nav, "قبض/صرف", listOf("نوعه", "البيان", "المجموع", "التاريخ", "رقم السند"), listOf(0.8f, 1.4f, 1f, 1f, 1f), rows,
        onAdd = { nav.navigate("voucher") }, onRow = { nav.navigate("voucherview/${list[it].number}") })
}

@Composable
fun VoucherViewScreen(nav: NavHostController, no: Int) {
    val ctx = LocalContext.current
    val lines = remember { Repo.voucherLines(no) }
    var confirm by remember { mutableStateOf(false) }
    val rows = lines.map { listOf(it[2], Repo.fmtN(Repo.d(it[1])), it[0]) }
    TableScreen(nav, "سند رقم $no", listOf("ملاحظات", "المبلغ", "الحساب"), listOf(1f, 1f, 1.2f), rows,
        header = {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(10.dp), horizontalArrangement = Arrangement.Center) {
                BlueButton("حذف السند", onClick = { confirm = true })
            }
        })
    if (confirm) ConfirmDialog("حذف السند وقيوده؟", onYes = {
        confirm = false
        val err = Repo.deleteVoucher(no)
        if (err == null) nav.popBackStack() else toast(ctx, err)
    }, onNo = { confirm = false })
}

@Composable
fun OptionsScreen(nav: NavHostController, key: String) {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("prefs", 0)
    val cfg: Pair<String, List<String>> = when (key) {
        "print" -> "خيارات الطباعة" to listOf("طباعة الفاتورة عند الحفظ", "إظهار الشعار", "طباعة التاريخ أسفل الفاتورة")
        "security" -> "خيارات الأمان" to listOf("تفعيل كلمة السر", "قفل تلقائي عند الخروج")
        "save" -> "خيارات حفظ البيانات" to listOf("حفظ تلقائي يومي", "نسخة احتياطية تلقائية")
        "thermal" -> "الطابعة الحرارية" to listOf("تفعيل الطابعة الحرارية", "حجم الورق 80mm")
        "barcode" -> "طباعة باركود الأصناف" to listOf("طباعة الباركود", "إظهار السعر على الباركود")
        "notify" -> "خيارات الإشعارات" to listOf("إشعار عند إضافة مبلغ", "تنبيه نفاذ الأصناف")
        else -> "خيارات أخرى" to listOf("تحديث أسعار البيع تلقائيا", "إظهار الوقت في شاشة الحساب")
    }
    Scaffold(topBar = { TopBar(cfg.first, onBack = { nav.popBackStack() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg)) {
            cfg.second.forEach { opt ->
                var on by remember { mutableStateOf(prefs.getBoolean(key + opt, false)) }
                Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(opt, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = on, onCheckedChange = {
                        on = it; prefs.edit().putBoolean(key + opt, it).apply()
                    }, colors = SwitchDefaults.colors(checkedTrackColor = Pink))
                }
                Divider(color = Color(0xFFDDDDDD))
            }
        }
    }
}

@Composable
fun ProfileScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("prefs", 0)
    var name by remember { mutableStateOf(prefs.getString("p_name", "") ?: "") }
    var addr by remember { mutableStateOf(prefs.getString("p_addr", "") ?: "") }
    var phone by remember { mutableStateOf(prefs.getString("p_phone", "") ?: "") }
    Scaffold(topBar = { TopBar("البيانات الشخصية", onBack = { nav.popBackStack() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg).padding(16.dp)) {
            LineField(name, { name = it; prefs.edit().putString("p_name", it).apply() }, "الإسم", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            LineField(addr, { addr = it; prefs.edit().putString("p_addr", it).apply() }, "العنوان", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            LineField(phone, { phone = it; prefs.edit().putString("p_phone", it).apply() }, "رقم الهاتف", modifier = Modifier.fillMaxWidth())
        }
    }
}
