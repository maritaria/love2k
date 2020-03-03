package engine.resources

import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack.stackPush
import java.nio.ByteBuffer

abstract class Shader(val id: Int) : AutoCloseable {
    val isCompiled: Boolean
        get() {
            return glGetShaderi(id, GL_COMPILE_STATUS) == GL_TRUE
        }
    val compileLog: String
        get() {
            val logLength = glGetShaderi(id, GL_INFO_LOG_LENGTH)
            return if (logLength > 0) {
                glGetShaderInfoLog(id)
            } else {
                ""
            }
        }

    fun compile(source: ByteBuffer) {
        stackPush().use { stack ->
            glShaderSource(
                id,
                stack.pointers(source),
                stack.ints(source.remaining())
            )
        }
        glCompileShader(id)
        // Check for compile failure and signal on failure
        if (!isCompiled) {
            throw RuntimeException("Failed to compile shader: " + compileLog)
        }
    }

    override fun close() {
        glDeleteShader(id)
    }
}

class VertexShader(id: Int) : Shader(id) {
    constructor() : this(glCreateShader(GL_VERTEX_SHADER))
}

abstract class ShaderLoader<T : Shader>(val files: ResourceManager<ByteBuffer>) : ResourceLoader<T> {
    override fun load(resource: String): T {
        val source = files.load(resource)
        val shader = createShader()
        shader.compile(source)
        return shader
    }

    abstract fun createShader(): T
}

class VertexShaderLoader(files: ResourceManager<ByteBuffer>) : ShaderLoader<VertexShader>(files) {
    override fun createShader(): VertexShader {
        return VertexShader()
    }
}

class FragmentShader(id: Int) : Shader(id) {
    constructor() : this(glCreateShader(GL_FRAGMENT_SHADER))
}

class FragmentShaderLoader(files: ResourceManager<ByteBuffer>) : ShaderLoader<FragmentShader>(files) {
    override fun createShader(): FragmentShader {
        return FragmentShader()
    }
}

class ShaderProgram(vShader: VertexShader, fShader: FragmentShader) {
    val id: Int = glCreateProgram()
    val isLinked: Boolean
        get() {
            return glGetProgrami(id, GL_LINK_STATUS) == GL_TRUE
        }
    val linkLog: String
        get() {
            val logLength = glGetProgrami(id, GL_INFO_LOG_LENGTH)
            return if (logLength > 0) {
                glGetProgramInfoLog(id)
            } else {
                ""
            }
        }

    init {
        glAttachShader(id, vShader.id);
        glAttachShader(id, fShader.id);
        glLinkProgram(id);
        if (!isLinked) {
            throw IllegalStateException("Failed to link program: " + linkLog);
        }
    }

    fun makeActive() {
        glUseProgram(id)
    }
}
