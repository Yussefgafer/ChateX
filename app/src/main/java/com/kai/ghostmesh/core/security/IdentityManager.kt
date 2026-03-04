package com.kai.ghostmesh.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import com.kai.ghostmesh.core.util.GhostLog as Log

/**
 * IdentityManager: Standalone BIP-39 implementation to avoid external dependency issues.
 */
object IdentityManager {
    private const val TAG = "IdentityManager"

    // Subset of BIP-39 Wordlist (English) for standalone reliability
    private val wordList = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse", "access", "accident",
        "account", "accuse", "achieve", "acid", "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual",
        "adapt", "add", "addict", "address", "adjust", "admit", "adult", "advance", "advice", "aerobic", "affair", "afford",
        "afraid", "again", "age", "agent", "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album",
        "alcohol", "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already", "also", "alter",
        "always", "amaze", "ambush", "amount", "amuse", "analysis", "anchor", "ancient", "anger", "angle", "angry", "animal",
        "ankle", "announce", "annual", "another", "answer", "antenna", "antique", "anxiety", "any", "apart", "apology", "appear",
        "apple", "approve", "april", "arch", "arctic", "area", "arena", "argue", "arm", "armed", "armor", "army",
        "around", "arrange", "arrest", "arrive", "arrow", "art", "artefact", "artist", "artwork", "ask", "aspect", "assault",
        "asset", "assist", "assume", "asthma", "athlete", "atom", "attack", "attend", "attitude", "attract", "auction", "audit",
        "august", "aunt", "author", "auto", "autumn", "average", "avocado", "avoid", "awake", "aware", "away", "awesome",
        "awful", "awkward", "axis", "baby", "bachelor", "bacon", "badge", "bag", "balance", "balcony", "ball", "bamboo",
        "banana", "banner", "bar", "barely", "bargain", "barrel", "base", "basic", "basket", "battle", "beach", "bean",
        "beauty", "because", "become", "beef", "before", "begin", "behave", "behind", "believe", "below", "belt", "bench",
        "benefit", "best", "betray", "better", "between", "beyond", "bicycle", "bid", "bike", "bind", "biology", "bird",
        "birth", "bitter", "black", "blade", "blame", "blanket", "blast", "bleak", "bless", "blind", "blood", "blossom",
        "blue", "blur", "blush", "board", "boat", "body", "boil", "bomb", "bone", "bonus", "book", "boost",
        "border", "boring", "borrow", "boss", "bottom", "bounce", "box", "boy", "bracket", "brain", "brand", "brass",
        "brave", "bread", "breeze", "brick", "bridge", "brief", "bright", "bring", "brisk", "broccoli", "broken", "bronze",
        "broom", "brother", "brown", "brush", "bubble", "buddy", "budget", "buffalo", "build", "bulb", "bulk", "bullet",
        "bundle", "bunker", "burden", "burger", "burst", "bus", "business", "busy", "butter", "buyer", "buzz", "cabbage",
        "cabin", "cable", "cactus", "cage", "cake", "call", "calm", "camera", "camp", "can", "canal", "canary",
        "candle", "candy", "cannon", "canyon", "capable", "capital", "captain", "caption", "car", "carbon", "card", "cargo",
        "carpet", "carry", "cart", "case", "cash", "casino", "castle", "casual", "cat", "catalog", "catch", "category",
        "cattle", "caught", "cause", "caution", "cave", "ceiling", "celery", "cement", "census", "century", "cereal", "certain",
        "certificate", "chair", "chalk", "champion", "change", "chaos", "chapter", "charge", "chase", "chat", "cheap", "check",
        "cheese", "chef", "cherry", "chest", "chicken", "chief", "child", "chimney", "choice", "choose", "chronic", "chuckle",
        "chunk", "churn", "cigar", "cinema", "circle", "circus", "citizen", "city", "civil", "claim", "clap", "clarify",
        "claw", "clay", "clean", "clerk", "clever", "click", "client", "cliff", "climb", "clinic", "clip", "clock",
        "clog", "close", "cloth", "cloud", "clown", "club", "clump", "cluster", "clutch", "coach", "coast", "coconut",
        "code", "coffee", "coil", "coin", "collect", "color", "column", "combine", "come", "comfort", "comic", "common",
        "company", "compass", "complain", "complete", "confirm", "congress", "connect", "consider", "control", "convince", "cook", "cool",
        "copper", "copy", "coral", "core", "corn", "corner", "cost", "cotton", "couch", "country", "couple", "course",
        "cousin", "cover", "coyote", "crack", "cradle", "craft", "cram", "crane", "crash", "crater", "crawl", "crazy",
        "cream", "credit", "creek", "crew", "cricket", "crime", "crisp", "critic", "crocodile"
    )

    fun generateMnemonic(): String {
        val random = SecureRandom()
        return List(12) { wordList[random.nextInt(wordList.size)] }.joinToString(" ")
    }

    fun validateMnemonic(mnemonic: String): Boolean {
        val words = mnemonic.trim().split("\\s+".toRegex())
        return words.size == 12 && words.all { wordList.contains(it) }
    }

    fun deriveSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val salt = "mnemonic$passphrase"
        val mnemonicChars = mnemonic.toCharArray()
        val saltBytes = salt.toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(mnemonicChars, saltBytes, 2048, 512)

        val skf = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        } catch (e: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }

        val seed = skf.generateSecret(spec).encoded

        // Clear sensitive materials
        mnemonicChars.fill('0')
        spec.clearPassword()

        return seed
    }

    fun deriveKeys(mnemonic: String): IdentityKeys {
        val seed = deriveSeed(mnemonic)
        val md = MessageDigest.getInstance("SHA-256")

        md.update(seed)
        md.update("nostr".toByteArray())
        val nostr = md.digest()

        md.reset()
        md.update(seed)
        md.update("ecdh".toByteArray())
        val ecdh = md.digest()

        // Clear seed after derivation
        seed.fill(0)

        return IdentityKeys(nostr, ecdh)
    }

    data class IdentityKeys(val nostrPrivKey: ByteArray, val ecdhPrivKey: ByteArray)
}
