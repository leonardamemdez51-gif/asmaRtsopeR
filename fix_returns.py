import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

# I will replace all "true\n        }" with "return true\n    }"
# Wait, let's see how I replaced it exactly in fix_repo3:
# '''            )
#        )
#
#        true
#        }
#    '''
content = content.replace(
'''            )
        )

        true
        }
    ''',
'''            )
        )

        return true
    }''')

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

