@file:JvmName("AppModule")

package com.sabar.githuborgsearch.di

import com.sabar.githuborgsearch.controllers.fragments.OrgDetailsFragment
import com.sabar.githuborgsearch.controllers.fragments.SearchFragment
import com.sabar.githuborgsearch.data.repository.OrganizationRepository
import com.sabar.githuborgsearch.data.repository.ReposRepository
import com.sabar.githuborgsearch.viewmodel.OrgDetailsViewModel
import com.sabar.githuborgsearch.viewmodel.SearchViewModel
import org.koin.androidx.fragment.dsl.fragment
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * AppModule:
 * - Dependencies: NetworkModule
 */
val AppModule = module {

    /* Repositories */
    single {
        OrganizationRepository(gitHubService = get())
    }

    single {
        ReposRepository(gitHubService = get())
    }

    /* Fragments */
    fragment {
        SearchFragment(picasso = get())
    }

    fragment {
        OrgDetailsFragment(picasso = get())
    }

    /* ViewModels */
    viewModel {
        SearchViewModel(orgRepository = get())
    }

    viewModel {
        OrgDetailsViewModel(reposRepository = get())
    }
}
