package engine

abstract class Game {
    abstract fun setup()
    abstract fun update()
    abstract fun render()

    open fun onKey(event : KeyEvent) {}
}