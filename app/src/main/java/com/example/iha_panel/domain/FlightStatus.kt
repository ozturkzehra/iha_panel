package com.example.iha_panel.domain

/** Sıra önemli: ordinal büyüdükçe risk artar (GO < CAUTION < NO_GO). */
enum class FlightStatus {
    GO, CAUTION, NO_GO
}
