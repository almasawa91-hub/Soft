package com.mohaseb.soft.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.mohaseb.soft.data.BillIn
import com.mohaseb.soft.data.Line
import com.mohaseb.soft.data.Named
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.*

data class KindCfg(val title: String, val listTitle: String, val trType: Int, val party: String)

fun kindCfg(kind: String): KindCfg = when (kind) {
    "sale" -> KindCfg("بيع", "قائمة المبيعات", 1, "العميل")
    "purchase" -> KindCfg("شراء", "قائمة المشتريات", 2, "المورد")
    "stockout" -> KindCfg("صرف مخزني", "صرف مخزني", 11, "الحساب")
    "stockin" -> KindCfg("توريد مخزني", "توريد مخزني", 21, "الحساب")
    "transfer" -> KindCfg("تحويل مخزني", "تحويل مخزني", 3, "")
    "adjust" -> KindCfg("تسوية مخزنية", "تسوية مخزنية", 4, "")
    "quote" -> KindCfg("عرض سعر", "فاتورة عرض سعر", 8, "العميل")
    else -> KindCfg("طلب شراء", "طلب شراء", 9, "المورد")
}

@Composable
fun ItemDialog(kind: String, curId: Long, initial: Line? = null, onAdd: (Line) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var qty by remember { mutableStateOf(if (initial != null) fmt(initial.qty) else "1") }
    var price by remember { mutableStateOf(if (initial != null) fmt(initial.price) else "") }
    var totalTxt by remember { mutableStateOf("") }
    val sugg = remember(name) { if (name.isBlank()) emptyList() else Repo.findItems(name.trim(), 4).filter { it != name.trim() } }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineField(name, { name = it }, "إسم الصنف", modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Info, null, tint = Blue, modifier = Modifier.size(18.dp))
            }
            if (sugg.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                sugg.forEach { s ->
                    Box(Modifier.padding(end = 6.dp).clickable {
                        name = s; price = fmt(Repo.itemPrice(s, kind, curId))
                    }.background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(s, fontSize = 13.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("+000", color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clickable { qty = fmt((qty.toDoubleOrNull() ?: 0.0) * 1000) })
                LineField(qty, { qty = it }, "", lineColor = Color(0xFFB5B5B5), modifier = Modifier.weight(1f).padding(horizontal = 8.dp), keyboard = KeyboardType.Number)
                Text("الكمية", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("+000", color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clickable { price = fmt((price.toDoubleOrNull() ?: 0.0) * 1000) })
                LineField(price, { price = it }, "", lineColor = Color(0xFFB5B5B5), modifier = Modifier.weight(1f).padding(horizontal = 8.dp), keyboard = KeyboardType.Number)
                Text("السعر", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.Calculate, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(18.dp))
                LineField(totalTxt, {
                    totalTxt = it
                    val q = qty.toDoubleOrNull() ?: 0.0
                    val t = it.toDoubleOrNull()
                    if (t != null && q > 0) price = fmt(t / q)
                }, "أو الإجمالي", lineColor = Color(0xFFB5B5B5), modifier = Modifier.weight(1f).padding(start = 6.dp), keyboard = KeyboardType.Number)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+إغلاق", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClose() })
                Spacer(Modifier.weight(1f))
                Text("إلغاء", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClose() })
                Spacer(Modifier.width(16.dp))
                Text("إضافة", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val q = qty.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && q > 0) onAdd(Line(name.trim(), q, price.toDoubleOrNull() ?: 0.0))
                    })
            }
        }
    }
}

@Composable
fun SaleScreen(nav: NavHostController, kind: String) {
    val ctx = LocalContext.current
    val cfg = remember(kind) { kindCfg(kind) }
    val isTrade = kind == "sale" || kind == "purchase"
    val hasParty = kind != "transfer" && kind != "adjust"
    val hasTax = isTrade || kind == "quote" || kind == "po"
    val branches = remember { Repo.branches() }
    val currencies = remember { Repo.currencies() }
    val boxes = remember { Repo.boxes() }
    val taxes = remember { Repo.rows("select id,name,per from tax where is_active=1 order by id") }
    val adjNames = listOf("عجز", "زيادة", "تالف", "افتتاحي")

    var cash by remember { mutableStateOf(false) }
    var isReturn by remember { mutableStateOf(false) }
    var br by remember { mutableStateOf(branches.firstOrNull() ?: Named(0, "المخزن الرئيسي")) }
    var toBr by remember { mutableStateOf(branches.lastOrNull() ?: Named(0, "المخزن الرئيسي")) }
    var cur by remember { mutableStateOf(currencies.firstOrNull() ?: Named(0, "محلي")) }
    var box by remember { mutableStateOf(boxes.firstOrNull() ?: Named(-3, "الصندوق")) }
    var party by remember { mutableStateOf("") }
    var partyError by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<Line>() }
    var discount by remember { mutableStateOf(0.0) }
    var taxId by remember { mutableStateOf(-1L) }
    var taxPer by remember { mutableStateOf(0.0) }
    var fees by remember { mutableStateOf(0.0) }
    var paid by remember { mutableStateOf(0.0) }
    var adjIdx by remember { mutableStateOf(0) }
    var dlg by remember { mutableStateOf("") }
    var editIdx by remember { mutableStateOf(-1) }
    var numDlg by remember { mutableStateOf("") }

    val total = lines.sumOf { it.qty * it.price }
    val taxAmt = (total - discount) * taxPer / 100.0
    val net = total - discount + taxAmt + fees
    val remaining = net - (if (cash) net else paid)
    val date = Repo.isoToday()
    val number = remember { Repo.nextBillNo(cfg.trType) }
    val title = (if (isReturn) "مرتجع " else "") + cfg.title + (if (isTrade) (if (cash) " نقدي" else " اجل") else "")
    val partyTypes: List<Int>? = when (kind) { "sale", "quote" -> listOf(0); "purchase", "po" -> listOf(1); else -> null }
    val sugg = remember(party) {
        if (party.isBlank()) emptyList() else Repo.findAccounts(party.trim(), partyTypes, 4).filter { it.name != party.trim() }
    }

    fun trySave() {
        if (hasParty && party.isBlank() && !(isTrade && cash)) { partyError = true; return }
        if (lines.isEmpty()) { toast(ctx, "أضف صنفاً واحداً على الأقل"); return }
        if (kind == "transfer" && br.id == toBr.id) { toast(ctx, "اختر مخزنين مختلفين"); return }
        if (Repo.currencyRate(cur.id) <= 0.0) { toast(ctx, "حدد سعر العملة أولاً من (سعر العملات)"); return }
        if (isTrade && !cash && paid > net + 0.001) { toast(ctx, "المبلغ المدفوع أكبر من الصافي"); return }
        dlg = "save"
    }

    Scaffold(topBar = {
        TopBar(title, onBack = { nav.popBackStack() }, right = {
            IconButton(onClick = { trySave() }) { Icon(Icons.Filled.Save, null, tint = Color.White) }
        })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg)) {
            Column(Modifier.fillMaxWidth().padding(6.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.5.dp, Blue, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (isTrade) {
                            Checkbox(checked = isReturn, onCheckedChange = { isReturn = it }, colors = CheckboxDefaults.colors(checkedColor = Pink))
                            Text("مرتجع", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(br.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).clickable { dlg = "br" }, textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        if (isTrade) {
                            Text(if (cash) "نقد" else "اجل", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Switch(checked = cash, onCheckedChange = { cash = it }, colors = SwitchDefaults.colors(checkedTrackColor = Pink))
                        } else if (kind == "adjust") {
                            Text(adjNames[adjIdx], fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { dlg = "adj" })
                            Icon(Icons.Filled.ArrowDropDown, null, tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { dlg = "cur" }) {
                        Text(cur.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                    }
                    if (isTrade && cash) {
                        Spacer(Modifier.width(14.dp))
                        Text(box.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { dlg = "box" })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text(Repo.dmy(date), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(0.9f))
                    if (hasParty) {
                        Row(Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                            LineField(party, { party = it; partyError = false }, "", modifier = Modifier.weight(1f))
                            if (partyError) Icon(Icons.Filled.Error, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Text(cfg.party, fontSize = 14.sp, color = Color(0xFF777777))
                        }
                    } else if (kind == "transfer") {
                        Row(Modifier.weight(2f).clickable { dlg = "tobr" }, verticalAlignment = Alignment.CenterVertically) {
                            Text(toBr.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("إلى مخزن", fontSize = 14.sp, color = Color(0xFF777777))
                        }
                    } else Spacer(Modifier.weight(2f))
                }
                if (partyError) {
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                        Box(Modifier.background(Color.Black, RoundedCornerShape(3.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("required", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (sugg.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
                    sugg.forEach { s ->
                        Box(Modifier.padding(end = 6.dp).clickable { party = s.name }
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(s.name, fontSize = 13.sp)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineField(notes, { notes = it }, "ملاحظات", lineColor = Color(0xFFB5B5B5), hintCenter = true, modifier = Modifier.weight(2f))
                    Text("رقم#$number", fontSize = 14.sp, color = Color(0xFF777777), modifier = Modifier.weight(0.9f))
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp).background(Color.White, RoundedCornerShape(22.dp)).border(1.5.dp, Blue, RoundedCornerShape(22.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PlusCircle(onClick = { dlg = "item" }, size = 30)
                Text("إكتب اسم الصنف", color = Color(0xFF9AA0A6), fontSize = 15.sp,
                    modifier = Modifier.weight(1f).padding(start = 8.dp).clickable { dlg = "item" }, textAlign = TextAlign.Center)
            }
            HeaderRow(listOf("الاجمالي", "السعر", "الكمية", "الصنف"), listOf(1f, 1f, 1f, 1.2f))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                lines.forEachIndexed { i, l ->
                    Row(Modifier.fillMaxWidth().background(Color.White).clickable { editIdx = i; dlg = "edit" }
                        .padding(vertical = 12.dp, horizontal = 6.dp)) {
                        Text(fmt(l.qty * l.price), fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(fmt(l.price), fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(fmt(l.qty), fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(l.name, fontSize = 13.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    }
                    Divider(color = Color(0xFFDDDDDD))
                }
            }
            Column(Modifier.fillMaxWidth().background(Color.White).padding(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Row { Text(fmt(net), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("الصافي", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)) }
                        if (isTrade) Row { Text(fmt(remaining), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("المبلغ المتبقي", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)) }
                    }
                    if (hasTax) Column(Modifier.weight(1f)) {
                        Row { Text(fmt(discount), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("الخصم", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable { numDlg = "disc" }) }
                        Row { Text(fmt(taxPer) + "%", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Row(Modifier.weight(1f).clickable { dlg = "tax" }, verticalAlignment = Alignment.CenterVertically) { Text("الضريبة", fontSize = 13.sp, fontWeight = FontWeight.Bold); Icon(Icons.Filled.ArrowDropDown, null, tint = Color.DarkGray, modifier = Modifier.size(14.dp)) } }
                        if (isTrade) {
                            Row { Text(fmt(fees), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("رسوم اخرى", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable { numDlg = "fees" }) }
                            if (!cash) Row { Text(fmt(paid), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("المبلغ المدفوع", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable { numDlg = "paid" }) }
                        }
                    }
                }
            }
        }
    }

    if (dlg == "save") SaveDialog(onYes = {
        dlg = ""
        val billType = when { isTrade -> if (cash) 1 else 2; kind == "stockout" || kind == "stockin" -> 2; else -> 0 }
        val partyType = when (kind) { "sale", "quote" -> 0; "purchase", "po" -> 1; else -> 7 }
        val partyTree = when (kind) { "sale", "quote" -> 123; "purchase", "po" -> 221; else -> 124 }
        val err = Repo.saveBill(BillIn(
            trType = cfg.trType, billType = billType, isBack = isReturn && isTrade, date = date, brId = br.id,
            toBrId = if (kind == "transfer") toBr.id else null, party = party.trim(), partyType = partyType, partyTree = partyTree,
            cusDefault = if (!hasParty) null else if (kind == "sale") -5L else -6L,
            currId = cur.id, cashId = box.id, notes = notes, discount = discount, taxId = taxId, taxPer = taxPer,
            fees = fees, paid = paid, adjId = if (kind == "adjust") adjIdx + 1 else 0, lines = lines.toList()))
        if (err == null) nav.popBackStack() else toast(ctx, err)
    }, onNo = { dlg = "" })
    if (dlg == "br") SimpleListDialog(branches.map { it.name }, onPick = { n -> br = branches.first { it.name == n }; dlg = "" }, onClose = { dlg = "" })
    if (dlg == "tobr") SimpleListDialog(branches.map { it.name }, onPick = { n -> toBr = branches.first { it.name == n }; dlg = "" }, onClose = { dlg = "" })
    if (dlg == "box") SimpleListDialog(boxes.map { it.name }, onPick = { n -> box = boxes.first { it.name == n }; dlg = "" }, onClose = { dlg = "" })
    if (dlg == "cur") SimpleListDialog(currencies.map { it.name }, onPick = { n -> cur = currencies.first { it.name == n }; dlg = "" }, onClose = { dlg = "" })
    if (dlg == "adj") SimpleListDialog(adjNames, onPick = { n -> adjIdx = adjNames.indexOf(n); dlg = "" }, onClose = { dlg = "" })
    if (dlg == "tax") SimpleListDialog(taxes.map { it[1].trim() + " (" + it[2] + "%)" }, onPick = { n ->
        val t = taxes.first { it[1].trim() + " (" + it[2] + "%)" == n }
        taxId = Repo.lng(t[0]); taxPer = Repo.d(t[2]); dlg = ""
    }, onClose = { dlg = "" })
    if (dlg == "item") ItemDialog(kind, cur.id, null, onAdd = { l -> lines.add(l); dlg = "" }, onClose = { dlg = "" })
    if (dlg == "edit" && editIdx >= 0) ItemDialog(kind, cur.id, lines[editIdx],
        onAdd = { l -> lines[editIdx] = l; editIdx = -1; dlg = "" },
        onClose = { editIdx = -1; dlg = "" })
    if (numDlg.isNotEmpty()) {
        var v by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { numDlg = "" }) {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).padding(16.dp)) {
                NumField(v, { v = it }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                    BlueButton("حفظ", onClick = {
                        val x = v.toDoubleOrNull() ?: 0.0
                        when (numDlg) { "disc" -> discount = x; "fees" -> fees = x; "paid" -> paid = x }
                        numDlg = ""
                    })
                }
            }
        }
    }
}

@Composable
fun InvoiceViewScreen(nav: NavHostController, id: Long, kind: String) {
    val ctx = LocalContext.current
    val cfg = remember(kind) { kindCfg(kind) }
    val b = remember(id) { Repo.bills(listOf(cfg.trType)).firstOrNull { it.id == id } }
    val lines = remember(id) { Repo.billLines(id) }
    var confirm by remember { mutableStateOf(false) }
    val rows = lines.map { listOf(fmt(it.qty * it.price), fmt(it.price), fmt(it.qty), it.name) }
    val head = if (b == null) cfg.title else (if (b.isBack) "مرتجع " else "") + cfg.title + " رقم " + b.number
    TableScreen(nav, head, listOf("الاجمالي", "السعر", "الكمية", "الصنف"), listOf(1f, 1f, 1f, 1.2f), rows,
        header = {
            if (b != null) Column(Modifier.fillMaxWidth().background(Color.White).padding(10.dp)) {
                Row { Text(Repo.dmy(b.date), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(if (b.party.isNotEmpty()) b.party else b.brName + (if (b.toBrName.isNotEmpty()) " ← " + b.toBrName else ""),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f), textAlign = TextAlign.Right) }
                Row(Modifier.padding(top = 4.dp)) {
                    Text("الصافي: " + fmt(b.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (b.trType == 1 || b.trType == 2) Text("المدفوع: " + fmt(b.paid) + " | المتبقي: " + fmt(b.amount - b.paid), fontSize = 13.sp, modifier = Modifier.weight(2f), textAlign = TextAlign.Right)
                }
                if (b.notes.isNotEmpty()) Text(b.notes, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                    BlueButton("حذف المستند", onClick = { confirm = true })
                }
            }
        })
    if (confirm) ConfirmDialog("حذف المستند وقيوده؟", onYes = {
        confirm = false
        val err = Repo.deleteBill(id)
        if (err == null) nav.popBackStack() else toast(ctx, err)
    }, onNo = { confirm = false })
}
