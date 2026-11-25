package com.kmpgaraj.xprinter_label

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kmpgaraj.xprinter_label.ui.theme.XPrinterLabelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XPrinterLabelTheme { LabelScreen() }
        }
    }
}
