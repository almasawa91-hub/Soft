package com.mohaseb.soft.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.fmt

@Composable
fun ItemsScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var sel by remember { mutableStateOf(-1) }
    var priceFor by remember { mutableStateOf(-1) }
    var delFor by remember { mutableStateOf(-1) }
    val inv = remember(refresh) { Repo.inventory() }
    val units = remember { Repo.units() }
    TableScreen(nav, "الأصناف", listOf("السعر", "الكمية", "الوحدة", "الصنف"), listOf(0.9f, 0.9f, 0.8f, 1.6f),
        inv.map { listOf(fmt(it.price), fmt(it.stock), it.unit, it.name) },
        onAdd = { showAdd = true }, onRow = { sel = it },
        footer = "عدد الأصناف: ${inv.size}")
    if (showAdd) FormDialog("إضافة صنف", listOf(
        FormField("اسم الصنف"), FormField("سعر البيع", numeric = true), FormField("الكمية الافتتاحية", numeric = true),
        FormField("تكلفة الوحدة الافتتاحية", numeric = true),
        FormField("الوحدة", options = units.map { it.name }, initial = units.firstOrNull()?.name ?: ""),
        FormField("باركود (اختياري)")
    ), onSubmit = { v ->
        if (v[0].isBlank()) "أدخل اسم الصنف"
        else Repo.addItem(v[0], v[1].toDoubleOrNull() ?: 0.0, v[2].toDoubleOrNull() ?: 0.0, v[3].toDoubleOrNull() ?: 0.0,
            units.firstOrNull { it.name == v[4] }?.id ?: 0L, v[5])
    }, onClose = { showAdd = false; refresh++ })
    if (sel >= 0) SimpleListDialog(listOf("تعديل السعر", "حركة الصنف", "حذف الصنف"), onPick = { c ->
        val i = sel; sel = -1
        when (c) { "تعديل السعر" -> priceFor = i; "حركة الصنف" -> nav.navigate("itemledger/${inv[i].id}"); else -> delFor = i }
    }, onClose = { sel = -1 })
    if (priceFor >= 0) PriceDialog(inv[priceFor].id, inv[priceFor].name, onClose = { priceFor = -1; refresh++ })
    if (delFor >= 0) ConfirmDialog("حذف الصنف؟", onYes = {
        val err = Repo.deleteItem(inv[delFor].id); delFor = -1
        if (err == null) refresh++ else toast(ctx, err)
    }, onNo = { delFor = -1 })
}

@Composable
fun PriceDialog(itemId: Long, name: String, onClose: () -> Unit) {
    val curs = remember { Repo.currencies() }
    FormDialog("سعر البيع: $name", listOf(
        FormField("العملة", options = curs.map { it.name }, initial = curs.first().name),
        FormField("السعر", numeric = true)
    ), onSubmit = { v ->
        val p = v[1].toDoubleOrNull()
        if (p == null || p < 0) "أدخل السعر" else Repo.setPrice(itemId, curs.first { it.name == v[0] }.id, p)
    }, onClose = onClose)
}

@Composable
fun PricesScreen(nav: NavHostController) {
    var refresh by remember { mutableStateOf(0) }
    var sel by remember { mutableStateOf(-1) }
    val inv = remember(refresh) { Repo.inventory() }
    TableScreen(nav, "أسعار البيع", listOf("السعر", "الصنف"), listOf(1f, 2f),
        inv.map { listOf(fmt(it.price), it.name) }, onRow = { sel = it })
    if (sel >= 0) PriceDialog(inv[sel].id, inv[sel].name, onClose = { sel = -1; refresh++ })
}

@Composable
fun StocktakeScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var sel by remember { mutableStateOf(-1) }
    val counted = remember { mutableStateMapOf<Long, Double>() }
    var confirm by remember { mutableStateOf(false) }
    val inv = remember(refresh) { Repo.inventory() }
    TableScreen(nav, "جرد مخزني", listOf("الفرق", "الفعلي", "الدفتري", "الصنف"), listOf(0.8f, 0.9f, 0.9f, 1.5f),
        inv.map { val c = counted[it.id]; listOf(if (c == null) "" else fmt(c - it.stock), if (c == null) "" else fmt(c), fmt(it.stock), it.name) },
        onAdd = { confirm = true }, onRow = { sel = it }, footer = "اضغط + لاعتماد الجرد")
    if (sel >= 0) FormDialog("الكمية الفعلية: " + inv[sel].name, listOf(FormField("الكمية الفعلية", numeric = true)), onSubmit = { v ->
        val q = v[0].toDoubleOrNull()
        if (q == null || q < 0) "أدخل كمية صحيحة" else { counted[inv[sel].id] = q; null }
    }, onClose = { sel = -1 })
    if (confirm) ConfirmDialog("اعتماد الجرد وتسجيل فروقه كتسوية مخزنية؟", onYes = {
        confirm = false
        val costs = Repo.itemCosts()
        val up = ArrayList<com.mohaseb.soft.data.Line>(); val down = ArrayList<com.mohaseb.soft.data.Line>()
        inv.forEach { r ->
            val c = counted[r.id]
            if (c != null && Math.abs(c - r.stock) > 0.0001) {
                val l = com.mohaseb.soft.data.Line(r.name, Math.abs(c - r.stock), costs[r.id] ?: 0.0)
                if (c > r.stock) up.add(l) else down.add(l)
            }
        }
        if (up.isEmpty() && down.isEmpty()) toast(ctx, "لا توجد فروقات")
        else {
            val br = Repo.branches().firstOrNull()?.id ?: 0L
            fun mk(lines: List<com.mohaseb.soft.data.Line>, adj: Int) = com.mohaseb.soft.data.BillIn(
                trType = 4, billType = 0, isBack = false, date = Repo.isoToday(), brId = br, toBrId = null, party = "", partyType = 7,
                partyTree = 124, cusDefault = null, currId = 0, cashId = -3, notes = "جرد مخزني", discount = 0.0, taxId = -1,
                taxPer = 0.0, fees = 0.0, paid = 0.0, adjId = adj, lines = lines)
            var err: String? = null
            if (up.isNotEmpty()) err = Repo.saveBill(mk(up, 2))
            if (err == null && down.isNotEmpty()) err = Repo.saveBill(mk(down, 1))
            if (err == null) { counted.clear(); refresh++; toast(ctx, "تم اعتماد الجرد") } else toast(ctx, err)
        }
    }, onNo = { confirm = false })
}
