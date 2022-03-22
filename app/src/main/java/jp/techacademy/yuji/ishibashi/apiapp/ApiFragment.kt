package jp.techacademy.yuji.ishibashi.apiapp

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import jp.techacademy.yuji.ishibashi.apiapp.databinding.ActivityMainBinding
import jp.techacademy.yuji.ishibashi.apiapp.databinding.FragmentApiBinding
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

class ApiFragment: Fragment() {
    private val TAG: String = "ApiFragment"

    private lateinit var binding: FragmentApiBinding

    private val apiAdapter by lazy { ApiAdapter(requireContext()) }
    private val handler = Handler(Looper.getMainLooper())

    private var fragmentCallback : FragmentCallback? = null // Fragment -> Activity にFavoriteの変更を通知する

    private var page = 0

    // Apiでデータを読み込み中ですフラグ。追加ページの読み込みの時にこれがないと、連続して読み込んでしまうので、それの制御のため
    private var isLoading = false
    //キーワード検索用（初期値はランチとする）
    private var mKeyword: String = "ランチ"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView start")
//        return inflater.inflate(R.layout.fragment_api, container, false) // fragment_api.xmlが反映されたViewを作成して、returnします
        binding = FragmentApiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach start")
        if (context is FragmentCallback) {
            fragmentCallback = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated start")
        // ここから初期化処理を行う
        // ApiAdapterのお気に入り追加、削除用のメソッドの追加を行う
        apiAdapter.apply {
            onClickAddFavorite = { // Adapterの処理をそのままActivityに通知する
                fragmentCallback?.onAddFavorite(it)
            }
            onClickDeleteFavorite = { // Adapterの処理をそのままActivityに通知する
                fragmentCallback?.onDeleteFavorite(it.id)
            }
            // Itemをクリックしたとき
            onClickItem = { id: String, name: String, address: String, imageUrl: String, url: String ->
                fragmentCallback?.onClickItem(id, name, address, imageUrl, url)
            }
        }
        // RecyclerViewの初期化
        binding.recyclerView.apply {
            adapter = apiAdapter
            layoutManager = LinearLayoutManager(requireContext()) // 一列ずつ表示

            addOnScrollListener(object: RecyclerView.OnScrollListener() { // Scrollを検知するListenerを実装する。これによって、RecyclerViewの下端に近づいた時に次のページを読み込んで、下に付け足す
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) { // dx はx軸方向の変化量(横) dy はy軸方向の変化量(縦) ここではRecyclerViewは縦方向なので、dyだけ考慮する
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy == 0) { // 縦方向の変化量(スクロール量)が0の時は動いていないので何も処理はしない
                        return
                    }
                    val totalCount = apiAdapter.itemCount // RecyclerViewの現在の表示アイテム数
                    val lastVisibleItem = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition() // RecyclerViewの現在見えている最後のViewHolderのposition
                    // totalCountとlastVisibleItemから全体のアイテム数のうちどこまでが見えているかがわかる(例:totalCountが20、lastVisibleItemが15の時は、現在のスクロール位置から下に5件見えていないアイテムがある)
                    // 一番下にスクロールした時に次の20件を表示する等の実装が可能になる。
                    // ユーザビリティを考えると、一番下にスクロールしてから追加した場合、一度スクロールが止まるので、ユーザーは気付きにくい
                    // ここでは、一番下から5番目を表示した時に追加読み込みする様に実装する
                    if (!isLoading && lastVisibleItem >= totalCount - 6) { // 読み込み中でない、かつ、現在のスクロール位置から下に5件見えていないアイテムがある
                        updateData(true, mKeyword)
                    }
                }
            })
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            updateData(false, mKeyword)
        }

        binding.fabSearch.setOnClickListener { view ->
            showInputKeywordDialog()
        }

        updateData(false, mKeyword)
    }

    /**
     * 検索に使用するキーボードを入力するダイアログを表示する関数
     */
    private fun showInputKeywordDialog() {
        val editText = AppCompatEditText(this.requireContext())
        editText.setText(mKeyword)
        AlertDialog.Builder(this.requireContext())
            .setTitle("キーワード検索")
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                //WebAPI側でエラーとなるため必ずキーワード入力が必要。
                if(editText.text.isNullOrEmpty()) {
                    Toast.makeText(this.requireContext(), "キーワードが未入力です。", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                //新しく検索しなおすため、isAddは必ずfalse
                mKeyword = editText.text.toString()
                updateData(false, mKeyword)
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun updateView() { // お気に入りが削除されたときの処理（Activityからコールされる）
        Log.d(TAG, "updateView start")
        binding.recyclerView.adapter?.notifyDataSetChanged() // RecyclerViewのAdapterに対して再描画のリクエストをする
    }

    private fun updateData(isAdd: Boolean = false, keyword: String?) {
        Log.d(TAG, "updateData start")
        if (isLoading) {
            return
        } else {
            isLoading = true
        }
        if (isAdd) {
            page ++
        } else {
            page = 0
        }

        val start = page * COUNT + 1
        val url = StringBuilder()
            .append(getString(R.string.base_url)) // https://webservice.recruit.co.jp/hotpepper/gourmet/v1/
            .append("?key=").append(getString(R.string.api_key)) // Apiを使うためのApiKey
            .append("&start=").append(start) // 何件目からのデータを取得するか
            .append("&count=").append(COUNT) // 1回で20件取得する
            .append("&keyword=").append(keyword ?: "") // お店の検索ワード。
            .append("&format=json") // ここで利用しているAPIは戻りの形をxmlかjsonが選択することができる。Androidで扱う場合はxmlよりもjsonの方が扱いやすいので、jsonを選択
            .toString()
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) { // Error時の処理
                e.printStackTrace()
                handler.post {
                    updateRecyclerView(listOf(), isAdd)
                }
                isLoading = false // 読み込み中フラグを折る
            }
            override fun onResponse(call: Call, response: Response) { // 成功時の処理
                var list = listOf<Shop>()
                response.body?.string()?.also {
                    val apiResponse = Gson().fromJson(it, ApiResponse::class.java)
                    list = apiResponse.results.shop
                }
                handler.post {
                    updateRecyclerView(list, isAdd)
                }
                isLoading = false // 読み込み中フラグを折る
            }
        })
    }

    private fun updateRecyclerView(list: List<Shop>, isAdd: Boolean) {
        Log.d(TAG, "updateRecyclerView start")
        if (isAdd) {
            apiAdapter.add(list)
        } else {
            apiAdapter.refresh(list)
        }
        binding.swipeRefreshLayout.isRefreshing = false // SwipeRefreshLayoutのくるくるを消す
    }

    companion object {
        private const val COUNT = 20 // 1回のAPIで取得する件数
    }
}