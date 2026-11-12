package com.servilocal.core.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.servilocal.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlinx.coroutines.delay

@Composable
fun PostLocationMap(
    latitude: Double?,
    longitude: Double?,
    locationLabel: String,
    modifier: Modifier = Modifier
) {
    if (latitude == null || longitude == null) return

    var mapReady by remember(latitude, longitude) { mutableStateOf(false) }
    LaunchedEffect(latitude, longitude) {
        delay(250)
        mapReady = true
    }

    if (!mapReady) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.post_location_map_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val point = remember(latitude, longitude) { GeoPoint(latitude, longitude) }
    val mapView = remember(context, latitude, longitude) {
        createPostLocationMap(context, point, locationLabel)
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        update = { map ->
            map.controller.setCenter(point)
            map.invalidate()
        }
    )
}

private fun createPostLocationMap(
    context: Context,
    point: GeoPoint,
    locationLabel: String
): MapView {
    Configuration.getInstance().userAgentValue = context.packageName
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        setBuiltInZoomControls(false)
        controller.setZoom(15.0)
        controller.setCenter(point)
        overlays.add(
            Marker(this).apply {
                position = point
                title = locationLabel
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
        )
    }
}
