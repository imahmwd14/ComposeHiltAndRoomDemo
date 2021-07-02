package com.example.composedemo

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val namesViewModel by viewModels<NamesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(
                    Modifier
                        .fillMaxSize()
                        .background(Companion.LightGray),
                ) {
                    val namesList by namesViewModel.dao.getAllNames().observeAsState()

                    namesList?.let {
                        Home(it, {
                            lifecycleScope.launch(IO) {
                                namesViewModel.dao.delete(it)
                            }
                        }) { name: String ->
                            lifecycleScope.launch(IO) {
                                namesViewModel.dao.insert(Name(name = name))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable
 * */

@Composable
fun Home(namesList: List<Name>, deleteName: (Name) -> Unit, addNewName: (String) -> Unit) {
    var newName by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()

            ) {
                items(namesList.size) { pos ->
                    val name = namesList[pos]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 2.dp, bottom = 2.dp),
                        elevation = 1.dp,
                        shape = CircleShape.copy(CornerSize(4.dp)),
                        border = BorderStroke(
                            1.dp,
                            Companion.Gray
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column {
                                Text(
                                    text = name.name,
                                    Modifier.padding(4.dp),
                                    style = MaterialTheme.typography.h6
                                )
                                Text(
                                    text = "ID: ${name.Id.toString()}",
                                    Modifier.padding(4.dp),
                                    style = MaterialTheme.typography.body2
                                )
                            }

                            IconButton(onClick = { deleteName(name) }) {
                                Icon(Icons.Filled.Delete, "")
                            }
                        }
                    }
                }
            }
        }
        Divider(Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = newName,
            onValueChange = { it -> newName = it },
            placeholder = { Text("New name") },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
        )
        Button(onClick = {
            addNewName(newName)
        }, Modifier.fillMaxWidth()) {
            Text(text = "Add")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHome() {
    Home(listOf(Name(name = "A"), Name(name = "B"), Name(name = "C"), Name(name = "D")), {}) {}
}

/**
 * Room
 * */

@Entity
data class Name(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    @ColumnInfo(name = "name") var name: String
)

@Dao
interface NamesDao {
    @Insert
    fun insert(name: Name)

    @Delete
    fun delete(name: Name)

    @Query("SELECT * FROM name")
    fun getAllNames(): LiveData<List<Name>>
}

@Database(entities = [Name::class], version = 1, exportSchema = false)
abstract class NamesDatabase() : RoomDatabase() {
    abstract fun getDao(): NamesDao
}


/**
 * Hilt
 * */

@HiltAndroidApp
class App : Application()

@HiltViewModel
class NamesViewModel @Inject constructor(
    val dao: NamesDao
) : ViewModel() {

}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideDao(@ApplicationContext context: Context): NamesDao =
        Room
            .databaseBuilder(context, NamesDatabase::class.java, "names.db")
            .build()
            .getDao()
}