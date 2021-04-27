package com.izettle.wrench.oss.list

import android.content.Context
import androidx.lifecycle.LiveData
import com.izettle.wrench.oss.LicenceMetadata
import com.izettle.wrench.oss.OssLoading

class ThirdPartyLicenceMetadataLiveData(val context: Context) : LiveData<List<LicenceMetadata>>() {

    init {
        run {
            postValue(OssLoading.getThirdPartyLicenceMetadata(context))
        }
    }
}
