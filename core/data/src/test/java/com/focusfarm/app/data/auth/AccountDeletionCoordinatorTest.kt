package com.focusfarm.app.data.auth

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountDeletionCoordinatorTest {
    private class RecentLoginRequiredForTest : Exception()

    @Test
    fun successDeletesRemoteBeforeClearingLocalData() = runTest {
        val calls = mutableListOf<String>()
        val result = AccountDeletionCoordinator.deleteAccount(
            hasSignedInAccount = true,
            deleteRemote = { calls += "firebase" },
            clearLocal = { calls += "local" },
        )
        assertThat(result).isEqualTo(AccountDeletionResult.Success)
        assertThat(calls).containsExactly("firebase", "local").inOrder()
    }

    @Test
    fun firebaseFailureOrRecentLoginDoesNotClearLocalData() = runTest {
        var localCalls = 0
        val failed = AccountDeletionCoordinator.deleteAccount(
            true,
            deleteRemote = { error("network") },
            clearLocal = { localCalls++ },
        )
        val reauth = AccountDeletionCoordinator.deleteAccount(
            true,
            deleteRemote = { throw RecentLoginRequiredForTest() },
            clearLocal = { localCalls++ },
            requiresRecentLogin = { it is RecentLoginRequiredForTest },
        )
        assertThat(failed).isEqualTo(AccountDeletionResult.RemoteDeletionFailed)
        assertThat(reauth).isEqualTo(AccountDeletionResult.RequiresRecentLogin)
        assertThat(localCalls).isEqualTo(0)
    }

    @Test
    fun signedOutAccountDeletionIsNotReportedAsSuccessButLocalClearWorks() = runTest {
        var remoteCalls = 0
        val noAccount = AccountDeletionCoordinator.deleteAccount(
            false,
            deleteRemote = { remoteCalls++ },
            clearLocal = { error("must not run") },
        )
        val localOnly = AccountDeletionCoordinator.clearLocalData { }
        val localFailure = AccountDeletionCoordinator.clearLocalData { error("database") }
        assertThat(noAccount).isEqualTo(AccountDeletionResult.NoSignedInAccount)
        assertThat(localOnly).isEqualTo(AccountDeletionResult.Success)
        assertThat(localFailure).isEqualTo(AccountDeletionResult.LocalCleanupFailed)
        assertThat(remoteCalls).isEqualTo(0)
    }

    @Test
    fun localFailureAfterFirebaseDeletionIsDistinctFromFullSuccess() = runTest {
        val result = AccountDeletionCoordinator.deleteAccount(
            true,
            deleteRemote = { },
            clearLocal = { error("database") },
        )
        assertThat(result).isEqualTo(AccountDeletionResult.AccountDeletedLocalCleanupFailed)
    }
}
