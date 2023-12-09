data class UserData(
    val phone: String,
    val userId: Long
) {
    companion object {
        const val FIELDS_COUNT = 2
    }

    override fun toString(): String = listOf(phone, userId.toString()).joinToString("\t")
}
