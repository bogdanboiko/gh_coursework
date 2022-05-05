package com.example.gh_coursework.ui.private_point

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.dolatkia.animatedThemeManager.AppTheme
import com.dolatkia.animatedThemeManager.ThemeFragment
import com.example.gh_coursework.MapState
import com.example.gh_coursework.R
import com.example.gh_coursework.databinding.FragmentPrivatePointsBinding
import com.example.gh_coursework.ui.helper.InternetCheckCallback
import com.example.gh_coursework.ui.helper.convertDrawableToBitmap
import com.example.gh_coursework.ui.helper.createAnnotationPoint
import com.example.gh_coursework.ui.helper.createOnMapClickEvent
import com.example.gh_coursework.ui.homepage.GetSyncStateCallback
import com.example.gh_coursework.ui.homepage.data.SyncingProgressState
import com.example.gh_coursework.ui.point_details.model.PointTagModel
import com.example.gh_coursework.ui.private_image_details.adapter.ImagesPreviewAdapter
import com.example.gh_coursework.ui.private_point.adapter.PointsListAdapter
import com.example.gh_coursework.ui.private_point.adapter.PointsListCallback
import com.example.gh_coursework.ui.private_point.model.PrivatePointDetailsModel
import com.example.gh_coursework.ui.private_point.model.PrivatePointPreviewModel
import com.example.gh_coursework.ui.private_point.tag_dialog.PointFilterByTagDialogFragment
import com.example.gh_coursework.ui.themes.MyAppTheme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapClickListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class PrivatePointsFragment : ThemeFragment(), PointsListCallback {
    private lateinit var binding: FragmentPrivatePointsBinding
    private lateinit var theme: MyAppTheme

    private lateinit var pointsFetchingJob: Job

    private lateinit var imagesPreviewAdapter: ImagesPreviewAdapter
    private lateinit var pointDetailsImagesLayoutManager: LinearLayoutManager
    private lateinit var pointsLayoutManager: LinearLayoutManager
    private val pointListAdapter = PointsListAdapter(this)

    private val viewModel: PointViewModel by viewModel()
    private var internetCheckCallback: InternetCheckCallback? = null

    private var pointCoordinates = emptyList<PrivatePointDetailsModel>()
    private var checkedTagList = emptyList<PointTagModel>()

    private lateinit var pointDetailsBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var pointsListBottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var mapboxMap: MapboxMap
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var mapState: MapState = MapState.PRESENTATION
    private var syncStateCallback: GetSyncStateCallback? = null

    private val onMapClickListener = OnMapClickListener { point ->
        val result = pointAnnotationManager.annotations.find {
            return@find it.point.latitude() == point.latitude()
                    && it.point.longitude() == point.longitude()
        }

        if (result == null) {
            val newPoint = PrivatePointPreviewModel(
                UUID.randomUUID().toString(),
                point.longitude(),
                point.latitude(),
                false
            )
            viewModel.addPoint(newPoint)
        }

        return@OnMapClickListener true
    }

    private val onPointClickEvent = OnPointAnnotationClickListener { annotation ->
        getPointDetailsDialog(annotation)

        true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        internetCheckCallback = context as? InternetCheckCallback
        syncStateCallback = context as? GetSyncStateCallback
    }

    override fun onDetach() {
        super.onDetach()

        internetCheckCallback = null
        syncStateCallback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPrivatePointsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configMap()
        configBottomNavBar()
        configMapSwitcherButton()
        configMapModSwitcher()
        configCancelButton()
        configBottomSheetDialog()
        onNavigateToHomepageButtonClickListener()
        fetchPoints()

        viewLifecycleOwner.lifecycleScope.launch {
            syncStateCallback?.getStateSubscribeTo()?.collect { state ->
                when (state) {
                    is SyncingProgressState.Loading -> binding.progressBar.loadBackground.visibility =
                        View.VISIBLE
                    is SyncingProgressState.FinishedSync -> binding.progressBar.loadBackground.visibility =
                        View.GONE
                }
            }
        }

        setFragmentResultListener(PointFilterByTagDialogFragment.REQUEST_KEY) { key, bundle ->
            val tagArray = bundle.getParcelableArray("tags")
            if (tagArray != null) {
                checkedTagList = tagArray.toList() as List<PointTagModel>

                if (tagArray.isEmpty()) {
                    binding.bottomSheetDialogPoints.emptyDataPlaceholder.text =
                        context?.resources?.getString(R.string.placeholder_private_points_list_empty)
                }

                fetchPoints()
            }
        }

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    requireActivity().finishAffinity()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun syncTheme(appTheme: AppTheme) {
        theme = appTheme as MyAppTheme
        val colorStates = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ), intArrayOf(
                theme.colorSecondaryVariant(requireContext()),
                theme.colorOnSecondary(requireContext())
            )
        )

        with(binding) {
            if (theme.id() == 0) {
                mapRoutePointModSwitcher.setImageResource(R.drawable.ic_points_light)
                homepageButton.setImageResource(R.drawable.ic_home_light)
                centralPointer.setImageResource(R.drawable.ic_pin_point_light)
            } else {
                mapRoutePointModSwitcher.setImageResource(R.drawable.ic_points_dark)
                homepageButton.setImageResource(R.drawable.ic_home_dark)
                centralPointer.setImageResource(R.drawable.ic_pin_point_dark)
            }

            cancelButton.backgroundTintList =
                ColorStateList.valueOf(theme.colorSecondary(requireContext()))
            fab.backgroundTintList =
                ColorStateList.valueOf(theme.colorSecondary(requireContext()))

            DrawableCompat.wrap(getPointsList.background)
                .setTint(theme.colorOnPrimary(requireContext()))
            getPointsList.iconTint = ColorStateList.valueOf(theme.colorSecondary(requireContext()))
            getPointsList.setTextColor(theme.colorPrimaryVariant(requireContext()))

            DrawableCompat.wrap(bottomAppBar.background)
                .setTint(theme.colorPrimary(requireContext()))
            bottomNavigationView.itemIconTintList = colorStates
            bottomNavigationView.itemTextColor = colorStates

            pointDetailsBottomSheetDialogLayout.root.backgroundTintList =
                ColorStateList.valueOf(theme.colorPrimary(requireContext()))
            pointDetailsBottomSheetDialogLayout.pointDetailsEditButton.imageTintList =
                ColorStateList.valueOf(theme.colorSecondary(requireContext()))
            pointDetailsBottomSheetDialogLayout.pointDetailsDeleteButton.imageTintList =
                ColorStateList.valueOf(theme.colorSecondary(requireContext()))
            pointDetailsBottomSheetDialogLayout.emptyDataPlaceholder.setTextColor(
                theme.colorSecondaryVariant(
                    requireContext()
                )
            )

            bottomSheetDialogPoints.root.backgroundTintList =
                ColorStateList.valueOf(theme.colorPrimary(requireContext()))
            bottomSheetDialogPoints.pointFilterByTagButton.imageTintList =
                ColorStateList.valueOf(theme.colorSecondary(requireContext()))
            bottomSheetDialogPoints.emptyDataPlaceholder.setTextColor(
                theme.colorSecondaryVariant(
                    requireContext()
                )
            )
        }
    }

    private fun configMap() {
        mapboxMap = binding.mapView.getMapboxMap().also {
            it.loadStyleUri(Style.MAPBOX_STREETS)
        }

        binding.mapView.compass.enabled = false

        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        pointAnnotationManager.addClickListener(onPointClickEvent)
    }

    private fun configBottomNavBar() {
        binding.bottomNavigationView.menu.getItem(2).isChecked = true
        binding.bottomNavigationView.menu.getItem(0).setOnMenuItemClickListener {
            if (internetCheckCallback?.isInternetAvailable() == true) {
                findNavController().navigate(
                    PrivatePointsFragmentDirections.actionPrivatePointsFragmentToPublicRoutesFragment(
                        "point"
                    )
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.no_internet_connection,
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

            return@setOnMenuItemClickListener true
        }
    }

    private fun configMapSwitcherButton() {
        binding.mapRoutePointModSwitcher.setOnClickListener {
            findNavController().navigate(
                PrivatePointsFragmentDirections
                    .actionPrivatePointsFragmentToPrivateRoutesFragment()
            )
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun configMapModSwitcher() {
        binding.fab.setOnClickListener {
            if (mapState == MapState.CREATOR) {
                executeClickAtPoint()
            } else {
                with(binding) {
                    mapRoutePointModSwitcher.visibility = View.INVISIBLE
                    homepageButton.visibility = View.INVISIBLE
                    getPointsList.visibility = View.INVISIBLE
                    centralPointer.visibility = View.VISIBLE
                    cancelButton.visibility = View.VISIBLE
                    fab.setImageDrawable(
                        context?.getDrawable(
                            R.drawable.ic_confirm
                        )
                    )
                }

                mapboxMap.addOnMapClickListener(onMapClickListener)
                pointAnnotationManager.removeClickListener(onPointClickEvent)
                mapState = MapState.CREATOR
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun configCancelButton() {
        binding.cancelButton.setOnClickListener {
            with(binding) {
                binding.mapRoutePointModSwitcher.visibility = View.VISIBLE
                binding.homepageButton.visibility = View.VISIBLE
                binding.getPointsList.visibility = View.VISIBLE
                centralPointer.visibility = View.INVISIBLE
                cancelButton.visibility = View.INVISIBLE
                fab.setImageDrawable(
                    context?.getDrawable(
                        R.drawable.ic_add
                    )
                )
            }

            mapboxMap.removeOnMapClickListener(onMapClickListener)
            pointAnnotationManager.addClickListener(onPointClickEvent)
            mapState = MapState.PRESENTATION
        }
    }

    private fun configBottomSheetDialog() {
        PagerSnapHelper().attachToRecyclerView(binding.pointDetailsBottomSheetDialogLayout.imageRecycler)

        pointDetailsImagesLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        pointDetailsBottomSheetBehavior = BottomSheetBehavior.from(binding.pointDetailsBottomSheetDialogLayout.pointBottomSheetDialog)
        pointDetailsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        pointsLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        pointsListBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetDialogPoints.pointsBottomSheetDialog)
        pointsListBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        getPointsDialog()
    }

    private fun getPointsDialog() {
        binding.bottomSheetDialogPoints.pointsRecyclerView.apply {
            adapter = pointListAdapter
            layoutManager = pointsLayoutManager
        }

        binding.getPointsList.setOnClickListener {
            pointDetailsBottomSheetBehavior.peekHeight = 0
            pointsListBottomSheetBehavior.peekHeight = resources.displayMetrics.heightPixels / 3

            if (pointsListBottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                pointsListBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.bottomSheetDialogPoints.pointFilterByTagButton.setOnClickListener {
            findNavController().navigate(
                PrivatePointsFragmentDirections.actionPrivatePointsFragmentToPointFilterByTagsDialogFragment(
                    checkedTagList.toTypedArray()
                )
            )
        }
    }

    private fun getPointDetailsDialog(annotation: PointAnnotation) {
        loadPointDetailsData(annotation)

        pointsListBottomSheetBehavior.peekHeight = 0
        pointDetailsBottomSheetBehavior.peekHeight = resources.displayMetrics.heightPixels / 3

        if (pointDetailsBottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            pointDetailsBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun onNavigateToHomepageButtonClickListener() {
        binding.homepageButton.setOnClickListener {
            findNavController().navigate(
                PrivatePointsFragmentDirections
                    .actionPrivatePointsFragmentToHomepageFragment()
            )
        }
    }

    private fun fetchPoints() {
        if (this::pointsFetchingJob.isInitialized) {
            pointsFetchingJob.cancel()
        }

        pointsFetchingJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.points.collect { points ->
                var filteredPoints = mutableListOf<PrivatePointDetailsModel>()

                if (checkedTagList.isNotEmpty()) {
                    points.forEach { point ->
                        checkedTagList.forEach tags@{
                            if (point.tagList.contains(it)) {
                                filteredPoints.add(point)
                                return@tags
                            }
                        }
                    }

                    if (filteredPoints.isEmpty()) {
                        binding.bottomSheetDialogPoints.emptyDataPlaceholder.text =
                            context?.resources?.getString(R.string.placeholder_private_no_point_found_by_tags)
                    }
                } else {
                    filteredPoints = points.toMutableList()
                }

                pointAnnotationManager.deleteAll()
                filteredPoints.forEach {
                    addAnnotationToMap(it)
                }

                if (filteredPoints.isNotEmpty()) {
                    binding.bottomSheetDialogPoints.emptyDataPlaceholder.visibility = View.GONE
                } else {
                    binding.bottomSheetDialogPoints.emptyDataPlaceholder.visibility = View.VISIBLE
                }

                pointCoordinates = filteredPoints
                pointListAdapter.submitList(filteredPoints)
            }
        }
    }

    private fun executeClickAtPoint() {
        val clickEvent = createOnMapClickEvent(
            Pair(
                resources.displayMetrics.widthPixels / 2,
                resources.displayMetrics.heightPixels / 2
            )
        )
        binding.mapView.dispatchTouchEvent(clickEvent.first)
        binding.mapView.dispatchTouchEvent(clickEvent.second)
    }

    private fun addAnnotationToMap(point: PrivatePointDetailsModel) {
        val pointIcon: Int = if (theme.id() == 0) {
            R.drawable.ic_pin_point_light
        } else {
            R.drawable.ic_pin_point_dark
        }

        activity?.applicationContext?.let {
            convertDrawableToBitmap(
                AppCompatResources.getDrawable(
                    it,
                    pointIcon
                )
            )?.let { image ->
                pointAnnotationManager.create(
                    createAnnotationPoint(
                        image,
                        Point.fromLngLat(point.x, point.y)
                    ).withData(JsonPrimitive(point.pointId))
                )
            }
        }
    }

    private fun loadPointDetailsData(annotation: PointAnnotation) {
        annotation.getData()?.asString?.let { pointId ->
            val point = pointCoordinates.find { it.pointId == pointId }
            point?.x?.let { eraseCameraToPoint(it, point.y) }

            if (point != null) {
                preparePointDetailsDialog(annotation, point)
            }
        }
    }

    private fun preparePointDetailsDialog(
        pointAnnotation: PointAnnotation,
        point: PrivatePointDetailsModel
    ) {
        binding.pointDetailsBottomSheetDialogLayout.apply {
            if (point.caption.isEmpty() && point.description.isEmpty() && point.tagList.isEmpty()) {
                emptyDataPlaceholder.visibility = View.VISIBLE
            } else {
                emptyDataPlaceholder.visibility = View.GONE
            }

            pointCaptionText.text = point.caption
            pointDescriptionText.text = point.description
            if (point.tagList.isEmpty()) {
                tagListTextView.text = ""
                tagListTextView.visibility = View.GONE
            } else {
                tagListTextView.text = point.tagList.joinToString(
                    ",",
                    "Tags: "
                ) { pointTagModel -> pointTagModel.name }
                tagListTextView.visibility = View.VISIBLE
            }


        imagesPreviewAdapter = ImagesPreviewAdapter {
            findNavController().navigate(
                PrivatePointsFragmentDirections.actionPrivatePointsFragmentToPrivateImageDetails(
                    point.pointId,
                    this@PrivatePointsFragment.pointDetailsImagesLayoutManager.findFirstVisibleItemPosition()
                )
            )
        }

        imageRecycler.apply {
            adapter = imagesPreviewAdapter
            layoutManager = this@PrivatePointsFragment.pointDetailsImagesLayoutManager
        }

        imagesPreviewAdapter.submitList(point.imageList)

        pointDetailsEditButton.setOnClickListener {
            if (!point.routeId.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    R.string.can_not_edit_route_point,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                findNavController().navigate(
                    PrivatePointsFragmentDirections
                        .actionPrivatePointsFragmentToPointDetailsFragment(point.pointId)
                )
            }
        }

        pointDetailsDeleteButton.setOnClickListener {
            if (!point.routeId.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    R.string.can_not_delete_route_point,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.deletePoint(point)

                pointAnnotationManager.delete(pointAnnotation)
                pointDetailsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }
}

    override fun onPointItemClick(pointDetails: PrivatePointDetailsModel) {
        eraseCameraToPoint(pointDetails.x, pointDetails.y)
        pointsListBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun eraseCameraToPoint(x: Double, y: Double) {
        binding.mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(x, y))
                .zoom(14.0)
                .build()
        )
    }
}