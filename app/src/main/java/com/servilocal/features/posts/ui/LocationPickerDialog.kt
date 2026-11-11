package com.servilocal.features.posts.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.servilocal.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private const val DEFAULT_LATITUDE = 4.5339
private const val DEFAULT_LONGITUDE = -75.6811

/** A point selector: choosing a point here does not require device-location permission. */
@Composable
fun LocationPickerDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (latitude: Double, longitude: Double) -> Unit
) {
    val context = LocalContext.current
    var selectedLatitude by rememberSaveable { mutableStateOf(initialLatitude) }
    var selectedLongitude by rememberSaveable { mutableStateOf(initialLongitude) }
    val latestOnPick by rememberUpdatedState<(Double, Double) -> Unit>({ latitude, longitude ->
        selectedLatitude = latitude
        selectedLongitude = longitude
    })
    val mapView = remember(context) {
        createLocationPickerMap(context) { latitude, longitude ->
            latestOnPick(latitude, longitude)
        }.apply {
            controller.setZoom(15.0)
            controller.setCenter(
                GeoPoint(initialLatitude ?: DEFAULT_LATITUDE, initialLongitude ?: DEFAULT_LONGITUDE)
            )
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.pick_post_location), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.pick_post_location_help), style = MaterialTheme.typography.bodySmall)
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    update = { map ->
                        map.overlays.removeAll(map.overlays.filterIsInstance<Marker>())
                        val latitude = selectedLatitude
                        val longitude = selectedLongitude
                        if (latitude != null && longitude != null) {
                            map.overlays.add(
                                Marker(map).apply {
                                    position = GeoPoint(latitude, longitude)
                                    title = context.getString(R.string.selected_post_location)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                            )
                        }
                        map.invalidate()
                    }
                )
                if (selectedLatitude != null && selectedLongitude != null) {
                    Text(
                        text = stringResource(
                            R.string.selected_coordinates,
                            selectedLatitude!!,
                            selectedLongitude!!
                        ),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val latitude = selectedLatitude
                            val longitude = selectedLongitude
                            if (latitude != null && longitude != null) onConfirm(latitude, longitude)
                        },
                        enabled = selectedLatitude != null && selectedLongitude != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.confirm_location))
                    }
                }
            }
        }
    }
}

private fun createLocationPickerMap(
    context: Context,
    onPointSelected: (latitude: Double, longitude: Double) -> Unit
): MapView {
    Configuration.getInstance().userAgentValue = context.packageName
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        setBuiltInZoomControls(true)
        overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    onPointSelected(point.latitude, point.longitude)
                    return true
                }

                override fun longPressHelper(point: GeoPoint): Boolean = false
            })
        )
    }
}
