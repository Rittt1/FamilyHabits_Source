package com.example.familyhabits

import android.os.Bundle
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF20243A)
private val Purple = Color(0xFF7164D8)
private val Pale = Color(0xFFF5F4FC)

data class Habit(val id: Int, val title: String, val member: String, val done: Boolean)

class HabitDb(context: Context) : SQLiteOpenHelper(context, "family_habits.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE habits(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, member TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT INTO habits(title,member,done) VALUES('Читать 20 минут','Мама',0),('Полить цветы','Папа',0),('Сделать зарядку','Аня',1)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun all(): List<Habit> {
        val result = mutableListOf<Habit>()
        readableDatabase.rawQuery("SELECT id,title,member,done FROM habits ORDER BY id DESC", null).use { c ->
            while(c.moveToNext()) result.add(Habit(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3)==1))
        }
        return result
    }
    fun add(title: String, member: String) {
        writableDatabase.execSQL("INSERT INTO habits(title,member,done) VALUES(?,?,0)", arrayOf(title, member))
    }
    fun toggle(id: Int, done: Boolean) {
        writableDatabase.execSQL("UPDATE habits SET done=? WHERE id=?", arrayOf(if(done) 1 else 0, id))
    }
    fun delete(id: Int) { writableDatabase.execSQL("DELETE FROM habits WHERE id=?", arrayOf(id)) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = HabitDb(this)
        setContent { FamilyHabitsApp(db) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyHabitsApp(db: HabitDb) {
    var habits by remember { mutableStateOf(db.all()) }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var member by remember { mutableStateOf("Мама") }
    val completed = habits.count { it.done }
    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = Pale)) {
        Scaffold(containerColor = Pale, floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Purple, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item {
                    Spacer(Modifier.height(18.dp))
                    Text("FAMILY HABITS", color=Purple, fontSize=13.sp)
                    Text("Привет, семья! 👋", color=Ink, fontSize=28.sp)
                    Text("Маленькие шаги — большие результаты", color=Color.Gray, fontSize=14.sp)
                    Spacer(Modifier.height(12.dp))
                    Card(colors=CardDefaults.cardColors(containerColor=Purple), shape=RoundedCornerShape(22.dp), modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Прогресс семьи сегодня", color=Color.White.copy(alpha=.85f))
                            Text("$completed из ${habits.size} привычек", color=Color.White, fontSize=25.sp)
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(progress={if(habits.isEmpty()) 0f else completed.toFloat()/habits.size}, modifier=Modifier.fillMaxWidth().height(8.dp), color=Color.White, trackColor=Color.White.copy(alpha=.25f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Привычки на сегодня", color=Ink, fontSize=20.sp)
                }
                items(habits, key={it.id}) { habit ->
                    Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=Color.White), modifier=Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                            Checkbox(checked=habit.done, onCheckedChange={ value -> db.toggle(habit.id,value); habits=db.all() }, colors=CheckboxDefaults.colors(checkedColor=Purple))
                            Column(Modifier.weight(1f)) {
                                Text(habit.title, color=Ink, fontSize=16.sp)
                                Text(habit.member, color=Color.Gray, fontSize=12.sp)
                            }
                            IconButton(onClick={ db.delete(habit.id); habits=db.all() }) {
                                Icon(Icons.Default.Delete, contentDescription="Удалить", tint=Color.LightGray)
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Семья — наша команда 💜", color=Purple, fontSize=14.sp)
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
        if(showAdd) AlertDialog(
            onDismissRequest={showAdd=false},
            title={Text("Новая привычка")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value=title,onValueChange={title=it},label={Text("Название")},singleLine=true)
                    Text("Кто выполняет?")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}) {
                        OutlinedTextField(value=member,onValueChange={},readOnly=true,modifier=Modifier.menuAnchor(),label={Text("Участник")})
                        ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                            listOf("Мама","Папа","Аня","Все вместе").forEach { option ->
                                DropdownMenuItem(text={Text(option)},onClick={member=option;expanded=false})
                            }
                        }
                    }
                }
            },
            confirmButton={ TextButton(onClick={ if(title.isNotBlank()){db.add(title.trim(),member);habits=db.all();title="";showAdd=false} }) { Text("Добавить") } },
            dismissButton={ TextButton(onClick={showAdd=false}) { Text("Отмена") } }
        )
    }
}
