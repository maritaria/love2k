package game

import engine.GameEntity
import engine.KeyEvent

open class Bat(game: PongGame) : GameEntity(game) {
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 10f
    var height: Float = 50f

    open override fun update() {
    }

    open override fun render() {
    }
}

class PlayerBat(game: PongGame) : Bat(game) {
    override fun update() {
        super.update()
    }

    override fun render() {
        super.render()
    }

    fun onKey(event: KeyEvent) {

    }
}

class ComputerBat(game: PongGame) : Bat(game) {
    override fun update() {
        super.update()
    }

    override fun render() {
        super.render()
    }
}