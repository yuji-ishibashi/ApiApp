package jp.techacademy.yuji.ishibashi.apiapp

interface FragmentCallback {
    // Itemを押したときの処理
    fun onClickItem(id: String, name: String, address: String, imageUrl: String, url: String)
    // お気に入り追加時の処理
    fun onAddFavorite(shop: Shop)
    // お気に入り削除時の処理
    fun onDeleteFavorite(id: String)
}