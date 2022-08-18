package de.neo.profi5e.extensions

import org.apache.commons.cli.CommandLine

fun CommandLine.getValue(option: String, default: String): String {
    return getOptionValue(option) ?: default
}

fun CommandLine.getValue(option: String, default: Int, radix: Int = 16): Int {
    return getOptionValue(option)?.replace("0x", "")?.toInt(radix) ?: default
}