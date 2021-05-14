package com.sabar.githuborgsearch.controllers.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import com.sabar.githuborgsearch.R
import com.sabar.githuborgsearch.data.model.Organization
import com.sabar.githuborgsearch.extensions.displayTextOrHide
import com.sabar.githuborgsearch.network.responsehandler.ApiResult
import com.sabar.githuborgsearch.network.responsehandler.formattedErrorMessage
import com.sabar.githuborgsearch.network.responsehandler.responseData
import com.sabar.githuborgsearch.viewmodel.OrgDetailsViewModel
import com.sabar.githuborgsearch.viewmodel.SearchViewModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.card_org_full.*
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.widget_search_bar.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Fragment associated with searching for an Organization and displaying
 * details about the Organization (or error messages if not found).
 */
class SearchFragment(private val picasso: Picasso) : Fragment() {

    private val viewModel: SearchViewModel by viewModel()

    /**
     * [OrgDetailsViewModel] reference used for pre-fetching view data for next screen,
     * and setting the selected org.
     */
    private val orgDetailsViewModel: OrgDetailsViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: savedInstanceState: $savedInstanceState")

        registerListeners()
        observeViewModel()
    }

    /** Set listeners */
    private fun registerListeners() {
        /* Trigger GitHub Org search call when either the Search button is pressed
           or the search key is pressed on keyboard */
        searchButton.setOnClickListener { performSearch() }
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    performSearch()
                    true
                }
                else -> false
            }
        }

        // Tap outside of text field dismisses keyboard
        searchFragment.setOnClickListener {
            activity?.let(this::hideSoftKeyBoard)
        }

        orgCardView.setOnClickListener {
            val apiResult = viewModel.organization.value
            apiResult.responseData?.let {
                onOrgSelected(it)
            } ?: Log.e(
                TAG,
                "registerListeners: cardView clicked but not success - apiResult = $apiResult"
            )
        }
    }

    private fun observeViewModel() {
        viewModel.organization.observe(viewLifecycleOwner) { orgApiResult ->
            Log.d(TAG, "(Observer) orgApiResult => $orgApiResult")
            orgApiResult?.let {
                updateUI(it)
            } ?: Log.d(TAG, "orgApiResult null")
        }
    }

    /**
     * Update UI in response to change in observed ViewModel data.
     */
    private fun updateUI(apiResult: ApiResult<Organization>) = when (apiResult) {
        is ApiResult.Loading -> {
            progressBar.isVisible = true
            errorTextView.isVisible = false
            orgCardView.isInvisible = true
        }
        is ApiResult.Cancelled -> {
            progressBar.isVisible = false
            errorTextView.isVisible = false
            orgCardView.isInvisible = true
        }
        is ApiResult.Exception -> {
            Log.e(TAG, "updateUI: ApiResult.Exception: $apiResult")
            Log.e(TAG, "stack trace: ${apiResult.throwable.stackTrace}")

            progressBar.isInvisible = true
            orgCardView.isInvisible = true
            displayErrorMessage(apiResult.formattedErrorMessage)
        }
        is ApiResult.Error -> {
            Log.e(TAG, "updateUI: ApiResult.Error: $apiResult")
            progressBar.isInvisible = true
            orgCardView.isInvisible = true
            displayErrorMessage(apiResult.formattedErrorMessage)
        }
        is ApiResult.Success -> {
            val org = apiResult.data
            progressBar.isInvisible = true
            errorTextView.isVisible = false
            showOrgDetails(org)
            prefetchTopRepos(org)
        }
    }

    private fun performSearch() {
        val orgQuery = searchEditText.text.toString().trim()

        when {
            orgQuery.isBlank() -> searchEditText.error = EMPTY_SEARCH_ERROR_MESSAGE
            else -> {
                // Dismiss Keyboard
                searchEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)

                // Fetch Org info
                viewModel.loadOrgDetails(orgQuery)
            }
        }
    }

    private fun showOrgDetails(org: Organization) {
        orgCardView.isVisible = true

        org.apply {
            picasso.load(avatarUrl).into(orgImageView)

            orgNameTextView.text = name
            orgLoginTextView.text = login

            mapOf(
                orgLocationTextView to location,
                orgBlogTextView to blogUrl,
                orgDescriptionTextView to description
            ).forEach { (textView, text) ->
                textView.displayTextOrHide(text)
            }
        }
    }

    private fun prefetchTopRepos(org: Organization) {
        orgDetailsViewModel.getReposForOrg(org)
    }

    private fun onOrgSelected(org: Organization) {
        Log.d(TAG, "onOrgSelected($org)")
        // set the selected organization in that ViewModel
        orgDetailsViewModel.selectedOrganization.value = org

        // Replace SearchFragment with OrgDetailsFragment
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.replace<OrgDetailsFragment>(R.id.fragment_container)
            ?.addToBackStack(null)
            ?.commit()
    }

    private fun hideSoftKeyBoard(parentActivity: Activity) {
        val inputMethodManager = parentActivity.getSystemService(Context.INPUT_METHOD_SERVICE)
            as? InputMethodManager

        val currentFocus = parentActivity.currentFocus ?: return
        inputMethodManager?.takeIf { it.isAcceptingText }
            ?.apply { hideSoftInputFromWindow(currentFocus.windowToken, 0) }
    }

    private fun displayErrorMessage(errorMessage: String?) = if (errorMessage.isNullOrBlank()) {
        errorTextView.isVisible = false
        errorTextView.text = ""
    } else {
        errorTextView.isVisible = true
        errorTextView.text = errorMessage
    }

    companion object {
        private const val TAG = "SearchFragment"
        internal const val EMPTY_SEARCH_ERROR_MESSAGE =
            "Please enter an organization name (e.g., 'nytimes')"
    }
}
