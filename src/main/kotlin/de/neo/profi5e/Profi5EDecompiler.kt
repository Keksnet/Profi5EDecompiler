package de.neo.profi5e

import de.neo.profi5e.extensions.getValue
import de.neo.profi5e.extensions.prefixedHexString
import de.neo.profi5e.extensions.replaceFileExtension
import de.neo.profi5e.instructions.AsmArg
import de.neo.profi5e.instructions.AsmInstruction
import de.neo.profi5e.instructions.InstructionRegister
import org.apache.commons.cli.*
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class Profi5EDecompiler {

    companion object {
        var cmd = CommandLine.Builder().build()
    }

}

fun loadInstructions() {
    val inputStream = Profi5EDecompiler::class.java.getResourceAsStream("/instructions.json")
        ?: throw IllegalStateException("instructions.json not found")
    val instructions = JSONObject((String(inputStream.readAllBytes(), StandardCharsets.UTF_8)))
    inputStream.close()

    instructions.keys().forEach { code ->
        val jsonInstructions = instructions.getJSONObject(code)
        val opcode = code.toUByte(16)
        val name = jsonInstructions.getString("name")
        val args = jsonInstructions.getJSONArray("args")
            .mapNotNull { AsmArg.fromString(it) }
        InstructionRegister.register(AsmInstruction(opcode, name, args))
    }
}

fun main(args: Array<String>) {
    val cliParser = DefaultParser()

    val options = Options()

    options
        .addOption(
            Option.builder()
                .option("in")
                .longOpt("input")
                .required(true)
                .hasArg(true)
                .optionalArg(false)
                .argName("binary")
                .desc("binary input file (required)")
                .build())
    options
        .addOption(
            Option.builder()
                .option("out")
                .longOpt("output")
                .required(false)
                .hasArg(true)
                .optionalArg(false)
                .argName("asm")
                .desc("assembly output file (default is input file with .asm extension)")
                .build())

    options.addOption("x", "include-hex", false, "include hex in output")
    options.addOption("d", "detailed", false, "write detailed output")
    options.addOption("n0x", "no-0x-prefix", false, "do not write 0x prefix in hex output")
    options.addOption("aih", "allow-inlined-hex", false, "allow inlined hex in output")

    options.addOption("t", "text", false, "read text input instead of binary")

    options.addOption("off", "offset", true, "offset in the decompiled file (default is 0x8000)")
    options.addOption("boff", "binary-offset", true, "offset in the binary file (default is 0x00)")
    options.addOption("blen", "binary-count", true, "count of bytes to read from the binary file (default is 0x87ff; ends at file end)")

    options.addOption("v", "verbose", false, "verbose output")
    options.addOption("vv", "very-verbose", false, "very verbose output")
    options.addOption("h", "help", false, "show help")

    val helpFormatter = HelpFormatter()
    helpFormatter.setOptionComparator { o1, o2 ->
        options.options.indexOf(o1) - options.options.indexOf(o2)
    }

    Profi5EDecompiler.cmd = try {
        cliParser.parse(options, args)
    }catch (e: MissingOptionException) {
        if (!args.contains("-h") && !args.contains("--help")) println("ERR: ${e.message}")
        helpFormatter.printHelp("java -jar Profi5EDecompiler.jar [options] -in <binary>", options)
        return
    }
    val cli = Profi5EDecompiler.cmd
    if (cli.hasOption("h")) {
        helpFormatter.printHelp("java -jar Profi5EDecompiler.jar [options] -in <binary>", options)
        return
    }

    logVerbose("loading instructions")
    loadInstructions()
    logVerbose("loaded ${InstructionRegister.instructionCount} instructions")

    val inputFile = cli.getValue("in", "test.5e")
    val outputFile = cli.getValue("out", inputFile.replaceFileExtension(".asm"))

    val binaryOffset = cli.getValue("boff", 0x00)
    val binaryLen = cli.getValue("blen", 0x87ff)

    logVerbose("loading input file")
    val decomp = Decompiler.getDecompiler(Path.of(inputFile), binaryOffset, binaryLen, cli.hasOption("t"))
    decomp.defaultCursorAddress = cli.getValue("off", 0x8000)

    logVerbose("decompiling. start at 0x${binaryOffset.prefixedHexString(4)}")
    println("Started decompilation of $inputFile")
    val asm = decomp.decompileContent()
    println("Finished decompilation of $inputFile at 0x${(decomp.cursor + binaryOffset).prefixedHexString(4)}")
    logVerbose("decompiled 0x${decomp.cursor.prefixedHexString(4)} bytes")

    if (cli.hasOption("v")) {
        println("printing ${asm.size} instructions...")
        if (cli.hasOption("n0x")) asm.forEach { println(it.second.replace("0x", "")) }
        else asm.forEach { println(it.second) }
        println("printed all instructions")
    }

    println("Writing output to $outputFile")
    if (cli.hasOption("n0x")) {
        Files.write(Path.of(outputFile), asm.joinToString("\n") { it.second.replace("0x", "") }.toByteArray())
    }else {
        Files.write(Path.of(outputFile), asm.joinToString("\n") { it.second }.toByteArray())
    }
    println("Finished writing output to $outputFile")

    if (cli.hasOption("d")) {
        println("Writing detailed output to ${outputFile.replaceFileExtension(".dump")}")
        var offset = decomp.defaultCursorAddress
        if (cli.hasOption("n0x")) {
            Files.write(Path.of(outputFile.replaceFileExtension(".dump")), asm.joinToString("\n") {
                if (it.second.endsWith(":")) {
                    offset = it.second.dropLast(1).toInt(16)
                    return@joinToString "####     ${it.second}"
                }
                "${offset++.prefixedHexString(4)}     ${it.second}"
            }.toByteArray())
        }else {
            Files.write(Path.of(outputFile.replaceFileExtension(".dump")), asm.joinToString("\n") {
                if (it.second.endsWith(":")) {
                    offset = it.second.dropLast(1).toInt(16)
                    return@joinToString "######     ${it.second}"
                }
                "0x${offset++.prefixedHexString(4)}     ${it.second}"
            }.toByteArray())
        }
        println("Finished writing detailed output to ${outputFile.replaceFileExtension(".dump")}")
    }
    println("Done.")
}

private fun logVerbose(msg: String) {
    if (Profi5EDecompiler.cmd.hasOption("v")) println(msg)
}