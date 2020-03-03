package game

import engine.Game
import engine.KeyEvent
import engine.resources.*
import org.lwjgl.opengl.GL11.glColor4fv
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL33.glClearColor

class PongGame : Game() {
    val files = ResourceManager(FileLoader())
    val images = ResourceManager(ImageLoader(files))
    val shaders = ShaderManager(files)
    val defaultShader = shaders.build("textured-vertex.glsl", "textured-fragment.glsl")
    // Resources
    val ballSprite: Image = images.load("ball.png")
    // Entities
    val balls = mutableListOf<Ball>()
    val playerBat = PlayerBat(this)
    val computerBat = ComputerBat(this)

    override fun setup() {
        // Load resources
        glClearColor(0f, 0f, 0f, 0f)
        balls += Ball(this)
    }


    override fun update() {
        computerBat.update()
        playerBat.update()
        balls.forEach { Ball -> Ball.update() }
    }

    override fun render() {
        defaultShader.makeActive()
        glUniform1i(glGetUniformLocation(defaultShader.id, "ourTexture"), 0);
        // Render tick
        computerBat.render()
        playerBat.render()
        balls.forEach { Ball -> Ball.render() }
    }

    override fun onKey(event: KeyEvent) {
        super.onKey(event)
        println("key $event")
        playerBat.onKey(event);
    }
}