package com.example.shamsical
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        Notifier.showToday(this)
        setContent { App() }
    }
}

private val RED = Color(0xFFD32F2F)

@Composable fun App() {
    val ctx = LocalContext.current
    var themes by remember { mutableStateOf(Store.themes(ctx)) }
    var cur by remember { mutableStateOf(Store.current(ctx)) }
    var items by remember { mutableStateOf(Store.items(ctx)) }
    fun upd(l: List<Item>) {
        items.filter { o -> l.none { it.id == o.id } }.forEach { Notifier.cancel(ctx, it) }
        items = l; Store.saveItems(ctx, l); l.forEach { Notifier.schedule(ctx, it) }
    }
    val cs = lightColorScheme(primary = Color(cur.primary), onPrimary = Color.White, background = Color(cur.bg), surface = Color(cur.bg),
        onBackground = Color(cur.text), onSurface = Color(cur.text), surfaceVariant = Color(cur.primary).copy(alpha = 0.12f))
    MaterialTheme(cs) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var tab by remember { mutableStateOf(0) }
        Scaffold(bottomBar = { NavigationBar { listOf("ماه", "هفته", "تم‌ها").forEachIndexed { i, t ->
            NavigationBarItem(tab == i, { tab = i }, icon = { Text(listOf("📅", "🗓", "🎨")[i]) }, label = { Text(t) }) } } }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) { when (tab) {
                0 -> MonthScreen(items, ::upd)
                1 -> WeekScreen(items)
                else -> ThemeScreen(themes, cur, { t -> cur = t; Store.setCurrent(ctx, t.name) },
                    { l -> themes = l; Store.saveThemes(ctx, l) })
            } }
        }
    } }
}

fun itemsOn(l: List<Item>, d: LocalDate) = l.filter { it.date == d.toString() }.sortedBy { if (it.event) it.start else -1 }

@Composable fun ItemRow(i: Item, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text((if (i.event) "${fmt(i.start)}–${fmt(i.end)}".fa() else "📝"), Modifier.width(100.dp), fontWeight = FontWeight.Bold)
        Text(i.title, Modifier.weight(1f))
        Text(when (i.alarm) { 1 -> "🔔"; 2 -> "🔕"; else -> "" })
    }
}

@Composable fun MonthScreen(items: List<Item>, upd: (List<Item>) -> Unit) {
    val today = LocalDate.now(); val tj = Jal.toJ(today)
    var sel by remember { mutableStateOf(today) }
    var vy by remember { mutableStateOf(tj.y) }; var vm by remember { mutableStateOf(tj.m) }
    var dlg by remember { mutableStateOf<Pair<Boolean, Item?>>(false to null) }
    val first = Jal.fromJ(vy, vm, 1); val n = Jal.len(vy, vm); val off = Jal.wd(first); val last = first.plusDays(n - 1L)
    val g = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton({ if (vm == 1) { vm = 12; vy-- } else vm-- }) { Text("▶") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${Jal.months[vm - 1]} $vy".fa(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${first.format(g)} – ${last.format(g)}", fontSize = 12.sp)
            }
            TextButton({ if (vm == 12) { vm = 1; vy++ } else vm++ }) { Text("◀") }
            TextButton({ vy = tj.y; vm = tj.m; sel = today }) { Text("امروز") }
        }
        Row { Jal.short.forEachIndexed { i, s -> Text(s, Modifier.weight(1f), textAlign = TextAlign.Center, color = if (i == 6) RED else Color.Unspecified, fontWeight = FontWeight.Bold) } }
        for (r in 0 until (off + n + 6) / 7) Row {
            for (c in 0..6) {
                val d = r * 7 + c - off + 1
                if (d !in 1..n) { Spacer(Modifier.weight(1f)) ; continue }
                val date = Jal.fromJ(vy, vm, d); val hol = Jal.holiday(date) != null
                val isSel = date == sel
                Column(Modifier.weight(1f).padding(2.dp).clip(CircleShape).background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = .25f) else Color.Transparent)
                    .then(if (date == today) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                    .clickable { sel = date }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.toString().fa(), fontSize = 17.sp, color = if (hol || c == 6) RED else Color.Unspecified, fontWeight = FontWeight.Bold)
                    Text(date.dayOfMonth.toString(), fontSize = 10.sp, color = Color.Gray)
                    Text(if (itemsOn(items, date).isNotEmpty()) "●" else " ", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Divider(Modifier.padding(vertical = 8.dp))
        Text(fullJ(sel), fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(fullG(sel), fontSize = 12.sp)
        Jal.holiday(sel)?.let { Text("🎉 $it", color = RED) }
        itemsOn(items, sel).forEach { i -> ItemRow(i) { dlg = true to i } }
        Button({ dlg = true to null }, Modifier.padding(top = 8.dp)) { Text("+ یادداشت / رویداد") }
    }
    if (dlg.first) EditDialog(dlg.second, sel, { upd(items.filter { x -> x.id != it.id } + it); dlg = false to null },
        dlg.second?.let { o -> { upd(items.filter { it.id != o.id }); dlg = false to null } }, { dlg = false to null })
}

@Composable fun EditDialog(it0: Item?, date: LocalDate, onSave: (Item) -> Unit, onDel: (() -> Unit)?, onClose: () -> Unit) {
    var title by remember { mutableStateOf(it0?.title ?: "") }
    var ev by remember { mutableStateOf(it0?.event ?: false) }
    var st by remember { mutableStateOf(fmt(it0?.start ?: 540)) }
    var en by remember { mutableStateOf(fmt(it0?.end ?: 600)) }
    var al by remember { mutableStateOf(it0?.alarm ?: 0) }
    var lead by remember { mutableStateOf((it0?.lead ?: 0).toString()) }
    AlertDialog(onDismissRequest = onClose,
        title = { Text(fullJ(date), fontSize = 16.sp) },
        confirmButton = { TextButton({ if (title.isNotBlank()) onSave(Item(it0?.id ?: (System.nanoTime() and 0x7fffffff), date.toString(), title, ev,
            parse(st) ?: 540, parse(en) ?: 600, al, lead.toIntOrNull() ?: 0)) }) { Text("ذخیره") } },
        dismissButton = { Row { if (onDel != null) TextButton(onDel) { Text("حذف", color = RED) }; TextButton(onClose) { Text("انصراف") } } },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(title, { title = it }, label = { Text("عنوان (مثلاً تولد دوست)") })
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(ev, { ev = it }); Text("  رویداد زمان‌دار") }
            if (ev) Row { OutlinedTextField(st, { st = it }, Modifier.weight(1f), label = { Text("شروع 09:30") })
                Spacer(Modifier.width(8.dp)); OutlinedTextField(en, { en = it }, Modifier.weight(1f), label = { Text("پایان 11:00") }) }
            listOf("بدون هشدار", "هشدار با صدا", "فقط اعلان بی‌صدا").forEachIndexed { i, t ->
                Row(Modifier.clickable { al = i }, verticalAlignment = Alignment.CenterVertically) { RadioButton(al == i, { al = i }); Text(t) } }
            if (al > 0) OutlinedTextField(lead, { lead = it }, label = { Text("چند دقیقه زودتر") })
            if (al > 0 && !ev) Text("برای یادداشت، هشدار ساعت ۰۸:۰۰ است", fontSize = 11.sp)
        } })
}

@Composable fun WeekScreen(items: List<Item>) {
    var off by remember { mutableStateOf(0) }
    val today = LocalDate.now()
    val start = today.minusDays(Jal.wd(today).toLong()).plusWeeks(off.toLong())
    val all = (0..6).flatMap { itemsOn(items, start.plusDays(it.toLong())) }
    Column(Modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton({ off-- }) { Text("▶") }
            Text("${fullJ(start).substringAfter(" ")} تا ${fullJ(start.plusDays(6)).substringAfter(" ")}", Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp)
            TextButton({ off++ }) { Text("◀") }
            TextButton({ off = 0 }) { Text("این هفته") }
        }
        Text("${all.count { it.event }} رویداد · ${all.count { !it.event }} یادداشت".fa(), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        androidx.compose.foundation.lazy.LazyColumn {
            items((0..6).toList()) { k ->
                val d = start.plusDays(k.toLong()); val h = Jal.holiday(d)
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(fullJ(d) + if (d == today) "  ← امروز" else "", fontWeight = FontWeight.Bold, color = if (h != null || k == 6) RED else Color.Unspecified)
                        h?.let { Text("🎉 $it", color = RED, fontSize = 12.sp) }
                        val l = itemsOn(items, d)
                        if (l.isEmpty()) Text("—", color = Color.Gray) else l.forEach { ItemRow(it) }
                    }
                }
            }
        }
    }
}

private fun hex(c: Int) = "%06X".format(c and 0xFFFFFF)
private fun unhex(s: String): Int? = s.trim().removePrefix("#").toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }

@Composable fun ThemeScreen(themes: List<Th>, cur: Th, apply: (Th) -> Unit, save: (List<Th>) -> Unit) {
    var name by remember { mutableStateOf("تم من") }
    var p by remember { mutableStateOf(hex(cur.primary)) }; var b by remember { mutableStateOf(hex(cur.bg)) }; var t by remember { mutableStateOf(hex(cur.text)) }
    fun edit(x: Th) { name = x.name; p = hex(x.primary); b = hex(x.bg); t = hex(x.text) }
    Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("تم‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        themes.forEach { x -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(x.primary)).border(1.dp, Color.Gray, CircleShape))
            Spacer(Modifier.width(8.dp)); Text(x.name + if (x.name == cur.name) " ✓" else "", Modifier.weight(1f))
            TextButton({ apply(x) }) { Text("اعمال") }; TextButton({ edit(x) }) { Text("ویرایش") }
            if (Store.defaults.none { it.name == x.name }) TextButton({ save(themes - x) }) { Text("حذف", color = RED) }
        } }
        Divider(Modifier.padding(vertical = 8.dp))
        OutlinedTextField(name, { name = it }, label = { Text("نام تم") })
        OutlinedTextField(p, { p = it }, label = { Text("رنگ اصلی (HEX)") })
        OutlinedTextField(b, { b = it }, label = { Text("رنگ پس‌زمینه (HEX)") })
        OutlinedTextField(t, { t = it }, label = { Text("رنگ متن (HEX)") })
        Button({ val x = Th(name, unhex(p) ?: return@Button, unhex(b) ?: return@Button, unhex(t) ?: return@Button)
            save(themes.filter { it.name != name || Store.defaults.any { d -> d.name == name } } + x); apply(x) }, Modifier.padding(top = 8.dp)) { Text("ذخیره و اعمال") }
    }
}
