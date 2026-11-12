package com.getfuckocus.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/** 고른 요일마다 반복되는 과제. 시작 시각부터 소요 시간이 지날 때까지 통과한 인증이 없으면 [missAction]을 한다. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** 인증 가능 시작 시각 (자정 기준 분) */
    val windowStartMinute: Int,
    /** 시작부터 마감까지 걸리는 시간 (분, 1 ~ 1439). 자정을 넘어가도 된다. */
    val durationMinutes: Int,
    val geoLat: Double? = null,
    val geoLng: Double? = null,
    val geoRadiusMeters: Float? = null,
    /** ML Kit 기본 모델 라벨 (영문, 쉼표로 여러 개, 예: "Book, Paper"). null이면 내용 검사를 하지 않는다. */
    val requiredLabel: String? = null,
    /** ML Kit 기본 모델이 라벨을 돌려주는 최소값(0.5)과 같게 둔다. 높이면 인식돼도 통과하지 못하는 경우가 잦다. */
    val minLabelConfidence: Float = 0.5f,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * "바로 시작"을 누른 시각. 이 시각이 속한 날의 회차는 정해진 시작 시각 대신 여기서 시작한다.
     * 다른 날의 회차에는 영향이 없다.
     */
    val startedAt: Long? = null,
    /** 반복 요일 비트마스크. 월요일이 1(비트 0), 일요일이 64(비트 6). 기본은 매일(127). */
    @ColumnInfo(defaultValue = "127")
    val repeatDays: Int = RepeatDays.EVERY_DAY,
    /** 회차가 시작할 때 알림 */
    @ColumnInfo(defaultValue = "1")
    val remindAtStart: Boolean = true,
    /** 마감 몇 분 전에 알릴지. 0이면 알리지 않는다. */
    @ColumnInfo(defaultValue = "15")
    val remindBeforeMinutes: Int = 15,
    /** 인증 방식 */
    @ColumnInfo(defaultValue = "PHOTO")
    val verifyMethod: VerifyMethod = VerifyMethod.PHOTO,
    /** 마감을 놓쳤을 때 할 일 */
    @ColumnInfo(defaultValue = "LOCK")
    val missAction: MissAction = MissAction.LOCK,
    /** NFC 인증에 쓸 태그 ([NfcTagEntity.id]). NFC 인증 과제에만 쓴다. */
    val nfcTagId: Long? = null,
    /** 억제 화면·잠금 화면에 크게 띄울 문구. 비어 있으면 기본 문구를 쓴다. */
    @ColumnInfo(defaultValue = "")
    val nagMessage: String = "",
    /** 억제 화면·잠금 화면에 띄울 이미지 (앱 내부 저장소 경로). */
    val nagImagePath: String? = null,
)

enum class VerifyMethod {
    /** 버튼 한 번으로 체크. 약 먹기처럼 가벼운 일과용. */
    TAP,

    /** 앱 카메라로 찍어서 인증. 시각·장소·사진 내용을 확인한다. */
    PHOTO,

    /** 정해 둔 장소 근처에서 체크. 출퇴근, 헬스장 가기처럼 어디에 가는 일과용. */
    LOCATION,

    /** 등록한 NFC 태그를 찍어서 체크. 약통, 책상처럼 그 자리에 가야 하는 일과용. */
    NFC,
}

/** 마감을 놓쳤을 때 할 일. 뒤로 갈수록 강하다. */
enum class MissAction {
    /** 알림과 기록으로만 챙긴다 */
    NOTIFY,

    /** 인증할 때까지 몇 분마다 글자·이미지 화면으로 덮는다. 그사이 다른 앱은 쓸 수 있다 */
    NAG,

    /** 인증할 때까지 허용한 앱 말고는 못 쓰게 잠근다 */
    LOCK,
}

/** 이 인증 방식으로 고를 수 있는 "놓쳤을 때" 동작. 버튼 한 번으로 풀리는 탭 체크에 앱 잠금은 의미가 없다. */
fun VerifyMethod.allowedMissActions(): List<MissAction> =
    if (this == VerifyMethod.TAP) listOf(MissAction.NOTIFY, MissAction.NAG) else MissAction.entries

/** 놓쳤을 때 억제 화면이나 잠금을 거는 과제인지. */
fun TaskEntity.enforcesOnMiss(): Boolean = missAction != MissAction.NOTIFY

/** 과제를 저장하기 전에 확인할 것. 문제가 없으면 null. */
fun saveProblem(method: VerifyMethod, hasGeoFence: Boolean, nfcTagId: Long?, missAction: MissAction): String? = when {
    method == VerifyMethod.LOCATION && !hasGeoFence -> "위치 인증은 장소를 지정해야 해요"
    method == VerifyMethod.NFC && nfcTagId == null -> "NFC 인증은 찍을 태그를 골라야 해요"
    missAction !in method.allowedMissActions() -> "탭 체크는 놓쳐도 앱을 잠글 수 없어요. 억제 화면을 써 보세요"
    else -> null
}

object RepeatDays {
    const val EVERY_DAY = 0b111_1111
    const val WEEKDAYS = 0b001_1111
    const val WEEKEND = 0b110_0000

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    fun contains(mask: Int, day: DayOfWeek): Boolean = (mask and bit(day)) != 0
}

fun TaskEntity.hasGeoFence(): Boolean =
    geoLat != null && geoLng != null && geoRadiusMeters != null

/** 쉼표로 구분한 라벨 목록. 이 중 하나만 사진에 보여도 통과한다. */
fun TaskEntity.requiredLabels(): List<String> =
    requiredLabel?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
