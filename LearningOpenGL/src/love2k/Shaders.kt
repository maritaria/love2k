package love2k

import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

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
    override val id: Int = glCreateProgram()

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