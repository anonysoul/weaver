package com.anonysoul.weaver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WeaverApplication

fun main(args: Array<String>) {
    runApplication<WeaverApplication>(*args)
}
