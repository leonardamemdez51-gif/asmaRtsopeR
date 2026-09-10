import re

with open('app/src/main/java/com/example/core/security/SessionManager.kt', 'r') as f:
    content = f.read()

content = content.replace('''    fun getRememberedUsername(): String? {
        return if (prefs?.getBoolean(KEY_REMEMBER_ME, false) == true) {
            prefs?.getString(KEY_SAVED_USERNAME, null)
        } else null
    }''', '''    fun getRememberedUsername(): String? {
        return if (prefs?.getBoolean(KEY_REMEMBER_ME, false) == true) {
            prefs?.getString(KEY_SAVED_USERNAME, null)
        } else null
    }

    fun getRememberedToken(): String? {
        return if (prefs?.getBoolean(KEY_REMEMBER_ME, false) == true) {
            prefs?.getString(KEY_SAVED_TOKEN, null)
        } else null
    }''')

with open('app/src/main/java/com/example/core/security/SessionManager.kt', 'w') as f:
    f.write(content)
