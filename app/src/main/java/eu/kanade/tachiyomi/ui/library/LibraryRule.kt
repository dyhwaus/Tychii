package eu.kanade.tachiyomi.ui.library

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.model.SManga
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.MR
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * User-defined library rules: the third layer of the smart library.
 * A rule is a named set of conditions (ANDed) plus an optional sort
 * override. Rules are stored as one versioned JSON document in the
 * `pref_library_rules` string preference — they are small user config,
 * not library data, so they need no database table.
 *
 * Matching is defined on [LibraryItem], the same data the smart groups
 * use. Missing fields have explicit behavior: a date a manga has no
 * record of (no last update, no last read) counts as infinitely long
 * ago, so "within N days" never matches it and "more than N days"
 * always does. A malformed condition (an unknown value, an
 * unparseable number, an operator that does not belong to its field)
 * never matches — a broken rule shows an empty library instead of
 * silently showing everything. A rule with no conditions matches
 * everything.
 */
@Serializable
data class LibraryRule(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val conditions: List<RuleCondition> = emptyList(),
    // A LibrarySort serialized with LibrarySort.serialize(), or null
    // to keep the category's sort.
    val sort: String? = null,
) {
    fun matches(item: LibraryItem, now: Instant = Clock.System.now()): Boolean {
        return conditions.all { it.matches(item, now) }
    }

    fun sortOrNull(): LibrarySort? {
        return parseLibrarySort(sort)
    }

    companion object {
        fun new(): LibraryRule {
            return LibraryRule(
                id = UUID.randomUUID().toString(),
                name = "",
                conditions = listOf(
                    RuleCondition(
                        field = RuleField.READING_STATE,
                        operator = RuleOperator.EQUALS,
                        value = ReadingStateValue.READING.name,
                    ),
                ),
            )
        }
    }
}

@Serializable
data class RuleCondition(
    val field: RuleField,
    val operator: RuleOperator,
    val value: String,
) {
    fun matches(item: LibraryItem, now: Instant): Boolean {
        val manga = item.libraryManga
        return when (field) {
            RuleField.READING_STATE -> {
                if (operator != RuleOperator.EQUALS) return false
                when (value) {
                    ReadingStateValue.READING.name -> SmartLibraryCategory.READING.matches(item, now)
                    ReadingStateValue.PAUSED.name -> SmartLibraryCategory.PAUSED.matches(item, now)
                    ReadingStateValue.NOT_STARTED.name -> !manga.hasStarted
                    ReadingStateValue.FINISHED.name -> manga.hasStarted && manga.unreadCount == 0L
                    else -> false
                }
            }
            RuleField.LAST_UPDATE_DAYS -> {
                val days = value.toIntOrNull() ?: return false
                // A cross-module property can't be smart-cast after a
                // null check, so it is read into a local first.
                val lastUpdate = manga.manga.lastUpdate
                when (operator) {
                    RuleOperator.WITHIN_DAYS -> lastUpdate != null &&
                        lastUpdate >= now - days.days
                    RuleOperator.MORE_THAN_DAYS -> lastUpdate == null ||
                        lastUpdate < now - days.days
                    else -> false
                }
            }
            RuleField.LAST_READ_DAYS -> {
                val days = value.toIntOrNull() ?: return false
                val lastRead = manga.lastRead
                when (operator) {
                    RuleOperator.WITHIN_DAYS -> lastRead != null &&
                        lastRead >= now - days.days
                    RuleOperator.MORE_THAN_DAYS -> lastRead == null ||
                        lastRead < now - days.days
                    else -> false
                }
            }
            RuleField.FAVORITE -> {
                if (operator != RuleOperator.EQUALS) return false
                // Every item in the library is a favorite, so "is
                // favorite" always matches here and "is not" never
                // does; the field exists so the doc's example rule
                // ("recently followed") can be written as stated.
                (value == "true") == (manga.manga.favoriteAt != null)
            }
            RuleField.STATUS -> {
                if (operator != RuleOperator.EQUALS) return false
                val status = value.toIntOrNull() ?: return false
                manga.manga.status.toInt() == status
            }
            RuleField.DOWNLOADED -> {
                if (operator != RuleOperator.EQUALS) return false
                (value == "true") == (item.isLocal || item.downloadCount > 0)
            }
            RuleField.UNREAD_COUNT -> {
                val count = value.toIntOrNull() ?: return false
                when (operator) {
                    RuleOperator.AT_LEAST -> manga.unreadCount >= count
                    RuleOperator.EQUALS -> manga.unreadCount == count.toLong()
                    else -> false
                }
            }
        }
    }
}

enum class ReadingStateValue {
    READING,
    PAUSED,
    NOT_STARTED,
    FINISHED,
}

@Serializable
enum class RuleField(val titleRes: StringResource) {
    READING_STATE(MR.strings.rule_field_reading_state),
    LAST_UPDATE_DAYS(MR.strings.rule_field_last_update),
    LAST_READ_DAYS(MR.strings.rule_field_last_read),
    FAVORITE(MR.strings.rule_field_favorite),
    STATUS(MR.strings.rule_field_status),
    DOWNLOADED(MR.strings.rule_field_downloaded),
    UNREAD_COUNT(MR.strings.rule_field_unread_count),
    ;

    fun operators(): List<RuleOperator> {
        return when (this) {
            READING_STATE, FAVORITE, STATUS, DOWNLOADED -> listOf(RuleOperator.EQUALS)
            LAST_UPDATE_DAYS, LAST_READ_DAYS -> listOf(
                RuleOperator.WITHIN_DAYS,
                RuleOperator.MORE_THAN_DAYS,
            )
            UNREAD_COUNT -> listOf(RuleOperator.AT_LEAST, RuleOperator.EQUALS)
        }
    }

    fun defaultValue(): String {
        return when (this) {
            READING_STATE -> ReadingStateValue.READING.name
            LAST_UPDATE_DAYS, LAST_READ_DAYS -> "7"
            FAVORITE, DOWNLOADED -> "true"
            STATUS -> SManga.ONGOING.toString()
            UNREAD_COUNT -> "1"
        }
    }
}

@Serializable
enum class RuleOperator(val titleRes: StringResource) {
    EQUALS(MR.strings.rule_op_equals),
    WITHIN_DAYS(MR.strings.rule_op_within_days),
    MORE_THAN_DAYS(MR.strings.rule_op_more_than_days),
    AT_LEAST(MR.strings.rule_op_at_least),
}

@Serializable
data class LibraryRuleFile(
    val version: Int,
    val rules: List<LibraryRule>,
)

const val CURRENT_LIBRARY_RULE_VERSION = 1

private val libraryRulesJson = Json { ignoreUnknownKeys = true }

fun parseLibrarySort(raw: String?): LibrarySort? {
    if (raw == null) return null
    val parsed = LibrarySort.deserialize(raw)
    return if (parsed.serialize() == raw) parsed else null
}

fun parseLibraryRules(json: String): List<LibraryRule> {
    if (json.isBlank()) return emptyList()
    return try {
        val element = libraryRulesJson.parseToJsonElement(json)
        val version = element.jsonObject["version"]?.jsonPrimitive?.intOrNull ?: return emptyList()
        if (version != CURRENT_LIBRARY_RULE_VERSION) return emptyList()
        libraryRulesJson.decodeFromJsonElement<LibraryRuleFile>(element).rules
    } catch (_: Exception) {
        emptyList()
    }
}

fun serializeLibraryRules(rules: List<LibraryRule>): String {
    return libraryRulesJson.encodeToString(
        LibraryRuleFile(CURRENT_LIBRARY_RULE_VERSION, rules),
    )
}
