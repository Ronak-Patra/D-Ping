package com.dping.app.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context, langCode: String): Context {
        val locale = getLocaleForCode(langCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun getLocaleForCode(langCode: String): Locale {
        return when (langCode.lowercase()) {
            // Indian Languages
            "hi" -> Locale("hi", "IN")
            "bn" -> Locale("bn", "IN")
            "pa" -> Locale("pa", "IN")
            "mr" -> Locale("mr", "IN")
            "gu" -> Locale("gu", "IN")
            "ta" -> Locale("ta", "IN")
            "te" -> Locale("te", "IN")
            "kn" -> Locale("kn", "IN")
            "ml" -> Locale("ml", "IN")
            "or" -> Locale("or", "IN")
            "as" -> Locale("as", "IN")
            "ur" -> Locale("ur", "IN")
            "ne" -> Locale("ne", "NP")
            "sa" -> Locale("sa", "IN")

            // Global Languages
            "en" -> Locale.ENGLISH
            "es" -> Locale("es", "ES")
            "zh", "zh-cn" -> Locale.SIMPLIFIED_CHINESE
            "zh-tw" -> Locale.TRADITIONAL_CHINESE
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "ru" -> Locale("ru", "RU")
            "he", "iw" -> Locale("iw", "IL")
            "tr" -> Locale("tr", "TR")
            "ko" -> Locale.KOREAN

            // Additional Regional
            "in", "id" -> Locale("in", "ID")
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "PT")
            "vi" -> Locale("vi", "VN")
            else -> Locale.ENGLISH
        }
    }
}
