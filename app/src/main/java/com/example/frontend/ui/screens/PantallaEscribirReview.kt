package com.example.frontend.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.frontend.ui.theme.AppColors

@Composable
fun PantallaEscribirResena(
    placeName: String,
    alPublicar: (estrellas: Int, texto: String) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.cream,
        bottomBar = {
            Button(
                onClick = { alPublicar(rating, reviewText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.tomato),
                shape = RoundedCornerShape(28.dp),
                enabled = rating > 0 && reviewText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(52.dp),
            ) {
                Text("Post Review", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.espresso,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("Write a Review", fontWeight = FontWeight.Bold, color = AppColors.espresso)
            }

            Spacer(Modifier.height(20.dp))
            Text(placeName, fontWeight = FontWeight.SemiBold, color = AppColors.espresso)
            Text(
                "How was your experience?",
                color = AppColors.muted,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(16.dp))
            Row {
                repeat(5) { index ->
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "${index + 1} stars",
                        tint = if (index < rating) AppColors.tomato else AppColors.border,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 4.dp)
                            .clickable { rating = index + 1 },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = reviewText,
                onValueChange = { reviewText = it },
                placeholder = { Text("Tell other students what to expect — taste, price, wait time...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.tomato,
                    unfocusedBorderColor = AppColors.border,
                ),
            )
        }
    }
}
