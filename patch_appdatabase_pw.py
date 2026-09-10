import re

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace('passwordHash = "admin123", // In production use Argon2/bcrypt', 'passwordHash = com.example.core.security.SecurityUtils.hashPassword("admin123"),')
content = content.replace('passwordHash = "cobrador123",', 'passwordHash = com.example.core.security.SecurityUtils.hashPassword("cobrador123"),')
content = content.replace('passwordHash = "super123",', 'passwordHash = com.example.core.security.SecurityUtils.hashPassword("super123"),')
content = content.replace('passwordHash = "consulta123",', 'passwordHash = com.example.core.security.SecurityUtils.hashPassword("consulta123"),')

# Check if there is fallbackToDestructiveMigration()
if "fallbackToDestructiveMigration()" in content:
    # Just comment it out, we don't want to destroy data automatically
    content = content.replace('.fallbackToDestructiveMigration()', '/* .fallbackToDestructiveMigration() */')

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'w') as f:
    f.write(content)

