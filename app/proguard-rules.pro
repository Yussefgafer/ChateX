# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.kai.ghostmesh.core.model.** { *; }
-keep class com.kai.ghostmesh.core.mesh.** { *; }

# Secp256k1 rules
-keep class fr.acinq.secp256k1.** { *; }
-keep class fr.acinq.bitcoin.** { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Compose rules
-keep class androidx.compose.** { *; }
