package game

import engine.GameEntity
import engine.Vector
import org.lwjgl.opengl.GL33.*

class Ball(game: PongGame) : GameEntity(game) {
    var x: Float = 0f
    var y: Float = 0f
    var radius: Float = 10f
    var speed: Vector = Vector(0f, 0f)


    override fun update() {
        x += speed.x;
        y += speed.y;
    }

    override fun render() {
        glPushMatrix()
        try {
            glTranslatef(this.x, this.y, 0f)
            game.ballSprite.draw()
        } finally {
            glPopMatrix()
        }
    }
}