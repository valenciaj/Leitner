package es.leitner.app

import es.leitner.app.ui.ModeNames
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Recorrido completo en modo Clásico: crear mazo, añadir tarjeta y repasarla. */
@RunWith(AndroidJUnit4::class)
class MainFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun crear_mazo_tarjeta_y_repasar() {
        val deckName = "Mazo ${System.currentTimeMillis()}"

        rule.onNodeWithText(ModeNames.NORMAL).performClick()

        rule.onNodeWithText("Nuevo mazo").performClick()
        rule.onNode(hasSetTextAction() and hasText("Nombre")).performTextInput(deckName)
        rule.onNodeWithText("Crear").performClick()

        rule.onNodeWithText(deckName).performClick()
        rule.onNodeWithText("Nueva tarjeta").performClick()
        rule.onNode(hasSetTextAction() and hasText("Pregunta")).performTextInput("perro")
        rule.onNode(hasSetTextAction() and hasText("Respuesta")).performTextInput("dog")
        rule.onNodeWithText("Guardar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("perro")).fetchSemanticsNodes().isNotEmpty() }

        rule.onNodeWithText("‹ Atrás").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Repasar")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Repasar").performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Ver respuesta")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ver respuesta").performClick()
        rule.onNodeWithText("dog").assertExists()
        rule.onNodeWithText("Acerté").performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Repaso terminado")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Aciertos: 1 · Fallos: 0").assertExists()
    }
}
