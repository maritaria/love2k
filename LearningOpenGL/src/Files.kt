import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_RGB
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels

fun loadResource(path: String): ByteBuffer {
    GL::class.java.classLoader.getResourceAsStream(path)!!.use { source ->
        Channels.newChannel(source).use { rbc ->
            var buffer = BufferUtils.createByteBuffer(8 * 1024)
            while (true) {
                val bytes = rbc.read(buffer)
                if (bytes == -1) {
                    break
                }
                if (buffer.remaining() == 0) {
                    buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2) // 50%
                }
            }
            buffer.flip()
            return buffer;
        }
    }

}

private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
    val newBuffer = BufferUtils.createByteBuffer(newCapacity)
    buffer.flip()
    newBuffer.put(buffer)
    return newBuffer
}

data class ImageBlob(val buffer: ByteBuffer, val width: Int, val height: Int, val channels: Int) : AutoCloseable {
    val format: Int
        get() = if (channels == 4) GL_RGBA else GL_RGB

    override fun close() {
        STBImage.stbi_image_free(buffer)
    }
}

fun loadImage(path: String): ImageBlob {
    val fileBuffer = try {
        loadResource(path)
    } catch (e: IOException) {
        throw RuntimeException(e)
    }
    MemoryStack.stackPush().use { stack ->
        val w = stack.mallocInt(1)
        val h = stack.mallocInt(1)
        val components = stack.mallocInt(1)
        val pixels = STBImage.stbi_load_from_memory(fileBuffer, w, h, components, 0)
        if (pixels == null) {
            throw RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason()!!)
        } else {
            return ImageBlob(pixels, w.get(0), h.get(0), components.get(0));
        }
    }
}