import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

# Remove any imports from the very beginning (before package)
content = re.sub(r'^import androidx\.room\.withTransaction\nimport com\.example\.data\.local\.AppDatabase\n', '', content)

# Add them after the package declaration
content = content.replace("package com.example.data.repository", "package com.example.data.repository\nimport androidx.room.withTransaction\nimport com.example.data.local.AppDatabase")

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

