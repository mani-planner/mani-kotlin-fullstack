package ru.workinprogress.feature.transaction.domain

import ru.workinprogress.feature.transaction.Transaction
import ru.workinprogress.feature.transaction.data.TransactionRepository
import ru.workinprogress.useCase.UseCase

class GetTransactionUseCase(private val transactionRepository: TransactionRepository) :
    UseCase<String, Transaction>() {

    override suspend fun invoke(params: String): Result<Transaction> {
        return withTry { transactionRepository.getById(params) }
    }
}