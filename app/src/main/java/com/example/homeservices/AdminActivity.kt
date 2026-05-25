package com.example.homeservices

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.homeservices.adapter.ServiceProviderAdapter
import com.example.homeservices.model.Category
import com.example.homeservices.model.LocationModel
import com.example.homeservices.model.ServiceProvider
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class AdminActivity : AppCompatActivity() {

    private lateinit var categoriesRecycler: RecyclerView
    private lateinit var providersRecycler: RecyclerView
    private lateinit var totalUsersText: TextView
    private lateinit var totalProvidersText: TextView
    private lateinit var totalRequestsText: TextView
    private lateinit var totalRevenueText: TextView

    private val database: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }
    private val storage: StorageReference by lazy { FirebaseStorage.getInstance().reference }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val categories = arrayListOf<Category>()
    private val providers = arrayListOf<ServiceProvider>()

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var providerAdapter: ServiceProviderAdapter

    private var imageUri: Uri? = null
    private var activeImageView: ImageView? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult

        imageUri = uri
        activeImageView?.setImageURI(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        setupToolbar()
        bindViews()
        setupLists()
        setupActions()

        loadStats()
        loadCategories()
        loadProviders()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "لوحة التحكم"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { logout() }
    }

    private fun bindViews() {
        totalUsersText = findViewById(R.id.tvTotalUsers)
        totalProvidersText = findViewById(R.id.tvTotalProviders)
        totalRequestsText = findViewById(R.id.tvTotalRequests)
        totalRevenueText = findViewById(R.id.tvTotalRevenue)
        categoriesRecycler = findViewById(R.id.rvCategories)
        providersRecycler = findViewById(R.id.rvProviders)
    }

    private fun setupLists() {
        categoryAdapter = CategoryAdapter(categories) { category -> showCategoryOptions(category) }
        providerAdapter = ServiceProviderAdapter(providers) { provider -> showProviderOptions(provider) }

        categoriesRecycler.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        providersRecycler.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = providerAdapter
        }
    }

    private fun setupActions() {
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            showCategoryDialog()
        }

        findViewById<Button>(R.id.btnAddProvider).setOnClickListener {
            showProviderDialog()
        }

        findViewById<MaterialCardView>(R.id.cardRequests).setOnClickListener {
            startActivity(Intent(this, ManageRequestsActivity::class.java))
        }
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun loadStats() {
        database.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalUsersText.text = snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                showMessage(error.message)
            }
        })

        database.child("ServiceProviders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalProvidersText.text = snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                showMessage(error.message)
            }
        })

        database.child("ServiceRequests").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalRequestsText.text = snapshot.childrenCount.toString()
                totalRevenueText.text = "$${calculateRevenue(snapshot)}"
            }

            override fun onCancelled(error: DatabaseError) {
                showMessage(error.message)
            }
        })
    }

    private fun calculateRevenue(snapshot: DataSnapshot): Int {
        var revenue = 0

        snapshot.children.forEach { request ->
            val isCompleted = request.child("status").value?.toString() == "completed"
            if (!isCompleted) return@forEach

            val hours = request.child("hoursRequested").value
                ?.toString()
                ?.toDoubleOrNull()
                ?.toInt() ?: 0

            revenue += hours * 40
        }

        return revenue
    }

    private fun loadCategories() {
        database.child("ServiceCategories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()

                snapshot.children.forEach { item ->
                    item.getValue(Category::class.java)?.let(categories::add)
                }

                categoryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                showMessage(error.message)
            }
        })
    }

    private fun loadProviders() {
        database.child("ServiceProviders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                providers.clear()

                snapshot.children.forEach { item ->
                    val provider = item.getValue(ServiceProvider::class.java) ?: return@forEach
                    providers.add(provider.copy(id = item.key.orEmpty()))
                }

                providers.sortByDescending { it.rate }
                providerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                showMessage(error.message)
            }
        })
    }

    private fun showCategoryDialog(category: Category? = null) {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_add_category)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val titleText = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val nameInput = dialog.findViewById<EditText>(R.id.etCategoryName)
        val imageView = dialog.findViewById<ImageView>(R.id.ivCategoryDialog)
        val saveButton = dialog.findViewById<Button>(R.id.btnSaveCategory)

        imageUri = null
        activeImageView = imageView

        if (category != null) {
            titleText.text = "تعديل الفئة"
            nameInput.setText(category.name)
            saveButton.text = "تحديث"
            loadImage(category.image, imageView)
        }

        imageView.setOnClickListener { imagePicker.launch("image/*") }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = "أدخل اسم الفئة"
                return@setOnClickListener
            }

            val pickedImage = imageUri
            if (pickedImage == null) {
                saveCategory(name, category?.image.orEmpty(), category?.id)
                dialog.dismiss()
                return@setOnClickListener
            }

            uploadImage(pickedImage) { imageUrl ->
                saveCategory(name, imageUrl, category?.id)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun saveCategory(name: String, imageUrl: String, categoryId: String?) {
        val id = categoryId ?: database.child("ServiceCategories").push().key ?: return
        val category = Category(id = id, name = name, image = imageUrl)

        database.child("ServiceCategories")
            .child(id)
            .setValue(category)
            .addOnFailureListener { showMessage(it.message ?: "تعذر حفظ الفئة") }
    }

    private fun showProviderDialog(provider: ServiceProvider? = null) {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_add_provider)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val titleText = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val nameInput = dialog.findViewById<EditText>(R.id.etProviderName)
        val priceInput = dialog.findViewById<EditText>(R.id.etProviderPrice)
        val descriptionInput = dialog.findViewById<EditText>(R.id.etProviderDesc)
        val categorySpinner = dialog.findViewById<Spinner>(R.id.spCategory)
        val imageView = dialog.findViewById<ImageView>(R.id.ivProviderDialog)
        val saveButton = dialog.findViewById<Button>(R.id.btnSaveProvider)

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        categorySpinner.adapter = spinnerAdapter
        imageUri = null
        activeImageView = imageView

        if (provider != null) {
            titleText.text = "تعديل المزود"
            nameInput.setText(provider.name)
            priceInput.setText(provider.pricePerHour.toString())
            descriptionInput.setText(provider.description)
            saveButton.text = "تحديث"
            loadImage(provider.image, imageView)

            val selectedCategory = categories.indexOfFirst { it.id == provider.categoryID }
            if (selectedCategory >= 0) categorySpinner.setSelection(selectedCategory)
        }

        imageView.setOnClickListener { imagePicker.launch("image/*") }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val price = priceInput.text.toString().toIntOrNull() ?: 0
            val categoryIndex = categorySpinner.selectedItemPosition

            if (name.isEmpty()) {
                nameInput.error = "أدخل اسم المزود"
                return@setOnClickListener
            }

            if (categoryIndex !in categories.indices) {
                showMessage("اختر فئة صحيحة")
                return@setOnClickListener
            }

            val categoryId = categories[categoryIndex].id
            val pickedImage = imageUri

            if (pickedImage == null) {
                saveProvider(name, price, description, categoryId, provider?.image.orEmpty(), provider)
                dialog.dismiss()
                return@setOnClickListener
            }

            uploadImage(pickedImage) { imageUrl ->
                saveProvider(name, price, description, categoryId, imageUrl, provider)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun saveProvider(
        name: String,
        price: Int,
        description: String,
        categoryId: String,
        imageUrl: String,
        oldProvider: ServiceProvider?
    ) {
        val id = oldProvider?.id ?: database.child("ServiceProviders").push().key ?: return

        val provider = ServiceProvider(
            id = id,
            name = name,
            description = description,
            categoryID = categoryId,
            image = imageUrl,
            rate = oldProvider?.rate ?: 0.0,
            pricePerHour = price,
            address = oldProvider?.address.orEmpty(),
            location = oldProvider?.location ?: LocationModel()
        )

        database.child("ServiceProviders")
            .child(id)
            .setValue(provider)
            .addOnFailureListener { showMessage(it.message ?: "تعذر حفظ المزود") }
    }

    private fun uploadImage(uri: Uri, onUploaded: (String) -> Unit) {
        val imageRef = storage.child("images/${UUID.randomUUID()}")

        imageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: RuntimeException("Upload failed")
                imageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                onUploaded(downloadUri.toString())
            }
            .addOnFailureListener {
                showMessage(it.message ?: "تعذر رفع الصورة")
            }
    }

    private fun showCategoryOptions(category: Category) {
        AlertDialog.Builder(this)
            .setTitle(category.name)
            .setItems(arrayOf("تعديل", "حذف")) { _, index ->
                when (index) {
                    0 -> showCategoryDialog(category)
                    1 -> confirmDelete(
                        title = "حذف الفئة",
                        message = "هل أنت متأكد من حذف هذه الفئة؟"
                    ) {
                        database.child("ServiceCategories").child(category.id).removeValue()
                    }
                }
            }
            .show()
    }

    private fun showProviderOptions(provider: ServiceProvider) {
        AlertDialog.Builder(this)
            .setTitle(provider.name)
            .setItems(arrayOf("تعديل", "حذف")) { _, index ->
                when (index) {
                    0 -> showProviderDialog(provider)
                    1 -> confirmDelete(
                        title = "حذف المزود",
                        message = "هل أنت متأكد من حذف هذا المزود؟"
                    ) {
                        database.child("ServiceProviders").child(provider.id).removeValue()
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(title: String, message: String, onDelete: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حذف") { _, _ -> onDelete() }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadImage(url: String, imageView: ImageView) {
        if (url.isNotBlank()) {
            Glide.with(this).load(url).into(imageView)
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
