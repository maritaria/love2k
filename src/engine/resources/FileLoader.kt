package engine.resources

import engine.Sprite
import org.lwjgl.BufferUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class FileLoader : ResourceLoader<ByteBuffer> {
    override fun load(resource: String): ByteBuffer {
        return try {
            val path = Paths.get(resource)
            if (Files.isReadable(path)) {
                readFromFile(path)
            } else {
                readFromResources(resource)
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to load image file", e);
        }
    }

    private fun readFromResources(resource: String): ByteBuffer {
        Sprite::class.java.classLoader.getResourceAsStream(resource)!!.use { source ->
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

    private fun readFromFile(path: Path): ByteBuffer {
        Files.newByteChannel(path).use { fc ->
            val buffer = BufferUtils.createByteBuffer(fc.size().toInt() + 1)
            while (fc.read(buffer) != -1) {
            }
            buffer.flip()
            return buffer;
        }
    }
}
