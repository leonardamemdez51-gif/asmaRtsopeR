package com.example.domain.usecases

import com.example.core.error.AppException
import com.example.core.error.Result
import com.example.core.logger.AppLogger
import com.example.data.local.ClientEntity
import com.example.domain.LoanCalculationResult
import com.example.domain.LoanCalculator
import com.example.domain.PlanType
import com.example.domain.ScoreCalculator
import com.example.domain.repositories.IClientRepository
import com.example.domain.repositories.IConfigRepository
import com.example.domain.repositories.ILoanRepository

class CalculateLoanUseCase(
    private val configRepository: IConfigRepository
) {
    suspend operator fun invoke(capital: Double, planTypeCode: String): Result<LoanCalculationResult> {
        return try {
            if (capital <= 0) {
                return Result.Error(AppException.ValidationException("El capital del préstamo debe ser mayor a 0"))
            }
            val config = configRepository.getConfigSync()
            val result = LoanCalculator.calculateLoan(capital, planTypeCode, config?.customLoanPlansJson)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(AppException.UnknownException("Error calculando simulación de préstamo", e))
        }
    }
}

class CreateLoanUseCase(
    private val loanRepository: ILoanRepository,
    private val clientRepository: IClientRepository
) {
    suspend operator fun invoke(
        clientId: Long,
        capital: Double,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String = "AGENTE",
        isRenewal: Boolean = false,
        previousLoanId: Long? = null
    ): Result<Long> {
        return try {
            if (capital <= 0) {
                return Result.Error(AppException.ValidationException("Monto inválido de préstamo"))
            }
            val client = clientRepository.getClientById(clientId)
                ?: return Result.Error(AppException.ValidationException("Cliente no encontrado"))

            if (client.status == "LISTA_NEGRA" || client.status == "BLOQUEADO" || client.status == "INACTIVO") {
                return Result.Error(AppException.ValidationException("No se puede otorgar préstamo: El cliente está en estado ${client.status}."))
            }

            val loanId = loanRepository.createLoan(
                clientId = clientId,
                capital = capital,
                planTypeCode = planTypeCode,
                skipSundays = skipSundays,
                collectorId = collectorId,
                collectorName = collectorName,
                currentUser = currentUser,
                currentUserRole = currentUserRole,
                isRenewal = isRenewal,
                previousLoanId = previousLoanId
            )
            AppLogger.audit("CREAR_PRESTAMO", currentUser, "Loan #$loanId created for client ${client.fullName}")
            Result.Success(loanId)
        } catch (e: Exception) {
            Result.Error(AppException.ValidationException(e.localizedMessage ?: "Error al crear el préstamo"))
        }
    }
}

class EditLoanUseCase(
    private val loanRepository: ILoanRepository
) {
    suspend operator fun invoke(
        loanId: Long,
        newCapital: Double,
        newPlanTypeCode: String,
        newSkipSundays: Boolean,
        currentUser: String,
        currentUserRole: String = "AGENTE"
    ): Result<Boolean> {
        return try {
            if (newCapital <= 0) {
                return Result.Error(AppException.ValidationException("El capital debe ser mayor a 0 MXN"))
            }
            val success = loanRepository.editLoanBeforeDisbursement(
                loanId = loanId,
                newCapital = newCapital,
                newPlanTypeCode = newPlanTypeCode,
                newSkipSundays = newSkipSundays,
                currentUser = currentUser,
                currentUserRole = currentUserRole
            )
            if (success) Result.Success(true)
            else Result.Error(AppException.ValidationException("No se pudo editar el préstamo #$loanId"))
        } catch (e: Exception) {
            Result.Error(AppException.ValidationException(e.localizedMessage ?: "Error al editar préstamo"))
        }
    }
}

class CancelLoanUseCase(
    private val loanRepository: ILoanRepository
) {
    suspend operator fun invoke(
        loanId: Long,
        currentUser: String,
        reason: String
    ): Result<Boolean> {
        return try {
            val success = loanRepository.cancelLoanBeforeDisbursement(loanId, currentUser, reason)
            if (success) Result.Success(true)
            else Result.Error(AppException.ValidationException("No se pudo cancelar el préstamo #$loanId"))
        } catch (e: Exception) {
            Result.Error(AppException.ValidationException(e.localizedMessage ?: "Error al cancelar préstamo"))
        }
    }
}

class AuthorizeLoanUseCase(
    private val loanRepository: ILoanRepository
) {
    suspend operator fun invoke(
        loanId: Long,
        currentUser: String,
        currentUserRole: String
    ): Result<Boolean> {
        return try {
            val success = loanRepository.authorizeLoan(loanId, currentUser, currentUserRole)
            if (success) Result.Success(true)
            else Result.Error(AppException.ValidationException("No se pudo autorizar el préstamo #$loanId"))
        } catch (e: Exception) {
            Result.Error(AppException.ValidationException(e.localizedMessage ?: "Error al autorizar préstamo"))
        }
    }
}

class DisburseLoanUseCase(
    private val loanRepository: ILoanRepository
) {
    suspend operator fun invoke(
        loanId: Long,
        collectorId: Long,
        collectorName: String,
        currentUser: String
    ): Result<Boolean> {
        return try {
            val success = loanRepository.disburseLoan(loanId, collectorId, collectorName, currentUser)
            if (success) Result.Success(true)
            else Result.Error(AppException.ValidationException("No se pudo desembolsar el préstamo #$loanId"))
        } catch (e: Exception) {
            Result.Error(AppException.ValidationException(e.localizedMessage ?: "Error al desembolsar préstamo"))
        }
    }
}

class RenewLoanUseCase(
    private val loanRepository: ILoanRepository
) {
    suspend operator fun invoke(
        clientId: Long,
        previousLoanId: Long,
        newCapital: Double,
        extraCapital: Double = 0.0,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String = "AGENTE"
    ): Result<Long> {
        return try {
            val newLoanId = loanRepository.renewPlusLoan(
                clientId = clientId,
                previousLoanId = previousLoanId,
                newCapital = newCapital,
                extraCapital = extraCapital,
                planTypeCode = planTypeCode,
                skipSundays = skipSundays,
                collectorId = collectorId,
                collectorName = collectorName,
                currentUser = currentUser,
                currentUserRole = currentUserRole
            )
            Result.Success(newLoanId)
        } catch (e: Exception) {
            Result.Error(AppException.ValidationException(e.localizedMessage ?: "Error al renovar préstamo"))
        }
    }
}

class ProcessPaymentUseCase(
    private val loanRepository: ILoanRepository
) {
    suspend operator fun invoke(
        loanId: Long,
        amount: Double,
        method: String,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        notes: String = ""
    ): Result<Boolean> {
        return try {
            if (amount <= 0) {
                return Result.Error(AppException.ValidationException("El monto del pago debe ser mayor a 0 MXN"))
            }
            val success = loanRepository.registerPayment(
                loanId = loanId,
                amount = amount,
                method = method,
                collectorId = collectorId,
                collectorName = collectorName,
                currentUser = currentUser,
                notes = notes
            )
            if (success) {
                Result.Success(true)
            } else {
                Result.Error(AppException.DatabaseException("No se pudo procesar el pago para el préstamo #$loanId"))
            }
        } catch (e: Exception) {
            Result.Error(AppException.UnknownException("Error al registrar pago", e))
        }
    }
}

class ScoreClientUseCase {
    operator fun invoke(paidInstallments: Int, overdueInstallments: Int): Int {
        return ScoreCalculator.calculateScore(paidInstallments, overdueInstallments)
    }
}
