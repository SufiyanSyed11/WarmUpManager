package com.warmup.manager.data.repository

import com.warmup.manager.data.db.AccountDao
import com.warmup.manager.data.db.SessionDao
import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.data.model.AccountWarmUpCalculation
import com.warmup.manager.data.model.Platform
import com.warmup.manager.data.model.WarmUpSessionEntity
import com.warmup.manager.util.WarmUpEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class WarmUpRepository(
    private val accountDao: AccountDao,
    private val sessionDao: SessionDao
) {
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getAccountsByPlatform(platform: Platform): Flow<List<AccountEntity>> =
        accountDao.getAccountsByPlatform(platform)

    fun getAccountById(id: Long): Flow<AccountEntity?> = accountDao.getAccountById(id)

    suspend fun getAccountByIdDirect(id: Long): AccountEntity? = accountDao.getAccountByIdDirect(id)

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    suspend fun deleteAccountById(id: Long) = accountDao.deleteAccountById(id)

    fun getSessionsForAccount(accountId: Long): Flow<List<WarmUpSessionEntity>> =
        sessionDao.getSessionsForAccount(accountId)

    suspend fun getSessionsForAccountDirect(accountId: Long): List<WarmUpSessionEntity> =
        sessionDao.getSessionsForAccountDirect(accountId)

    fun getAllSessions(): Flow<List<WarmUpSessionEntity>> = sessionDao.getAllSessions()

    suspend fun insertSession(session: WarmUpSessionEntity): Long = sessionDao.insertSession(session)

    suspend fun deleteSession(session: WarmUpSessionEntity) = sessionDao.deleteSession(session)

    /**
     * Combines accounts and their sessions to compute live warmup calculation
     */
    fun getPlatformCalculations(platform: Platform): Flow<List<AccountWarmUpCalculation>> {
        return combine(
            accountDao.getAccountsByPlatform(platform),
            sessionDao.getAllSessions()
        ) { accounts, allSessions ->
            val sessionsByAccount = allSessions.groupBy { it.accountId }
            accounts.map { account ->
                val sessions = sessionsByAccount[account.id] ?: emptyList()
                WarmUpEngine.calculateAccountProgress(account, sessions)
            }
        }
    }

    fun getAllCalculations(): Flow<List<AccountWarmUpCalculation>> {
        return combine(
            accountDao.getAllAccounts(),
            sessionDao.getAllSessions()
        ) { accounts, allSessions ->
            val sessionsByAccount = allSessions.groupBy { it.accountId }
            accounts.map { account ->
                val sessions = sessionsByAccount[account.id] ?: emptyList()
                WarmUpEngine.calculateAccountProgress(account, sessions)
            }
        }
    }

    fun getAccountCalculation(accountId: Long): Flow<AccountWarmUpCalculation?> {
        return combine(
            accountDao.getAccountById(accountId),
            sessionDao.getSessionsForAccount(accountId)
        ) { account, sessions ->
            if (account == null) null
            else WarmUpEngine.calculateAccountProgress(account, sessions)
        }
    }
}
