package com.izettle.wrench.configurationlist

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.izettle.wrench.core.Bolt
import com.izettle.wrench.database.WrenchApplication
import com.izettle.wrench.database.WrenchConfigurationWithValues
import com.izettle.wrench.dialogs.scope.ScopeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_configurations.*
import se.eelde.toggles.R

@AndroidEntryPoint
class ConfigurationsFragment : Fragment(), SearchView.OnQueryTextListener, ConfigurationRecyclerViewAdapter.Listener {
    private var currentFilter: CharSequence? = null
    private var searchView: SearchView? = null

    private val args: ConfigurationsFragmentArgs by navArgs()

    private val model by viewModels<ConfigurationViewModel>()

    private fun updateConfigurations(wrenchConfigurations: List<WrenchConfigurationWithValues>) {
        var adapter = list.adapter as ConfigurationRecyclerViewAdapter?
        if (adapter == null) {
            adapter = ConfigurationRecyclerViewAdapter(this, model)
            list.adapter = adapter
        }
        adapter.submitList(wrenchConfigurations)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(STATE_FILTER, searchView!!.query)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            currentFilter = savedInstanceState.getCharSequence(STATE_FILTER)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_configurations, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.setApplicationId(args.applicationId)

        list.layoutManager = LinearLayoutManager(context)

        model.wrenchApplication.observe(viewLifecycleOwner, Observer { wrenchApplication ->
            if (wrenchApplication != null) {
                (activity as AppCompatActivity).supportActionBar!!.title = wrenchApplication.applicationLabel
            }
        })

        model.defaultScopeLiveData.observe(viewLifecycleOwner, Observer { scope ->
            if (scope != null && list.adapter != null) {
                list.adapter!!.notifyDataSetChanged()
            }
        })

        model.selectedScopeLiveData.observe(viewLifecycleOwner, Observer { scope ->
            if (scope != null && list.adapter != null) {
                list.adapter!!.notifyDataSetChanged()
            }
            // fragmentConfigurationsBinding.scopeButton.text = scope!!.name
        })

        model.configurations.observe(viewLifecycleOwner, Observer { wrenchConfigurationWithValues ->
            if (wrenchConfigurationWithValues != null) {
                updateConfigurations(wrenchConfigurationWithValues)
            }
        })

        model.isListEmpty.observe(viewLifecycleOwner, Observer { isEmpty ->
            val animator = animator
            if (isEmpty == null || isEmpty) {
                animator.displayedChild = animator.indexOfChild(no_configurations_empty_view)
            } else {
                animator.displayedChild = animator.indexOfChild(list)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_configurations_list, menu)

        val item = menu.findItem(R.id.action_filter_configurations)
        searchView = item.actionView as SearchView
        searchView!!.setOnQueryTextListener(this)

        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                return true // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true // Return true to expand action view
            }
        })

        if (currentFilter.isNullOrBlank()) {
            item.expandActionView()
            searchView!!.setQuery(currentFilter, true)
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        model.setQuery(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_restart_application -> {
                model.wrenchApplication.observe(this, object : Observer<WrenchApplication> {
                    override fun onChanged(wrenchApplication: WrenchApplication?) {
                        model.wrenchApplication.removeObserver(this)

                        if (wrenchApplication == null) {
                            return
                        }

                        val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        activityManager.killBackgroundProcesses(wrenchApplication.packageName)

                        val intent = context!!.packageManager.getLaunchIntentForPackage(wrenchApplication.packageName)
                        if (intent != null) {
                            context!!.startActivity(Intent.makeRestartActivityTask(intent.component))
                        } else if (this@ConfigurationsFragment.view != null) {
                            Snackbar.make(this@ConfigurationsFragment.requireView(), R.string.application_not_installed, Snackbar.LENGTH_LONG).show()
                        }
                    }
                })

                return true
            }
            R.id.action_application_settings -> {
                model.wrenchApplication.observe(this, object : Observer<WrenchApplication> {
                    override fun onChanged(wrenchApplication: WrenchApplication?) {
                        model.wrenchApplication.removeObserver(this)
                        if (wrenchApplication == null) {
                            return
                        }

                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", wrenchApplication.packageName, null)))
                    }
                })
                return true
            }
            R.id.action_delete_application -> {
                model.deleteApplication(model.wrenchApplication.value!!)
                Navigation.findNavController(animator).navigateUp()
                return true
            }
            R.id.action_change_scope -> {
                val args = ConfigurationsFragmentArgs.fromBundle(requireArguments())
                ScopeFragment.newInstance(args.applicationId).show(childFragmentManager, null)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun configurationClicked(v: View, configuration: WrenchConfigurationWithValues) {
        if (model.selectedScopeLiveData.value == null) {
            Snackbar.make(animator, "No selected scope found", Snackbar.LENGTH_LONG).show()
            return
        }

        val selectedScopeId = model.selectedScopeLiveData.value!!.id

        if (TextUtils.equals(String::class.java.name, configuration.type) || TextUtils.equals(Bolt.TYPE.STRING, configuration.type)) {

            v.findNavController().navigate(ConfigurationsFragmentDirections.actionConfigurationsFragmentToStringValueFragment(configuration.id, selectedScopeId))

        } else if (TextUtils.equals(Int::class.java.name, configuration.type) || TextUtils.equals(Bolt.TYPE.INTEGER, configuration.type)) {

            v.findNavController().navigate(ConfigurationsFragmentDirections.actionConfigurationsFragmentToIntegerValueFragment(configuration.id, selectedScopeId))

        } else if (TextUtils.equals(Boolean::class.java.name, configuration.type) || TextUtils.equals(Bolt.TYPE.BOOLEAN, configuration.type)) {

            v.findNavController().navigate(ConfigurationsFragmentDirections.actionConfigurationsFragmentToBooleanValueFragment(configuration.id, selectedScopeId))

        } else if (TextUtils.equals(Enum::class.java.name, configuration.type) || TextUtils.equals(Bolt.TYPE.ENUM, configuration.type)) {

            v.findNavController().navigate(ConfigurationsFragmentDirections.actionConfigurationsFragmentToEnumValueFragment(configuration.id, selectedScopeId))

        } else {

            Snackbar.make(animator, "Not sure what to do with type: " + configuration.type!!, Snackbar.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val STATE_FILTER = "STATE_FILTER"
    }
}
