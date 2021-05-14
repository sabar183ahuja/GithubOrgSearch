package com.sabar.githuborgsearch.network

import com.sabar.githuborgsearch.data.model.Organization
import com.sabar.githuborgsearch.data.model.Repo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * Retrofit interface for GitHub API.
 */
interface GitHubService {

    /**
     * Search for an organization.
     */
    @Headers("Accept: application/vnd.github.v3+json", "Content-Type: application/json")
    @GET("orgs/{org}")
    fun getOrg(@Path("org") org: String): Call<Organization>

    /**
     * Fetch the list of repositories associated with an organization.
     */
    @Headers("Accept: application/vnd.github.v3+json", "Content-Type: application/json")
    @GET("orgs/{org}/repos")
    fun getRepositoriesForOrg(@Path("org") org: String): Call<List<Repo>>
}
