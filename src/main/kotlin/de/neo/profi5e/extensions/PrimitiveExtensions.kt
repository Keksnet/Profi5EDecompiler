package de.neo.profi5e.extensions

fun String.replaceFileExtension(newExtension: String) = substring(0, lastIndexOf('.')) + newExtension

fun Int.prefixedHexString(len: Int) = toString(16).padStart(len, '0')
fun UByte.prefixedHexString(len: Int) = toString(16).padStart(len, '0')