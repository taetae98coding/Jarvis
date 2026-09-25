# 쓰는 라이브러리 대부분(Koin, kotlinx.serialization, Compose, DataStore)은 필요한 규칙을 AAR 의 소비자 규칙으로
# 싣고 온다. 여기에는 그것으로 모자라 release APK 스모크 테스트에서 죽은 것만 적는다(docs/platform/android.html#release-build).

# Glance 1.2.0 이 끌고 오는 WorkManager 2.7.1 의 Room 2.2.5 는 `WorkDatabase_Impl` 을 Class.forName + newInstance 로
# 만든다. AGP 9 의 R8 full mode 는 keep 규칙에 없는 기본 생성자를 지워서, 앱 시작 때 androidx.startup 이
# "Failed to create an instance of androidx.work.impl.WorkDatabase" 로 죽는다. WorkManager 를 Room 2.6+ 쓰는 버전으로
# 올리면 걷어낼 수 있다.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
