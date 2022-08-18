package de.neo.profi5e.instructions

import java.lang.IllegalArgumentException

data class AsmInstruction(
    val opcode: UByte,
    val name: String,
    val args: List<AsmArg>,
)

enum class AsmArg {
    A, B, C, D, E, H, L, M, F,
    KO, KA, ADR, SP,
    INDEX_0, INDEX_1, INDEX_2, INDEX_3, INDEX_4, INDEX_5, INDEX_6, INDEX_7;

    companion object {
        fun fromString(arg: Any): AsmArg? {
            return fromString(arg as String)
        }

        private fun fromString(arg: String): AsmArg? {
            val asmArg = try {
                AsmArg.valueOf(arg.uppercase())
            }catch (_: IllegalArgumentException) {
                try {
                    AsmArg.valueOf("INDEX_$arg")
                }catch (_: IllegalArgumentException) {
                    null
                }
            }
            return asmArg
        }
    }
}