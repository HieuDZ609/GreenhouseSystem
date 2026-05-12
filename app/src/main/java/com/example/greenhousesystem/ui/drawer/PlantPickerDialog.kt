package com.example.greenhousesystem.ui.drawer

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.greenhousesystem.R
import com.example.greenhousesystem.databinding.DialogPlantPickerBinding
import com.example.greenhousesystem.model.PlantProfile
import com.example.greenhousesystem.ui.SharedDeviceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*

/**
 * PlantPickerDialog — Dialog chọn loại cây (Phiên bản nâng cấp).
 *
 * Cải tiến:
 * 1. Kết nối SharedDeviceViewModel → gọi sharedViewModel.selectPlant()
 *    thay vì trực tiếp Firebase. ViewModel lo phần sync threshold tự động.
 * 2. Khi chọn cây, hiển thị recommended LED mode phía dưới tên.
 * 3. Card selected: scale lên 1.06 + stroke lime + check icon (Lottie nhỏ).
 * 4. Staggered appearance cho từng grid item (cascade xuống).
 * 5. Dialog style: dark glassmorphic (background trong suốt + card đen xanh).
 *
 * Luồng:
 * Load plantProfiles từ Firebase → hiển thị grid → user tap →
 * sharedViewModel.selectPlant(id) → Dialog dismiss → SharedViewModel
 * tự cập nhật selectedPlant + thresholds cho toàn app.
 */
class PlantPickerDialog : DialogFragment() {

    // Kết nối Activity-scope SharedDeviceViewModel
    private val sharedViewModel: SharedDeviceViewModel by activityViewModels()

    private val db = FirebaseDatabase.getInstance().reference.child("GreenHouseSystem")
    private var currentPlantId = "TOMATO"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_plant_picker, null)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerPlants)
        val loadingView = view.findViewById<LottieAnimationView>(R.id.lottiePickerLoading)

        recycler.layoutManager = GridLayoutManager(context, 2)

        // Lấy plant hiện tại từ ViewModel (không cần fetch Firebase)
        currentPlantId = sharedViewModel.selectedPlant.value?.id ?: "TOMATO"

        // Load danh sách cây từ Firebase plantProfiles
        loadingView.visibility = View.VISIBLE
        db.child("plantProfiles").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                loadingView.visibility = View.GONE

                val plants = mutableListOf<Pair<String, PlantProfile>>()
                snapshot.children.forEach { child ->
                    val profile = PlantProfile(
                        id                 = child.key ?: "",
                        name               = child.child("name").getValue(String::class.java) ?: "",
                        icon               = child.child("icon").getValue(String::class.java) ?: "🌱",
                        tempMin            = child.child("tempMin").getValue(Double::class.java) ?: 15.0,
                        tempMax            = child.child("tempMax").getValue(Double::class.java) ?: 35.0,
                        humidityMin        = child.child("humidityMin").getValue(Double::class.java) ?: 40.0,
                        humidityMax        = child.child("humidityMax").getValue(Double::class.java) ?: 90.0,
                        recommendedLedMode = child.child("recommendedLedMode").getValue(String::class.java) ?: "VEGETATIVE"
                    )
                    plants.add(Pair(child.key ?: "", profile))
                }

                val adapter = PlantPickerAdapter(
                    plants      = plants,
                    currentId   = currentPlantId,
                    onSelect    = { plantId ->
                        // Gọi ViewModel → tự sync threshold + notify toàn app
                        sharedViewModel.selectPlant(plantId)
                        dismiss()
                    }
                )
                recycler.adapter = adapter

                // Staggered appearance: delay cho mỗi item khi load
                recycler.post { animateGridItems(recycler) }
            }
            override fun onCancelled(error: DatabaseError) {
                loadingView.visibility = View.GONE
            }
        })

        // Dialog style: dark background, bo góc lớn
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.DarkDialogStyle)
            .setTitle("🌱 Chọn loại cây trồng")
            .setView(view)
            .setNegativeButton("Hủy") { _, _ -> dismiss() }
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        return dialog
    }

    /**
     * animateGridItems — Cascade animation cho các grid item.
     * Mỗi item: alpha 0→1 + translationY 20→0, delay tăng theo index.
     */
    private fun animateGridItems(recycler: RecyclerView) {
        for (i in 0 until (recycler.adapter?.itemCount ?: 0)) {
            val view = recycler.layoutManager?.findViewByPosition(i) ?: continue
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350L)
                .setStartDelay(i * 60L)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  PlantPickerAdapter — RecyclerView adapter cho grid 2 cột
// ─────────────────────────────────────────────────────────────────────────
class PlantPickerAdapter(
    private val plants    : List<Pair<String, PlantProfile>>,
    private val currentId : String,
    private val onSelect  : (String) -> Unit
) : RecyclerView.Adapter<PlantPickerAdapter.PlantViewHolder>() {

    inner class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon       : TextView = view.findViewById(R.id.tvPlantIcon)
        val tvName       : TextView = view.findViewById(R.id.tvPlantName)
        val tvThreshold  : TextView = view.findViewById(R.id.tvPlantThreshold)
        val tvLedMode    : TextView = view.findViewById(R.id.tvRecommendedMode)
        val card         : com.google.android.material.card.MaterialCardView =
            view.findViewById(R.id.cardPlantItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val (id, plant) = plants[position]
        val isSelected = id == currentId
        val ctx = holder.itemView.context

        holder.tvIcon.text = plant.icon
        holder.tvName.text = plant.name
        holder.tvThreshold.text = "${plant.tempMin}~${plant.tempMax}°C"

        // Recommended LED mode
        val ledModeLabel = when (plant.recommendedLedMode) {
            "GERMINATION" -> "💡 Nảy mầm"
            "VEGETATIVE"  -> "💡 Phát triển"
            "FLOWERING"   -> "💡 Ra hoa"
            "FRUITING"    -> "💡 Đậu quả"
            else          -> "💡 Thủ công"
        }
        holder.tvLedMode.text = ledModeLabel

        // Style: selected vs unselected
        holder.card.apply {
            if (isSelected) {
                strokeColor  = ctx.getColor(R.color.lime_accent)
                strokeWidth  = 4
                setCardBackgroundColor(ctx.getColor(R.color.card_selected_bg))
                scaleX = 1.04f
                scaleY = 1.04f
            } else {
                strokeColor  = ctx.getColor(R.color.card_stroke_dark)
                strokeWidth  = 1
                setCardBackgroundColor(ctx.getColor(R.color.card_bg_dark))
                scaleX = 1f
                scaleY = 1f
            }
        }

        // Click: spring scale + notify
        holder.card.setOnClickListener {
            // Spring animation
            it.animate()
                .scaleX(0.93f).scaleY(0.93f).setDuration(100L)
                .withEndAction {
                    it.animate()
                        .scaleX(1f).scaleY(1f).setDuration(350L)
                        .setInterpolator(OvershootInterpolator(4f))
                        .start()
                    onSelect(id)
                }.start()
        }
    }

    override fun getItemCount() = plants.size
}