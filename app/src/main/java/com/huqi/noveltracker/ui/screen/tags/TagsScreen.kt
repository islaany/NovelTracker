package com.huqi.noveltracker.ui.screen.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.huqi.noveltracker.data.model.Tag
import com.huqi.noveltracker.ui.component.TagChip

private val PALETTE = listOf(
    "#7C4DFF", "#5C8A5C", "#C99A3B", "#E57373",
    "#4FC3F7", "#BA68C8", "#FF8A65", "#A1887F"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    navController: NavHostController,
    viewModel: TagsViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(PALETTE[0]) }
    val tags = viewModel.tags.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.addTag(name, selectedColor)
                name = ""
            }) {
                Icon(Icons.Default.Add, contentDescription = "添加标签")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("新标签名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("选择颜色", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PALETTE.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (hex == selectedColor)
                                    Modifier.background(
                                        Color.Transparent,
                                        RoundedCornerShape(50)
                                    )
                                else Modifier
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.size(4.dp))
            Text("已有标签", style = MaterialTheme.typography.labelMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags, key = { it.name }) { tag: Tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TagChip(name = tag.name, colorHex = tag.color)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteTag(tag) }) {
                            Icon(Icons.Default.Close, contentDescription = "删除标签")
                        }
                    }
                }
            }
        }
    }
}
