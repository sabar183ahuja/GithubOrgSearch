package com.sabar.githuborgsearch.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.sabar.githuborgsearch.data.model.Organization
import com.sabar.githuborgsearch.data.repository.OrganizationRepository
import com.sabar.githuborgsearch.network.responsehandler.ApiResult

class SearchViewModel(
    private val orgRepository: OrganizationRepository
) : ViewModel() {

    val organization: LiveData<ApiResult<Organization>> = orgRepository.organization

    init {
        Log.d(TAG, "initializing $TAG - ${hashCode()}")
    }

    /**
     * Try to retrieve the details for a GitHub Organization.
     */
    fun loadOrgDetails(searchInput: String) = orgRepository.getOrganization(searchInput)

    override fun onCleared() {
        super.onCleared()
        orgRepository.cancelGetOrganizationCall()
    }

    companion object {
        private const val TAG = "SearchViewModel"
    }
}
