package com.yugentech.quill.ui.more.subscriptions.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.yugentech.quill.R

@Composable
fun SubscribedContent(
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val url = "https://play.google.com/store/account/subscriptions?package=${context.packageName}"

    // Randomize all 8 avatars
    val airaCluster = remember {
        listOf(
            R.drawable.aira1, R.drawable.aira2, R.drawable.aira3, R.drawable.aira4,
            R.drawable.aira9, R.drawable.aira6, R.drawable.aira7, R.drawable.aira8
        )
            .shuffled()
            .map { id ->
                id to (Math.random() > 0.5)
            }
    }

    val topRow = airaCluster.take(4)
    val bottomRow = airaCluster.drop(4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(1f))

        // Overlapping Avatar Grid (2 Rows of 4)
        Column(
            verticalArrangement = Arrangement.spacedBy((-48).dp), // Increased vertical overlap
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP ROW
            Row(
                horizontalArrangement = Arrangement.spacedBy((-48).dp), // Increased horizontal overlap
                verticalAlignment = Alignment.CenterVertically
            ) {
                topRow.forEachIndexed { index, (drawableId, isFlipped) ->
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp) // Larger size
                            .zIndex(index.toFloat())
                            .clip(CircleShape)
                            .scale(scaleX = if (isFlipped) -1f else 1f, scaleY = 1f)
                    )
                }
            }

            // BOTTOM ROW
            Row(
                horizontalArrangement = Arrangement.spacedBy((-56).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomRow.forEachIndexed { index, (drawableId, isFlipped) ->
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp) // Larger size
                            .zIndex((index + 4).toFloat())
                            .clip(CircleShape)
                            .scale(scaleX = if (isFlipped) -1f else 1f, scaleY = 1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Quill Pro",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "You've unlocked the full reading experience. Aira is ready with 100 daily queries to help you dive deeper into your library and uncover new insights.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Actions using your provided button styling
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape
            ) {
                Text(
                    text = "Continue Reading",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
                        context.startActivity(browserIntent)
                    }
                }
            ) {
                Text(
                    text = "Manage Subscription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}