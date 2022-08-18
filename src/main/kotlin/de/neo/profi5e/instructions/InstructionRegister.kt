package de.neo.profi5e.instructions

object InstructionRegister {

    private val instructions = HashMap<UByte, AsmInstruction>()

    var instructionCount = 0
        private set

    fun register(instruction: AsmInstruction) {
        instructions[instruction.opcode] = instruction
        instructionCount++
    }

    fun get(opcode: UByte): AsmInstruction {
        return instructions[opcode] ?: throw IllegalArgumentException("No instruction for opcode $opcode")
    }

}