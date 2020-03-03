package love2k

import love2k.GlDataType.GlFloat
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

interface Vertex {
    fun writeIntoBuffer(buffer: ByteBuffer)
}

class VectorVertex(val vector: Vector) : Vertex {
    constructor(x: Float, y: Float, z: Float) : this(Vector(x, y, z))

    override fun writeIntoBuffer(buffer: ByteBuffer) {
        buffer.putFloat(vector.x)
        buffer.putFloat(vector.y)
        buffer.putFloat(vector.z)
    }

    class Layout : VertexLayout<VectorVertex>() {
        override val attributes: Sequence<VertexAttribute>
            get() = listOf(VertexAttribute(GlFloat, 3)).asSequence()

    }
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
        var offset: Long = 0
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
        val buffer = MemoryUtil.memAlloc(stride * vertices.size)
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
        val size: Int = dataType.size * itemCount
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
        glDeleteBuffers(vertexBufferId)
        glDeleteBuffers(elementsBufferId)
        TODO("Mark instance as invalid with some boolean flag")
    }
}