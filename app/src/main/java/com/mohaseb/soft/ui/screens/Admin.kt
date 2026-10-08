package com.mohaseb.soft.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.fmt

/**
 * شاشة إدارة عامة: query تعيد (id، الأعمدة المعروضة...) ، وfields لنافذة الإضافة،
 * وusedSql (اختياري) استعلام يعيد عدد مرات استخدام السجل (?) قبل الحذف.
 */
@Composable
fun CrudScreen(nav: NavHostController, title: String, cols: List<String>, weights: List<Float>, query: String,
               fields: List<FormField>, onSubmit: (List<String>) -> String?,
               deleteSql: String? = null, usedSql: String? = null, protectedIds: Set<Long> = emptySet(),
               onRowClick: ((List<String>) -> Unit)? = null) {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<List<String>?>(null) }
    val data = remember(refresh) { Repo.rows(query) }
    TableScreen(nav, title, cols, weights, data.map { it.drop(1) },
        onAdd = if (fields.isNotEmpty()) ({ showAdd = true }) else null,
        onRow = { i -> if (onRowClick != null) onRowClick(data[i]) else if (deleteSql != null) del = data[i] })
    if (showAdd) FormDialog(title, fields, onSubmit, onClose = { showAdd = false; refresh++ })
    if (del != null) ConfirmDialog("حذف السجل؟", onYes = {
        val r = del!!; del = null
        val id = Repo.lng(r[0])
        when {
            id in protectedIds -> toast(ctx, "لا يمكن حذف هذا السجل")
            usedSql != null && Repo.lng(Repo.one(usedSql, id, id)) > 0 -> toast(ctx, "السجل مستخدم ولا يمكن حذفه")
            else -> { val err = Repo.attempt { Repo.exec(deleteSql!!, id) }; if (err == null) refresh++ else toast(ctx, err) }
        }
    }, onNo = { del = null })
}

fun addSimple(sql: String, vararg a: Any?): String? = Repo.attempt { Repo.exec(sql, *a) }

@Composable
fun WarehousesScreen(nav: NavHostController) = CrudScreen(nav, "المخازن", listOf("المخزن"), listOf(1f),
    "select id,name from branches order by id", listOf(FormField("اسم المخزن")),
    { v -> if (v[0].isBlank()) "أدخل الاسم" else addSimple("insert into branches(name,date_,IS_ACTIVE) values(?,?,1)", v[0].trim(), Repo.isoToday()) },
    deleteSql = "delete from branches where id=?", usedSql = "select count(*) from bills where br_id=? or to_br_id=?", protectedIds = setOf(0L))

@Composable
fun UnitsScreen(nav: NavHostController) = CrudScreen(nav, "وحدات القياس", listOf("الاختصار", "الوحدة"), listOf(1f, 1.5f),
    "select id,code,name from units order by id", listOf(FormField("اسم الوحدة"), FormField("الاختصار")),
    { v -> if (v[0].isBlank()) "أدخل الاسم" else addSimple("insert into units(name,code) values(?,?)", v[0].trim(), v[1].ifBlank { v[0].trim() }) },
    deleteSql = "delete from units where id=?", usedSql = "select count(*) from items where unit_id=? or ?=0", protectedIds = setOf(0L))

@Composable
fun ItemTypesScreen(nav: NavHostController) = CrudScreen(nav, "التصنيفات", listOf("التصنيف"), listOf(1f),
    "select id,name from item_type order by id", listOf(FormField("اسم التصنيف")),
    { v -> if (v[0].isBlank()) "أدخل الاسم" else addSimple("insert into item_type(name) values(?)", v[0].trim()) },
    deleteSql = "delete from item_type where id=?", usedSql = "select count(*) from items where item_type_id=? or ?=0", protectedIds = setOf(0L))

@Composable
fun GroupsScreen(nav: NavHostController) = CrudScreen(nav, "مجموعة الصنف", listOf("المجموعة"), listOf(1f),
    "select id,name from \"groups\" order by id", listOf(FormField("اسم المجموعة")),
    { v -> if (v[0].isBlank()) "أدخل الاسم" else addSimple("insert into \"groups\"(name) values(?)", v[0].trim()) },
    deleteSql = "delete from \"groups\" where id=?", usedSql = "select count(*) from customers where g_id=? or ?=0", protectedIds = setOf(0L))

@Composable
fun TaxesScreen(nav: NavHostController) = CrudScreen(nav, "الضريبة", listOf("افتراضي", "النسبة %", "الاسم"), listOf(0.8f, 1f, 1.5f),
    "select id,case when is_default=1 then '✓' else '' end,per,name from tax order by id",
    listOf(FormField("اسم الضريبة"), FormField("النسبة %", numeric = true)),
    { v -> if (v[0].isBlank()) "أدخل الاسم" else addSimple("insert into tax(name,tax_type_id,per,is_active,is_default) values(?,1,?,1,0)", v[0].trim(), v[1].toDoubleOrNull() ?: 0.0) },
    deleteSql = "delete from tax where id=?", protectedIds = setOf(-1L, 0L))

@Composable
fun CurrenciesScreen(nav: NavHostController) = CrudScreen(nav, "العملات", listOf("الرمز", "العملة"), listOf(1f, 1.5f),
    "select id,code_name,name from currency order by id",
    listOf(FormField("اسم العملة"), FormField("الرمز (مثال USD)"), FormField("اسم الجزء الصغير (مثال سنت)")),
    { v -> if (v[0].isBlank()) "أدخل الاسم" else addSimple("insert into currency(name,curr_type,fils_name,code_name) values(?,1,?,?)", v[0].trim(), v[2], v[1]) },
    deleteSql = "delete from currency where id=?",
    usedSql = "select (select count(*) from transactions where curr_id=?)+(select count(*) from bills where curr_id=?)", protectedIds = setOf(0L, 1L))

@Composable
fun UsersScreen(nav: NavHostController) = CrudScreen(nav, "المستخدمين والصلاحيات", listOf("الحالة", "المستخدم"), listOf(1f, 1.5f),
    "select id,case when is_active=1 then 'فعال' else 'غير مفعل' end,name from users order by id", emptyList(), { null })

@Composable
fun ClosingScreen(nav: NavHostController) = CrudScreen(nav, "إقفال سنوي", listOf("تاريخ الإنشاء", "تاريخ الإقفال"), listOf(1f, 1f),
    "select id,now_,date_ from closing_year order by id desc", emptyList(), { null })

@Composable
fun RatesScreen(nav: NavHostController) {
    var refresh by remember { mutableStateOf(0) }
    var sel by remember { mutableStateOf(-1) }
    val data = remember(refresh) {
        Repo.rows("select c.id,c.name,ifnull((select price from currency_price where curr_id=c.id and t_date is null),0) from currency c where c.id!=0 order by c.id")
    }
    TableScreen(nav, "سعر العملات", listOf("السعر مقابل المحلي", "العملة"), listOf(1.2f, 1.5f),
        data.map { listOf(fmt(Repo.d(it[2])), it[1]) }, onRow = { sel = it })
    if (sel >= 0) FormDialog("سعر " + data[sel][1], listOf(FormField("السعر الجديد", numeric = true)), onSubmit = { v ->
        val p = v[0].toDoubleOrNull()
        val id = data[sel][0]
        if (p == null || p <= 0) "أدخل سعراً صحيحاً" else Repo.attempt {
            val today = Repo.isoToday()
            if (Repo.one("select count(*) from currency_price where curr_id=? and f_date=?", id, today) != "0")
                Repo.exec("update currency_price set price=? where curr_id=? and f_date=?", p, id, today)
            else Repo.exec("insert into currency_price(curr_id,f_date,price) values(?,?,?)", id, today, p)
        }
    }, onClose = { sel = -1; refresh++ })
}

@Composable
fun CeilingsScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(-1L) }
    val data = remember(refresh) {
        Repo.rows("select l.id,l.db,l.cr,cu.name,c.name from cus_limit l join customers c on c.id=l.cus_id join currency cu on cu.id=l.curr_id order by c.name")
    }
    // ملاحظة: في القاعدة cr = الحد الأعلى للرصيد المدين، db = الحد الأعلى للرصيد الدائن (حسب منطق المحفزات)
    TableScreen(nav, "سقف الحساب", listOf("سقف الدائن", "سقف المدين", "العملة", "الحساب"), listOf(1f, 1f, 0.8f, 1.5f),
        data.map { listOf(fmt(Repo.d(it[1])), fmt(Repo.d(it[2])), it[3], it[4]) },
        onAdd = { showAdd = true }, onRow = { del = Repo.lng(data[it][0]) })
    if (showAdd) {
        val accs = remember { Repo.accountNames() }
        val curs = remember { Repo.currencies() }
        FormDialog("سقف الحساب", listOf(
            FormField("الحساب", options = accs.map { it.name }), FormField("العملة", options = curs.map { it.name }, initial = curs.first().name),
            FormField("أقصى رصيد مدين", numeric = true), FormField("أقصى رصيد دائن", numeric = true)
        ), onSubmit = { v ->
            val a = accs.firstOrNull { it.name == v[0] }
            if (a == null) "اختر الحساب" else addSimple("insert or replace into cus_limit(cus_id,curr_id,cr,db) values(?,?,?,?)",
                a.id, curs.first { it.name == v[1] }.id, v[2].toDoubleOrNull() ?: 0.0, v[3].toDoubleOrNull() ?: 0.0)
        }, onClose = { showAdd = false; refresh++ })
    }
    if (del >= 0) ConfirmDialog("حذف السقف؟", onYes = {
        val err = addSimple("delete from cus_limit where id=?", del); del = -1
        if (err == null) refresh++ else toast(ctx, err)
    }, onNo = { del = -1 })
}
