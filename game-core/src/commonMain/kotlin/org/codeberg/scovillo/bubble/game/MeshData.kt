package org.codeberg.scovillo.bubble.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renderer-neutral triangle data. The arrays use three floats per vertex;
 * normals use three and colours use four floats per vertex when present.
 */
data class MeshData(
    val vertices: FloatArray,
    val normals: FloatArray? = null,
    val colors: FloatArray? = null,
) {
    init {
        require(vertices.size % 3 == 0) { "Vertices must contain x/y/z triples." }
        require(normals == null || normals.size == vertices.size) { "Normals must match vertices." }
        require(colors == null || colors.size == vertexCount * 4) { "Colours must contain r/g/b/a values." }
    }

    val vertexCount: Int get() = vertices.size / 3
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MeshData

        if (!vertices.contentEquals(other.vertices)) return false
        if (!normals.contentEquals(other.normals)) return false
        if (!colors.contentEquals(other.colors)) return false
        if (vertexCount != other.vertexCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vertices.contentHashCode()
        result = 31 * result + (normals?.contentHashCode() ?: 0)
        result = 31 * result + (colors?.contentHashCode() ?: 0)
        result = 31 * result + vertexCount
        return result
    }
}

/** Geometry only: a unit sphere represented as OpenGL triangle strips. */
object BubbleMeshFactory {
    fun create(smoothnessDegrees: Float = 12f): List<MeshData> {
        require(smoothnessDegrees > 0f) { "Smoothness must be positive." }
        val strips = mutableListOf<MeshData>()
        var angleA = -90f
        while (angleA < 90f) {
            val vertices = mutableListOf<Float>()
            val r1 = cos(angleA * PI / 180.0).toFloat()
            val r2 = cos((angleA + smoothnessDegrees) * PI / 180.0).toFloat()
            val h1 = sin(angleA * PI / 180.0).toFloat()
            val h2 = sin((angleA + smoothnessDegrees) * PI / 180.0).toFloat()
            var angleB = 0f
            while (angleB <= 360f) {
                val cosine = cos(angleB * PI / 180.0).toFloat()
                val sine = (-sin(angleB * PI / 180.0)).toFloat()
                vertices += r2 * cosine
                vertices += h2
                vertices += r2 * sine
                vertices += r1 * cosine
                vertices += h1
                vertices += r1 * sine
                angleB += smoothnessDegrees
            }
            // A sphere's unit position vector is also its outward normal.
            strips += MeshData(vertices.toFloatArray(), vertices.toFloatArray())
            angleA += smoothnessDegrees
        }
        return strips
    }
}

/** Geometry only for the layered, extruded star. Materials remain renderer concerns. */
object StarMeshFactory {
    data class Parts(
        val face: MeshData,
        val glow: MeshData,
        val bevel: MeshData,
        val side: MeshData,
        val faceLight: MeshData,
    )

    private const val FACE_SCALE = 0.84f
    private const val FACE_Y = -0.20f
    private const val BEVEL_Y = -0.06f
    private const val BACK_Y = 0.16f
    private val points = arrayOf(
        floatArrayOf(0f, 1f),
        floatArrayOf(-0.5f, 0f),
        floatArrayOf(0.5f, 0f),
        floatArrayOf(-1.5f, 0f),
        floatArrayOf(-0.75f, -0.75f),
        floatArrayOf(-1f, -1.75f),
        floatArrayOf(0f, -1.25f),
        floatArrayOf(1.5f, 0f),
        floatArrayOf(0.75f, -0.75f),
        floatArrayOf(1f, -1.75f),
    )
    private val triangles = intArrayOf(
        0, 1, 2, 1, 3, 4, 4, 5, 6, 8, 6, 9,
        2, 8, 7, 2, 1, 4, 4, 8, 2, 8, 4, 6,
    )
    private val perimeter = intArrayOf(0, 1, 3, 4, 5, 6, 9, 8, 7, 2)

    fun create(): Parts = Parts(
        face = faceMesh(FACE_SCALE, FACE_Y),
        glow = faceMesh(1f, BACK_Y + 0.01f),
        bevel = edgeMesh(BEVEL_Y, FACE_Y, 1f, FACE_SCALE, -0.72f),
        side = edgeMesh(BACK_Y, BEVEL_Y, 1f, 1f, 0f),
        faceLight = faceLightMesh(),
    )

    private fun faceMesh(scale: Float, y: Float): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        triangles.forEach { index ->
            vertices += point(index, scale, y).asList()
            normals += listOf(0f, -1f, 0f)
        }
        return MeshData(vertices.toFloatArray(), normals.toFloatArray())
    }

    private fun faceLightMesh(): MeshData {
        val vertices = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        forEachEdge { start, end ->
            vertices += listOf(0f, FACE_Y - 0.01f, -0.34f)
            colors += listOf(1f, .82f, .30f, .42f)
            vertices += point(start, FACE_SCALE, FACE_Y - .01f).asList()
            colors += listOf(.34f, .12f, 0f, 0f)
            vertices += point(end, FACE_SCALE, FACE_Y - .01f).asList()
            colors += listOf(.34f, .12f, 0f, 0f)
        }
        return MeshData(vertices.toFloatArray(), colors = colors.toFloatArray())
    }

    private fun edgeMesh(
        firstY: Float,
        secondY: Float,
        firstScale: Float,
        secondScale: Float,
        normalY: Float,
    ): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        forEachEdge { start, end ->
            addQuad(
                vertices, normals,
                point(start, firstScale, firstY),
                point(end, firstScale, firstY),
                point(end, secondScale, secondY),
                point(start, secondScale, secondY),
                edgeNormal(start, end, normalY),
            )
        }
        return MeshData(vertices.toFloatArray(), normals.toFloatArray())
    }

    private inline fun forEachEdge(block: (Int, Int) -> Unit) {
        perimeter.indices.forEach { index ->
            block(perimeter[index], perimeter[(index + 1) % perimeter.size])
        }
    }

    private fun point(index: Int, scale: Float, y: Float) =
        floatArrayOf(points[index][0] * scale, y, points[index][1] * scale)

    private fun edgeNormal(start: Int, end: Int, normalY: Float): FloatArray {
        val x = points[end][1] - points[start][1]
        val z = points[start][0] - points[end][0]
        val length = sqrt(x * x + normalY * normalY + z * z)
        return floatArrayOf(x / length, normalY / length, z / length)
    }

    private fun addQuad(
        vertices: MutableList<Float>,
        normals: MutableList<Float>,
        first: FloatArray,
        second: FloatArray,
        third: FloatArray,
        fourth: FloatArray,
        normal: FloatArray,
    ) {
        arrayOf(first, second, third, first, third, fourth).forEach { vertex ->
            vertices += vertex.asList()
            normals += normal.asList()
        }
    }
}
