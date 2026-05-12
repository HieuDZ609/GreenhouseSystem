package com.example.greenhousesystem

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.greenhousesystem.databinding.ActivityMainBinding
import com.example.greenhousesystem.databinding.NavHeaderBinding
import com.example.greenhousesystem.service.AlertScheduler
import com.example.greenhousesystem.ui.SharedDeviceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * MainActivity — Activity trung tâm của ứng dụng.
 *
 * ══ KHỞI TẠO SharedDeviceViewModel ══
 * SharedDeviceViewModel được tạo qua `by viewModels()` ở Activity scope.
 * Tất cả Fragment con truy cập bằng `by activityViewModels()` và
 * sẽ nhận CÙNG một instance — đảm bảo:
 * • Firebase listener chỉ mở 1 lần (không duplicate)
 * • Data sync real-time giữa Home, Control, Chart, Threshold
 *
 * ══ AUTH STATE OBSERVER ══
 * Theo dõi FirebaseAuth.AuthStateListener:
 * Nếu user đăng xuất trong khi đang dùng app (session hết hạn,
 * bị kick bởi admin, tự đăng xuất) → tự động navigate về Login.
 * Dùng lifecycleScope để tránh memory leak.
 *
 * ══ NAVIGATION DRAWER ══
 * Drawer layout với nav_header_main.xml hiển thị:
 * - Avatar, tên user, email từ FirebaseAuth.currentUser
 * - Garden name từ Firebase users/$uid/gardenName
 * - Online status dot (sync với SharedDeviceViewModel.deviceStatus)
 *
 * ══ BOTTOMNAV VISIBILITY ══
 * Tự động ẩn/hiện BottomNavigationView và AppBar dựa trên destination:
 * • Ẩn tại: Login, Register, Account, Threshold (màn hình phụ)
 * • Hiện tại: Home, Control, Chart, Notification (tab chính)
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_LOGGED_IN = "extra_is_logged_in"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Binding riêng cho Navigation Drawer header
    // Cần inflate thủ công vì header không phải fragment
    private lateinit var navHeaderBinding: NavHeaderBinding

    // SharedDeviceViewModel ở Activity scope — tất cả Fragment con
    // gọi activityViewModels() sẽ nhận cùng instance này.
    val sharedViewModel: SharedDeviceViewModel by viewModels()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference.child("GreenHouseSystem")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inflate nav header binding để access tvHeaderName, tvHeaderEmail, etc.
        val headerView = binding.navigationView.getHeaderView(0)
        navHeaderBinding = NavHeaderBinding.bind(headerView)

        setupNavigation()
        setupDrawer()
        setupBottomNavStyle()
        observeAuthState()
        observeSharedViewModel()
        handleInitialDestination()
    }

    // ─────────────────────────────────────────────────────────────────────
    //  NAVIGATION — Kết nối NavController với BottomNav
    // ─────────────────────────────────────────────────────────────────────
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // setupWithNavController: tự handle back stack, tab selection
        binding.bottomNavigation.setupWithNavController(navController)

        // Theo dõi destination để ẩn/hiện UI elements
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideOnIds = setOf(
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.accountFragment,
                R.id.thresholdFragment
            )

            val shouldHide = destination.id in hideOnIds
            binding.bottomNavigation.visibility = if (shouldHide) View.GONE else View.VISIBLE
            binding.appBarLayout.visibility     = if (shouldHide) View.GONE else View.VISIBLE

            // Lock drawer ở màn hình Auth để tránh mở nhầm
            val lockDrawer = destination.id in setOf(
                R.id.loginFragment, R.id.registerFragment
            )
            binding.drawerLayout.setDrawerLockMode(
                if (lockDrawer) DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                else DrawerLayout.LOCK_MODE_UNLOCKED
            )

            // Cập nhật Toolbar title theo destination
            val title = when (destination.id) {
                R.id.homeFragment         -> "GreenHouse"
                R.id.controlFragment      -> "Điều khiển"
                R.id.chartFragment        -> "Biểu đồ"
                R.id.notificationFragment -> "Thông báo"
                else                      -> "GreenHouse"
            }
            // binding.toolbarTitle?.text = title  // uncomment nếu có TextView trong Toolbar
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  NAVIGATION DRAWER — Setup menu items và header
    // ─────────────────────────────────────────────────────────────────────
    private fun setupDrawer() {
        // Hamburger button mở drawer
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.navigationView)
        }

        // Header click → navigate đến Account
        navHeaderBinding.ivNavAvatar.setOnClickListener {
            binding.drawerLayout.closeDrawers()
            navController.navigate(R.id.accountFragment)
        }

        // Menu items
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawers()  // Đóng drawer trước khi navigate

            when (menuItem.itemId) {
                R.id.nav_account -> {
                    navController.navigate(R.id.accountFragment)
                    true
                }
                R.id.nav_threshold -> {
                    navController.navigate(R.id.thresholdFragment)
                    true
                }
                R.id.nav_plant-> {
                    // Hiện dialog chọn cây (không navigate fragment)
                    com.example.greenhousesystem.ui.drawer.PlantPickerDialog()
                        .show(supportFragmentManager, "PlantPicker")
                    true
                }
                R.id.nav_logout -> {
                    handleLogout()
                    true
                }
                else -> false
            }
        }

        // Load thông tin user lên header lần đầu
        loadNavHeaderInfo()
    }

    /**
     * loadNavHeaderInfo — Điền thông tin user vào Navigation Drawer header.
     * Nguồn 1: FirebaseAuth.currentUser → tên, email
     * Nguồn 2: Firebase Realtime DB users/$uid → gardenName
     */
    private fun loadNavHeaderInfo() {
        val user = auth.currentUser ?: return

        navHeaderBinding.apply {
            tvHeaderName.text  = user.displayName?.ifEmpty { "Người dùng" } ?: "Người dùng"
            tvHeaderEmail.text = user.email ?: ""

            // Load avatar từ photoUrl (nếu có), fallback về ic_person
            user.photoUrl?.let { uri ->
                Glide.with(this@MainActivity)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(ivNavAvatar)
            }
        }

        // Đọc gardenName từ Firebase (async)
        db.child("users").child(user.uid).child("gardenName")
            .get()
            .addOnSuccessListener { snap ->
                val gardenName = snap.getValue(String::class.java)
                navHeaderBinding.tvGardenName.text =
                    gardenName?.ifEmpty { "Vườn của tôi" } ?: "Vườn của tôi"
            }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  AUTH STATE OBSERVER — Tự động redirect khi session hết hạn
    //
    //  repeatOnLifecycle(STARTED): pause khi Activity bị che (minimize),
    //  resume khi quay lại → tránh navigate khi không visible.
    // ─────────────────────────────────────────────────────────────────────
    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Dùng Firebase AuthStateListener qua Flow wrapper
                // (Kotlin coroutine bridge với Firebase callback)
                var authListener: FirebaseAuth.AuthStateListener? = null

                try {
                    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                            val currentUser = firebaseAuth.currentUser

                            // Nếu user bị null (đăng xuất / session hết hạn)
                            if (currentUser == null) {
                                // Kiểm tra xem có đang ở màn hình Auth chưa
                                val currentDest = navController.currentDestination?.id
                                val isAtAuthScreen = currentDest in setOf(
                                    R.id.loginFragment, R.id.registerFragment
                                )

                                if (!isAtAuthScreen) {
                                    // Navigate về Login, clear toàn bộ back stack
                                    navController.navigate(R.id.action_global_to_login)
                                    // Dừng AlertWorker khi logout
                                    AlertScheduler.stop(applicationContext)
                                }
                            } else {
                                // User mới đăng nhập → refresh header info
                                loadNavHeaderInfo()
                            }
                        }
                        auth.addAuthStateListener(authListener!!)

                        // Coroutine bị cancel → remove listener (tránh leak)
                        cont.invokeOnCancellation {
                            authListener?.let { auth.removeAuthStateListener(it) }
                        }
                    }
                } finally {
                    authListener?.let { auth.removeAuthStateListener(it) }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OBSERVE SharedDeviceViewModel — Cập nhật header theo trạng thái
    // ─────────────────────────────────────────────────────────────────────
    private fun observeSharedViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Online dot trong nav header
                launch {
                    sharedViewModel.deviceStatus.collectLatest { status ->
                        val isOnline = status.status == "online"
                        navHeaderBinding.viewNavOnlineDot.setBackgroundResource(
                            if (isOnline) R.drawable.circle_green
                            else R.drawable.circle_gray
                        )
                    }
                }

                // Unread count cho notification badge
                // Sẽ hiện badge trên BottomNav notification tab
                // (cần custom BadgeDrawable trong BottomNav)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOGOUT — Đăng xuất hoàn toàn
    // ─────────────────────────────────────────────────────────────────────
    private fun handleLogout() {
        // Dừng background workers trước khi logout
        AlertScheduler.stop(applicationContext)

        // Đăng xuất Firebase Auth
        auth.signOut()

        // Navigate về Login và clear toàn bộ back stack
        // authStateListener sẽ tự detect và navigate,
        // nhưng gọi thẳng ở đây để UX nhanh hơn.
        navController.navigate(R.id.action_global_to_login)
    }

    private fun setupBottomNavStyle() {
        binding.bottomNavigation.apply {
            setBackgroundColor(Color.parseColor("#0F1E12"))
            itemIconTintList = resources.getColorStateList(
                R.color.bottom_nav_color_dark, null)
            itemTextColor = resources.getColorStateList(
                R.color.bottom_nav_color_dark, null)
            // Elevation = 0 để không có shadow trên nền tối
            elevation = 0f
        }
    }

    /**
     * handleInitialDestination — Xử lý khi app khởi động lần đầu.
     * Nếu intent cho biết chưa đăng nhập → navigate đến Login.
     * (Intent được set bởi SplashActivity dựa trên auth.currentUser)
     */
    private fun handleInitialDestination() {
        val isLoggedIn = intent.getBooleanExtra(EXTRA_IS_LOGGED_IN, false)
        if (!isLoggedIn) {
            // Post delay để NavController khởi tạo xong
            binding.root.post {
                navController.navigate(R.id.action_home_to_login)
            }
        }
    }

    /** Hardware back button: đóng drawer trước nếu đang mở. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
            binding.drawerLayout.closeDrawers()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}