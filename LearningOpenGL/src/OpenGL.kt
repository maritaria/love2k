import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

interface GraphicsHandle : AutoCloseable {
    val id: Int
}

abstract class Shader(type: Int, source: ByteBuffer) : GraphicsHandle {
    final override val id = glCreateShader(type)

    init {
        MemoryStack.stackPush().use { stack ->
            glShaderSource(
                id,
                stack.pointers(source),
                stack.ints(source.remaining())
            )
        }
        glCompileShader(id)
        if (glGetShaderi(id, GL_COMPILE_STATUS) != GL_TRUE) {
            throw RuntimeException("Failed to compile shader: ${glGetShaderInfoLog(id)}")
        }
    }

    override fun close() {
        glDeleteShader(id)
    }
}

class VertexShader(source: ByteBuffer) : Shader(GL_VERTEX_SHADER, source)

class FragmentShader(source: ByteBuffer) : Shader(GL_FRAGMENT_SHADER, source)

class ShaderProgram(vertexShader: VertexShader, fragmentShader: FragmentShader) : GraphicsHandle {
    override val id: Int = glCreateProgram();

    init {
        glAttachShader(id, vertexShader.id)
        glAttachShader(id, fragmentShader.id)
        glLinkProgram(id)

        if (glGetProgrami(id, GL_LINK_STATUS) != GL_TRUE) {
            throw RuntimeException("Failed to link shaders: ${glGetProgramInfoLog(id)}")
        }
    }

    override fun close() {
        glDeleteProgram(id)
    }

    fun activate() {
        glUseProgram(id)
    }
}

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
    }
}

interface Vertex {
    fun writeIntoBuffer(buffer: ByteBuffer);
}

class VectorVertex(val vector: Vector) : Vertex {
    override fun writeIntoBuffer(buffer: ByteBuffer) {
        buffer.putFloat(vector.x);
        buffer.putFloat(vector.y);
        buffer.putFloat(vector.z);
    }
}

enum class GlDataType(val value: kotlin.Int, val size: kotlin.Int, val normalized: Boolean = false) {
    Byte(GL11.GL_BYTE, 1),
    UByte(GL11.GL_UNSIGNED_BYTE, 1),
    Short(GL11.GL_SHORT, 2),
    UShort(GL11.GL_UNSIGNED_SHORT, 2),
    Int(GL11.GL_INT, 4),
    UInt(GL11.GL_UNSIGNED_INT, 4),
    Float(GL11.GL_FLOAT, 4),
    NFloat(GL11.GL_FLOAT, 4, normalized = true),
    Double(GL11.GL_DOUBLE, 8),
    NDouble(GL11.GL_DOUBLE, 8, normalized = true),
    Bytes2(GL11.GL_2_BYTES, 2),
    Bytes3(GL11.GL_3_BYTES, 3),
    Bytes4(GL11.GL_4_BYTES, 4),
}

enum class GlIndicesMode(val mode: Int) {
    Points(GL_POINTS),
    Lines(GL_LINES),
    LineStrip(GL_LINE_STRIP),
    LineLoop(GL_LINE_LOOP),
    LinesAdjacency(GL_LINES_ADJACENCY),
    LineStripAdjacency(GL_LINE_STRIP_ADJACENCY),
    Triangles(GL_TRIANGLES),
    TriangleStrip(GL_TRIANGLE_STRIP),
    TriangleFan(GL_TRIANGLE_FAN),
    TrianglesAdjacency(GL_TRIANGLES_ADJACENCY),
    TriangleStripAdjacency(GL_TRIANGLE_STRIP_ADJACENCY),
}

abstract class VertexLayout<VertexType : Vertex> {
    protected abstract val attributes: Sequence<VertexAttribute>
    private val stride: Int
        get() = attributes.sumBy { entry -> entry.size }

    // Responsible for configuring the vertex array buffer slots
    fun uploadAttributeLayout() {
        // Cache in case the implementation
        val attributes = this.attributes
        var offset: Long = 0;
        attributes.forEachIndexed { index, vertexAttribute ->
            offset += vertexAttribute.size
            glVertexAttribPointer(
                index,
                vertexAttribute.itemCount,
                vertexAttribute.dataType.value,
                vertexAttribute.dataType.normalized,
                stride,
                offset
            )
            // Apparently we can also directly pass the data
        }
    }

    fun buildDataBuffer(vertices: Collection<VertexType>): ByteBuffer {
        val stride = stride
        val buffer = ByteBuffer.allocateDirect(stride * vertices.size)
        vertices.forEachIndexed { index, vertex ->
            val posBefore = buffer.position()
            vertex.writeIntoBuffer(buffer)
            val posAfter = buffer.position()
            val posDelta = posAfter - posBefore
            check(posDelta == stride) { "Incorrect number of bytes written to buffer by element, stride: $stride, actual: $posDelta" }
        }
        return buffer
    }

    protected class VertexAttribute(val dataType: GlDataType, val itemCount: Int) {
        val size: Int = dataType.size * itemCount;
    }
}

class Mesh<T : Vertex>(
    private val layout: VertexLayout<T>,
    vertices: Collection<T>,
    private val indexLayout: GlIndicesMode,
    triangles: IntArray
) : GraphicsHandle {
    override val id: Int = glGenVertexArrays()
    private val vertexBufferId: Int = glGenBuffers()
    private val elementsBufferId: Int = glGenBuffers()
    private val indicesLength: Int = triangles.size

    init {
        glBindVertexArray(id)
        layout.uploadAttributeLayout()
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId)
        glBufferData(GL_ARRAY_BUFFER, layout.buildDataBuffer(vertices), GL_STATIC_DRAW)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementsBufferId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, triangles, GL_STATIC_DRAW)

        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun render() {
        glBindVertexArray(id)
        glDrawElements(indexLayout.mode, indicesLength, GL_UNSIGNED_INT, 0)
    }

    override fun close() {
        glDeleteVertexArrays(id)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
