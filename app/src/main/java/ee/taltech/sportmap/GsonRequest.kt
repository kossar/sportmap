package ee.taltech.sportmap

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.google.gson.Gson
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.reflect.Method
import java.nio.charset.Charset

class GsonRequest<T>(
    url: String,
    private val clazz: Class<T>,
    private val headers: MutableMap<String, String>?,
    private val listener: Response.Listener<T>,
    errorListener: Response.ErrorListener
) : Request<T>(Method.POST, url, errorListener) {

    private val gson = Gson()

    override fun getHeaders(): MutableMap<String, String> = headers ?: super.getHeaders()

    override fun deliverResponse(response: T) = listener.onResponse(response)

    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            val json = String(
                response?.data ?: ByteArray(0),
                Charset.forName(HttpHeaderParser.parseCharset(response?.headers)))
            Response.success(
                gson.fromJson(json, clazz),
                HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (e: Exception) {
            Response.error(ParseError(e))
        }
    }
}