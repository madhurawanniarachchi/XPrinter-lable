package com.kmpgaraj.xprinter_label.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons on a 24x24 grid with a 2 px round stroke, drawn from SVG path data
 * so the app needs no icon library. Tint them with `Icon(tint = ...)`.
 */
object AppIcons {

    val Printer: ImageVector by lazy {
        strokeIcon(
            "Printer",
            "M6 9V3h12v6",
            "M5 9h14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2z",
            "M8 14h8a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z"
        )
    }

    val ChevronDown: ImageVector by lazy {
        strokeIcon("ChevronDown", "M6 9l6 6 6-6")
    }

    val Close: ImageVector by lazy {
        strokeIcon("Close", "M6 6l12 12", "M18 6L6 18")
    }

    val Calendar: ImageVector by lazy {
        strokeIcon(
            "Calendar",
            "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
            "M3 10h18",
            "M8 3v4",
            "M16 3v4"
        )
    }

    private fun strokeIcon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            for (data in paths) {
                addPath(
                    pathData = addPathNodes(data),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()
}
