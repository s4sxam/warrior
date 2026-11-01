package com.tanay.warrior.nnn.data

import com.tanay.warrior.nnn.domain.*
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * On-disk shape of [NnnState]. Plain strings/numbers only (no java.time, no enums) so the format is
 * stable and a future/unknown value can be skipped instead of crashing. Bump [CURRENT_VERSION] and add
 * a migration in [toDomain] when the shape changes.
 */
@Serializable
data class NnnStateDto(
    val version: Int = CURRENT_VERSION,
    val challenge: ChallengeDto? = null,
    val events: List<EventDto> = emptyList(),
    val selfChecks: List<SelfCheckDto> = emptyList(),
    val segments: List<SegmentDto> = emptyList(),
    val settings: SettingsDto = SettingsDto(),
    val allowlist: List<String> = emptyList()
) {
    companion object { const val CURRENT_VERSION = 1 }
}

@Serializable
data class ChallengeDto(
    val id: String, val title: String, val startDate: String, val endDate: String, val zoneId: String,
    val status: String, val createdAtMs: Long, val startedAtMs: Long? = null, val completedAtMs: Long? = null,
    val safeBrowserNavigationFails: Boolean = true, val deviceGuardEnabled: Boolean = false,
    val strictDomainGuard: Boolean = false, val strictAckVersion: Int? = null, val consentVersion: Int = 0
)

@Serializable
data class ReviewDto(val decision: String, val reason: String, val atMs: Long)

@Serializable
data class EventDto(
    val id: String, val challengeId: String, val atMs: Long, val challengeDate: String, val source: String,
    val originalOutcome: String, val matchedDomain: String? = null, val category: String? = null,
    val confidence: Int? = null, val dedupKey: String, val reviews: List<ReviewDto> = emptyList()
)

@Serializable
data class SelfCheckDto(val date: String, val atMs: Long)

@Serializable
data class SegmentDto(val source: String, val state: String, val startMs: Long, val endMs: Long? = null)

@Serializable
data class SettingsDto(
    val storeDomainDetail: Boolean = false, val retentionDays: Int = NnnSettings.DEFAULT_RETENTION_DAYS,
    val notificationsEnabled: Boolean = false, val quietStartMin: Int? = null, val quietEndMin: Int? = null,
    val focusMessage: String = ""
)

// ── mapping ─────────────────────────────────────────────────────────────────────

fun NnnState.toDto() = NnnStateDto(
    challenge = challenge?.let {
        ChallengeDto(
            it.id, it.title, it.startDate.toString(), it.endDate.toString(), it.zoneId.id, it.status.name,
            it.createdAt.toEpochMilli(), it.startedAt?.toEpochMilli(), it.completedAt?.toEpochMilli(),
            it.rules.safeBrowserNavigationFails, it.rules.deviceGuardEnabled, it.rules.strictDomainGuard,
            it.rules.strictAcknowledgedConsentVersion, it.consentVersion
        )
    },
    events = events.map { e ->
        EventDto(e.id, e.challengeId, e.at.toEpochMilli(), e.challengeDate.toString(), e.source.name, e.originalOutcome.name,
            e.matchedDomain, e.category, e.confidence, e.dedupKey,
            e.reviews.map { ReviewDto(it.decision.name, it.reason.name, it.at.toEpochMilli()) })
    },
    selfChecks = selfChecks.map { SelfCheckDto(it.date.toString(), it.at.toEpochMilli()) },
    segments = segments.map { SegmentDto(it.source.name, it.state.name, it.start.toEpochMilli(), it.end?.toEpochMilli()) },
    settings = SettingsDto(settings.storeDomainDetail, settings.retentionDays, settings.notificationsEnabled,
        settings.quietHoursStartMinute, settings.quietHoursEndMinute, settings.focusMessage),
    allowlist = allowlist.toList().sorted()
)

/** Never throws: anything unreadable is skipped (records) or dropped (challenge) rather than crashing the app. */
fun NnnStateDto.toDomain(): NnnState = NnnState(
    challenge = challenge?.let { runCatching { it.toDomain() }.getOrNull() },
    events = events.mapNotNull { runCatching { it.toDomain() }.getOrNull() },
    selfChecks = selfChecks.mapNotNull { runCatching { SelfCheck(LocalDate.parse(it.date), Instant.ofEpochMilli(it.atMs)) }.getOrNull() },
    segments = segments.mapNotNull {
        runCatching {
            MonitoringSegment(MonitorSource.valueOf(it.source), MonitorState.valueOf(it.state),
                Instant.ofEpochMilli(it.startMs), it.endMs?.let(Instant::ofEpochMilli))
        }.getOrNull()
    },
    settings = NnnSettings(settings.storeDomainDetail, settings.retentionDays, settings.notificationsEnabled,
        settings.quietStartMin, settings.quietEndMin, settings.focusMessage),
    allowlist = allowlist.toSet()
)

private fun ChallengeDto.toDomain() = NnnChallenge(
    id = id, title = title, startDate = LocalDate.parse(startDate), endDate = LocalDate.parse(endDate),
    zoneId = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()),
    status = ChallengeStatus.valueOf(status), createdAt = Instant.ofEpochMilli(createdAtMs),
    startedAt = startedAtMs?.let(Instant::ofEpochMilli), completedAt = completedAtMs?.let(Instant::ofEpochMilli),
    rules = NnnRuleSet(safeBrowserNavigationFails, deviceGuardEnabled, strictDomainGuard, strictAckVersion),
    consentVersion = consentVersion
)

private fun EventDto.toDomain() = NnnEvent(
    id = id, challengeId = challengeId, at = Instant.ofEpochMilli(atMs), challengeDate = LocalDate.parse(challengeDate),
    source = EventSource.valueOf(source), originalOutcome = EventOutcome.valueOf(originalOutcome),
    matchedDomain = matchedDomain, category = category, confidence = confidence, dedupKey = dedupKey,
    reviews = reviews.map { ReviewEntry(ReviewDecision.valueOf(it.decision), ReviewReason.valueOf(it.reason), Instant.ofEpochMilli(it.atMs)) }
)
