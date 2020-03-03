package engine.resources

import engine.Color
import game.Constants
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_MODELVIEW
import org.lwjgl.opengl.GL11.GL_PROJECTION
import org.lwjgl.opengl.GL11.glBegin
import org.lwjgl.opengl.GL11.glColor4f
import org.lwjgl.opengl.GL11.glEnd
import org.lwjgl.opengl.GL11.glLoadIdentity
import org.lwjgl.opengl.GL11.glMatrixMode
import org.lwjgl.opengl.GL11.glOrtho
import org.lwjgl.opengl.GL11.glTexCoord2f
import org.lwjgl.opengl.GL11.glVertex2f
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import java.io.Closeable
import java.nio.ByteBuffer


class ImageLoader(val files: ResourceManager<ByteBuffer>) : ResourceLoader<Image> {
    override fun load(resource: String): Image {
        return loadImageData(resource).use { data: RawImageData ->
            TextureBasedImage(data.uploadTexture(), data.width, data.height);
        }
    }

    private fun loadImageData(imagePath: String): RawImageData {
        val fileBuffer = files.load(imagePath)
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val components = stack.mallocInt(1)

            // Decode the image
            val imageBuffer = STBImage.stbi_load_from_memory(fileBuffer, w, h, components, 0)
            if (imageBuffer == null) {
                throw RuntimeException("Failed to parse image data: " + STBImage.stbi_failure_reason()!!)
            }
            return RawImageData(w.get(0), h.get(0), components.get(0), imageBuffer);
        }
    }

    private data class RawImageData(val width: Int, val height: Int, val channelCount: Int, val imageData: ByteBuffer) :
        Closeable {
        override fun close() {
            STBImage.stbi_image_free(imageData);
        }

        fun uploadTexture(): Int {
            // Acquire texture ID
            val texID = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texID)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            // Set texture filtering parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            // Setup the texture settings
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            // Pre-format the data if necessary
            val format: Int = if (channelCount == 3) GL_RGB else GL_RGBA
            // Upload texture pixels
            glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, imageData)
            glGenerateMipmap(GL_TEXTURE_2D)
            // Texture is ready
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

    class TextureBasedImage(
        val id: Int,
        override val width: Int,
        override val height: Int
    ) : Image {
        private val buffer = glGenVertexArrays()

        init {
            glBindVertexArray(buffer)
            // Vertices
            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers())
            val vertices = floatArrayOf(
                // positions          // colors           // texture coords
                0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, // top right
                0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, // bottom right
                -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, // bottom left
                -0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f    // top left
            )
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
            // Indices
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, glGenBuffers())
            val indices = intArrayOf(
                0, 1, 3,
                1, 2, 3
            )
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
            // Position
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * 4, 0)
            glEnableVertexAttribArray(0)
            // Color
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * 4, 3 * 4)
            glEnableVertexAttribArray(1)
            // TexCoord
            glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * 4, 6 * 4)
            glEnableVertexAttribArray(2)
        }

        override fun draw() {
            // Bind the texture for upcomming texture related operations
            glColor4fv(Color.White.toArray())
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, id)
            glBindVertexArray(buffer)
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
        }
    }
}