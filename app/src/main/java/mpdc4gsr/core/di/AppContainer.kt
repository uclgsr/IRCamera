package mpdc4gsr.core.di

import android.content.Context

interface AppContainer {
    val context: Context
}

class DefaultAppContainer(
    override val context: Context
) : AppContainer
