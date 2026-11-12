package com.getfuckocus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class, AttemptEntity::class, NfcTagEntity::class], version = 9, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun nfcTagDao(): NfcTagDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "getfocus.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9,
                )
                .build()

        /**
         * 인증 방식에 위치·NFC 추가, 놓쳤을 때 할 일, 억제 화면 문구·이미지.
         * 지금까지처럼 사진 인증은 잠그고 탭 체크는 알림만 한다.
         * 장소 조건이 있던 탭 체크는 위치 인증으로 옮긴다 (탭 체크는 이제 장소를 보지 않는다).
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN missAction TEXT NOT NULL DEFAULT 'LOCK'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN nfcTagId INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN nagMessage TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN nagImagePath TEXT")
                db.execSQL("UPDATE tasks SET missAction = 'NOTIFY' WHERE verifyMethod = 'TAP'")
                db.execSQL(
                    "UPDATE tasks SET verifyMethod = 'LOCATION' WHERE verifyMethod = 'TAP' " +
                        "AND geoLat IS NOT NULL AND geoLng IS NOT NULL AND geoRadiusMeters IS NOT NULL"
                )
            }
        }

        /** 등록한 NFC 태그 */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS nfc_tags (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uid TEXT NOT NULL, " +
                        "name TEXT NOT NULL, createdAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_nfc_tags_uid ON nfc_tags (uid)")
            }
        }

        /** 마감 시각(windowEndMinute) → 소요 시간(durationMinutes). SQLite 호환을 위해 테이블을 다시 만든다. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        windowStartMinute INTEGER NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        geoLat REAL,
                        geoLng REAL,
                        geoRadiusMeters REAL,
                        requiredLabel TEXT,
                        minLabelConfidence REAL NOT NULL,
                        enabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO tasks_new (id, title, windowStartMinute, durationMinutes, geoLat, geoLng,
                        geoRadiusMeters, requiredLabel, minLabelConfidence, enabled, createdAt)
                    SELECT id, title, windowStartMinute, MAX(windowEndMinute - windowStartMinute, 1), geoLat, geoLng,
                        geoRadiusMeters, requiredLabel, minLabelConfidence, enabled, createdAt
                    FROM tasks
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        /** 라벨 기준 신뢰도 기본값 0.6 → 0.5 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE tasks SET minLabelConfidence = 0.5 WHERE minLabelConfidence > 0.5")
            }
        }

        /** "바로 시작" 시각 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN startedAt INTEGER")
            }
        }

        /** 반복 요일. 기존 과제는 매일(127)로 둔다 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatDays INTEGER NOT NULL DEFAULT 127")
            }
        }

        /** 알림 설정. 기존 과제는 시작 알림 켬, 마감 15분 전 알림 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN remindAtStart INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN remindBeforeMinutes INTEGER NOT NULL DEFAULT 15")
            }
        }

        /** 인증 방식. 기존 과제와 기록은 모두 사진 인증이었다 */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN verifyMethod TEXT NOT NULL DEFAULT 'PHOTO'")
                db.execSQL("ALTER TABLE attempts ADD COLUMN method TEXT NOT NULL DEFAULT 'PHOTO'")
            }
        }
    }
}
