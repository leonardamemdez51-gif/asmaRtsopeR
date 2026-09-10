package com.example.ui.navigation

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login")
    object Dashboard : NavRoute("dashboard")
    object SupervisorDashboard : NavRoute("supervisor_dashboard")
    object ClientList : NavRoute("client_list")
    object NewClient : NavRoute("new_client")
    object EditClient : NavRoute("edit_client/{clientId}") {
        fun createRoute(clientId: Long) = "edit_client/$clientId"
    }
    object ClientDetail : NavRoute("client_detail/{clientId}") {
        fun createRoute(clientId: Long) = "client_detail/$clientId"
    }
    object LoanList : NavRoute("loan_list")
    object NewLoan : NavRoute("new_loan?clientId={clientId}") {
        fun createRoute(clientId: Long? = null) = if (clientId != null) "new_loan?clientId=$clientId" else "new_loan"
    }
    object RenewalLoan : NavRoute("renewal_loan/{loanId}") {
        fun createRoute(loanId: Long) = "renewal_loan/$loanId"
    }
    object LoanDetail : NavRoute("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    object ProcessPayment : NavRoute("process_payment/{loanId}") {
        fun createRoute(loanId: Long) = "process_payment/$loanId"
    }
    object CashRegister : NavRoute("cash_register")
    object CollectionVisits : NavRoute("collection_visits")
    object DailyTasks : NavRoute("daily_tasks")
    object Reports : NavRoute("reports")
    object AuditLogs : NavRoute("audit_logs")
    object Settings : NavRoute("settings")
    object UserManagement : NavRoute("user_management")
    object ForgotPassword : NavRoute("forgot_password")
    object ChangePassword : NavRoute("change_password")
    object UserProfile : NavRoute("user_profile")
    object NotificationCenter : NavRoute("notification_center")
    object CollectorMap : NavRoute("collector_map")
    object RouteManagement : NavRoute("route_management")
    object DocumentTypeConfig : NavRoute("document_type_config")
}

