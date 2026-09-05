package com.mtpnda.biocue.wear

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mtpnda.biocue.wear.presentation.MainActivity

class MainTileService : TileService() {
    private val RESOURCES_VERSION = "1"

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val singleTileTimeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(tileLayout(this, requestParams.deviceConfiguration))
                            .build()
                    )
                    .build()
            )
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(singleTileTimeline)
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )
    }

    private fun tileLayout(
        context: Context,
        deviceParameters: androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        
        val launchAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(context.packageName)
                    .setClassName(MainActivity::class.java.name)
                    .addKeyToExtraMapping(
                        "launch_state",
                        ActionBuilders.AndroidStringExtra.Builder().setValue("CAROUSEL").build()
                    )
                    .build()
            )
            .build()

        val clickModifier = ModifiersBuilders.Clickable.Builder()
            .setId("launch_app")
            .setOnClick(launchAction)
            .build()
            
        val textFontStyle = androidx.wear.protolayout.LayoutElementBuilders.FontStyle.Builder()
            .setColor(argb(0xFFFFFFFF.toInt()))
            .setWeight(androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD)
            .build()
            
        val titleFontStyle = androidx.wear.protolayout.LayoutElementBuilders.FontStyle.Builder()
            .setColor(argb(0xFF94A3B8.toInt()))
            .build()

        return LayoutElementBuilders.Column.Builder()
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("BioCue")
                    .setFontStyle(titleFontStyle)
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setHeight(androidx.wear.protolayout.DimensionBuilders.dp(16f))
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Box.Builder()
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setClickable(clickModifier)
                            .setBackground(
                                ModifiersBuilders.Background.Builder()
                                    .setColor(argb(0xFF4F46E5.toInt()))
                                    .setCorner(ModifiersBuilders.Corner.Builder().setRadius(androidx.wear.protolayout.DimensionBuilders.dp(24f)).build())
                                    .build()
                            )
                            .setPadding(
                                ModifiersBuilders.Padding.Builder()
                                    .setStart(androidx.wear.protolayout.DimensionBuilders.dp(16f))
                                    .setEnd(androidx.wear.protolayout.DimensionBuilders.dp(16f))
                                    .setTop(androidx.wear.protolayout.DimensionBuilders.dp(8f))
                                    .setBottom(androidx.wear.protolayout.DimensionBuilders.dp(8f))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText("⚡ Log Emotion")
                            .setFontStyle(textFontStyle)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
