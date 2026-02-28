import fr.acinq.secp256k1.Secp256k1

fun main() {
    println("Methods in Secp256k1:")
    Secp256k1::class.java.methods.forEach { println(it.name) }
}
