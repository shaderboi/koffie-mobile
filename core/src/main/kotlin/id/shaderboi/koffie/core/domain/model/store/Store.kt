package id.shaderboi.koffie.core.domain.model.store

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import id.shaderboi.koffie.core.domain.model.common.Coordinate
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class Store(
    val id: Int,
    val name: String,
    val address: String,
    val coordinate: Coordinate
): Parcelable
