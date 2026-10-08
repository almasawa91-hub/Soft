package com.mohaseb.soft.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.*

@Composable
fun OpeningBalanceScreen(nav: NavHostController) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var local by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val date = Repo.dmyToday()
    val currencies = remember { Repo.currencies() }
    val foreign = currencies.firstOrNull { it.id != 0L }
    val sugg = remember(name) { if (name.isBlank()) emptyList() else Repo.findAccounts(name.trim(), null, 4).filter { it.name != name.trim() } }

    fun save(debtor: Boolean) {
        val a = amount.toDoubleOrNull() ?: 0.0
        if (name.isBlank() || a <= 0.0) { error = true; return }
        val curId = if (local) 0L else (foreign?.id ?: 0L)
        if (Repo.currencyRate(curId) <= 0.0) { toast(ctx, "حدد سعر العملة أولاً من (سعر العملات)"); return }
        val err = Repo.attempt { Repo.ensureAccount(name, if (debtor) 0 else 1, if (debtor) 123 else 221) }
            ?: Repo.saveOpening(Repo.lng(Repo.one("select id from customers where name=?", name.trim())), a, debtor, notes, date, curId)
        if (err == null) nav.popBackStack() else toast(ctx, err)
    }

    Scaffold(topBar = { TopBar("إضافة رصيد إفتتاحي", onBack = { nav.popBackStack() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg)) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("حساب رأس المال", fontSize = 12.sp, color = Color(0xFF777777))
                Spacer(Modifier.width(10.dp))
                Text("رأس المال", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    style = TextStyle(textDecoration = TextDecoration.Underline))
            }
            Divider(color = Color(0xFFDDDDDD))
            Column(Modifier.background(Color.White).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Calculate, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.text.BasicTextField(value = amount, onValueChange = { amount = it },
                            singleLine = true, textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                            modifier = Modifier.weight(1f).padding(start = 6.dp).drawBehind {
                                drawLine(Color(0xFFB5B5B5), 
                                    androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                                    androidx.compose.ui.geometry.Offset(size.width, size.height - 1f), 
                                    strokeWidth = 2f)
                            }.padding(vertical = 6.dp),
                            decorationBox = { inner -> Box { if (amount.isEmpty()) Text("المبلغ", color = Color(0xFF9AA0A6), fontSize = 15.sp); inner() } })
                    }
                    Spacer(Modifier.width(12.dp))
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        LineField(name, { name = it; error = false }, "الإسم", modifier = Modifier.weight(1f))
                        if (error) Icon(Icons.Filled.Error, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
                if (sugg.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                    sugg.forEach { s ->
                        Box(Modifier.padding(end = 6.dp).clickable { name = s.name }
                            .background(Color(0xFFE3F2FD), androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(s.name, fontSize = 13.sp)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Text(date, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.CameraAlt, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(18.dp))
                    LineField(notes, { notes = it }, "ملاحظات", lineColor = Color(0xFFB5B5B5), modifier = Modifier.weight(1f).padding(start = 6.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !local, onClick = { local = false })
                        Text(foreign?.name ?: "دولار", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = local, onClick = { local = true },
                            colors = RadioButtonDefaults.colors(selectedColor = Pink))
                        Text("محلي", fontSize = 14.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.Center) {
                    Row(Modifier.clickable { save(false) }
                        .background(Color(0xFF2278B0), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .padding(horizontal = 26.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("دائن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("▲", color = Color(0xFF8BC34A), fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Row(Modifier.clickable { save(true) }
                        .background(Color(0xFF2278B0), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .padding(horizontal = 26.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("مدين", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("▼", color = Color.Red, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun OpeningListScreen(nav: NavHostController) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var del by remember { mutableStateOf(-1L) }
    val list = remember(refresh) { Repo.openings() }
    val rows = list.map { listOf(it[4], it[5], fmt(Repo.d(it[2])), it[3], it[1]) }
    TableScreen(nav, "قيد إفتتاحي", listOf("التاريخ", "العملة", "المبلغ", "النوع", "الحساب"), listOf(1f, 0.8f, 1f, 0.8f, 1.4f), rows,
        onAdd = { nav.navigate("opening") }, onRow = { del = Repo.lng(list[it][0]) })
    if (del >= 0) ConfirmDialog("حذف القيد الافتتاحي؟", onYes = {
        val err = Repo.deleteTransaction(del); del = -1
        if (err == null) refresh++ else toast(ctx, err)
    }, onNo = { del = -1 })
}
