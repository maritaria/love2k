import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil.NULL

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
            glBindVertexArray(triangle)
            glBindTexture(GL_TEXTURE_2D, texture.id);
            // glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

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

fun setupTriangle(): Int {
    // Triangle data
    val vertices = floatArrayOf(
        // positions          // colors           // texture coords
        0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,   // top right
        0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f,   // bottom right
        -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,   // bottom left
        -0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f    // top left
    )
    val indices = intArrayOf(
        0, 1, 3,   // first triangle
        1, 2, 3    // second triangle
    )
    // VAO
    val vao = glGenVertexArrays()
    glBindVertexArray(vao)
    // VBO
    val vbo = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)


    // Vertex buffer layout
    glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * 4, 0)
    glEnableVertexAttribArray(0)
    glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * 4, 3 * 4)
    glEnableVertexAttribArray(1)
    glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * 4, 6 * 4)
    glEnableVertexAttribArray(2)


    val ebo = glGenBuffers()
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
    // glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

    glBindVertexArray(0)
    glDisableVertexAttribArray(2)
    glBindBuffer(GL_ARRAY_BUFFER, 0)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    return vao
}

fun setupShader(): ShaderProgram {
    val v = VertexShader(loadResource("hello-image.v.glsl"))
    val f = FragmentShader(loadResource("hello-image.f.glsl"))
    return ShaderProgram(v, f)
}

fun setupTexture(): Texture2D {
    return Texture2D(loadResource("ball.png"))
}
