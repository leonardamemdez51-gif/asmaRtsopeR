import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''            )
        )

        true
        }
    }''',
'''            )
        )

        true
        }
    ''')

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

