package com.mtpnda.biocue.mockemitter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mtpnda.biocue.mockemitter.ui.MockEmitterScreen
import com.mtpnda.biocue.mockemitter.ui.theme.MockEmitterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MockEmitterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MockEmitterTheme {
                MockEmitterScreen(viewModel = viewModel)
            }
        }
    }
}
