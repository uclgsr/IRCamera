package mpdc4gsr.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.http.repository.LmsRepository
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.kotlinx.coroutines.Dispatchers
import com.kotlinx.coroutines.launch

class PolicyViewModel : BaseViewModel() {
    val htmlViewData = SingleLiveEvent<HtmlBean>()

    fun getUrl(type: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val urlType =
                when (type) {
                    1 -> 21
                    2 -> 22
                    3 -> 23
                    else -> 21
                }
            val result = LmsRepository.getStatementUrl(urlType.toString())
            if (result != null && !result.htmlContent.isNullOrBlank()) {
                htmlViewData.postValue(HtmlBean(body = result.htmlContent, action = 1))
            } else {
                htmlViewData.postValue(HtmlBean())
            }
        }
    }

    data class HtmlBean(val body: String? = null, val action: Int = 0)
}
