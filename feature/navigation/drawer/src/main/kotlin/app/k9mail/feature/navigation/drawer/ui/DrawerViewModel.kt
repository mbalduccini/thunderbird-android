package app.k9mail.feature.navigation.drawer.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class DrawerViewModel(
    private val getDrawerConfig: UseCase.GetDrawerConfig,
    private val getDisplayAccounts: UseCase.GetDisplayAccounts,
    private val getDisplayFoldersForAccount: UseCase.GetDisplayFoldersForAccount,
    private val syncMail: UseCase.SyncMail,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState,
),
    ViewModel {

    init {
        viewModelScope.launch {
            getDrawerConfig().collectLatest { config ->
                updateState {
                    it.copy(config = config)
                }
            }
        }

        viewModelScope.launch {
            loadAccounts()
        }

        viewModelScope.launch {
            loadFolders()
        }
    }

    private suspend fun loadAccounts() {
        getDisplayAccounts().collectLatest { accounts ->
            updateAccounts(accounts)
        }
    }

    private fun updateAccounts(accounts: List<DisplayAccount>) {
        val selectedAccount = accounts.find { it.account.uuid == state.value.selectedAccount?.account?.uuid }
            ?: accounts.firstOrNull()

        updateState {
            it.copy(
                accounts = accounts.toImmutableList(),
                selectedAccount = selectedAccount,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun loadFolders() {
        state.map {
            it.selectedAccount?.let { selectAccount ->
                Pair(selectAccount.account.uuid, it.config.showUnifiedFolders)
            }
        }.filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { (accountUuid, showUnifiedInbox) ->
                getDisplayFoldersForAccount(accountUuid, showUnifiedInbox)
            }.collectLatest { folders ->
                updateFolders(folders)
            }
    }

    private fun updateFolders(displayFolders: List<DisplayFolder>) {
        val selectedFolder = displayFolders.find {
            it.id == state.value.selectedFolder?.id
        } ?: displayFolders.firstOrNull()

        updateState {
            it.copy(
                folders = displayFolders.toImmutableList(),
                selectedFolder = selectedFolder,
            )
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnRefresh -> refresh()
            is Event.OnAccountClick -> selectAccount(event.account)
            is Event.OnFolderClick -> selectFolder(event.folder)
            is Event.OnAccountViewClick -> {
                selectAccount(
                    state.value.accounts.nextOrFirst(event.account)!!,
                )
            }

            Event.OnAccountSelectorClick -> updateState { it.copy(showAccountSelector = it.showAccountSelector.not()) }
            Event.OnManageFoldersClick -> emitEffect(Effect.OpenManageFolders)
            Event.OnSettingsClick -> emitEffect(Effect.OpenSettings)
        }
    }

    private fun selectAccount(account: DisplayAccount) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    selectedAccount = account,
                )
            }
        }

        emitEffect(Effect.OpenAccount(account.account))
    }

    private fun ImmutableList<DisplayAccount>.nextOrFirst(account: DisplayAccount): DisplayAccount? {
        val index = indexOf(account)
        return if (index == -1) {
            null
        } else if (index == size - 1) {
            get(0)
        } else {
            get(index + 1)
        }
    }

    private fun selectFolder(folder: DisplayFolder) {
        updateState {
            it.copy(selectedFolder = folder)
        }

        if (folder is DisplayAccountFolder) {
            emitEffect(Effect.OpenFolder(folder.folder.id))
        } else if (folder is DisplayUnifiedFolder) {
            emitEffect(Effect.OpenUnifiedFolder)
        }

        viewModelScope.launch {
            delay(DRAWER_CLOSE_DELAY)
            emitEffect(Effect.CloseDrawer)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            updateState {
                it.copy(isLoading = true)
            }

            syncMail(state.value.selectedAccount?.account).collect()

            updateState {
                it.copy(isLoading = false)
            }
        }
    }
}

/**
 * Delay before closing the drawer to avoid the drawer being closed immediately and give time
 * for the ripple effect to finish.
 */
private const val DRAWER_CLOSE_DELAY = 250L