package de.neo.profi5e.instructions

import de.neo.profi5e.extensions.prefixedHexString

data class Instruction(
    val opcode: UByte,
    val operand: List<UByte>
) {
    override fun toString(): String {
        return "Instruction(opcode=${opcode.prefixedHexString(2)}, " +
                "operand=${operand.joinToString(",") { it.prefixedHexString(2) }})"
    }
}