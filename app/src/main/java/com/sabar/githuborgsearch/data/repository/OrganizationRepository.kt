package com.sabar.githuborgsearch.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sabar.githuborgsearch.data.model.Organization
import com.sabar.githuborgsearch.network.GitHubService
import com.sabar.githuborgsearch.network.responsehandler.ApiResult
import com.sabar.githuborgsearch.network.responsehandler.ResponseConverter
import com.sabar.githuborgsearch.network.responsehandler.defaultResponseConverter
import com.sabar.githuborgsearch.network.responsehandler.isCompleted
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Repository responsible for retrieving data about an organization on GitHub
 * from the database or over the network.
 */

class OrganizationRepository(private val gitHubService: GitHubService) {

    init {
        Log.d(TAG, "initializing $TAG - ${hashCode()}")
    }

    private val responseConverter: ResponseConverter<Organization> =
        ::defaultResponseConverter

    // basic in-memory cache (orgName: String => Organization)
    private val orgCache: MutableMap<String, ApiResult<Organization>> = HashMap()

    /* Mutable backing field */
    private val _organization = MutableLiveData<ApiResult<Organization>>()

    /* Publicly exposed immutable LiveData */
    val organization: LiveData<ApiResult<Organization>> = _organization

    private var orgCall: Call<Organization>? = null

    /**
     * Retrieve the details for a GitHub Organization from database (currently unimplemented)
     * or network.
     */
    fun getOrganization(organizationName: String) {
        Log.d(TAG, "getOrganization($organizationName)")

        // Check if response cached
        if (organizationName in orgCache) {
            val cachedResult = orgCache[organizationName]
            if (cachedResult.isCompleted) {
                Log.d(TAG, "$organizationName in orgCache & completed")
                _organization.value = cachedResult
                return
            } else {
                Log.d(TAG, "$organizationName in orgCache, but result not completed")
            }
        }

        // Set as loading
        _organization.value = ApiResult.Loading

        orgCall = gitHubService.getOrg(organizationName)
        orgCall?.enqueue(
            object : Callback<Organization> {
                override fun onResponse(call: Call<Organization>, response: Response<Organization>) {
                    Log.d(TAG, "getOrganization - onResponse: response.body = ${response.body()}")

                    val apiResult = responseConverter(response)
                    _organization.value = apiResult
                    orgCache[organizationName] = apiResult
                }

                override fun onFailure(call: Call<Organization>, t: Throwable) {
                    Log.e(TAG, t.message, t)
                    val apiResult = ApiResult.Exception(t)
                    orgCache[organizationName] = apiResult
                    _organization.value = apiResult
                }
            }
        )
    }

    internal fun cancelGetOrganizationCall() {
        orgCall?.cancel()
        _organization.value = ApiResult.Cancelled
    }

    companion object {
        private const val TAG = "OrganizationRepository"
    }
}
