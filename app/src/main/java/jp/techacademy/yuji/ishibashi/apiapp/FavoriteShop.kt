package jp.techacademy.yuji.ishibashi.apiapp

import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class FavoriteShop: RealmObject() {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var address: String = ""
    var imageUrl: String = ""
    var url: String = ""
    var isDelete: Boolean = false

    companion object {
        fun findAll(): List<FavoriteShop> = // お気に入りのShopを全件取得
            Realm.getDefaultInstance().use { realm ->
                realm.where(FavoriteShop::class.java)
                    .findAll().let {
                        realm.copyFromRealm(it)
                    }
            }

        fun findAllWithoutDelete(): List<FavoriteShop> = // お気に入りのShopを全件取得
            Realm.getDefaultInstance().use { realm ->
                realm.where(FavoriteShop::class.java)
                    .equalTo(FavoriteShop::isDelete.name, false)
                    .findAll().let {
                        realm.copyFromRealm(it)
                    }
            }

        fun findBy(id: String): FavoriteShop? = // お気に入りされているShopをidで検索して返す。お気に入りに登録されていなければnullで返す
            Realm.getDefaultInstance().use { realm ->
                realm.where(FavoriteShop::class.java)
                    .equalTo(FavoriteShop::id.name, id)
                    .findFirst()?.let {
                        realm.copyFromRealm(it)
                    }
            }

        fun insertOrUpdate(favoriteShop: FavoriteShop) = // お気に入り追加
            GlobalScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    Realm.getDefaultInstance().use { realm ->
                        realm.executeTransaction { realm ->
                            realm.insertOrUpdate(favoriteShop)
                        }
                    }
                }
            }

        fun delete(id: String) = // idでお気に入りから削除するお気に入り追加
            GlobalScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    Realm.getDefaultInstance().use { realm ->
                        realm.where(FavoriteShop::class.java)
                            .equalTo(FavoriteShop::id.name, id)
                            .findFirst()?.also { deleteShop ->
                                realm.executeTransaction {
                                    deleteShop.deleteFromRealm()
                                }
                            }
                    }
                }
            }
    }
}