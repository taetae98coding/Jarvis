package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.domain.battery.BatteryRepository
import org.koin.dsl.module

val batteryDataModule = module {
    single<BatteryRepository> { DefaultBatteryRepository(createBatterySource(get())) }
}
