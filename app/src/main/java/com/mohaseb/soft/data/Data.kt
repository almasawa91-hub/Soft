package com.mohaseb.soft.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Named(val id: Long, val name: String)
data class Line(val name: String, val qty: Double, val price: Double)
data class EntryRow(val id: Long, val account: String, val amount: Double, val notes: String)
data class BillRow(val id: Long, val number: Int, val date: String, val party: String, val amount: Double,
                   val billType: Int, val isBack: Boolean, val paid: Double, val notes: String,
                   val trType: Int, val brName: String, val toBrName: String)
data class VoucherRow(val number: Int, val isPay: Boolean, val date: String, val descr: String,
                      val total: Double, val currency: String)
data class AccRow(val id: Long, val name: String, val type: String, val balance: Double, val curr: String)
data class InvRow(val id: Long, val name: String, val stock: Double, val cost: Double, val price: Double, val unit: String)
data class AccBal(val id: Long, val accP: Long, val bal: Double)

/** مدخلات فاتورة/مستند مخزني. القيود المحاسبية تنتج تلقائياً من محفزات قاعدة البيانات. */
class BillIn(
    val trType: Int, val billType: Int, val isBack: Boolean, val date: String,
    val brId: Long, val toBrId: Long?, val party: String, val partyType: Int, val partyTree: Int,
    val cusDefault: Long?, val currId: Long, val cashId: Long, val notes: String,
    val discount: Double, val taxId: Long, val taxPer: Double, val fees: Double, val paid: Double,
    val adjId: Int, val lines: List<Line>
)

object Repo {
    const val DB_NAME = "inv.db"
    lateinit var appCtx: Context
    lateinit var sdb: SQLiteDatabase

    fun dbFile(): File = appCtx.getDatabasePath(DB_NAME)

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        val f = dbFile()
        if (!f.exists()) {
            f.parentFile?.mkdirs()
            appCtx.assets.open(DB_NAME).use { i -> FileOutputStream(f).use { o -> i.copyTo(o) } }
        }
        open()
    }

    fun open() {
        sdb = SQLiteDatabase.openDatabase(dbFile().path, null, SQLiteDatabase.OPEN_READWRITE)
    }

    fun close() {
        if (sdb.isOpen) sdb.close()
    }

    // ---------- أدوات عامة ----------
    fun rows(sql: String, vararg args: Any?): List<List<String>> {
        val out = ArrayList<List<String>>()
        val c = sdb.rawQuery(sql, Array(args.size) { args[it].toString() })
        try {
            val n = c.columnCount
            while (c.moveToNext()) {
                val r = ArrayList<String>(n)
                for (i in 0 until n) r.add(c.getString(i) ?: "")
                out.add(r)
            }
        } finally {
            c.close()
        }
        return out
    }

    fun one(sql: String, vararg args: Any?): String = rows(sql, *args).firstOrNull()?.firstOrNull() ?: ""

    fun exec(sql: String, vararg args: Any?) {
        sdb.execSQL(sql, arrayOf<Any?>(*args))
    }

    fun lng(s: String): Long = s.toDoubleOrNull()?.toLong() ?: 0L
    fun d(s: String): Double = s.toDoubleOrNull() ?: 0.0

    fun cv(vararg p: Pair<String, Any?>): ContentValues {
        val c = ContentValues()
        for ((k, v) in p) {
            when (v) {
                null -> c.putNull(k)
                is String -> c.put(k, v)
                is Int -> c.put(k, v)
                is Long -> c.put(k, v)
                is Double -> c.put(k, v)
                is Boolean -> c.put(k, if (v) 1 else 0)
            }
        }
        return c
    }

    fun <T> tx(block: () -> T): T {
        sdb.beginTransaction()
        try {
            val r = block()
            sdb.setTransactionSuccessful()
            return r
        } finally {
            sdb.endTransaction()
        }
    }

    /** ينفذ العملية داخل معاملة؛ يعيد null عند النجاح أو رسالة خطأ بالعربية. */
    fun attempt(block: () -> Unit): String? = try {
        tx(block); null
    } catch (e: Exception) {
        friendly(e)
    }

    fun friendly(e: Exception): String {
        val m = e.message ?: return "حدث خطأ"
        return when {
            m.contains("CLOSED YEAR") -> "السنة المالية مقفلة"
            m.contains("UNIQUE") -> "الاسم موجود مسبقاً"
            m.contains("Invalid Delete") -> "لا يمكن الحذف: السجل مستخدم"
            m.contains(" (code") -> m.substringBefore(" (code")
            else -> m
        }
    }

    fun isoToday(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    fun dmyToday(): String = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
    fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    fun dmy(iso: String): String =
        if (iso.length >= 10 && iso[4] == '-') iso.substring(8, 10) + "-" + iso.substring(5, 7) + "-" + iso.substring(0, 4) else iso

    // ---------- قوائم مرجعية ----------
    fun named(sql: String, vararg a: Any?): List<Named> = rows(sql, *a).map { Named(lng(it[0]), it[1].trim()) }
    fun branches(): List<Named> = named("select id,name from branches where IS_ACTIVE=1 or IS_ACTIVE is null order by id")
    fun currencies(): List<Named> = named("select id,name from currency order by id")
    fun boxes(): List<Named> = named("select id,name from customers where cus_type_id=4 order by id")
    fun units(): List<Named> = named("select id,name from units order by id")
    fun accountNames(): List<Named> = named("select id,name from customers order by name")
    fun branchName(): String = one("select name from branches order by id limit 1").ifBlank { "المخزن الرئيسي" }

    fun currencyRate(id: Long): Double =
        if (id == 0L) 1.0 else d(one("select price from currency_price where curr_id=? and t_date is null", id))

    fun findAccounts(q: String, types: List<Int>?, limit: Int = 6): List<Named> {
        val t = if (types == null) "" else " and cus_type_id in (" + types.joinToString(",") + ")"
        return named("select id,name from customers where name like ?$t order by name limit $limit", "%$q%")
    }

    fun findItems(q: String, limit: Int = 6): List<String> =
        rows("select name from items where name like ? order by name limit $limit", "%$q%").map { it[0] }

    fun ensureAccount(name: String, type: Int, tree: Int): Long {
        val n = name.trim()
        val id = one("select id from customers where name=?", n)
        if (id.isNotEmpty()) return lng(id)
        return sdb.insertOrThrow("customers", null,
            cv("name" to n, "cus_type_id" to type, "acc_p_id" to tree, "g_id" to 0, "gsm" to ""))
    }

    fun ensureItem(name: String): Long {
        val n = name.trim()
        val id = one("select id from items where name=?", n)
        if (id.isNotEmpty()) return lng(id)
        return sdb.insertOrThrow("items", null,
            cv("name" to n, "item_type_id" to 0, "item_type_id_" to 0, "unit_id" to 0, "u_val" to 1, "curr_id" to 0))
    }

    // ---------- الأصناف والمخزون ----------
    fun itemCosts(): Map<Long, Double> {
        val m = HashMap<Long, Double>()
        rows("""
            select i.id, case when (i.o_qty+ifnull(p.q,0))>0 then (i.o_qty*i.o_cost+ifnull(p.v,0))/(i.o_qty+ifnull(p.q,0)) else i.o_cost end
            from items i left join (
              select bt.item_id item_id, sum(bt.qty_pr*bt.u_val) q, sum(bt.qty_pr*bt.u_val*bt.cost_price) v
              from bill_transactions bt join bills b on b.id=bt.bill_id
              where b.tr_type in (2,21) and b.is_back=0 group by bt.item_id) p on p.item_id=i.id
        """).forEach { m[lng(it[0])] = d(it[1]) }
        return m
    }

    fun inventory(): List<InvRow> {
        val costs = itemCosts()
        return rows("""
            select i.id, i.name, i.price, ifnull(u.name,''),
              i.o_qty
              + ifnull((select sum(bt.qty_pr*bt.u_val) from bill_transactions bt join bills b on b.id=bt.bill_id
                  where bt.item_id=i.id and ((b.tr_type in (2,21) and b.is_back=0) or (b.tr_type=1 and b.is_back=1) or (b.tr_type=4 and b.adj_id in (2,4)))),0)
              - ifnull((select sum(bt.qty_pr*bt.u_val) from bill_transactions bt join bills b on b.id=bt.bill_id
                  where bt.item_id=i.id and ((b.tr_type in (1,11) and b.is_back=0) or (b.tr_type=2 and b.is_back=1) or (b.tr_type=4 and b.adj_id not in (2,4)))),0)
            from items i left join units u on u.id=i.unit_id
            where i.IS_ACTIVE=1 or i.IS_ACTIVE is null order by i.name
        """).map { InvRow(lng(it[0]), it[1], d(it[4]), costs[lng(it[0])] ?: 0.0, d(it[2]), it[3]) }
    }

    fun inventoryValue(): Double = inventory().sumOf { if (it.stock > 0) it.stock * it.cost else 0.0 }

    fun itemPrice(name: String, kind: String, currId: Long): Double {
        val id = one("select id from items where name=?", name.trim())
        if (id.isEmpty()) return 0.0
        return when (kind) {
            "sale", "quote" -> {
                val p = one("select sls_u_price from item_price where item_id=? and curr_id=? order by rowid desc limit 1", id, currId)
                if (p.isNotEmpty()) d(p) else d(one("select price from items where id=?", id))
            }
            "purchase", "po" -> {
                val c = one("select bt.cost_price from bill_transactions bt join bills b on b.id=bt.bill_id where bt.item_id=? and b.tr_type=2 order by bt.rowid desc limit 1", id)
                if (c.isNotEmpty()) d(c) else d(one("select o_cost from items where id=?", id))
            }
            else -> itemCosts()[lng(id)] ?: 0.0
        }
    }

    fun addItem(name: String, price: Double, qty: Double, cost: Double, unitId: Long, barcode: String): String? = attempt {
        sdb.insertOrThrow("items", null, cv(
            "name" to name.trim(), "item_type_id" to 0, "item_type_id_" to 0, "price" to price,
            "o_qty" to qty, "o_cost" to cost, "o_br_id" to 0, "o_date" to isoToday(),
            "unit_id" to unitId, "u_val" to 1, "barcode" to barcode, "curr_id" to 0))
    }

    fun setPrice(itemId: Long, currId: Long, price: Double): String? = attempt {
        exec("delete from item_price where item_id=? and curr_id=?", itemId, currId)
        exec("insert into item_price(item_id,curr_id,unit_id,SLS_U_PRICE,date_) values(?,?,0,?,?)", itemId, currId, price, isoToday())
        if (currId == 0L) exec("update items set price=? where id=?", price, itemId)
    }

    fun deleteItem(itemId: Long): String? {
        if (lng(one("select count(*) from bill_transactions where item_id=?", itemId)) > 0) return "الصنف مستخدم في فواتير ولا يمكن حذفه"
        return attempt { exec("delete from items where id=?", itemId) }
    }

    // ---------- الفواتير والمستندات المخزنية ----------
    fun nextBillNo(trType: Int): Int = lng(one("select ifnull(max(bill_no2),0)+1 from bills where tr_type=?", trType)).toInt()

    fun saveBill(b: BillIn): String? = attempt {
        val total = b.lines.sumOf { it.qty * it.price }
        val base = total - b.discount
        val taxAmt = base * b.taxPer / 100.0
        val amount = base + taxAmt + b.fees
        val no = nextBillNo(b.trType)
        val paid = if (b.billType == 1) amount else minOf(b.paid, amount)
        val cusId: Long? = if (b.party.isNotBlank()) ensureAccount(b.party, b.partyType, b.partyTree) else b.cusDefault
        val id = sdb.insertOrThrow("bills", null, cv(
            "date_" to b.date, "remarks" to b.notes, "bill_no" to no.toString(), "curr_id" to b.currId,
            "bill_type" to b.billType, "tr_type" to b.trType, "is_back" to b.isBack, "br_id" to b.brId,
            "cus_id" to cusId, "to_br_id" to b.toBrId, "amount" to amount, "d_amount" to b.discount,
            "tax_amount" to taxAmt, "bill_no2" to no, "cash_id" to b.cashId, "adj_id" to b.adjId,
            "discount_id" to 0, "d_val" to 0.0, "paid_amount" to paid, "tax_id" to b.taxId, "t_val" to b.taxPer,
            "time_" to nowTime(), "cost2" to b.fees, "cost_id" to -24))
        val sellSide = b.trType == 1 || b.trType == 11 || b.trType == 8
        for (l in b.lines) {
            val itemId = ensureItem(l.name)
            val unit = lng(one("select unit_id from items where id=?", itemId))
            sdb.insertOrThrow("bill_transactions", null, cv(
                "bill_id" to id, "item_id" to itemId, "item_type_id" to 0, "curr_id" to b.currId, "qty" to l.qty,
                "cost_price" to (if (sellSide) 0.0 else l.price), "sls_u_price" to (if (sellSide) l.price else 0.0),
                "d_amount" to 0.0, "cash_id" to b.cashId, "unit_id" to unit, "u_val" to 1.0, "base_unit" to unit,
                "qty_pr" to l.qty, "qty_t" to l.qty.toString(), "u_cost2" to 0.0, "date_" to b.date))
        }
        // تحديث الفاتورة ليوزّع المحفز الخصم والرسوم على بنود الفاتورة
        if (b.discount != 0.0 || b.fees != 0.0) exec("update bills set d_amount=d_amount where id=?", id)
    }

    fun bills(trTypes: List<Int>): List<BillRow> = rows("""
        select b.id, b.bill_no2, b.date_, ifnull(c.name,''), b.amount, b.bill_type, b.is_back, b.paid_amount,
               ifnull(b.remarks,''), b.tr_type, ifnull(f.name,''), ifnull(t.name,'')
        from bills b left join customers c on c.id=b.cus_id
        left join branches f on f.id=b.br_id left join branches t on t.id=b.to_br_id
        where b.tr_type in (${trTypes.joinToString(",")}) order by b.id desc
    """).map {
        BillRow(lng(it[0]), lng(it[1]).toInt(), it[2], it[3].trim(), d(it[4]), lng(it[5]).toInt(), lng(it[6]) == 1L,
            d(it[7]), it[8], lng(it[9]).toInt(), it[10].trim(), it[11].trim())
    }

    /** بنود الفاتورة: الاسم، الكمية، السعر */
    fun billLines(id: Long): List<Line> = rows("""
        select i.name, bt.qty_pr, case when bt.sls_u_price>0 then bt.sls_u_price else bt.cost_price end
        from bill_transactions bt join items i on i.id=bt.item_id where bt.bill_id=? order by bt.rowid
    """, id).map { Line(it[0], d(it[1]), d(it[2])) }

    fun deleteBill(id: Long): String? = attempt {
        exec("delete from bill_transactions where bill_id=?", id)
        exec("delete from bills where id=?", id)
    }

    // ---------- سندات القبض والصرف ----------
    fun nextVoucherNo(): Int = lng(one("select ifnull(max(cast(p_id as integer)),0)+1 from transactions where bill_id in (0,-2)")).toInt()

    fun saveVoucher(isPay: Boolean, dmyDate: String, currId: Long, boxId: Long, desc: String, entries: List<EntryRow>): String? = attempt {
        val no = nextVoucherNo()
        for (e in entries) {
            exec("INSERT INTO transactions(cus_id,[in],[out],date_,remarks,param2,curr_id,bill_id,p_id,p_remarks,cash_id,tr_type) VALUES(?,?,?,?,?,?,?,0,?,?,?,?)",
                e.id, if (isPay) 1 else -1, e.amount, dmyDate, e.notes.ifBlank { desc }, nowTime(), currId, no, desc, boxId, if (isPay) 6 else 5)
        }
    }

    fun vouchers(): List<VoucherRow> = rows("""
        select t.p_id, min(t.tr_type), min(t.date_), ifnull(max(t.p_remarks),''), sum(cast(t.[out] as real)), ifnull(max(c.name),'')
        from transactions t left join currency c on c.id=t.curr_id
        where t.tr_type in (5,6) and t.bill_id in (0,-2) group by t.p_id order by cast(t.p_id as integer) desc
    """).map { VoucherRow(lng(it[0]).toInt(), lng(it[1]) == 6L, it[2], it[3], d(it[4]), it[5]) }

    /** سطور السند: الحساب، المبلغ، الملاحظات */
    fun voucherLines(no: Int): List<List<String>> = rows("""
        select ifnull(c.name,''), cast(t.[out] as real), ifnull(t.remarks,'')
        from transactions t left join customers c on c.id=(case when t.tr_type=5 then t.t_cus_id else t.cus_id end)
        where t.p_id=? and t.tr_type in (5,6) and t.bill_id in (0,-2)
    """, no)

    fun deleteVoucher(no: Int): String? = attempt {
        exec("delete from transactions where p_id=? and bill_id in (0,-2) and tr_type in (5,6,0)", no)
    }

    // ---------- القيد الافتتاحي والقيد اليومي ----------
    fun saveOpening(accId: Long, amount: Double, debtor: Boolean, notes: String, dmyDate: String, currId: Long): String? = attempt {
        exec("INSERT INTO transactions(cus_id,[in],[out],date_,remarks,param2,curr_id,bill_id,fund_id,tr_type) VALUES(?,?,?,?,?,?,?,-1,-13,-1)",
            accId, if (debtor) 1 else -1, amount, dmyDate, notes.ifBlank { "قيد افتتاحي" }, nowTime(), currId)
    }

    /** id، الحساب، المبلغ، مدين/دائن، التاريخ، العملة */
    fun openings(): List<List<String>> = rows("""
        select t.id, ifnull(c.name,''), cast(t.[out] as real), case when t.cus_id=-13 then 'دائن' else 'مدين' end, t.date_, ifnull(cu.name,'')
        from transactions t left join customers c on c.id=(case when t.cus_id=-13 then t.t_cus_id else t.cus_id end)
        left join currency cu on cu.id=t.curr_id
        where t.bill_id=-1 and t.tr_type=-1 order by t.id desc
    """)

    fun deleteTransaction(id: Long): String? = attempt { exec("delete from transactions where id=?", id) }

    fun saveDaily(debitId: Long, creditId: Long, amount: Double, notes: String, dmyDate: String, currId: Long): String? = attempt {
        val no = nextVoucherNo()
        exec("INSERT INTO transactions(cus_id,t_cus_id,[in],[out],date_,remarks,param2,curr_id,bill_id,p_id,p_remarks,tr_type) VALUES(?,?,1,?,?,?,?,?,-2,?,?,-2)",
            debitId, creditId, amount, dmyDate, notes, nowTime(), currId, no, notes)
    }

    /** id، مدين، دائن، المبلغ، التاريخ، البيان */
    fun dailies(): List<List<String>> = rows("""
        select t.id, ifnull(a.name,''), ifnull(b.name,''), cast(t.[out] as real), t.date_, ifnull(t.remarks,'')
        from transactions t left join customers a on a.id=t.cus_id left join customers b on b.id=t.t_cus_id
        where t.tr_type=-2 and t.bill_id=-2 order by t.id desc
    """)

    // ---------- الحسابات ----------
    fun accountBalances(): List<AccRow> = rows("""
        select v.id, v.name, ct.name, v.balance, v.curr
        from cus_curr v join customers c on c.id=v.id join cus_type ct on ct.id=c.cus_type_id order by v.name
    """).map { AccRow(lng(it[0]), it[1].trim(), it[2], d(it[3]), it[4]) }

    fun addAccount(name: String, gsm: String, typeName: String, treeName: String): String? = attempt {
        val type = lng(one("select id from cus_type where name=?", typeName))
        var tree = lng(one("select id from account_tree where trim(name)=?", treeName.trim()))
        if (tree == 0L) tree = when (type.toInt()) { 0 -> 123L; 1 -> 221L; 2 -> 311L; 4 -> 121L; 5 -> 321L; 6 -> 42L; else -> 124L }
        sdb.insertOrThrow("customers", null, cv("name" to name.trim(), "gsm" to gsm, "cus_type_id" to type, "acc_p_id" to tree, "g_id" to 0))
    }

    fun deleteAccount(id: Long): String? {
        if (id < 0) return "لا يمكن حذف حساب النظام"
        return attempt { exec("delete from customers where id=?", id) }
    }

    /** التاريخ، البيان، مدين، دائن، رقم المستند */
    fun statement(id: Long, currId: Long): List<List<String>> = rows("""
        select t.date_, ifnull(t.remarks,''),
          case when t.t_cus_id=? then 0.0 when cast(t.[in] as real)>0 then cast(t.[out] as real) else 0.0 end,
          case when t.t_cus_id=? then cast(t.[out] as real) when cast(t.[in] as real)<0 then cast(t.[out] as real) else 0.0 end,
          ifnull(t.p_ref_no,'')
        from transactions t where (t.cus_id=? or t.t_cus_id=?) and t.curr_id=?
        order by substr(t.date_,7,4)||substr(t.date_,4,2)||substr(t.date_,1,2), t.id
    """, id, id, id, id, currId)

    // ---------- التقارير ----------
    private const val TX = """
        select cus_id acc, cast([in] as real)*cast([out] as real)*(case when curr_id=0 then 1.0 else curr_price end) v from transactions
        union all
        select t_cus_id, -cast([out] as real)*(case when curr_id=0 then 1.0 else curr_price end) from transactions where t_cus_id is not null
    """

    fun balances(): List<AccBal> = rows("""
        select c.id, c.acc_p_id, sum(x.v) from ($TX) x join customers c on c.id=x.acc group by c.id
    """).map { AccBal(lng(it[0]), lng(it[1]), d(it[2])) }

    /** id، الاسم، مدين، دائن */
    fun trial(): List<List<String>> = rows("""
        select c.id, c.name, sum(case when x.v>0 then x.v else 0 end), sum(case when x.v<0 then -x.v else 0 end)
        from ($TX) x join customers c on c.id=x.acc group by c.id order by c.id
    """)

    /** سجل حركة الصنف: التاريخ، البيان، الرقم، وارد، صادر */
    fun itemLedger(itemId: Long): List<List<String>> = rows("""
        select b.date_, tt.name || case when b.is_back=1 then ' (مرتجع)' else '' end, b.bill_no2,
          case when (b.tr_type in (2,21) and b.is_back=0) or (b.tr_type=1 and b.is_back=1) or (b.tr_type=4 and b.adj_id in (2,4)) then bt.qty_pr*bt.u_val else 0 end,
          case when (b.tr_type in (1,11) and b.is_back=0) or (b.tr_type=2 and b.is_back=1) or (b.tr_type=4 and b.adj_id not in (2,4)) then bt.qty_pr*bt.u_val else 0 end
        from bill_transactions bt join bills b on b.id=bt.bill_id join tran_type tt on tt.id=b.tr_type
        where bt.item_id=? and b.tr_type in (1,2,4,11,21) order by b.date_, b.id
    """, itemId)

    private val SALES = setOf(-1L, -5L)
    private val SALES_RET = setOf(-9L, -11L)
    private val DISC_ALLOWED = setOf(-8L)
    private val PURCH = setOf(-2L, -6L, -30L, -15L, -16L)
    private val PURCH_RET = setOf(-10L, -12L)
    private val DISC_EARNED = setOf(-7L)

    /** قائمة الدخل. القيمة التي تبدأ بـ * تعرض بخط عريض. */
    fun incomeStatement(): List<Pair<String, String>> {
        val bl = balances()
        fun g(ids: Set<Long>) = bl.filter { it.id in ids }.sumOf { it.bal }
        val sales = -g(SALES)
        val sret = g(SALES_RET)
        val disc = g(DISC_ALLOWED)
        val netSales = sales - sret - disc
        val opening = g(setOf(-14L))
        val purch = g(PURCH)
        val pret = -g(PURCH_RET)
        val earned = -g(DISC_EARNED)
        val netPurch = purch - pret - earned
        val closing = inventoryValue()
        val cogs = opening + netPurch - closing
        val gross = netSales - cogs
        val sys = SALES + SALES_RET + DISC_ALLOWED + PURCH + PURCH_RET + DISC_EARNED
        val other = bl.filter { it.accP.toString().startsWith("4") && it.id !in sys }.sumOf { -it.bal }
        val expenses = bl.filter { it.accP.toString().startsWith("3") && it.id !in sys }.sumOf { it.bal }
        val net = gross + other - expenses
        return listOf(
            "المبيعات" to fmtN(sales), "مردودات المبيعات" to fmtN(sret), "الخصم المسموح به" to fmtN(disc),
            "*صافي المبيعات" to fmtN(netSales),
            "بضاعة أول المدة" to fmtN(opening), "صافي المشتريات" to fmtN(netPurch), "بضاعة آخر المدة" to fmtN(closing),
            "*تكلفة المبيعات" to fmtN(cogs),
            "*مجمل الربح" to fmtN(gross),
            "إيرادات أخرى" to fmtN(other), "المصروفات" to fmtN(expenses),
            "*صافي الربح / الخسارة" to fmtN(net)
        )
    }

    fun netProfit(): Double {
        val v = incomeStatement().last().second
        return v.replace(",", "").toDoubleOrNull() ?: 0.0
    }

    /** المركز المالي: (الأصول، الخصوم وحقوق الملكية) */
    fun financialPosition(): Pair<List<Pair<String, String>>, List<Pair<String, String>>> {
        val bl = balances()
        val names = HashMap<Long, String>()
        rows("select id,name from account_tree").forEach { names[lng(it[0])] = it[1].trim() }
        val assets = ArrayList<Pair<String, String>>()
        var ta = 0.0
        bl.filter { it.accP.toString().startsWith("1") && it.id != -14L }.groupBy { it.accP }.forEach { (k, v) ->
            val s = v.sumOf { it.bal }
            ta += s
            assets.add((names[k] ?: "أصول") to fmtN(s))
        }
        val inv = inventoryValue()
        ta += inv
        assets.add("المخزون (بسعر التكلفة المتوسط)" to fmtN(inv))
        assets.add("*إجمالي الأصول" to fmtN(ta))
        val le = ArrayList<Pair<String, String>>()
        var tl = 0.0
        bl.filter { it.accP.toString().startsWith("2") }.groupBy { it.accP }.forEach { (k, v) ->
            val s = -v.sumOf { it.bal }
            tl += s
            le.add((names[k] ?: "خصوم") to fmtN(s))
        }
        val np = netProfit()
        tl += np
        le.add("صافي الربح / الخسارة" to fmtN(np))
        le.add("*إجمالي الخصوم وحقوق الملكية" to fmtN(tl))
        return assets to le
    }

    fun fmtN(v: Double): String {
        val r = Math.round(v * 100.0) / 100.0
        return if (r == Math.floor(r)) String.format(Locale.US, "%.0f", r) else String.format(Locale.US, "%.2f", r)
    }
}
