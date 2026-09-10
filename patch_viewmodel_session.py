import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

# Replace the initial value of _userSession
old_init = """    private val _userSession = MutableStateFlow(
        UserSession(
            user = UserEntity(
                id = 1,
                username = "admin",
                passwordHash = "admin123",
                fullName = "Carlos Mendoza (Admin)",
                role = "ADMINISTRADOR",
                phone = "555-100-2000",
                email = "admin@rama.com",
                assignedZone = "Zona Centro"
            ),
            isLoggedIn = true
        )
    )"""

new_init = """    private val _userSession = MutableStateFlow(UserSession())"""

content = content.replace(old_init, new_init)

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)

