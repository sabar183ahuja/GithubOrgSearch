package com.sabar.githuborgsearch.controllers.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.sabar.githuborgsearch.R
import com.sabar.githuborgsearch.adapters.RepoListAdapter
import com.sabar.githuborgsearch.controllers.activities.MainActivity
import com.sabar.githuborgsearch.data.model.Organization
import com.sabar.githuborgsearch.data.model.Repo
import com.sabar.githuborgsearch.network.responsehandler.ApiResult
import com.sabar.githuborgsearch.network.responsehandler.formattedErrorMessage
import com.sabar.githuborgsearch.viewmodel.OrgDetailsViewModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.card_org_condensed.*
import kotlinx.android.synthetic.main.fragment_org_details.*
import kotlinx.android.synthetic.main.screen_list.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Fragment which displays the top (most-starred) repos for
 * an organization.
 */
class OrgDetailsFragment(private val picasso: Picasso) : Fragment() {

    private val viewModel: OrgDetailsViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_org_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If selectedOrganization (LiveData) is null, the ViewModel was destroyed.
        // Restore from Bundle. Otherwise we don't need to do anything.
        restoreFromBundle(savedInstanceState)

        // RecyclerView setup
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        recyclerView.adapter = RepoListAdapter(viewModel, this, ::onRepoSelected)
        recyclerView.layoutManager = LinearLayoutManager(context)

        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.selectedOrganization.value?.let { org ->
            outState.putParcelable(KEY_SELECTED_ORGANIZATION, org)
        }
    }

    private fun restoreFromBundle(savedInstanceState: Bundle?) {
        if (viewModel.selectedOrganization.value == null) {
            savedInstanceState?.getParcelable<Organization>(KEY_SELECTED_ORGANIZATION)?.let {
                viewModel.selectedOrganization.value = it
            }
        }
    }

    private fun onRepoSelected(repo: Repo) =
        (activity as MainActivity).openWebContent(repo.repoUrl)

    private fun observeViewModel() {
        viewModel.selectedOrganization.observe(viewLifecycleOwner) { org: Organization? ->
            Log.d(TAG, "(Observer): selectedOrganization => $org")
            org?.let { newOrg ->
                viewModel.getReposForOrg(newOrg)
                showCondensedOrgDetails(newOrg)
            }
        }

        viewModel.topRepos.observe(viewLifecycleOwner) { topReposResource ->
            Log.d(TAG, "(Observer): topRepos => $topReposResource")
            topReposResource?.let { updateUI(it) }
        }
    }

    private fun updateUI(apiResult: ApiResult<List<Repo>>) = when (apiResult) {
        is ApiResult.Loading -> {
            repoProgressBar.isVisible = true
            repoErrorTextView.isVisible = false
        }
        is ApiResult.Cancelled -> {
            repoProgressBar.isVisible = false
            repoErrorTextView.isVisible = false
        }
        is ApiResult.Exception -> {
            repoProgressBar.isVisible = false
            displayErrorMessage(apiResult.formattedErrorMessage)
        }
        is ApiResult.Error -> {
            repoProgressBar.isVisible = false
            displayErrorMessage(apiResult.formattedErrorMessage)
        }
        is ApiResult.Success -> {
            // If an org. exists but owns 0 repos, display an error message (e.g. 'nytime')
            // Other updates handled by RepoListAdapter.
            orgOwnsNoReposErrorMessage.isVisible = apiResult.data.isEmpty()
            repoProgressBar.isVisible = false
            repoErrorTextView.isVisible = false
        }
    }

    private fun showCondensedOrgDetails(org: Organization) {
        org.apply {
            picasso.load(avatarUrl).into(orgAvatarImageView)
            condensedOrgNameTextView.text = name
            condensedOrgLoginTextView.text = getString(R.string.org_login_condensed, login)
        }
    }

    private fun displayErrorMessage(errorMessage: String?) = if (errorMessage.isNullOrBlank()) {
        repoErrorTextView.isVisible = false
        repoErrorTextView.text = ""
    } else {
        repoErrorTextView.isVisible = true
        repoErrorTextView.text = errorMessage
    }

    companion object {
        private const val TAG = "OrgDetailsFragment"
        private const val KEY_SELECTED_ORGANIZATION = "selected_org_details"
    }
}
