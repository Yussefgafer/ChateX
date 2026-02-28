import subprocess
import os

# Create a temporary Kotlin file to check the method names
with open("CheckSecp.kt", "w") as f:
    f.write("""
import fr.acinq.secp256k1.Secp256k1

fun main() {
    println("Methods:")
    Secp256k1::class.java.methods.forEach { println(it.name) }
}
""")

# This won't work because I don't have the jar on classpath here.
