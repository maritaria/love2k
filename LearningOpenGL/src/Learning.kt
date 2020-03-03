import ModelVertex.Layout
import love2k.*
import love2k.GlDataType.GlFloat
import love2k.GlIndicesMode.Triangles
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.ByteBuffer

fun main() {
    glfwInit()
    try {
        glfwDefaultWindowHints()

        val window = glfwCreateWindow(800, 600, "LearnOpenGL", NULL, NULL)
        if (window == NULL) {
            throw RuntimeException("Failed to create window")
        }

        glfwMakeContextCurrent(window)
        glfwSetFramebufferSizeCallback(window, ::handleFramebufferResize)

        val caps = GL.createCapabilities()
        println("Supports direct state: ${caps.GL_ARB_direct_state_access}");

        println("-- OpenGL Driver info --")
        println("Version: ${glGetString(GL_VERSION)}")
        println("GLSL: ${glGetString(GL_SHADING_LANGUAGE_VERSION)}")
        println("Vendor: ${glGetString(GL_VENDOR)}")
        println("Renderer: ${glGetString(GL_RENDERER)}")

        println("Extensions:")
        for (index in 0 until glGetInteger(GL_NUM_EXTENSIONS)) {
            println("- ${glGetStringi(GL11.GL_EXTENSIONS, index)}")
        }

        glViewport(0, 0, 800, 600)

        // Prepare render
        val triangle = setupTriangle()
        val shader = setupShader()
        val texture = setupTexture()

        while (!glfwWindowShouldClose(window)) {
            glClearColor(0.2f, 0.3f, 0.3f, 1.0f)
            glClear(GL_COLOR_BUFFER_BIT)

            glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
            // Perform render
            shader.activate()
            glBindTexture(GL_TEXTURE_2D, texture.id);
            triangle.render()

            glfwSwapBuffers(window)
            glfwPollEvents()
        }
    } finally {
        glfwTerminate()
    }
}

fun handleFramebufferResize(window: Long, width: Int, height: Int) {
    glViewport(0, 0, width, height)
}

class ModelVertex(val pos: Vector, val color: Vector, val uv: Vector) : Vertex {
    constructor(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, u: Float, v: Float)
            : this(Vector(x, y, z), Vector(r, g, b), Vector(u, v, 0f))

    override fun writeIntoBuffer(buffer: ByteBuffer) {
        buffer.putFloat(pos.x)
        buffer.putFloat(pos.y)
        buffer.putFloat(pos.z)
        buffer.putFloat(color.x)
        buffer.putFloat(color.y)
        buffer.putFloat(color.z)
        buffer.putFloat(uv.x)
        buffer.putFloat(uv.y)
    }

    class Layout : VertexLayout<ModelVertex>() {
        override val attributes: Sequence<VertexAttribute>
            get() = listOf(
                VertexAttribute(GlFloat, 3),
                VertexAttribute(GlFloat, 3),
                VertexAttribute(GlFloat, 2)
            ).asSequence()
    }
}

fun setupTriangle(): Mesh<ModelVertex> {
    return Mesh(
        Layout(),
        listOf(
            ModelVertex(0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f),
            ModelVertex(0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f),
            ModelVertex(-0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f),
            ModelVertex(-0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f)
        ),
        // TODO: Replace with (triangle) builder pattern
        Triangles,
        intArrayOf(
            0, 1, 3,   // first triangle
            1, 2, 3    // second triangle
        )
    )
}

fun setupShader(): ShaderProgram {
    val v = VertexShader(loadResource("hello-image.v.glsl"))
    val f = FragmentShader(loadResource("hello-image.f.glsl"))
    return ShaderProgram(v, f)
}

fun setupTexture(): Texture2D {
    return Texture2D(loadResource("ball.png"))
}
