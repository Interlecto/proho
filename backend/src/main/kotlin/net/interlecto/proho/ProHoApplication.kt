package net.interlecto.proho

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulithic

@SpringBootApplication
@ConfigurationPropertiesScan
@Modulithic(sharedModules = ["shared"])
class ProHoApplication

fun main(args: Array<String>) {
    runApplication<ProHoApplication>(*args)
}
