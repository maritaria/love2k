package engine

import game.PongGame

abstract class GameEntity(val game : PongGame) {
    abstract fun update()
    abstract fun render()
}