package com.mohaseb.soft.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.fmt

fun balText(b: Double, curr: String): String {
    if (Math.abs(b) < 0.005) return "0"
    val s = fmt(Math.abs(b)) + (if (b > 0) " مدين" else " دائن")
    return if (curr == "محلي" || curr.isBlank()) s else "$s $curr"
}

@Composable
fun AccountsScreen(nav: NavHostController, autoAdd: Boolean = false) {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(autoAdd) }
    var sel by remember { mutableStateOf(-1) }
    var delId by remember { mutableStateOf(0L) }
    var filter by remember { mutableStateOf("الكل") }
    val all = remember(refresh) { Repo.accountBalances() }
    val types = remember { listOf("الكل") + Repo.rows("select name from cus_type order by id").map { it[0] } }
    val shown = all.filter { filter == "الكل" || it.type == filter }
    TableScreen(nav, "دليل الحسابات", listOf("الرصيد", "النوع", "الحساب"), listOf(1.2f, 1f, 1.5f),
        shown.map { listOf(balText(it.balance, it.curr), it.type, it.name) },
        onAdd = { showAdd = true }, onRow = { sel = it },
        header = { Chips(types, filter) { filter = it } })
    if (showAdd) FormDialog("إضافة حساب", listOf(
        FormField("اسم الحساب"), FormField("رقم الهاتف", numeric = true),
        FormField("نوع الحساب", options = types.drop(1), initial = types.getOrElse(1) { "" }),
        FormField("التصنيف في الشجرة (اختياري)", options = Repo.rows("select trim(name) from account_tree where id>=100 order by id").map { it[0] })
    ), onSubmit = { v ->
        if (v[0].isBlank()) "أدخل اسم الحساب" else Repo.addAccount(v[0], v[1], v[2], v[3])
    }, onClose = { showAdd = false; refresh++ })
    if (sel >= 0) SimpleListDialog(listOf("كشف حساب", "حذف الحساب"), onPick = { c ->
        val a = shown[sel]; sel = -1
        if (c == "كشف حساب") nav.navigate("statement/${a.id}") else delId = a.id
    }, onClose = { sel = -1 })
    if (delId != 0L) ConfirmDialog("حذف الحساب؟", onYes = {
        val err = Repo.deleteAccount(delId); delId = 0L
        if (err == null) refresh++ else toast(ctx, err)
    }, onNo = { delId = 0L })
}

@Composable
fun StatementScreen(nav: NavHostController, id: Long) {
    val name = remember { Repo.one("select name from customers where id=?", id) }
    val currencies = remember { Repo.currencies() }
    var cur by remember { mutableStateOf(currencies.firstOrNull()?.name ?: "محلي") }
    val curId = currencies.firstOrNull { it.name == cur }?.id ?: 0L
    val data = remember(curId) { Repo.statement(id, curId) }
    var run = 0.0
    var td = 0.0
    var tc = 0.0
    val rows = ArrayList<List<String>>()
    data.forEach {
        val db = Repo.d(it[2]); val cr = Repo.d(it[3])
        run += db - cr; td += db; tc += cr
        rows.add(listOf(balText(run, ""), fmt(cr), fmt(db), it[1], it[0]))
    }
    rows.add(listOf(balText(run, ""), fmt(tc), fmt(td), "الإجمالي", ""))
    TableScreen(nav, name.trim(), listOf("الرصيد", "دائن", "مدين", "البيان", "التاريخ"), listOf(1.1f, 0.8f, 0.8f, 1.4f, 1f), rows,
        header = { if (currencies.size > 1) Chips(currencies.map { it.name }, cur) { cur = it } })
}

@Composable
fun DailyListScreen(nav: NavHostController) {
    var refresh by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(-1L) }
    val ctx = LocalContext.current
    val list = remember(refresh) { Repo.dailies() }
    val rows = list.map { listOf(it[5], it[4], fmt(Repo.d(it[3])), it[2], it[1]) }
    TableScreen(nav, "قيد يومي", listOf("البيان", "التاريخ", "المبلغ", "دائن", "مدين"), listOf(1.2f, 1f, 0.9f, 1.1f, 1.1f), rows,
        onAdd = { showAdd = true }, onRow = { del = Repo.lng(list[it][0]) })
    if (showAdd) {
        val accs = remember { Repo.accountNames() }
        val curs = remember { Repo.currencies() }
        FormDialog("قيد يومي", listOf(
            FormField("الحساب المدين", options = accs.map { it.name }), FormField("الحساب الدائن", options = accs.map { it.name }),
            FormField("المبلغ", numeric = true), FormField("البيان"),
            FormField("العملة", options = curs.map { it.name }, initial = curs.first().name)
        ), onSubmit = { v ->
            val a = v[2].toDoubleOrNull() ?: 0.0
            val d = accs.firstOrNull { it.name == v[0] }; val c = accs.firstOrNull { it.name == v[1] }
            val cu = curs.first { it.name == v[4] }
            when {
                d == null || c == null -> "اختر الحسابين"
                d.id == c.id -> "الحسابان متطابقان"
                a <= 0.0 -> "أدخل المبلغ"
                Repo.currencyRate(cu.id) <= 0.0 -> "حدد سعر العملة أولاً"
                else -> Repo.saveDaily(d.id, c.id, a, v[3], Repo.dmyToday(), cu.id)
            }
        }, onClose = { showAdd = false; refresh++ })
    }
    if (del >= 0) ConfirmDialog("حذف القيد؟", onYes = {
        val err = Repo.deleteTransaction(del); del = -1
        if (err == null) refresh++ else toast(ctx, err)
    }, onNo = { del = -1 })
}
