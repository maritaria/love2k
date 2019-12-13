package engine

import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI
import org.lwjgl.glfw.GLFWWindowSizeCallbackI
import org.lwjgl.system.MemoryStack.stackPush


/**
 * Invokes the specified callbacks using the current window and framebuffer sizes of the specified GLFW window.
 * Source: https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/glfw/GLFWUtil.java
 *
 * @param window            the GLFW window
 * @param windowSizeCB      the window size callback, may be null
 * @param framebufferSizeCB the framebuffer size callback, may be null
 */
fun glfwInvokeWithInitial(
    window: Long,
    windowSizeCB: GLFWWindowSizeCallbackI?,
    framebufferSizeCB: GLFWFramebufferSizeCallbackI?
) {
    stackPush().use { stack ->
        val w = stack.mallocInt(1)
        val h = stack.mallocInt(1)

        if (windowSizeCB != null) {
            glfwGetWindowSize(window, w, h)
            windowSizeCB.invoke(window, w.get(0), h.get(0))
        }

        if (framebufferSizeCB != null) {
            glfwGetFramebufferSize(window, w, h)
            framebufferSizeCB.invoke(window, w.get(0), h.get(0))
        }
    }

}