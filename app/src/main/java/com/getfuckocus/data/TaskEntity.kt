package com.getfuckocus.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/** 고른 요일마다 반복되는 과제. 시작 시각부터 소요 시간이 지날 때까지 통과한 인증이 없으면 잠금이 걸린다. */
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
)

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
