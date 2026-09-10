import re

with open('app/src/main/java/com/example/ui/screens/LoanDetailScreen.kt', 'r') as f:
    content = f.read()

old_card = """                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = l.clientName,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text("CURP: ${l.clientCurp}", style = MaterialTheme.typography.bodySmall)
                                }
                                RamaStatusBadge(status = l.status)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Capital Original:")
                                Text(currencyFormat.format(l.capital), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Interés Ganado:")
                                Text(currencyFormat.format(l.interestAmount), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Acordado:")
                                Text(currencyFormat.format(l.totalAmount), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Pagado:")
                                Text(currencyFormat.format(l.paidAmount), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo Restante:")
                                Text(
                                    currencyFormat.format(l.remainingBalance),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cuota Diaria:")
                                Text(currencyFormat.format(l.dailyPayment), fontWeight = FontWeight.Bold)
                            }
                        }
                    }"""

new_card = """                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val currentMora = installments.filter { it.status == "MORA" || it.status == "VENCIDO" }.sumOf { it.lateFee }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Folio: ${if(l.folio.isNotEmpty()) l.folio else "PR-${l.id}"}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(text = "Cliente: ${l.clientName}", style = MaterialTheme.typography.bodyMedium)
                                }
                                RamaStatusBadge(status = l.status)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Producto:", style = MaterialTheme.typography.bodySmall)
                                Text(l.planType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monto Original:", style = MaterialTheme.typography.bodySmall)
                                Text(currencyFormat.format(l.capital), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total:", style = MaterialTheme.typography.bodySmall)
                                Text(currencyFormat.format(l.totalAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pagado:", style = MaterialTheme.typography.bodySmall)
                                Text(currencyFormat.format(l.paidAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    currencyFormat.format(l.remainingBalance),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Mora Activa:", style = MaterialTheme.typography.bodySmall)
                                Text(currencyFormat.format(currentMora), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if(currentMora > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fecha inicial:", style = MaterialTheme.typography.bodySmall)
                                Text(dateFormat.format(Date(l.startDate)), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fecha final:", style = MaterialTheme.typography.bodySmall)
                                Text(dateFormat.format(Date(l.expectedEndDate)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }"""

content = content.replace(old_card, new_card)

with open('app/src/main/java/com/example/ui/screens/LoanDetailScreen.kt', 'w') as f:
    f.write(content)

