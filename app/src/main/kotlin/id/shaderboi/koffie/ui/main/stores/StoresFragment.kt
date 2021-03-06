package id.shaderboi.koffie.ui.main.stores

import Permission
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import id.shaderboi.koffie.core.util.Resource
import id.shaderboi.koffie.databinding.FragmentStoresBinding
import id.shaderboi.koffie.ui.main.stores.adapter.StoresAdapter
import id.shaderboi.koffie.ui.main.stores.adapter.StoresShimmerAdapter
import id.shaderboi.koffie.ui.main.stores.view_model.StoresEvent
import id.shaderboi.koffie.ui.main.stores.view_model.StoresViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import java.text.DecimalFormat
import javax.inject.Inject
import Bundle as BundleConst


@AndroidEntryPoint
class StoresFragment : Fragment() {
    private var _binding: FragmentStoresBinding? = null
    val binding get() = _binding!!

    private val storesViewModel by viewModels<StoresViewModel>()

    @Inject
    lateinit var numberFormatter: DecimalFormat

    var mapViewBundle: Bundle? = null
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(BundleConst.MAPVIEW_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoresBinding.inflate(inflater, container, false)

        binding.mapView.onCreate(mapViewBundle)
        binding.mapView.getMapAsync { _map ->
            map = _map
            setupMap()
        }
        val layoutParams = binding.appbar.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.behavior = AppBarLayout.Behavior().apply {
            setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean = false
            })
        }

        askPermission()
        collectUIEvent()
        setupView()

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        var mapViewBundle = outState.getBundle(BundleConst.MAPVIEW_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(BundleConst.MAPVIEW_KEY, mapViewBundle)
        }

        binding.mapView.onSaveInstanceState(mapViewBundle)
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }
    private var cancellationTokenSource = CancellationTokenSource()

    private fun setupMap() {
        map.isMyLocationEnabled = true

        fusedLocationClient.getCurrentLocation(
            PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val location = task.result
                val locationCoordinate = LatLng(location.latitude, location.longitude)
                onMyLocationReady(locationCoordinate)
            }
        }
    }

    private fun onMyLocationReady(location: LatLng) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13F))
        storesViewModel.onEvent(StoresEvent.Load(location))
    }

    override fun onLowMemory() {
        super.onLowMemory()

        binding.mapView.onLowMemory()
    }

    override fun onPause() {
        binding.mapView.onPause()

        super.onPause()
    }

    override fun onStop() {
        super.onStop()

        binding.mapView.onStop()
    }

    override fun onStart() {
        super.onStart()

        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()

        binding.mapView.onResume()
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()

        super.onDestroy()
    }

    private fun collectUIEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    storesViewModel.stores.collectLatest { res ->
                        when (res) {
                            is Resource.Error -> {
                            }
                            is Resource.Loaded -> {
                                binding.shimmerFrameLayoutMain.hideShimmer()

                                binding.recyclerViewStores.adapter =
                                    StoresAdapter(res.data, numberFormatter) { store ->
                                        val navController = findNavController()
                                        val action =
                                            StoresFragmentDirections.actionNavigationHomeStoresToNavigationHomeMain(
                                                store.store.id,
                                                store
                                            )
                                        navController.navigate(action)
                                    }
                                res.data.forEach { storeWithDistance ->
                                    map.addMarker(
                                        MarkerOptions()
                                            .position(storeWithDistance.store.coordinate.toLatLng())
                                            .title(storeWithDistance.store.name)
                                    )
                                }
                            }
                            is Resource.Loading -> {
                                binding.recyclerViewStores.adapter = StoresShimmerAdapter()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupView() {
    }

    private fun askPermission() {
        if (EasyPermissions.hasPermissions(requireContext(), *Permission.location)) {
            return
        }

        EasyPermissions.requestPermissions(
            this,
            "You need to accept location permission to be able to use delivery service",
            Permission.REQUEST_CODE_LOCATION,
            *Permission.location
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}