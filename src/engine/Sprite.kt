package engine

import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack.stackPush
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths

data class RawImageData(val width: Int, val height: Int, val channelCount: Int, val imageData: ByteBuffer) : Closeable {
    override fun close() {
        stbi_image_free(imageData);
    }

    fun createTexture(): Int {
        val texID = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texID)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        val format: Int = if (channelCount == 3) GL_RGB else GL_RGBA;

        if (format == GL_RGB) {
            if (width and 3 != 0) {
                glPixelStorei(GL_UNPACK_ALIGNMENT, 2 - (width and 1))
            }
        } else {
            premultiplyAlpha()
            glEnable(GL_BLEND)
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        }

        glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, imageData)

        // Original code had mip-map generation, removed since not used
        return texID
    }

    @ExperimentalUnsignedTypes
    private fun premultiplyAlpha() {
        val pixelSize = 4;
        val rowSize = width * pixelSize
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * rowSize) + (x * pixelSize)
                // We need to jump through some hoops to make the signing work with the arithmetic
                val mask = 0xFF.toUByte()
                // Get the raw pixel data
                val red = imageData.get(index + 0).toUByte() and mask
                val green = imageData.get(index + 1).toUByte() and mask
                val blue = imageData.get(index + 2).toUByte() and mask
                val alpha = imageData.get(index + 3).toUByte() and mask
                // Compute the alpha multiplier for the new pixel values
                val alphaFactor = alpha.toFloat() / 255.0f
                // Scale colors according to alpha channel
                val newRed = (red.toFloat() * alphaFactor).toUInt().toByte()
                val newGreen = (green.toFloat() * alphaFactor).toUInt().toByte()
                val newBlue = (blue.toFloat() * alphaFactor).toUInt().toByte()
                imageData.put(index + 0, newRed)
                imageData.put(index + 1, newGreen)
                imageData.put(index + 2, newBlue)
            }
        }
    }
}

fun loadImage(imagePath: String): RawImageData {
    val fileBuffer: ByteBuffer
    try {
        fileBuffer = ioResourceToByteBuffer(imagePath, 8 * 1024)
    } catch (e: IOException) {
        throw RuntimeException(e)
    }
    stackPush().use { stack ->
        val w = stack.mallocInt(1)
        val h = stack.mallocInt(1)
        val components = stack.mallocInt(1)

        // Decode the image
        val imageBuffer = stbi_load_from_memory(fileBuffer, w, h, components, 0)
        if (imageBuffer == null) {
            throw RuntimeException("Failed to load image: " + stbi_failure_reason()!!)
        } else {
            return RawImageData(w.get(0), h.get(0), components.get(0), imageBuffer);
        }
    }
}


private fun ioResourceToByteBuffer(resource: String, initialBufferSize: Int): ByteBuffer {
    val path = Paths.get(resource)
    if (Files.isReadable(path)) {
        Files.newByteChannel(path).use { fc ->
            val buffer = createByteBuffer(fc.size().toInt() + 1)
            while (fc.read(buffer) != -1) {
            }
            buffer.flip()
            return buffer;
        }
    } else {
        Sprite::class.java.classLoader.getResourceAsStream(resource)!!.use { source ->
            Channels.newChannel(source).use { rbc ->
                var buffer = createByteBuffer(initialBufferSize)
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
}

private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
    val newBuffer = createByteBuffer(newCapacity)
    buffer.flip()
    newBuffer.put(buffer)
    return newBuffer
}

fun loadSprite(imagePath: String): Sprite {
    return loadImage(imagePath).use { data: RawImageData ->
        Sprite(data.createTexture(), data.width, data.height);
    }
}

data class Sprite(val textureId: Int, val width: Int, val height: Int) {
    fun draw() {
        val w = width.toFloat();
        val h = height.toFloat();

        // Bind the texture for upcomming texture related operations
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Push the vertices into the video card
        glBegin(GL_TRIANGLES);

        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(0.0f, 0.0f);

        glTexCoord2f(1.0f, 0.0f);
        glVertex2f(w, 0.0f);

        glTexCoord2f(1.0f, 1.0f);
        glVertex2f(w, h);

        glTexCoord2f(0.0f, 1.0f);
        glVertex2f(0.0f, h);

        glEnd();
    }
}