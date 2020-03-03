package game

import engine.GameWindow
import org.lwjgl.system.Configuration

fun main(args: Array<String>) {
    Configuration.DEBUG.set(true)
    GameWindow { _ -> TestGame() }.run()
}
