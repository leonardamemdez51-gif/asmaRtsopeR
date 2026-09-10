import re

with open('app/src/main/java/com/example/ui/components/SmartPaymentScheduleComponent.kt', 'r') as f:
    content = f.read()

# Replace variables
content = content.replace(
    'var selectedFilter by remember { mutableStateOf("TODOS") } // "TODOS", "PAGADO", "PENDIENTE", "VENCIDO", "MORA", "PARCIAL"',
    '''var selectedFilter by remember { mutableStateOf("TODOS") } 
    var selectedOrder by remember { mutableStateOf("FECHA") } // "FECHA", "NUMERO", "SALDO", "MORA", "ESTADO"
    var showOrderMenu by remember { mutableStateOf(false) }'''
)

old_filter_logic = """    val filteredInstallments = remember(installments, selectedFilter) {
        when (selectedFilter) {
            "PAGADO" -> installments.filter { it.status == "PAGADO" }
            "PENDIENTE" -> installments.filter { it.status == "PENDIENTE" }
            "VENCIDO" -> installments.filter { it.status == "VENCIDO" }
            "MORA" -> installments.filter { it.status == "MORA" }
            "PARCIAL" -> installments.filter { it.status == "PARCIAL" }
            else -> installments
        }
    }"""

new_filter_logic = """    val filteredInstallments = remember(installments, selectedFilter, selectedOrder) {
        val nowMs = System.currentTimeMillis()
        val todayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(nowMs))
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val filtered = when (selectedFilter) {
            "PAGADAS" -> installments.filter { it.status == "PAGADO" }
            "PENDIENTES" -> installments.filter { it.status == "PENDIENTE" }
            "VENCIDAS" -> installments.filter { it.status == "VENCIDO" }
            "PARCIALES" -> installments.filter { it.status == "PARCIAL" }
            "HOY" -> installments.filter { sdf.format(Date(it.dueDate)) == todayFmt && it.status != "PAGADO" }
            "PROXIMAS" -> installments.filter { it.dueDate > nowMs && it.status != "PAGADO" }
            "CON_MORA" -> installments.filter { it.lateFee > 0.0 }
            "SIN_MORA" -> installments.filter { it.lateFee <= 0.0 }
            else -> installments
        }
        
        when (selectedOrder) {
            "FECHA" -> filtered.sortedBy { it.dueDate }
            "NUMERO" -> filtered.sortedBy { it.installmentNumber }
            "SALDO" -> filtered.sortedByDescending { it.remainingAmount }
            "MORA" -> filtered.sortedByDescending { it.lateFee }
            "ESTADO" -> filtered.sortedBy { it.status }
            else -> filtered
        }
    }"""
content = content.replace(old_filter_logic, new_filter_logic)

old_chips = """            val filterOptions = listOf(
                "TODOS" to "Todos (${installments.size})",
                "PAGADO" to "Pagados ($paidCount)",
                "PENDIENTE" to "Pendientes ($pendingCount)",
                "VENCIDO" to "Vencidos ($overdueCount)",
                "MORA" to "Mora ($lateFeeCount)",
                "PARCIAL" to "Parciales ($partialCount)"
            )

            filterOptions.forEach { (code, label) ->
                FilterChip(
                    selected = selectedFilter == code,
                    onClick = { selectedFilter = code },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }"""

new_chips = """            val filterOptions = listOf(
                "TODOS" to "Todas",
                "PENDIENTES" to "Pendientes",
                "PAGADAS" to "Pagadas",
                "PARCIALES" to "Parciales",
                "VENCIDAS" to "Vencidas",
                "HOY" to "Hoy",
                "PROXIMAS" to "Próximas",
                "CON_MORA" to "Con mora",
                "SIN_MORA" to "Sin mora"
            )

            filterOptions.forEach { (code, label) ->
                FilterChip(
                    selected = selectedFilter == code,
                    onClick = { selectedFilter = code },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }"""
content = content.replace(old_chips, new_chips)

old_header = """        // Header Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Calendario de Pagos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row {
                IconButton(onClick = { isTableView = false }) {
                    Icon(
                        Icons.Default.ViewTimeline,
                        contentDescription = "Vista de Línea de Tiempo",
                        tint = if (!isTableView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { isTableView = true }) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = "Vista de Tabla",
                        tint = if (isTableView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }"""

new_header = """        // Header Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Calendario de Pagos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row {
                Box {
                    IconButton(onClick = { showOrderMenu = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Ordenar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showOrderMenu,
                        onDismissRequest = { showOrderMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Ordenar por Fecha") }, onClick = { selectedOrder = "FECHA"; showOrderMenu = false })
                        DropdownMenuItem(text = { Text("Ordenar por Número") }, onClick = { selectedOrder = "NUMERO"; showOrderMenu = false })
                        DropdownMenuItem(text = { Text("Ordenar por Saldo") }, onClick = { selectedOrder = "SALDO"; showOrderMenu = false })
                        DropdownMenuItem(text = { Text("Ordenar por Mora") }, onClick = { selectedOrder = "MORA"; showOrderMenu = false })
                        DropdownMenuItem(text = { Text("Ordenar por Estado") }, onClick = { selectedOrder = "ESTADO"; showOrderMenu = false })
                    }
                }
                IconButton(onClick = { isTableView = false }) {
                    Icon(
                        Icons.Default.ViewTimeline,
                        contentDescription = "Vista de Línea de Tiempo",
                        tint = if (!isTableView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { isTableView = true }) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = "Vista de Tabla",
                        tint = if (isTableView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }"""
content = content.replace(old_header, new_header)

with open('app/src/main/java/com/example/ui/components/SmartPaymentScheduleComponent.kt', 'w') as f:
    f.write(content)

