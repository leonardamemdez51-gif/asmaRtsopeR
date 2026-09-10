import re

with open('app/src/main/java/com/example/ui/screens/ProcessPaymentScreen.kt', 'r') as f:
    content = f.read()

old_header = """                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("¡Pago Registrado Exitosamente!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            Spacer(modifier = Modifier.height(16.dp))"""

new_header = """                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("¡Pago Registrado Exitosamente!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("RAMA ERP MICROFINANZAS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))"""

content = content.replace(old_header, new_header)

old_body = """                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Método:", style = MaterialTheme.typography.bodySmall)
                                Text(pay.method, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monto Pagado:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    currencyFormat.format(pay.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nuevo Saldo Restante:", style = MaterialTheme.typography.bodySmall)
                                val newBal = (currLoan.remainingBalance - pay.amount).coerceAtLeast(0.0)
                                Text(currencyFormat.format(newBal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }"""

new_body = """                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Método:", style = MaterialTheme.typography.bodySmall)
                                Text(pay.method, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Usuario:", style = MaterialTheme.typography.bodySmall)
                                Text(pay.collectorName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo Anterior:", style = MaterialTheme.typography.bodySmall)
                                Text(currencyFormat.format(currLoan.remainingBalance), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monto Aplicado:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    currencyFormat.format(pay.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo Posterior:", style = MaterialTheme.typography.bodySmall)
                                val newBal = (currLoan.remainingBalance - pay.amount).coerceAtLeast(0.0)
                                Text(currencyFormat.format(newBal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }"""

content = content.replace(old_body, new_body)

# Replace horizontal dividers properly
content = content.replace('Divider(modifier = Modifier.padding(vertical = 6.dp))', 'HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))')

with open('app/src/main/java/com/example/ui/screens/ProcessPaymentScreen.kt', 'w') as f:
    f.write(content)

