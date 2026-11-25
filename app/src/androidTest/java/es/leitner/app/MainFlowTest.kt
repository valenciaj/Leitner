package es.leitner.app

import es.leitner.app.ui.ModeNames
import es.leitner.core.Mode
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.onNodeWithContentDescription
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

    /** Textos de la app en el idioma del emulador. */
    private fun s(id: Int, vararg args: Any): String = rule.activity.getString(id, *args)

    /** Espera a que aparezca [text]; si no aparece, el error incluye el árbol de la pantalla. */
    private fun waitForText(text: String) {
        try {
            rule.waitUntil(10_000) { rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            val tree = rule.onAllNodes(isRoot()).printToString()
            throw AssertionError("No aparece '$text'. Pantalla:\n$tree", e)
        }
    }

    @Test
    fun crear_mazo_tarjeta_y_repasar() {
        val deckName = "Mazo ${System.currentTimeMillis()}"

        rule.onNodeWithText(s(ModeNames.of(Mode.NORMAL))).performClick()

        // La lista de mazos tarda un poco más en aparecer la primera vez (carga las fuentes).
        waitForText(s(R.string.new_deck))
        rule.onNodeWithText(s(R.string.new_deck)).performClick()
        rule.onNode(hasSetTextAction() and hasText(s(R.string.name))).performTextInput(deckName)
        rule.onNodeWithText(s(R.string.create)).performClick()

        rule.onNodeWithText(deckName).performClick()
        rule.onNodeWithText(s(R.string.new_card)).performClick()
        rule.onNode(hasSetTextAction() and hasText(s(R.string.question))).performTextInput("perro")
        rule.onNode(hasSetTextAction() and hasText(s(R.string.answer))).performTextInput("dog")
        rule.onNodeWithText(s(R.string.save)).performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("perro")).fetchSemanticsNodes().isNotEmpty() }

        rule.onNodeWithContentDescription(s(R.string.back)).performClick()
        val reviewDeck = s(R.string.review_deck_cd, deckName)
        rule.waitUntil(5_000) { rule.onAllNodes(hasContentDescription(reviewDeck)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription(reviewDeck).performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText(s(R.string.show_answer))).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(s(R.string.show_answer)).performClick()
        rule.onNodeWithText("dog").assertExists()

        // Una fallada se repite al final de la sesión.
        rule.onNodeWithText(s(R.string.wrong)).performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(s(R.string.retry))).fetchSemanticsNodes().isNotEmpty() }

        // Deshacer vuelve a la tarjeta con la respuesta a la vista.
        rule.onNodeWithContentDescription(s(R.string.undo)).performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(s(R.string.right))).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(s(R.string.right)).performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText(s(R.string.review_done))).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(s(R.string.review_result, 1, 0)).assertExists()
    }
}
