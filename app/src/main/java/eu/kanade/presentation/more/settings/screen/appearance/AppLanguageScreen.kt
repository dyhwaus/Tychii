package eu.kanade.presentation.more.settings.screen.appearance

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import org.xmlpull.v1.XmlPullParser
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.R
import tachiyomi.presentation.core.i18n.stringResource
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.RadioButtonPreference

class AppLanguageScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val langs = remember { getLangs(context) }
        var currentLanguage by remember {
            mutableStateOf(AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: "")
        }

        LaunchedEffect(currentLanguage) {
            val locale = if (currentLanguage.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(currentLanguage)
            }
            AppCompatDelegate.setApplicationLocales(locale)
        }

        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = stringResource(MR.strings.pref_app_language),
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(
                                imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier.padding(contentPadding),
            ) {
                items(langs) {
                    RadioButtonPreference(
                        title = it.displayName,
                        summary = it.localizedDisplayName,
                        selected = currentLanguage == it.langTag,
                        onClick = { currentLanguage = it.langTag },
                    )
                }
            }
        }
    }

    private fun getLangs(context: Context): List<Language> = buildList {
        val parser = context.resources.getXml(R.xml.locales_config)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                for (i in 0..<parser.attributeCount) {
                    if (parser.getAttributeName(i) == "name") {
                        val langTag = parser.getAttributeValue(i)
                        val displayName = LocaleHelper.getLocalizedDisplayName(langTag)
                        if (displayName.isNotEmpty()) {
                            add(Language(langTag, displayName, LocaleHelper.getDisplayName(langTag)))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        sortBy { it.displayName }
        add(0, Language("", context.stringResource(MR.strings.label_default), null))
    }

    private data class Language(
        val langTag: String,
        val displayName: String,
        val localizedDisplayName: String?,
    )
}
