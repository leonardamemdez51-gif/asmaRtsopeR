import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

# distributionByZone
content = content.replace('if (amount > 0) amount else 15000.0, count = if (count > 0) count else 12', 'amount, count = count')

# distributionByCollector
old_dist_coll = '''        val distributionByCollector = availableCollectors.filter { it != "TODOS" }.ifEmpty { listOf("Juan Cobrador", "Pedro Campo", "Cobrador Principal") }.map { coll ->
            val collLoans = filteredLoans.filter { it.collectorName.equals(coll, ignoreCase = true) }
            val collCollected = collLoans.sumOf { it.paidAmount }
            ChartDataPoint(coll, if (collCollected > 0) collCollected else 18500.0, count = collLoans.size)
        }'''
new_dist_coll = '''        val distributionByCollector = availableCollectors.filter { it != "TODOS" }.map { coll ->
            val collLoans = filteredLoans.filter { it.collectorName.equals(coll, ignoreCase = true) }
            val collCollected = collLoans.sumOf { it.paidAmount }
            ChartDataPoint(coll, collCollected, count = collLoans.size)
        }'''
content = content.replace(old_dist_coll, new_dist_coll)

# loansByPlan
old_loans_plan = '''        val loansByPlan = listOf("PLAN_20_DIAS", "PLAN_30_DIAS").map { plan ->
            val planLoans = filteredLoans.filter { it.planType == plan }
            val label = if (plan == "PLAN_20_DIAS") "Plan 20 Días" else "Plan 30 Días"
            ChartDataPoint(label, planLoans.sumOf { it.capital }.let { if (it > 0) it else 35000.0 }, count = planLoans.size.let { if (it > 0) it else 18 })
        }'''
new_loans_plan = '''        val loansByPlan = listOf("PLAN_20_DIAS", "PLAN_30_DIAS").map { plan ->
            val planLoans = filteredLoans.filter { it.planType == plan }
            val label = if (plan == "PLAN_20_DIAS") "Plan 20 Días" else "Plan 30 Días"
            ChartDataPoint(label, planLoans.sumOf { it.capital }, count = planLoans.size)
        }'''
content = content.replace(old_loans_plan, new_loans_plan)


# clientsByStatus
old_clients = '''        val clientsByStatus = listOf(
            ChartDataPoint("Activos", activeClients.size.toDouble().let { if (it > 0) it else 42.0 }, count = activeClients.size),
            ChartDataPoint("Pendientes", clientsList.count { it.status == "PENDIENTE" }.toDouble().let { if (it > 0) it else 5.0 }, count = clientsList.count { it.status == "PENDIENTE" }),
            ChartDataPoint("Inactivos", clientsList.count { it.status == "INACTIVO" }.toDouble().let { if (it > 0) it else 12.0 }, count = clientsList.count { it.status == "INACTIVO" })
        )'''
new_clients = '''        val clientsByStatus = listOf(
            ChartDataPoint("Activos", activeClients.size.toDouble(), count = activeClients.size),
            ChartDataPoint("Pendientes", clientsList.count { it.status == "PENDIENTE" }.toDouble(), count = clientsList.count { it.status == "PENDIENTE" }),
            ChartDataPoint("Inactivos", clientsList.count { it.status == "INACTIVO" }.toDouble(), count = clientsList.count { it.status == "INACTIVO" })
        )'''
content = content.replace(old_clients, new_clients)


# portfolioHealth
old_health = '''        val portfolioHealth = listOf(
            ChartDataPoint("Al Corriente", activeLoans.count { it.lateFeeAmount <= 0.0 }.toDouble().let { if (it > 0) it else 85.0 }, count = activeLoans.count { it.lateFeeAmount <= 0.0 }),
            ChartDataPoint("Atraso (1-3 días)", activeLoans.count { it.lateFeeAmount > 0.0 }.toDouble().let { if (it > 0) it else 10.0 }, count = activeLoans.count { it.lateFeeAmount > 0.0 }),
            ChartDataPoint("Mora Crítica", activeLoans.count { it.status == "CANCELADO" }.toDouble().let { if (it > 0) it else 5.0 }, count = activeLoans.count { it.status == "CANCELADO" })
        )'''
new_health = '''        val portfolioHealth = listOf(
            ChartDataPoint("Al Corriente", activeLoans.count { it.lateFeeAmount <= 0.0 }.toDouble(), count = activeLoans.count { it.lateFeeAmount <= 0.0 }),
            ChartDataPoint("Atraso (1-3 días)", activeLoans.count { it.lateFeeAmount > 0.0 }.toDouble(), count = activeLoans.count { it.lateFeeAmount > 0.0 }),
            ChartDataPoint("Mora Crítica", activeLoans.count { it.status == "CANCELADO" }.toDouble(), count = activeLoans.count { it.status == "CANCELADO" })
        )'''
content = content.replace(old_health, new_health)

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)

