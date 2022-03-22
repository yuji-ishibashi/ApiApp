package jp.techacademy.yuji.ishibashi.apiapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import jp.techacademy.yuji.ishibashi.apiapp.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {
    private val TAG: String = "WebViewActivity"

    private lateinit var binding: ActivityWebViewBinding

    private var mFavoriteShop: FavoriteShop? = null

    private var isFavorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate start")
        //setContentView(R.layout.activity_web_view)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)

        // ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        val shop_id = intent.getStringExtra(KEY_ID).toString()

        mFavoriteShop = FavoriteShop.findBy(shop_id)

        if(mFavoriteShop != null) {
            //論理削除のためお気に入りでなくてもデータがある場合があるので削除フラグで判断
            isFavorite = !(mFavoriteShop!!.isDelete)
        } else {
            //データがない場合は必ずお気に入りではない
            isFavorite = false
            //お気に入り登録対象のお店情報データクラスを作成
            mFavoriteShop = FavoriteShop().apply {
                id = shop_id
                name = intent.getStringExtra(KEY_NAME).toString()
                address = intent.getStringExtra(KEY_ADDRESS).toString()
                imageUrl = intent.getStringExtra(KEY_IMAGE_URL).toString()
                url = intent.getStringExtra(KEY_URL).toString()
                //このタイミングではお気に入り対象ではないため削除フラグを立てておく
                isDelete = true
            }
        }

        binding.contentWebView.webView.loadUrl(mFavoriteShop!!.url)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "onCreateOptionsMenu start")
        // レイアウトファイルのインフレート
        menuInflater.inflate(R.menu.menu_favorite, menu)

        val menuFavorite = menu!!.findItem(R.id.action_favorite)
        if(isFavorite) {
            //お気に入り登録されている場合
            menuFavorite.setIcon(R.drawable.ic_star)
        } else {
            //お気に入り登録されていない場合
            menuFavorite.setIcon(R.drawable.ic_star_border)
        }
        // onCreateOptionsMenu()のオーバーライド時は、常にtrueを返却
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected start")
        var returnVal: Boolean = true

        when(item.itemId) {
            R.id.action_favorite ->
                if(isFavorite) {
                    deleteFavorite()
                    isFavorite = false
                    //アイコンを変更するためメニューを再生成
                    invalidateOptionsMenu()
                } else {
                    addFavorite()
                    isFavorite = true
                    //アイコンを変更するためメニューを再生成
                    invalidateOptionsMenu()
                }
            else ->
                // オプションメニューのItem以外が選択された場合は、
                // 親クラス(super)のonOptionsItemSelected(item:)メソッドの
                // 返り値(デフォルトではfalse)を返却
                returnVal = super.onOptionsItemSelected(item)
        }

        return returnVal
    }

    private fun addFavorite() {
        Log.d(TAG, "addFavorite start")
        mFavoriteShop!!.isDelete = false
        FavoriteShop.insertOrUpdate(mFavoriteShop!!)
    }

    private fun deleteFavorite() {
        Log.d(TAG, "deleteFavorite start")
        //論理削除に変更
        //FavoriteShop.delete(mFavoriteShop!!.id)
        mFavoriteShop!!.isDelete = true
        FavoriteShop.insertOrUpdate(mFavoriteShop!!)
    }

    companion object {
        private const val KEY_ID = "key_id"
        private const val KEY_NAME = "key_name"
        private const val KEY_ADDRESS = "key_address"
        private const val KEY_IMAGE_URL = "key_image_url"
        private const val KEY_URL = "key_url"
        //お気に入り登録のために必要な情報をあらかじめ渡す。
        fun start(activity: Activity, id: String, name: String, address: String, imageUrl: String, url: String) {
            activity.startActivity(Intent(activity, WebViewActivity::class.java).putExtra(KEY_ID, id).putExtra(
                KEY_NAME, name).putExtra(KEY_ADDRESS, address).putExtra(KEY_IMAGE_URL, imageUrl).putExtra(KEY_URL, url))
        }
    }
}