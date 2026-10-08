package com.mohaseb.soft.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.EntryRow
import com.mohaseb.soft.data.Named
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun VoucherScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var dis by remember { mutableStateOf(false) }
    val currencies = remember { Repo.currencies() }
    val boxes = remember { Repo.boxes() }
    var currency by remember { mutableStateOf(currencies.firstOrNull() ?: Named(0, "محلي")) }
    var box by remember { mutableStateOf(boxes.firstOrNull() ?: Named(-3, "الصندوق")) }
    var desc by remember { mutableStateOf("") }
    val entries = remember { mutableStateListOf<EntryRow>() }
    var showAccounts by remember { mutableStateOf(false) }
    var showBox by remember { mutableStateOf(false) }
    var showCur by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
        val total = entries.sumOf { it.amount }
    val date = SimpleDateFormat("dd-MM-yyyy").format(Date())
    val number = remember { Repo.nextVoucherNo() }
    val title = if (dis) "سند صرف" else "سند قبض"

    fun shareText(): String = "$title رقم $number - التاريخ $date - المجموع ${fmt(total)} - البيان $desc"

    Scaffold(topBar = {
        TopBar(title, onBack = { nav.popBackStack() }, right = {
            PdfIcon { PdfShare(ctx, title, listOf("الحساب", "المبلغ", "ملاحظات"),
                entries.map { listOf(it.account, fmt(it.amount), it.notes) }) }
            IconButton(onClick = { showSave = true }) { Icon(Icons.Filled.Save, null, tint = Color.White) }
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("جديد") }, onClick = { entries.clear(); desc = ""; menu = false })
                DropdownMenuItem(text = { Text("حذف") }, onClick = { entries.clear(); menu = false })
                DropdownMenuItem(text = { Text("مشاركة") }, onClick = {
                    menu = false
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText())
                    }, "مشاركة"))
                })
                DropdownMenuItem(text = { Text("إشعار رسالة") }, onClick = {
                    menu = false
                    try { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", shareText())) } catch (e: Exception) { }
                })
                DropdownMenuItem(text = { Text("إشعار واتساب") }, onClick = {
                    menu = false
                    try {
                        val wa = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; setPackage("com.whatsapp"); putExtra(Intent.EXTRA_TEXT, shareText())
                        }
                        ctx.startActivity(wa)
                    } catch (e: Exception) {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText())
                        }, "مشاركة"))
                    }
                })
                DropdownMenuItem(text = { Text("طباعة حرارية") }, onClick = {
                    menu = false
                    PdfShare(ctx, title, listOf("الحساب", "المبلغ", "ملاحظات"),
                        entries.map { listOf(it.account, fmt(it.amount), it.notes) })
                })
            }
        })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg)) {
            Column(Modifier.fillMaxWidth().padding(6.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.5.dp, Blue, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(box.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).clickable { showBox = true })
                    Text(currency.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).clickable { showCur = true }, textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        Text(if (dis) "صرف" else "قبض", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = dis, onCheckedChange = { dis = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Pink))
                    }
                }
                Row(Modifier.padding(top = 6.dp)) {
                    Text(date, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("رقم#$number", fontSize = 14.sp, color = Color(0xFF777777), modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Right)
                }
                LineField(desc, { desc = it }, "البيان", lineColor = Color(0xFFB5B5B5),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp).background(Color.White, RoundedCornerShape(22.dp)).border(1.5.dp, Blue, RoundedCornerShape(22.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PlusCircle(onClick = { showAccounts = true }, size = 30)
                Text("القيد", color = Color(0xFF9AA0A6), fontSize = 15.sp,
                    modifier = Modifier.weight(1f).padding(start = 8.dp).clickable { showAccounts = true },
                    textAlign = TextAlign.Center)
            }
            HeaderRow(listOf("ملاحظات", "المبلغ", "الحساب"), listOf(1f, 1f, 1.2f))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                entries.forEach { e ->
                    Row(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp, horizontal = 6.dp)) {
                        Text(e.notes, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(fmt(e.amount), fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(e.account, fontSize = 13.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    }
                    Divider(color = Color(0xFFDDDDDD))
                }
            }
            Row(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 10.dp, horizontal = 16.dp)) {
                Text(fmt(total), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("المجموع", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Right)
            }
        }
    }

    if (showAccounts) EntryDialog(onAdd = { e -> entries.add(e); showAccounts = false }, onClose = { showAccounts = false })

    if (showBox) SimpleListDialog(boxes.map { it.name }, onPick = { n -> box = boxes.first { it.name == n }; showBox = false }, onClose = { showBox = false })

    if (showCur) SimpleListDialog(currencies.map { it.name }, onPick = { n -> currency = currencies.first { it.name == n }; showCur = false }, onClose = { showCur = false })

    if (showSave) SaveDialog(onYes = {
        showSave = false
        if (entries.isEmpty()) toast(ctx, "أضف قيداً واحداً على الأقل")
        else if (Repo.currencyRate(currency.id) <= 0.0) toast(ctx, "حدد سعر العملة أولاً من (سعر العملات)")
        else {
            val err = Repo.saveVoucher(dis, date, currency.id, box.id, desc, entries.toList())
            if (err == null) nav.popBackStack() else toast(ctx, err)
        }
    }, onNo = { showSave = false })
}

@Composable
fun EntryDialog(onAdd: (EntryRow) -> Unit, onClose: () -> Unit) {
    var acc by remember { mutableStateOf("") }
    var amt by remember { mutableStateOf("") }
    var nt by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val sugg = remember(acc) { if (acc.isBlank()) emptyList() else Repo.findAccounts(acc.trim(), null, 5).filter { it.name != acc.trim() } }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineField(acc, { acc = it; error = false }, "إسم الحساب", modifier = Modifier.weight(1f))
                if (error) Icon(Icons.Filled.Error, null, tint = Color.Red, modifier = Modifier.size(16.dp))
            }
            if (sugg.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                sugg.forEach { s ->
                    Box(Modifier.padding(end = 6.dp).clickable { acc = s.name }
                        .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(s.name, fontSize = 13.sp)
                    }
                }
            }
            if (error) {
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.background(Color.Black, RoundedCornerShape(3.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("غير صحيح", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.Calculate, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(20.dp))
                NumField(amt, { amt = it }, modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
            Text("المبلغ", fontSize = 12.sp, color = Color(0xFF777777), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.CameraAlt, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(20.dp))
                LineField(nt, { nt = it }, "ملاحظات", lineColor = Color(0xFFB5B5B5), modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                BlueButton("+إغلاق", onClick = onClose)
                Spacer(Modifier.width(12.dp))
                BlueButton("إضافة", onClick = {
                    val id = Repo.one("select id from customers where name=?", acc.trim())
                    val a = amt.toDoubleOrNull() ?: 0.0
                    if (id.isEmpty() || a <= 0.0) error = true
                    else onAdd(EntryRow(Repo.lng(id), acc.trim(), a, nt))
                })
            }
        }
    }
}
