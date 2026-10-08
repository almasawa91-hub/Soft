package com.mohaseb.soft.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

val BlueDark = Color(0xFF0A4F8C)
val Blue = Color(0xFF1273B5)
val HeaderBlue = Color(0xFF1565A0)
val Pink = Color(0xFFE73A7E)
val PageBg = Color(0xFFEFEFEF)
val PlusDark = Color(0xFF3F6173)
val EmptyBlue = Color(0xFF3E68D8)
val BarBrush = Brush.horizontalGradient(listOf(BlueDark, Blue))

fun fmt(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else String.format(java.util.Locale.US, "%.2f", d)

@Composable
fun TopBar(title: String, showMenu: Boolean = false, onMenu: () -> Unit = {},
           onBack: (() -> Unit)? = null, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(56.dp).background(BarBrush).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (showMenu) IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, null, tint = Color.White) }
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            modifier = Modifier.weight(1f).padding(start = 8.dp))
        right()
    }
}

@Composable
fun HeaderRow(cols: List<String>, weights: List<Float>, arrowAt: Int = -1) {
    Row(Modifier.fillMaxWidth().background(HeaderBlue).padding(vertical = 10.dp)) {
        cols.forEachIndexed { i, c ->
            Row(Modifier.weight(weights[i]), verticalAlignment = Alignment.CenterVertically) {
                Text(c, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                if (i == arrowAt) Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("لا توجد نتائج...", color = EmptyBlue, fontSize = 16.sp)
    }
}

@Composable
fun ListBottomBar(center: String = ".........", onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).background(BarBrush).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(center, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center)
        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(38.dp).clickable { onAdd() }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, null, tint = Blue, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
fun Pill(text: String, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick)
        .background(Blue, RoundedCornerShape(6.dp))
        .border(1.5.dp, Color.White, RoundedCornerShape(6.dp))
        .padding(horizontal = 16.dp, vertical = 5.dp)) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PlusCircle(onClick: () -> Unit, dark: Boolean = true, size: Int = 34) {
    Surface(shape = CircleShape, color = if (dark) PlusDark else Blue, modifier = Modifier.size(size.dp).clickable { onClick() }) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size((size - 12).dp))
        }
    }
}

@Composable
fun LineField(value: String, onChange: (String) -> Unit, hint: String = "",
              lineColor: Color = Pink, modifier: Modifier = Modifier,
              hintCenter: Boolean = false, onDone: (() -> Unit)? = null,
              keyboard: KeyboardType = KeyboardType.Text) {
    BasicTextField(value = value, onValueChange = onChange, singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier.drawBehind {
            drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                androidx.compose.ui.geometry.Offset(size.width, size.height - 1f), 2f)
        }.padding(vertical = 6.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                if (value.isEmpty() && hint.isNotEmpty())
                    Text(hint, color = Color(0xFF9AA0A6), fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (hintCenter) TextAlign.Center else TextAlign.Right)
                inner()
            }
        })
}

@Composable
fun NumField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(value = value, onValueChange = onChange, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
        modifier = modifier.drawBehind {
            drawLine(Pink, androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                androidx.compose.ui.geometry.Offset(size.width, size.height - 1f), 2f)
        }.padding(vertical = 6.dp),
        decorationBox = { inner -> Box { inner() } })
}

@Composable
fun SaveDialog(onYes: () -> Unit, onNo: () -> Unit) {
    Dialog(onDismissRequest = onNo) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).padding(16.dp)) {
            Text("حفظ", color = Color.Black, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onYes) { Text("نعم", color = Color.Black, fontSize = 15.sp) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onNo) { Text("لا", color = Color.Black, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun BlueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(Modifier.then(modifier).clickable(onClick = onClick)
        .background(Color(0xFF2278B0), RoundedCornerShape(6.dp))
        .padding(horizontal = 22.dp, vertical = 8.dp)) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun CartIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(46.dp)) {
        val w = size.width; val h = size.height
        drawContext.canvas.nativeCanvas.apply {
            val gray = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true }
            drawRect(w * 0.16f, h * 0.28f, w * 0.78f, h * 0.62f, gray)
            drawLine(w * 0.16f, h * 0.62f, w * 0.10f, h * 0.80f, gray)
            drawLine(w * 0.78f, h * 0.62f, w * 0.86f, h * 0.80f, gray)
            gray.style = android.graphics.Paint.Style.FILL
            drawCircle(w * 0.32f, h * 0.76f, 5f, gray)
            drawCircle(w * 0.64f, h * 0.76f, 5f, gray)
            val tp = android.graphics.Paint().apply { color = android.graphics.Color.GREEN; textSize = h * 0.30f; isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
            drawText(Char(36).toString(), w * 0.47f, h * 0.55f, tp)
            val bl = android.graphics.Paint().apply { color = android.graphics.Color.rgb(0x21, 0x96, 0xF3); strokeWidth = 5f; style = android.graphics.Paint.Style.STROKE; isAntiAlias = true }
            drawLine(w * 0.55f, h * 0.14f, w * 0.92f, h * 0.14f, bl)
            drawLine(w * 0.80f, h * 0.05f, w * 0.92f, h * 0.14f, bl)
            drawLine(w * 0.80f, h * 0.23f, w * 0.92f, h * 0.14f, bl)
        }
    }
}

@Composable
fun HandMoneyIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(46.dp)) {
        val w = size.width; val h = size.height
        drawContext.canvas.nativeCanvas.apply {
            val bill = android.graphics.Paint().apply { color = android.graphics.Color.rgb(0x90, 0xA4, 0xAE); style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
            drawRect(w * 0.30f, h * 0.12f, w * 0.85f, h * 0.40f, bill)
            val inn = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f }
            drawRect(w * 0.36f, h * 0.18f, w * 0.79f, h * 0.34f, inn)
            val hand = android.graphics.Paint().apply { color = android.graphics.Color.rgb(0x60, 0x70, 0x78); style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true }
            drawArc(w * 0.10f, h * 0.40f, w * 0.75f, h * 0.95f, 0f, 180f, false, hand)
        }
    }
}

@Composable
fun DocsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(46.dp)) {
        val w = size.width; val h = size.height
        drawContext.canvas.nativeCanvas.apply {
            val border = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f }
            val fill = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.FILL }
            drawRect(w * 0.28f, h * 0.10f, w * 0.88f, h * 0.75f, fill); drawRect(w * 0.28f, h * 0.10f, w * 0.88f, h * 0.75f, border)
            drawRect(w * 0.12f, h * 0.25f, w * 0.72f, h * 0.90f, fill); drawRect(w * 0.12f, h * 0.25f, w * 0.72f, h * 0.90f, border)
            val bar = android.graphics.Paint().apply { style = android.graphics.Paint.Style.FILL }
            bar.color = android.graphics.Color.rgb(0x21, 0x96, 0xF3); drawRect(w * 0.20f, h * 0.55f, w * 0.30f, h * 0.80f, bar)
            bar.color = android.graphics.Color.GREEN; drawRect(w * 0.36f, h * 0.45f, w * 0.46f, h * 0.80f, bar)
            bar.color = android.graphics.Color.RED; drawRect(w * 0.52f, h * 0.60f, w * 0.62f, h * 0.80f, bar)
        }
    }
}

@Composable
fun PdfIcon(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Canvas(modifier.size(30.dp).clickable { onClick() }) {
        val w = size.width
        drawContext.canvas.nativeCanvas.apply {
            val red = android.graphics.Paint().apply { color = android.graphics.Color.rgb(0xE5, 0x39, 0x35); style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
            drawCircle(w / 2, w / 2, w / 2, red)
            val wh = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = w * 0.09f; isAntiAlias = true }
            drawLine(w * 0.5f, w * 0.22f, w * 0.26f, w * 0.78f, wh)
            drawLine(w * 0.5f, w * 0.22f, w * 0.74f, w * 0.78f, wh)
            drawLine(w * 0.36f, w * 0.55f, w * 0.64f, w * 0.55f, wh)
        }
    }
}

@Composable
fun YoutubeIcon(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Canvas(modifier.size(40.dp).clickable { onClick() }) {
        val w = size.width; val h = size.height
        drawContext.canvas.nativeCanvas.apply {
            val wh = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
            drawCircle(w / 2, h / 2, w / 2, wh)
            val rd = android.graphics.Paint().apply { color = android.graphics.Color.rgb(0xE5, 0x39, 0x35); style = android.graphics.Paint.Style.FILL }
            drawRoundRect(w * 0.22f, h * 0.32f, w * 0.78f, h * 0.68f, 6f, 6f, rd)
            val tr = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.FILL }
            val path = android.graphics.Path().apply {
                moveTo(w * 0.45f, h * 0.40f); lineTo(w * 0.62f, h * 0.50f); lineTo(w * 0.45f, h * 0.60f); close()
            }
            drawPath(path, tr)
        }
    }
}

@Composable
fun LogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(40.dp)) {
        val w = size.width; val h = size.height
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true }
            drawArc(w * 0.10f, h * 0.15f, w * 0.90f, h * 0.85f, 20f, 140f, false, p)
            drawArc(w * 0.25f, h * 0.35f, w * 1.05f, h * 1.05f, 200f, 120f, false, p)
        }
    }
}
