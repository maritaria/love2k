package engine

import game.Constants
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI
import org.lwjgl.glfw.GLFWWindowSizeCallbackI
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.MemoryUtil.NULL

data class KeyEvent(val key: Int, val scancode: Int, val action: Int, val modifiers: Int)

class GameWindow(val gameFactory: (GameWindow) -> Game) {
    private var window: Long = NULL
    private lateinit var gameInstance: Game;
    var width: Int = 0
        private set
    var height: Int = 0
        private set

    fun run() {
        try {
            init()
            loop()
            // Destroy window
            glfwDestroyWindow(window)
        } finally {
            // Terminate GLFW
            glfwTerminate()
        }
    }

    private fun init() {

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err))


        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        // Configure our window
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)

        // Create the window
        window = glfwCreateWindow(Constants.WindowWidth, Constants.WindowHeight, "Hello World!", NULL, NULL)
        if (window == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        // Setup callbacks
        // TODO: Free callbacks on window close
        glfwSetKeyCallback(window, this::onKey)
        glfwSetWindowRefreshCallback(window, this::render)
        glfwSetWindowSizeCallback(window, this::onWindowResize)
        glfwSetFramebufferSizeCallback(window, this::onFramebufferSizeChanged)

        // Get the resolution of the primary monitor
        val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())

        // Center our window
        if (videoMode != null) {
            glfwSetWindowPos(
                window,
                (videoMode.width() - Constants.WindowWidth) / 2,
                (videoMode.height() - Constants.WindowHeight) / 2
            )
        }
        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        // Not calling this function results in no openGL context for any GL invokes
        GL.createCapabilities()
        initFeatures()
        // Enable v-sync
        glfwSwapInterval(1)
        // Make the window visible
        glfwShowWindow(window)
        // Invoke the callbacks with the initial sizes
        glfwInvokeWithInitial(
            window,
            GLFWWindowSizeCallbackI(this::onWindowResize),
            GLFWFramebufferSizeCallbackI(this::onFramebufferSizeChanged)
        )
        // Framework setup completed, boot game instance
        gameInstance = gameFactory(this)
        gameInstance.setup()
    }

    private fun initFeatures() {
        GLUtil.setupDebugMessageCallback()
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun loop() {
        // Set the clear color
        val bgColor = Constants.BackgroundColor;
        glClearColor(bgColor.red, bgColor.green, bgColor.blue, bgColor.alpha)
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            update()
            render(window)
        }
    }

    private fun update() {
        // Poll for window events. The key callback above will only be
        // invoked during this call.
        glfwPollEvents()
        // Game update tick logic
        gameInstance.update()
    }

    private fun render(window: Long) {
        // Clear the framebuffer
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        // Game render logic
        gameInstance.render()
        // Swap the color buffers
        glfwSwapBuffers(window)
    }

    private fun onFramebufferSizeChanged(window: Long, width: Int, height: Int) {
        println("Framebuffer size changed")
        glViewport(0, 0, width, height)
    }

    private fun onWindowResize(window: Long, width: Int, height: Int) {
        println("Window resized")
        this.width = width
        this.width = height

//        glMatrixMode(GL_PROJECTION)
//        glLoadIdentity()
//        glOrtho(0.0, width.toDouble(), height.toDouble(), 0.0, -1.0, 1.0)
//        glMatrixMode(GL_MODELVIEW)
    }

    private fun onKey(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        val event = KeyEvent(key, scancode, action, mods)
        if (event.key == GLFW_KEY_ESCAPE && event.action == GLFW_RELEASE) {
            close()
        }
        gameInstance.onKey(event)
    }

    fun close() {
        glfwSetWindowShouldClose(this.window, true)
    }
}