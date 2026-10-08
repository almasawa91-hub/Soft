package com.mohaseb.soft.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mohaseb.soft.ui.*

@Composable
fun SettingsScreen(nav: NavHostController) {
    val rows: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit>> = listOf(
        Triple(Icons.Filled.Person, "البيانات الشخصية") { nav.navigate("profile") },
        Triple(Icons.Filled.Print, "خيارات الطباعة") { nav.navigate("options/print") },
        Triple(Icons.Filled.Lock, "خيارات الأمان") { nav.navigate("options/security") },
        Triple(Icons.Filled.Group, "المستخدمين والصلاحيات") { nav.navigate("users") },
        Triple(Icons.Filled.Category, "التصنيفات") { nav.navigate("itemtypes") },
        Triple(Icons.Filled.Assignment, "مجموعة الصنف") { nav.navigate("groups") },
        Triple(Icons.Filled.Straighten, "وحدات القياس") { nav.navigate("units") },
        Triple(Icons.Filled.SaveAlt, "خيارات حفظ البيانات") { nav.navigate("options/save") },
        Triple(Icons.Filled.Print, "الطابعة الحرارية") { nav.navigate("options/thermal") },
        Triple(Icons.Filled.Percent, "الضريبة") { nav.navigate("taxes") },
        Triple(Icons.Filled.QrCode, "طباعة باركود الأصناف") { nav.navigate("options/barcode") },
        Triple(Icons.Filled.Notifications, "خيارات الإشعارات") { nav.navigate("options/notify") },
        Triple(Icons.Filled.Tune, "خيارات أخرى") { nav.navigate("options/other") },
        Triple(Icons.Filled.CardMembership, "تفعيل الإشتراك") { }
    )
    Scaffold(topBar = { TopBar("إعدادات", onBack = { nav.popBackStack() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(PageBg).verticalScroll(rememberScrollState())) {
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth().background(Color.White).clickable { r.third() }
                    .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(30.dp).background(Color(0xFF2278B0), CircleShape)) {
                        Icon(r.first, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(r.second, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Divider(color = Color(0xFFDDDDDD))
            }
        }
    }
}
