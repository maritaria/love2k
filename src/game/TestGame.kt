package game

import engine.Color
import engine.Game
import engine.resources.FileLoader
import engine.resources.ImageLoader
import engine.resources.ResourceManager
import engine.resources.ShaderManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL33.*
import java.nio.ByteBuffer


class TestGame : Game() {
    val files = ResourceManager(FileLoader())
    val images = ResourceManager(ImageLoader(files))
    val shaders = ShaderManager(files)
    // Resources
    val myImage: ImageLoader.TextureBasedImage = images.load("test.png") as ImageLoader.TextureBasedImage
    val myShader = shaders.build("textured-vertex.glsl", "textured-fragment.glsl")
    var customImage: Int = 0;

    override fun setup() {
        myShader.makeActive()
        val uniformIndex = glGetUniformLocation(myShader.id, "ourTexture")
        glUniform1i(uniformIndex, 0)


        customImage = glGenTextures()
        glBindTexture(GL11.GL_TEXTURE_2D, customImage)
        // set the texture wrapping parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        // set texture filtering parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        val byteBuffer = ByteBuffer.allocateDirect(16)
        byteBuffer.put(0)
        byteBuffer.put(-1)
        byteBuffer.put(-1)
        byteBuffer.put(-1)
        byteBuffer.put(-1)
        byteBuffer.put(0)
        byteBuffer.put(0)
        byteBuffer.put(0)
        byteBuffer.put(0)
        byteBuffer.put(0)
        byteBuffer.put(0)
        byteBuffer.rewind()
        byteBuffer.flip()

        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGB,
            1,
            1,
            0,
            GL_RGB,
            GL_UNSIGNED_BYTE,
            byteBuffer
        )
        glGenerateMipmap(GL_TEXTURE_2D)
    }

    override fun update() {
    }

    override fun render() {
        // debugTexture(customImage, 0f, 0f, 0.2f, 0.2f)

        debugTexture(myImage.id, 0f, 0f, 0.2f, 0.2f);
    }

    fun renderCustom() {

        val vertices = floatArrayOf(
            // positions          // colors           // texture coords
            0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, // top right
            0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, // bottom right
            -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, // bottom left
            -0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f    // top left
        )

        val indices = intArrayOf(
            0, 1, 3,
            1, 2, 3
        )

        val buffer = glGenVertexArrays()
        val verticesBuffer = glGenBuffers()
        val indicesBuffer = glGenBuffers()
        glBindVertexArray(buffer)
        // Vertices
        glBindBuffer(GL_ARRAY_BUFFER, verticesBuffer)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        // Indices
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
        // Position
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8, 0)
        glEnableVertexAttribArray(0)
        // Color
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 8, 3)
        glEnableVertexAttribArray(1)
        // TexCoord
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 8, 6)
        glEnableVertexAttribArray(2)

        glColor4fv(Color.White.toArray())
        glBindTexture(GL_TEXTURE_2D, myImage.id)
        glActiveTexture(GL_TEXTURE0)

        glUseProgram(myShader.id)
        glBindVertexArray(buffer)
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

        glDeleteBuffers(indicesBuffer)
        glDeleteBuffers(verticesBuffer)
        glDeleteVertexArrays(buffer)

    }
}

fun debugTexture(image: Int, x: Float, y: Float, width: Float, height: Float) {
    //usually glOrtho would not be included in our game loop
    //however, since it's deprecated, let's keep it inside of this debug function which we will remove later
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity()
    glOrtho(0.0, Constants.WindowWidth.toDouble(), Constants.WindowHeight.toDouble(), 0.0, 1.0, -1.0)
    glMatrixMode(GL_MODELVIEW)
    glLoadIdentity()
    glEnable(GL_TEXTURE_2D) //likely redundant; will be removed upon migration to "modern GL"

    //bind the texture before rendering it
    glBindTexture(GL_TEXTURE_2D, image)

    //setup our texture coordinates
    //(u,v) is another common way of writing (s,t)
    val u = 0f
    val v = 0f
    val u2 = 1f
    val v2 = 1f

    //immediate mode is deprecated -- we are only using it for quick debugging
    glColor4f(1f, 1f, 1f, 1f)
    glBegin(GL_QUADS)
    glTexCoord2f(u, v)
    glVertex2f(x, y)
    glTexCoord2f(u, v2)
    glVertex2f(x, y + height)
    glTexCoord2f(u2, v2)
    glVertex2f(x + width, y + height)
    glTexCoord2f(u2, v)
    glVertex2f(x + width, y)
    glEnd()
}