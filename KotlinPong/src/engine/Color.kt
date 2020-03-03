package engine

data class Color(val red: Float, val green: Float, val blue: Float, val alpha: Float) {
    constructor(red: Float, green: Float, blue: Float) : this(red, green, blue, 0f)
    constructor(original: Color, alpha: Float) : this(original.red, original.green, original.blue, alpha)

    fun toArray(): FloatArray {
        return arrayOf(red, green, blue, alpha).toFloatArray()
    }

    companion object {
        val White: Color = Color(1f, 1f, 1f)
    }
}