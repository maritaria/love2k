import GlDataType.GlFloat
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
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
    constructor(x: Float, y: Float, z: Float) : this(Vector(x, y, z))

    override fun writeIntoBuffer(buffer: ByteBuffer) {
        buffer.putFloat(vector.x);
        buffer.putFloat(vector.y);
        buffer.putFloat(vector.z);
    }

    class Layout : VertexLayout<VectorVertex>() {
        override val attributes: Sequence<VertexAttribute>
            get() = listOf(VertexAttribute(GlFloat, 3)).asSequence()

    }
}

enum class GlDataType(val value: Int, val size: Int, val normalized: Boolean = false) {
    GlByte(GL_BYTE, 1),
    GlUByte(GL_UNSIGNED_BYTE, 1),
    GlShort(GL_SHORT, 2),
    GlUShort(GL_UNSIGNED_SHORT, 2),
    GlInt(GL_INT, 4),
    GlUInt(GL_UNSIGNED_INT, 4),
    GlFloat(GL_FLOAT, 4),
    GlFloatNormalized(GL_FLOAT, 4, normalized = true),
    GlDouble(GL_DOUBLE, 8),
    GlDoubleNormalized(GL_DOUBLE, 8, normalized = true),
    GlBytes2(GL_2_BYTES, 2),
    GlBytes3(GL_3_BYTES, 3),
    GlBytes4(GL_4_BYTES, 4),
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
    val attributeCount: Int
        get() = attributes.count()
    private val stride: Int
        get() = attributes.sumBy { entry -> entry.size }

    // Responsible for configuring the vertex array buffer slots
    fun uploadAttributeLayout() {
        // Cache in case the implementation
        val attributes = this.attributes
        var offset: Long = 0;
        attributes.forEachIndexed { index, vertexAttribute ->
            glVertexAttribPointer(
                index,
                vertexAttribute.itemCount,
                vertexAttribute.dataType.value,
                vertexAttribute.dataType.normalized,
                stride,
                offset
            )
            glEnableVertexAttribArray(index)
            offset += vertexAttribute.size
            // Apparently we can also directly pass the data
        }
    }

    fun buildDataBuffer(vertices: Collection<VertexType>): ByteBuffer {
        val stride = stride
        val buffer = memAlloc(stride * vertices.size)
        vertices.forEach { vertex ->
            val posBefore = buffer.position()
            vertex.writeIntoBuffer(buffer)
            val posAfter = buffer.position()
            val posDelta = posAfter - posBefore
            check(posDelta == stride) { "Incorrect number of bytes written to buffer by element, stride: $stride, actual: $posDelta" }
        }
        return buffer.flip()
    }

    protected class VertexAttribute(val dataType: GlDataType, val itemCount: Int) {
        val size: Int = dataType.size * itemCount;
    }
}

class Mesh<T : Vertex>(
    private val layout: VertexLayout<T>,
    vertices: Collection<T>,
    private val indexLayout: GlIndicesMode,
    indices: IntArray
) : GraphicsHandle {
    override val id: Int = glGenVertexArrays()
    private val vertexBufferId: Int = glGenBuffers()
    private val elementsBufferId: Int = glGenBuffers()
    private val indicesLength: Int = indices.size

    init {
        glBindVertexArray(id)
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId)
        glBufferData(GL_ARRAY_BUFFER, layout.buildDataBuffer(vertices), GL_STATIC_DRAW)
        layout.uploadAttributeLayout()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementsBufferId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)

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
