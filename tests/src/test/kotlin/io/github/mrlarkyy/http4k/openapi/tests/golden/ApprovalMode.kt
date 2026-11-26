package io.github.mrlarkyy.http4k.openapi.tests.golden

/** Whether a golden run compares against approved files or rewrites them. */
enum class ApprovalMode {
    Verify,
    Approve,
    ;

    companion object {
        const val PROPERTY = "h4oa.golden.approve"

        private val CI_FALSE = setOf("", "false", "0")

        /**
         * Resolves the mode from the approval property and the `CI` environment value. Approval is refused when `CI`
         * is set to anything other than empty, `false` or `0`.
         */
        fun resolve(
            approve: String?,
            ci: String?,
        ): ApprovalMode {
            if (approve?.trim()?.lowercase() != "true") return Verify
            if (ci != null && ci.trim().lowercase() !in CI_FALSE) {
                throw IllegalStateException(
                    "Approving golden files is refused on CI (CI=$ci); approve locally and commit the files",
                )
            }
            return Approve
        }
    }
}
