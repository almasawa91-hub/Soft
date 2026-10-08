package com.mohaseb.soft.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.*
import java.io.File

data class Section(val title: String, val items: List<String>)

@Composable
fun HomeScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                File(ctx.getDatabasePath(Repo.DB_NAME).path).inputStream().use { it.copyTo(out) }
            }
        }
    }

    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            Repo.close()
            ctx.contentResolver.openInputStream(uri)?.use { inp ->
                File(ctx.getDatabasePath(Repo.DB_NAME).path).outputStream().use { inp.copyTo(it) }
            }
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                ctx.startActivity(intent)
                (ctx as? android.app.Activity)?.finish()
            }
        }
    }

    fun routeFor(item: String): String = when (item) {
        "صرف مخزني" -> "docs/stockout"
        "توريد مخزني" -> "docs/stockin"
        "تحويل مخزني" -> "docs/transfer"
        "تسوية مخزنية" -> "docs/adjust"
        "إضافة مخزن" -> "warehouses"
        "جرد مخزني" -> "stocktake"
        "قيد يومي" -> "daily"
        "قيد إفتتاحي" -> "openings"
        "إضافة حساب" -> "accounts_new"
        "حركة الصندوق" -> "report/boxes"
        "دليل الحسابات" -> "accounts"
        "إقفال سنوي" -> "closing"
        "الاصناف" -> "items"
        "أسعار البيع" -> "prices"
        "وحدات الصنف" -> "units"
        "فاتورة عرض سعر" -> "docs/quote"
        "طلب شراء" -> "docs/po"
        "إضافة عملة" -> "currencies"
        "سعر العملات" -> "rates"
        "سقف الحساب" -> "ceilings"
        "حركة الأصناف" -> "report/move"
        "ميزان المراجعة" -> "report/trial"
        "قائمة الدخل" -> "report/income"
        "المركز المالي" -> "report/financial"
        "تقارير أخرى" -> "report/other"
        else -> "home"
    }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopBar(
                    title = "المحاسب الذهبي",
                    showMenu = true,
                    onMenu = { drawerOpen = true }
                )
            },
            bottomBar = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(BarBrush)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    YoutubeIcon {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
                    }
                    Text(
                        Repo.branchName(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    LogoIcon()
                }
            }
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .background(PageBg)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        HandMoneyIcon()
                        Spacer(Modifier.height(4.dp))
                        Pill("قبض/صرف") { nav.navigate("vouchers") }
                        Spacer(Modifier.height(14.dp))
                        DocsIcon()
                        Spacer(Modifier.height(4.dp))
                        Pill("الحسابات") { nav.navigate("accounts") }
                    }

                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        CartIcon()
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("المبيعات") { nav.navigate("docs/sale") }
                            Spacer(Modifier.width(8.dp))
                            PlusCircle(onClick = { nav.navigate("new/sale") })
                        }
                        Spacer(Modifier.height(14.dp))
                        CartIcon()
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("المشتريات") { nav.navigate("docs/purchase") }
                            Spacer(Modifier.width(8.dp))
                            PlusCircle(onClick = { nav.navigate("new/purchase") })
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                val sections = listOf(
                    Section(
                        "عمليات مخزنية",
                        listOf("صرف مخزني", "توريد مخزني", "تحويل مخزني", "تسوية مخزنية", "إضافة مخزن", "جرد مخزني")
                    ),
                    Section(
                        "قيود وحسابات",
                        listOf("قيد يومي", "قيد إفتتاحي", "إضافة حساب", "حركة الصندوق", "دليل الحسابات", "إقفال سنوي")
                    ),
                    Section(
                        "اصناف",
                        listOf("الاصناف", "أسعار البيع", "وحدات الصنف", "فاتورة عرض سعر", "طلب شراء")
                    ),
                    Section(
                        "العملات",
                        listOf("إضافة عملة", "سعر العملات", "سقف الحساب")
                    ),
                    Section(
                        "التقارير",
                        listOf("حركة الأصناف", "ميزان المراجعة", "قائمة الدخل", "المركز المالي", "تقارير أخرى")
                    )
                )

                sections.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(HeaderBlue)
                            .clickable { expanded[s.title] = !(expanded[s.title] ?: false) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (expanded[s.title] == true) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null,
                            tint = Color.White,
                            modifier = Modifier.padding(start = 12.dp).size(20.dp)
                        )
                        Text(
                            s.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                            textAlign = TextAlign.Right
                        )
                    }

                    if (expanded[s.title] == true) {
                        s.items.forEach { item ->
                            Column {
                                Text(
                                    item,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .clickable { nav.navigate(routeFor(item)) }
                                        .padding(vertical = 13.dp, horizontal = 16.dp),
                                    textAlign = TextAlign.Right
                                )
                                Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { drawerOpen = false }
                )

                Column(
                    Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(BarBrush)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LogoIcon(Modifier.size(34.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "المحاسب الذهبي",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    val drawerItems = listOf(
                        "حفظ نسخة إحتياطية",
                        "إسترجاع قاعدة البيانات",
                        "جوجل درايف",
                        "دليل الحسابات",
                        "إعدادات",
                        "للتواصل والدعم",
                        "حول البرنامج",
                        "خروج"
                    )

                    val drawerIcons = listOf(
                        Icons.Filled.Save,
                        Icons.Filled.SettingsBackupRestore,
                        Icons.Filled.CloudUpload,
                        Icons.Filled.MenuBook,
                        Icons.Filled.Settings,
                        Icons.Filled.Phone,
                        Icons.Filled.Info,
                        Icons.Filled.ExitToApp
                    )

                    drawerItems.forEachIndexed { i, t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    drawerOpen = false
                                    when (t) {
                                        "حفظ نسخة إحتياطية" -> createBackup.launch("golden_accountant_backup.db")
                                        "إسترجاع قاعدة البيانات" -> restoreBackup.launch(arrayOf("*/*"))
                                        "جوجل درايف" -> {
                                            val intent = ctx.packageManager.getLaunchIntentForPackage("com.google.android.apps.docs")
                                            if (intent != null) ctx.startActivity(intent)
                                        }
                                        "دليل الحسابات" -> nav.navigate("accounts")
                                        "إعدادات" -> nav.navigate("settings")
                                        "للتواصل والدعم" -> {
                                            val em = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@mohaseb.soft"))
                                                putExtra(Intent.EXTRA_SUBJECT, "دعم المحاسب الذهبي")
                                            }
                                            ctx.startActivity(Intent.createChooser(em, "للتواصل والدعم"))
                                        }
                                        "حول البرنامج" -> {
                                            val v = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                                            android.widget.Toast.makeText(ctx, "المحاسب الذهبي - الإصدار $v", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                        "خروج" -> (ctx as? android.app.Activity)?.finishAffinity()
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                drawerIcons[i],
                                null,
                                tint = Blue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                t,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                    }
                }
            }
        }
    }
}
