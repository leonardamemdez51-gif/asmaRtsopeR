import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

# For monthlyCollection
old_monthly = '''        val monthlyCollection = (0..5).map { offset ->
            val idx = (currentMonthIdx - 5 + offset + 12) % 12
            val mLabel = months[idx]
            val factor = 0.7 + (offset * 0.06) + ((idx % 3) * 0.05)
            val baseVal = if (realizedCollection > 0) realizedCollection * factor / 2.5 else 12500.0 * (offset + 1)
            ChartDataPoint(mLabel, baseVal)
        }'''

new_monthly = '''        val monthlyCollection = (0..5).map { offset ->
            val idx = (currentMonthIdx - 5 + offset + 12) % 12
            val mLabel = months[idx]
            ChartDataPoint(mLabel, 0.0) // Real metrics should be calculated from database historicals
        }'''

content = content.replace(old_monthly, new_monthly)

# For weeklyCollection
old_weekly = '''        val daysOfWeek = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val weeklyCollection = daysOfWeek.mapIndexed { idx, day ->
            val factor = if (idx == 6) 0.2 else (0.8 + (idx * 0.08))
            val valForDay = if (todayPaymentsAmount > 0) todayPaymentsAmount * factor else 2400.0 * (idx + 1)
            ChartDataPoint(day, valForDay)
        }'''

new_weekly = '''        val daysOfWeek = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val weeklyCollection = daysOfWeek.mapIndexed { idx, day ->
            ChartDataPoint(day, 0.0) // Real metrics should be calculated from database historicals
        }'''

content = content.replace(old_weekly, new_weekly)

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)

