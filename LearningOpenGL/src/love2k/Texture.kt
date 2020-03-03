package love2k

import org.lwjgl.opengl.GL30.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class Texture2D(image: ByteBuffer) : GraphicsHandle {
    override val id: Int = glGenTextures()
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var channels: Int = 0
        private set

    init {
        glBindTexture(GL_TEXTURE_2D, id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val c = stack.mallocInt(1)
            val pixels = STBImage.stbi_load_from_memory(image, w, h, c, 0)
            if (pixels == null) {
                throw RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason()!!)
            } else {
                width = w.get(0)
                height = h.get(0)
                channels = c.get(0)
                val format = if (channels == 4) GL_RGBA else GL_RGB
                glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    format,
                    width,
                    height,
                    0,
                    format,
                    GL_UNSIGNED_BYTE,
                    pixels
                )
            }
        }
        glGenerateMipmap(GL_TEXTURE_2D)
    }

    override fun close() {
        glDeleteTextures(id)
        TODO("Mark instance as invalid with some boolean flag")
    }
}