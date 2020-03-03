package love2k

class Vector(var x: Float, var y: Float, var z: Float) {
    constructor(x: Float, y: Float) : this(x, y, 0.0f)
    constructor(l: Float) : this(l, l, l)

    operator fun plus(l: Float): Vector {
        return Vector(this.x + l, this.y + l, this.z + l)
    }

    operator fun plus(vec: Vector): Vector {
        return Vector(this.x + vec.x, this.y + vec.y, this.z + vec.z)
    }

    operator fun minus(l: Float): Vector {
        return this + (-l)
    }

    operator fun unaryMinus(): Vector {
        return Vector(-this.x, -this.y, -this.z)
    }

    operator fun times(l: Float): Vector {
        return Vector(this.x * l, this.y * l, this.z * l)
    }

    operator fun times(vec: Vector): Vector {
        return Vector(this.x * vec.x, this.y * vec.y, this.z * vec.z)
    }

    operator fun div(l: Float): Vector {
        return Vector(this.x / l, this.y / l, this.z / z)
    }

    operator fun div(vec: Vector): Vector {
        return Vector(this.x / vec.x, this.y / vec.y, this.z / vec.z)
    }
}