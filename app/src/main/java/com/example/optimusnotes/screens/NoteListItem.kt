package com.example.optimusnotes.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.optimusnotes.roomdb.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteListItem(note: Note, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            .clickable {
                navController.navigate("noteDetail/${note.id}") // Ensure this route exists in NavHost
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = note.title,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Description
            Text(
                text = note.description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp) // Padding below description
            )

            // ✅ Fix: Avoid using remember {} for date formatting
            val formattedDateTime = SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(Date(note.timestamp))

            // Date Display (Created on)
            Text(
                text = "Created on: $formattedDateTime",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(top = 8.dp) // Padding between description and date
            )
        }
    }
}
