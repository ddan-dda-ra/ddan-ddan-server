package notbe.tmtm.ddanddanserver.domain.model.version

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {

    companion object {
        fun parse(version: String): SemanticVersion {
            val regex = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")
            val matchResult = regex.matchEntire(version.trim())
                ?: throw IllegalArgumentException("Invalid semantic version format: $version. Expected format: x.y.z")

            val (major, minor, patch) = matchResult.destructured
            return SemanticVersion(
                major = major.toInt(),
                minor = minor.toInt(),
                patch = patch.toInt()
            )
        }
    }

    override fun compareTo(other: SemanticVersion): Int {
        return when {
            this.major != other.major -> this.major.compareTo(other.major)
            this.minor != other.minor -> this.minor.compareTo(other.minor)
            else -> this.patch.compareTo(other.patch)
        }
    }

    override fun toString(): String = "$major.$minor.$patch"
}
