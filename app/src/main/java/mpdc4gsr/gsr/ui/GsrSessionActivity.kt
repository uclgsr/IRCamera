package mpdc4gsr.gsr.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import mpdc4gsr.gsr.GsrOrchestrator
import mpdc4gsr.gsr.model.SessionStateStore

@AndroidEntryPoint
class GsrSessionActivity : ComponentActivity() {

    @Inject lateinit var orchestrator: GsrOrchestrator
    @Inject lateinit var sessionStateStore: SessionStateStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orchestrator.start()
        setContent {
            SessionDashboardScreen(
                sessionState = sessionStateStore.sessionSnapshot,
                onStartSimulation = { orchestrator.launchSimulationSession() },
                onStop = { orchestrator.stopSession() },
            )
        }
    }
}
