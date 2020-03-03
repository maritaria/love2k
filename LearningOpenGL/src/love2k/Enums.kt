package love2k

import org.lwjgl.opengl.GL33

enum class GlDataType(val value: Int, val size: Int, val normalized: Boolean = false) {
    GlByte(GL33.GL_BYTE, 1),
    GlUByte(GL33.GL_UNSIGNED_BYTE, 1),
    GlShort(GL33.GL_SHORT, 2),
    GlUShort(GL33.GL_UNSIGNED_SHORT, 2),
    GlInt(GL33.GL_INT, 4),
    GlUInt(GL33.GL_UNSIGNED_INT, 4),
    GlFloat(GL33.GL_FLOAT, 4),
    GlFloatNormalized(GL33.GL_FLOAT, 4, normalized = true),
    GlDouble(GL33.GL_DOUBLE, 8),
    GlDoubleNormalized(GL33.GL_DOUBLE, 8, normalized = true),
    GlBytes2(GL33.GL_2_BYTES, 2),
    GlBytes3(GL33.GL_3_BYTES, 3),
    GlBytes4(GL33.GL_4_BYTES, 4),
}

enum class GlIndicesMode(val mode: Int) {
    Points(GL33.GL_POINTS),
    Lines(GL33.GL_LINES),
    LineStrip(GL33.GL_LINE_STRIP),
    LineLoop(GL33.GL_LINE_LOOP),
    LinesAdjacency(GL33.GL_LINES_ADJACENCY),
    LineStripAdjacency(GL33.GL_LINE_STRIP_ADJACENCY),
    Triangles(GL33.GL_TRIANGLES),
    TriangleStrip(GL33.GL_TRIANGLE_STRIP),
    TriangleFan(GL33.GL_TRIANGLE_FAN),
    TrianglesAdjacency(GL33.GL_TRIANGLES_ADJACENCY),
    TriangleStripAdjacency(GL33.GL_TRIANGLE_STRIP_ADJACENCY),
}
