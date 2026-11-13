package com.example.data.network

import com.example.data.content.AyahTafsir
import com.example.data.content.SurahTafsirBundle
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class AlQuranSurahListResponse(
    val code: Int = 0,
    val data: List<AlQuranSurahMeta> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahMeta(
    val number: Int = 0,
    val name: String = "",
    val englishName: String = "",
    val numberOfAyahs: Int = 0,
    val revelationType: String = ""
)

@JsonClass(generateAdapter = true)
data class AlQuranEditionsResponse(
    val code: Int = 0,
    val data: List<AlQuranSurahEdition> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahEdition(
    val number: Int = 0,
    val name: String = "",
    val revelationType: String = "",
    val numberOfAyahs: Int = 0,
    val ayahs: List<AlQuranAyahDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranAyahDto(
    val numberInSurah: Int = 0,
    val text: String = ""
)

interface AlQuranApi {
    @GET("surah")
    suspend fun getSurahList(): AlQuranSurahListResponse

    @GET("surah/{number}/editions/quran-uthmani,ar.muyassar")
    suspend fun getSurahWithMuyassarTafsir(@Path("number") surahNumber: Int): AlQuranEditionsResponse
}

object QuranCloudRepository {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: AlQuranApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/v1/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlQuranApi::class.java)
    }

    suspend fun fetchAllSurahSummaries(): Result<List<AlQuranSurahMeta>> = runCatching {
        api.getSurahList().data
    }

    suspend fun fetchSurahWithTafsir(surahNumber: Int): Result<SurahTafsirBundle> = runCatching {
        val response = api.getSurahWithMuyassarTafsir(surahNumber)
        val quranEd = response.data.getOrNull(0) ?: error("Missing quran text")
        val tafsirEd = response.data.getOrNull(1)
        val ayahs = quranEd.ayahs.mapIndexed { index, ayah ->
            val tafsirText = tafsirEd?.ayahs?.getOrNull(index)?.text ?: ""
            AyahTafsir(
                numberInSurah = ayah.numberInSurah,
                arabicText = ayah.text,
                tafsirMuyassar = tafsirText
            )
        }
        val revArabic = if (quranEd.revelationType.equals("Meccan", ignoreCase = true)) "مكية" else "مدنية"
        SurahTafsirBundle(
            surahNumber = quranEd.number,
            nameArabic = quranEd.name,
            revelationType = revArabic,
            totalAyahs = quranEd.numberOfAyahs,
            themeSummary = "التفسير الميسر • ${quranEd.name} ($revArabic • ${quranEd.numberOfAyahs} آيات)",
            ayahs = ayahs
        )
    }
}
