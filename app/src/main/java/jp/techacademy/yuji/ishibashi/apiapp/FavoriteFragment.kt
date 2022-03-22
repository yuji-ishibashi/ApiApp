package jp.techacademy.yuji.ishibashi.apiapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import jp.techacademy.yuji.ishibashi.apiapp.databinding.FragmentFavoriteBinding

class FavoriteFragment: Fragment() {
    private lateinit var binding: FragmentFavoriteBinding

    private val favoriteAdapter by lazy { FavoriteAdapter(requireContext()) }

    // FavoriteFragment -> MainActivity に削除を通知する
    private var fragmentCallback : FragmentCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCallback) {
            fragmentCallback = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // fragment_favorite.xmlが反映されたViewを作成して、returnします
        //return inflater.inflate(R.layout.fragment_favorite, container, false)
        binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ここから初期化処理を行う
        // FavoriteAdapterのお気に入り削除用のメソッドの追加を行う
        favoriteAdapter.apply {
            // Adapterの処理をそのままActivityに通知
            onClickDeleteFavorite = {
                fragmentCallback?.onDeleteFavorite(it.id)
            }
            // Itemをクリックしたとき
            onClickItem = {  id: String, name: String, address: String, imageUrl: String, url: String ->
                fragmentCallback?.onClickItem(id, name, address, imageUrl, url)
            }
        }
        // RecyclerViewの初期化
        binding.recyclerView.apply {
            adapter = favoriteAdapter
            layoutManager = LinearLayoutManager(requireContext()) // 一列ずつ表示
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            updateData()
        }
        updateData()
    }

    fun updateData() {
        //論理削除に変更したため削除フラグが立っていないデータのみ取得するように変更
        //favoriteAdapter.refresh(FavoriteShop.findAll())
        favoriteAdapter.refresh(FavoriteShop.findAllWithoutDelete())
        binding.swipeRefreshLayout.isRefreshing = false
    }
}