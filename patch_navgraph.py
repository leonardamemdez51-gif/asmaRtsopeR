import re

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'r') as f:
    content = f.read()

effect = """    LaunchedEffect(session.isLoggedIn) {
        if (!session.isLoggedIn) {
            navController.navigate(NavRoute.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }"""

# Insert it after LaunchedEffect(uiMessage)
content = content.replace('        uiMessage?.let {\n            snackbarHostState.showSnackbar(it)\n            viewModel.clearUiMessage()\n        }\n    }', '        uiMessage?.let {\n            snackbarHostState.showSnackbar(it)\n            viewModel.clearUiMessage()\n        }\n    }\n\n' + effect)

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'w') as f:
    f.write(content)
