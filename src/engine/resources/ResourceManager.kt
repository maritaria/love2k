package engine.resources

class ResourceManager<T>(private val loader: ResourceLoader<T>) {
    private val loaded: Map<String, T> = mutableMapOf()

    fun load(path: String): T {
        return loaded.getOrElse(path, { loader.load(path) });
    }
}
