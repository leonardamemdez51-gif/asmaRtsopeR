package com.example.core.config

data class Branch(
    val id: String,
    val name: String,
    val zone: String,
    val address: String,
    val phone: String,
    val managerName: String,
    val isHeadquarters: Boolean = false
)

object BranchConfig {
    val defaultBranches = listOf(
        Branch(
            id = "SUC-001",
            name = "Sucursal Centro Principal",
            zone = "Zona Centro",
            address = "Av. Reforma 100, Col. Juárez, CDMX",
            phone = "555-100-2000",
            managerName = "Carlos Mendoza",
            isHeadquarters = true
        ),
        Branch(
            id = "SUC-002",
            name = "Sucursal Norte",
            zone = "Zona Norte",
            address = "Calz. Vallejo 450, Col. Industrial, CDMX",
            phone = "555-300-4000",
            managerName = "Roberto Gómez"
        ),
        Branch(
            id = "SUC-003",
            name = "Sucursal Sur",
            zone = "Zona Sur",
            address = "Av. Insurgentes Sur 2200, Col. Chimalistac, CDMX",
            phone = "555-500-6000",
            managerName = "Lucía Ramírez"
        )
    )

    fun getBranchByZone(zone: String): Branch {
        return defaultBranches.find { it.zone.equals(zone, ignoreCase = true) }
            ?: defaultBranches.first()
    }
}
