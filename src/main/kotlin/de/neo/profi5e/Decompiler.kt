package de.neo.profi5e

import de.neo.profi5e.extensions.getValue
import de.neo.profi5e.extensions.prefixedHexString
import de.neo.profi5e.instructions.AsmArg
import de.neo.profi5e.instructions.Instruction
import de.neo.profi5e.instructions.InstructionRegister
import java.lang.Integer.max
import java.nio.file.Files
import java.nio.file.Path
import java.util.SortedMap

class Decompiler(private val content: List<UByte>) {

    var defaultCursorAddress = 0x8000

    var cursorAddress = 0x00
    var cursor = 0

    var line = 0
    private val lineRegister = object : HashMap<Int, Int>() { // address -> line number
        override operator fun get(key: Int): Int { // if not found, return line to insert at
            if (!containsKey(key)) {
                val temp = mutableMapOf<Int, Int>()
                for ((index, address) in keys.withIndex()) {
                    if (address > key) {
                        temp[key] = index
                        break
                    }
                }
                if(!temp.containsKey(key)) {
                    put(key, keys.indexOf(keys.max()))
                }
                putAll(temp)
            }
            return super.get(key)!!
        }
    }
    private val lineInjections = HashMap<Int, String>() // line number -> injection

    fun decompileContent(): List<Pair<Instruction?, String>> {
        cursor = 0
        cursorAddress = defaultCursorAddress
        val result = ArrayList<Pair<Instruction?, String>>()
        while (cursor < content.size) {
            val oldCursor = cursor
            val instruction = parseNextLine()
            val n0x = Profi5EDecompiler.cmd.hasOption("n0x")
            val asm = if (n0x) decompileInstruction(instruction).replace("0x", "") else decompileInstruction(instruction)
            val hex = if(Profi5EDecompiler.cmd.hasOption("x")) {
                if (n0x) {
                    (" ".repeat(max(20 - asm.length, 1))
                            + "; ${instruction.opcode.prefixedHexString(2)} "
                            + instruction.operand.joinToString(" ") { it.prefixedHexString(2) })
                }else {
                    (" ".repeat(max(20 - asm.length, 1))
                            + "; 0x${instruction.opcode.prefixedHexString(2)} "
                            + instruction.operand.joinToString(" ") { "0x${it.prefixedHexString(2)}" })
                }
            }else ""
            result.add(Pair(instruction, asm + hex))
            lineRegister[cursorAddress] = line++
            cursorAddress += (cursor - oldCursor)
        }
        var lineOffset = 0
        for (injection in lineInjections) {
            logVeryVerbose("${cursorAddress - cursor} ${injection.key} ${injection.key - (cursorAddress - cursor)}")
            injection.value.split("\n")
                .filter { it.isNotBlank() }
                .forEach {
                    result.add(injection.key + lineOffset++, Pair(null, it))
                }
        }
        return result
    }

    fun decompileInstruction(instruction: Instruction): String {
        val asm = InstructionRegister.get(instruction.opcode)
        var start = 0
        return asm.name + " " + asm.args.joinToString(",") { arg ->
            if (Profi5EDecompiler.cmd.hasOption("vv")) println("arg: $arg start: $start instruction: $instruction")
            val pair = decompileArg(arg, start, instruction)
            start += pair.first
            pair.second
        }
    }

    private fun decompileArg(arg: AsmArg, start: Int, instruction: Instruction): Pair<Int, String> {
        return when (arg) {
            AsmArg.KO -> Pair(1, instruction.operand[start].prefixedHexString(2))
            AsmArg.KA -> Pair(1, instruction.operand[start].prefixedHexString(2))
            AsmArg.ADR -> {
                val address = instruction.operand[start + 1].prefixedHexString(2) + instruction.operand[start].prefixedHexString(2)
                val labelName = "@label_${address.toInt(16).prefixedHexString(4)}"
                val injection = if (compareAddressToMemoryRange(address.toInt(16)) == 0) labelName
                else "$address:\n$labelName\n${defaultCursorAddress.prefixedHexString(4)}:\n"
                val injectionAddress = lineRegister[address.toInt(16)]
                if (!lineInjections.containsKey(injectionAddress)
                    || !lineInjections[injectionAddress]!!.contains(injection)) {
                    lineInjections[injectionAddress] = (lineInjections[injectionAddress] ?: "") + injection
                }
                Pair(2, labelName)
            }
            else -> Pair(0, arg.name.lowercase())
        }
    }

    private fun parseNextLine(): Instruction {
        val opcode = content[cursor++]
        val len = probeInstructionArgLength(opcode)
        val args = mutableListOf<UByte>()
        if (len > 0) {
            for (i in 0 until len) {
                if (Profi5EDecompiler.cmd.hasOption("vv")) println("$opcode $i $cursor")
                args.add(content[cursor++])
            }
        }
        return Instruction(opcode, args)
    }

    private fun probeInstructionArgLength(inst: UByte): Int {
        var len = 0
        val args = InstructionRegister.get(inst).args
        for (i in args.indices) {
            when (args[i]) {
                AsmArg.KO -> len++
                AsmArg.KA -> len++
                AsmArg.ADR -> len += 2
                else -> {}
            }
        }
        if (Profi5EDecompiler.cmd.hasOption("vv")) println("len: $len")
        return len
    }

    private fun compareAddressToMemoryRange(address: Int): Int {
        val cli = Profi5EDecompiler.cmd
        val offset = cli.getValue("off", 0x8000)
        return if (address >= offset && address <= offset + content.size) {
            0 // address is in range
        }else {
            if (address < offset) {
                -1 // address is below range
            }else {
                1 // address is above range
            }
        }
    }

    private fun logVeryVerbose(msg: String) {
        if (Profi5EDecompiler.cmd.hasOption("vv")) println(msg)
    }


    companion object {
        fun getDecompiler(path: Path, offset: Int = -1, length: Int = -1, textMode: Boolean = false): Decompiler {
            var bytes = if (textMode) {
                Files.readString(path)
                    .filter { !it.isWhitespace() }
                    .chunked(2)
                    .filter { it != "0x" }
                    .map { it.toUByte(16) }
            }else Files.readAllBytes(path)
                .toMutableList()
                .map { it.toUByte() }
            bytes = bytes.filterIndexed { i, _ ->
                val off = if (offset == -1) 0x00 else offset    // set auto offset to 0x00
                val len = if (length == -1) 0x87ff else length  // set auto length to 0x87ff (max binary size for Profi-5E)
                if (i < off) false else i < off + len
            }
            return Decompiler(bytes)
        }
    }

}