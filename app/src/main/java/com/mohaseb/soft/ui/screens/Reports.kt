package com.mohaseb.soft.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.fmt

@Composable
fun ItemLedgerScreen(nav: NavHostController, itemId: Long) {
    val name = remember { Repo.one("select name from items where id=?", itemId) }
    val open = remember { Repo.d(Repo.one("select o_qty from items where id=?", itemId)) }
    var run = open
    val rows = ArrayList<List<String>>()
    rows.add(listOf(fmt(run), "", "", "رصيد أول المدة", ""))
    Repo.itemLedger(itemId).forEach {
        val i = Repo.d(it[3]); val o = Repo.d(it[4])
        run += i - o
        rows.add(listOf(fmt(run), fmt(o), fmt(i), it[1] + " #" + it[2], Repo.dmy(it[0])))
    }
    TableScreen(nav, name, listOf("الرصيد", "صادر", "وارد", "البيان", "التاريخ"), listOf(0.9f, 0.8f, 0.8f, 1.6f, 1f), rows)
}

@Composable
fun ReportScreen(nav: NavHostController, type: String) {
    when (type) {
        "move" -> {
            val inv = remember { Repo.inventory() }
            TableScreen(nav, "حركة الأصناف", listOf("الكمية", "الصنف"), listOf(1f, 2f), inv.map { listOf(fmt(it.stock), it.name) },
                onRow = { nav.navigate("itemledger/${inv[it].id}") }, footer = "اختر صنفاً لعرض حركته")
        }
        "trial" -> {
            val d = remember { Repo.trial() }
            val rows = ArrayList<List<String>>()
            var td = 0.0; var tc = 0.0
            d.forEach {
                val db = Repo.d(it[2]); val cr = Repo.d(it[3])
                if (db != 0.0 || cr != 0.0) { rows.add(listOf(fmt(cr), fmt(db), it[1].trim())); td += db; tc += cr }
            }
            rows.add(listOf(fmt(tc), fmt(td), "الإجمالي"))
            TableScreen(nav, "ميزان المراجعة", listOf("دائن", "مدين", "الحساب"), listOf(1f, 1f, 1.6f), rows)
        }
        "income" -> KeyValueScreen(nav, "قائمة الدخل", remember { Repo.incomeStatement() })
        "financial" -> {
            val p = remember { Repo.financialPosition() }
            KeyValueScreen(nav, "المركز المالي", p.first + listOf("*—" to "") + p.second)
        }
        "debtors", "creditors" -> {
            val deb = type == "debtors"
            val list = remember { Repo.accountBalances().filter { if (deb) it.balance > 0.005 else it.balance < -0.005 && it.id > 0 } }
            val rows = list.map { listOf(balText(it.balance, it.curr), it.name) }
            TableScreen(nav, if (deb) "أرصدة مدينة" else "أرصدة دائنة", listOf("الرصيد", "الحساب"), listOf(1f, 1.6f), rows,
                onRow = { nav.navigate("statement/${list[it].id}") })
        }
        "inventory" -> {
            val inv = remember { Repo.inventory() }
            val rows = ArrayList<List<String>>()
            inv.forEach { rows.add(listOf(fmt(it.stock * it.cost), fmt(it.cost), fmt(it.stock), it.name)) }
            rows.add(listOf(fmt(inv.sumOf { if (it.stock > 0) it.stock * it.cost else 0.0 }), "", "", "الإجمالي"))
            TableScreen(nav, "قيمة المخزون", listOf("القيمة", "متوسط التكلفة", "الكمية", "الصنف"), listOf(1f, 1f, 0.8f, 1.5f), rows)
        }
        "boxes" -> {
            val boxes = remember { Repo.boxes() }
            val bal = remember { Repo.accountBalances() }
            val rows = boxes.map { b -> listOf(bal.filter { it.id == b.id }.joinToString(" ، ") { balText(it.balance, it.curr) }.ifBlank { "0" }, b.name) }
            TableScreen(nav, "حركة الصندوق", listOf("الرصيد", "الصندوق"), listOf(1.3f, 1.3f), rows,
                onRow = { nav.navigate("statement/${boxes[it].id}") })
        }
        else -> {
            val items = listOf("أرصدة مدينة" to "debtors", "أرصدة دائنة" to "creditors", "قيمة المخزون" to "inventory", "حركة الصندوق" to "boxes")
            TableScreen(nav, "تقارير أخرى", listOf("التقرير"), listOf(1f), items.map { listOf(it.first) }, onRow = { nav.navigate("report/${items[it].second}") })
        }
    }
}
