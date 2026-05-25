package com.example.homeservices

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homeservices.adapter.ServiceProviderAdapter
import com.example.homeservices.model.Category
import com.example.homeservices.model.ServiceProvider
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CustomerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var categoriesRecycler: RecyclerView
    private lateinit var providersRecycler: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var searchView: SearchView

    private lateinit var providerAdapter: ServiceProviderAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val db by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private val providersList = ArrayList<ServiceProvider>()

    private var selectedCategory: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        setupToolbar()
        setupDrawer()
        setupViews()
        setupRecyclerViews()
        setupSearch()

        loadCategories()
        loadProviders()

        handleBackPress()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.title = "Home Services"
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)

        val navigationView: NavigationView =
            findViewById(R.id.nav_view)

        navigationView.setNavigationItemSelectedListener(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        updateNavHeader(navigationView)
    }

    private fun setupViews() {
        searchView = findViewById(R.id.searchView)

        categoriesRecycler = findViewById(R.id.rvCategories)

        providersRecycler = findViewById(R.id.rvProviders)

        emptyStateText = findViewById(R.id.tvNoProviders)
    }

    private fun setupRecyclerViews() {

        categoriesRecycler.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        providersRecycler.layoutManager =
            LinearLayoutManager(this)

        providerAdapter = ServiceProviderAdapter(
            providersList
        ) { provider ->

            val intent =
                Intent(this, ProviderDetailsActivity::class.java)

            intent.putExtra("provider", provider)

            startActivity(intent)
        }

        providersRecycler.adapter = providerAdapter
    }

    private fun setupSearch() {

        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {
                    filterProviders(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterProviders(newText)
                    return true
                }
            }
        )
    }

    private fun filterProviders(query: String?) {

        val text = query?.trim()?.lowercase() ?: ""

        if (text.isEmpty()) {
            updateProviders(providersList)
            return
        }

        val filtered = providersList.filter { provider ->

            provider.name.lowercase().contains(text) ||
                    provider.description.lowercase().contains(text)
        }

        updateProviders(ArrayList(filtered))
    }

    private fun updateNavHeader(navigationView: NavigationView) {

        val header = navigationView.getHeaderView(0)

        val userName =
            header.findViewById<TextView>(R.id.tvUserName)

        val userEmail =
            header.findViewById<TextView>(R.id.tvUserEmail)

        val currentUser = auth.currentUser ?: return

        userEmail.text = currentUser.email

        db.child("Users")
            .child(currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                val name =
                    snapshot.child("name").value?.toString()

                userName.text = name ?: "User"
            }
    }

    private fun loadCategories() {

        db.child("ServiceCategories")
            .get()
            .addOnSuccessListener { snapshot ->

                val categories = ArrayList<Category>()

                for (item in snapshot.children) {

                    val category =
                        item.getValue(Category::class.java)

                    if (category != null) {
                        categories.add(category)
                    }
                }

                categoriesRecycler.adapter =
                    CategoryAdapter(categories) { category ->

                        selectedCategory = category

                        searchView.setQuery("", false)

                        loadProviders(category)
                    }
            }
    }

    private fun loadProviders(category: Category? = null) {

        val providersRef = db.child("ServiceProviders")

        val request = if (category == null) {
            providersRef
        } else {
            providersRef
                .orderByChild("categoryID")
                .equalTo(category.id)
        }

        request.get()
            .addOnSuccessListener { snapshot ->

                providersList.clear()

                for (item in snapshot.children) {

                    val provider =
                        item.getValue(ServiceProvider::class.java)

                    if (provider != null) {

                        providersList.add(
                            provider.copy(
                                id = item.key ?: ""
                            )
                        )
                    }
                }

                updateProviders(providersList)
            }
    }

    private fun updateProviders(
        list: ArrayList<ServiceProvider>
    ) {

        emptyStateText.visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE

        providersRecycler.visibility =
            if (list.isEmpty()) View.GONE else View.VISIBLE

        providersRecycler.adapter =
            ServiceProviderAdapter(list) { provider ->

                val intent =
                    Intent(this, ProviderDetailsActivity::class.java)

                intent.putExtra("provider", provider)

                startActivity(intent)
            }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.nav_home -> {

                selectedCategory = null

                searchView.setQuery("", false)

                loadProviders()
            }

            R.id.nav_profile -> {
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                )
            }

            R.id.nav_requests -> {
                startActivity(
                    Intent(this, MyRequestsActivity::class.java)
                )
            }

            R.id.nav_logout -> {

                auth.signOut()

                startActivity(
                    Intent(this, MainActivity::class.java)
                )

                finish()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    private fun handleBackPress() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {

                        drawerLayout.closeDrawer(GravityCompat.START)

                    } else {

                        finish()
                    }
                }
            }
        )
    }
}