# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ─────────────────────────────────────────────────────
# Preserve line number information for debugging stack traces.
# ─────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─────────────────────────────────────────────────────
# GSON - Genel kurallar
# ─────────────────────────────────────────────────────
# Gson uses type tokens for generic types
-keepattributes Signature
-keepattributes *Annotation*

# Gson TypeToken ve ilgili sınıflar
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ─────────────────────────────────────────────────────
# Uygulama Data Model Sınıfları (Gson ile serialize/deserialize ediliyor)
# R8 bunların field isimlerini değiştirmemeli!
# ─────────────────────────────────────────────────────
-keep class com.berat.sakus.data.HatBilgisi { *; }
-keep class com.berat.sakus.data.HatBilgisi$Companion { *; }
-keep class com.berat.sakus.data.NewsItem { *; }
-keep class com.berat.sakus.data.NewsItem$Companion { *; }
-keep class com.berat.sakus.data.Duyuru { *; }
-keep class com.berat.sakus.data.Duyuru$Companion { *; }
-keep class com.berat.sakus.data.DurakBilgisi { *; }
-keep class com.berat.sakus.data.DurakBilgisi$Companion { *; }
-keep class com.berat.sakus.data.AracKonumu { *; }
-keep class com.berat.sakus.data.AracKonumu$Companion { *; }
-keep class com.berat.sakus.data.HatSeferBilgisi { *; }
-keep class com.berat.sakus.data.HatSeferBilgisi$Companion { *; }
-keep class com.berat.sakus.data.GuzergahSefer { *; }
-keep class com.berat.sakus.data.GuzergahSefer$Companion { *; }
-keep class com.berat.sakus.data.SeferDetay { *; }
-keep class com.berat.sakus.data.SeferDetay$Companion { *; }
-keep class com.berat.sakus.data.TarifeBilgisi { *; }
-keep class com.berat.sakus.data.TarifeBilgisi$Companion { *; }
-keep class com.berat.sakus.data.TarifeTipi { *; }
-keep class com.berat.sakus.data.TarifeTipi$Companion { *; }
-keep class com.berat.sakus.data.TarifeGrubu { *; }
-keep class com.berat.sakus.data.TarifeGrubu$Companion { *; }
-keep class com.berat.sakus.data.TarifeGuzergah { *; }
-keep class com.berat.sakus.data.TarifeGuzergah$Companion { *; }
-keep class com.berat.sakus.data.TarifeUcret { *; }
-keep class com.berat.sakus.data.TarifeUcret$Companion { *; }
-keep class com.berat.sakus.data.YonBilgisi { *; }
-keep class com.berat.sakus.data.YonBilgisi$Companion { *; }
-keep class com.berat.sakus.data.HatGuzergahBilgisi { *; }
-keep class com.berat.sakus.data.HatGuzergahBilgisi$Companion { *; }
-keep class com.berat.sakus.data.DurakVarisi { *; }
-keep class com.berat.sakus.data.DurakClusterItem { *; }

# Extracted model classes (new package)
-keep class com.berat.sakus.data.models.** { *; }

# Route ("Nasıl Giderim") API model sınıfları
-keep class com.berat.sakus.data.models.RouteRequest { *; }
-keep class com.berat.sakus.data.models.RouteResponse { *; }
-keep class com.berat.sakus.data.models.RouteLocation { *; }
-keep class com.berat.sakus.data.models.RouteData { *; }
-keep class com.berat.sakus.data.models.RoutePlan { *; }
-keep class com.berat.sakus.data.models.Itinerary { *; }
-keep class com.berat.sakus.data.models.Leg { *; }
-keep class com.berat.sakus.data.models.Stop { *; }
-keep class com.berat.sakus.data.models.StationEstimate { *; }
-keep class com.berat.sakus.data.models.NearLine { *; }
-keep class com.berat.sakus.data.models.NearLineResponse { *; }
-keep class com.berat.sakus.data.models.NearLineRoute { *; }

# FilterOption ve TransportLine (Gson ile kullanılıyor)
-keep class com.berat.sakus.ui.screens.FilterOption { *; }
-keep class com.berat.sakus.ui.screens.TransportLine { *; }
-keep class com.berat.sakus.ui.screens.TransportLine$Companion { *; }
-keep class com.berat.sakus.ui.screens.TransportType { *; }

# ─────────────────────────────────────────────────────
# Room Database Entity'leri ve DAO'lar
# ─────────────────────────────────────────────────────
-keep class com.berat.sakus.data.local.entity.** { *; }
-keep class com.berat.sakus.data.local.dao.** { *; }
-keep class com.berat.sakus.data.local.SakusDatabase { *; }

# ─────────────────────────────────────────────────────
# OkHttp
# ─────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─────────────────────────────────────────────────────
# Google Play Services - Genel (Maps için gerekli)
# Tüm GMS sınıflarını koru — Maps renderer'ı reflection + dynamic loading kullanıyor
# ─────────────────────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# GMS sınıf isimlerini de koru (reflection erişimleri için)
-keepnames class com.google.android.gms.** { *; }

# Google Play Services - Maps özel kurallar
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.maps.internal.** { *; }
-keep class com.google.android.gms.maps.model.** { *; }

# Google Play Services - Dahili dinamik modül yükleme
-keep class com.google.android.gms.dynamic.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.common.internal.** { *; }

# SafeParcel - Google Maps bunu tile/renderer verileri için kullanıyor
-keep class com.google.android.gms.common.internal.safeparcel.** { *; }
-keepclassmembers class * extends com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable {
    public static final ** CREATOR;
}

# Maps Compose (maps-compose kütüphanesi)
-keep class com.google.maps.android.** { *; }
-keep class com.google.maps.android.compose.** { *; }
-keep class * implements com.google.maps.android.clustering.ClusterItem { *; }

# Google Maps Renderer - Reflection kullanıyor
-keepattributes InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepForSdk *;
}

# @KeepForSdk ile işaretlenmiş tüm sınıflar
-keep @com.google.android.gms.common.annotation.KeepForSdk class * { *; }

# Maps tile ve renderer sınıfları
-keep public class com.google.android.gms.maps.SupportMapFragment { *; }
-keep public class com.google.android.gms.maps.GoogleMapOptions { *; }
-keep public class com.google.android.gms.maps.MapView { *; }
-keep public class com.google.android.gms.maps.MapsInitializer { *; }
-keep public class com.google.android.gms.maps.OnMapReadyCallback { *; }

# Native method'ları koru (Maps OpenGL renderer bu yöntemi kullanır)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Play Services Location (konum izni ile ilgili)
-keep class com.google.android.gms.location.** { *; }

# AndroidX Fragment - Google Maps SupportMapFragment bu sınıfa bağlı
-keep class androidx.fragment.app.** { *; }


# ─────────────────────────────────────────────────────
# Kotlin Koroutinler
# ─────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**

# ─────────────────────────────────────────────────────
# Coil (Image Loading)
# ─────────────────────────────────────────────────────
-dontwarn coil3.**
-keep class coil3.** { *; }

# ─────────────────────────────────────────────────────
# Keep enum classes
# ─────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─────────────────────────────────────────────────────
# Keep Parcelable & Serializable
# ─────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}