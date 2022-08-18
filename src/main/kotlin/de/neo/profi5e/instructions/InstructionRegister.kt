package de.neo.profi5e.instructions

import de.neo.profi5e.Profi5EDecompiler
import de.neo.profi5e.extensions.prefixedHexString

object InstructionRegister {

    private val instructions = HashMap<UByte, AsmInstruction>()

    var instructionCount = 0
        private set

    fun register(instruction: AsmInstruction) {
        instructions[instruction.opcode] = instruction
        instructionCount++
    }

    fun get(opcode: UByte): AsmInstruction {
        return instructions[opcode]
            ?: if (Profi5EDecompiler.cmd.hasOption("aih")) {
                AsmInstruction(opcode, "*${opcode.prefixedHexString(2)}", listOf())
            }else {
                throw IllegalArgumentException("No instruction for opcode $opcode")
            }
    }

}