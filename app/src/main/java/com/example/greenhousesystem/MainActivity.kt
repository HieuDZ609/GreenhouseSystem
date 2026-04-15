package com.example.greenhousesystem

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat // Đã thêm thư viện này để mở Drawer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.greenhousesystem.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Các fragment KHÔNG hiện BottomNavigation và Toolbar
    private val authFragments = setOf(
        R.id.loginFragment,
        R.id.registerFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseDatabase.getInstance("https://greenhousesystem-97224-default-rtdb.asia-southeast1.firebasedatabase.app").setPersistenceEnabled(true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupDrawer()
        setupDrawerMenu()
        observeDestinationChanges()
    }

    private fun setupNavigation() {
        // Lấy NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Kết nối BottomNavigation với NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Tự bắt sự kiện click cho nút Menu custom để mở Drawer từ bên trái
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupDrawer() {
        binding.navigationView.setupWithNavController(navController)

        // Cập nhật thông tin user vào header drawer
        val header = binding.navigationView.getHeaderView(0)
        val currentUser = FirebaseAuth.getInstance().currentUser
        header.findViewById<android.widget.TextView>(R.id.tvDrawerName).text =
            currentUser?.displayName ?: "Người dùng"
        header.findViewById<android.widget.TextView>(R.id.tvDrawerEmail).text =
            currentUser?.email ?: ""
    }

    private fun setupDrawerMenu() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_account -> {
                    // TODO: mở màn hình thông tin tài khoản
                    true
                }
                R.id.nav_plant -> {
                    // TODO: mở dialog chọn loại cây
                    true
                }
                R.id.nav_threshold -> {
                    // TODO: mở màn hình ngưỡng cảnh báo
                    true
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất không?")
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Đăng xuất") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                navController.navigate(R.id.loginFragment)
            }
            .show()
    }

    // Ẩn/hiện BottomNav và Toolbar tùy màn hình
    private fun observeDestinationChanges() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in authFragments) {
                binding.bottomNavigation.visibility = View.GONE
                binding.toolbar.visibility = View.GONE
            } else {
                binding.bottomNavigation.visibility = View.VISIBLE
                binding.toolbar.visibility = View.VISIBLE
            }
        }
    }
}