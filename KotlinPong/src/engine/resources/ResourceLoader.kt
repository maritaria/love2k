package engine.resources

interface ResourceLoader<T> {
    fun load(resource: String): T
}