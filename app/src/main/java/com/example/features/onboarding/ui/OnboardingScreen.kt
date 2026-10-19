package com.example.features.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp).testTag("onboarding_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_servilocal_logo),
                    contentDescription = "Ícono de ServiLocal",
                    modifier = Modifier.size(104.dp).clip(RoundedCornerShape(28.dp))
                )
                Spacer(Modifier.height(24.dp))
                Text("ServiLocal", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Servicios confiables, cerca de ti.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(28.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .14f)) {
                            Icon(Icons.Default.Handyman, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(18.dp).size(42.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Lo local resuelve", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
                        Text("Encuentra profesionales de tu comunidad en Armenia y el Quindío.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Spacer(Modifier.height(22.dp))
                OnboardingBenefit(Icons.Default.VerifiedUser, "Perfiles y servicios revisados")
                OnboardingBenefit(Icons.Default.LocationOn, "Opciones cerca de tu zona")
                OnboardingBenefit(Icons.Default.CheckCircle, "Coordina directamente por chat")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth().height(54.dp).testTag("onboarding_continue"), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Comenzar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text("Tu comunidad, a un toque de distancia", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun OnboardingBenefit(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
