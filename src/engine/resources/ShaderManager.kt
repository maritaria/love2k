package engine.resources

import java.nio.ByteBuffer

class ShaderManager(files: ResourceManager<ByteBuffer>) {
    val vertexShaders = ResourceManager(VertexShaderLoader(files))
    val fragmentShaders = ResourceManager(FragmentShaderLoader(files))

    fun build(vertexShaderPath: String, fragmentShaderPath: String): ShaderProgram {
        return ShaderProgram(vertexShaders.load(vertexShaderPath), fragmentShaders.load(fragmentShaderPath))
    }
}