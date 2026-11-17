package eu.kanade.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R

/**
 * Neko's typography: M PLUS Rounded 1c for every text style, with
 * tight letter spacing. Ported from Neko (nekomangaorg/Neko).
 */
object Typefaces {

    private val defaultTypography = Typography()

    private const val LETTER_SPACING = -.15

    val mplusRounded = FontFamily(
        Font(R.font.mplus_rounded1c_thin, FontWeight.Thin),
        Font(R.font.mplus_rounded1c_black, FontWeight.Black),
        Font(R.font.mplus_rounded1c_bold, FontWeight.Bold),
        Font(R.font.mplus_rounded1c_extra_bold, FontWeight.ExtraBold),
        Font(R.font.mplus_rounded1c_medium, FontWeight.Medium),
        Font(R.font.mplus_rounded1c_semi_bold, FontWeight.SemiBold),
        Font(R.font.mplus_rounded1c_regular, FontWeight.Normal),
    )

    /**
     * The user's MTR Song-style font (FAKE-MTRSong 80's Special V4,
     * a replica of the Hong Kong MTR Song typeface they sent in).
     * Single weight; used for the metal card titles, where the
     * letters are carved into the metal. The TTF itself is not in
     * the repo — it is a binary the GitHub push cannot carry, so it
     * must be placed at app/src/main/res/font/mtr_song.ttf before
     * building. Glyphs it lacks fall back to the default font.
     */
    val mtrSong = FontFamily(
        Font(R.font.mtr_song),
    )

    val appTypography = Typography(
        displayLarge = defaultTypography.displayLarge.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        displayMedium = defaultTypography.displayMedium.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        displaySmall = defaultTypography.displaySmall.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        headlineLarge = defaultTypography.headlineLarge.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        headlineMedium = defaultTypography.headlineMedium.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        headlineSmall = defaultTypography.headlineSmall.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        titleLarge = defaultTypography.titleLarge.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        titleMedium = defaultTypography.titleMedium.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        titleSmall = defaultTypography.titleSmall.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        bodyLarge = defaultTypography.bodyLarge.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        bodyMedium = defaultTypography.bodyMedium.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        bodySmall = defaultTypography.bodySmall.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        labelLarge = defaultTypography.labelLarge.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        labelMedium = defaultTypography.labelMedium.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
        labelSmall = defaultTypography.labelSmall.copy(
            fontFamily = mplusRounded,
            letterSpacing = LETTER_SPACING.sp,
        ),
    )
}
