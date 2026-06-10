package com.focusfarm.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap

@Composable
fun ShareGardenDialog(
    bitmap: Bitmap,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2A22)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Bahceni Paylas",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFFE4B156),
            )

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Garden preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
            )

            Text(
                text = "Odaklanarak bahcemi buyuttum! Sen de katil.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD8C9AF),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Iptal")
                }

                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4B156)),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Paylas", color = Color(0xFF0F1712))
                }
            }
        }
    }
}
