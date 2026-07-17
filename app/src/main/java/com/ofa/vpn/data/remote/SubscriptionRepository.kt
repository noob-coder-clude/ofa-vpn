package com.ofa.vpn.data.remote

import com.ofa.vpn.data.local.ServerDao
import com.ofa.vpn.data.local.SubscriptionDao
import com.ofa.vpn.data.model.Server
import com.ofa.vpn.data.model.Subscription
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدیریت ساب‌ها: دانلود، پارس، ذخیره
 */
@Singleton
class SubscriptionRepository @Inject constructor(
    private val subFetcher: SubFetcher,
    private val serverDao: ServerDao,
    private val subscriptionDao: SubscriptionDao
) {
    /**
     * دانلود و پارس یک ساب و ذخیره سرورها تو DB
     */
    suspend fun refresh(subscription: Subscription): Result<Int> {
        return try {
            val raw = subFetcher.fetchRaw(subscription.url).getOrThrow()
            val servers = subFetcher.parse(raw, subscription.id)

            // حذف سرورهای قدیمی این ساب
            serverDao.deleteBySubscription(subscription.id)
            // ذخیره سرورهای جدید
            serverDao.insertAll(servers)

            // بروزرسانی آمار ساب
            subscriptionDao.updateRefresh(
                subscription.id,
                System.currentTimeMillis(),
                servers.size
            )

            Result.success(servers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * اضافه کردن ساب جدید
     */
    suspend fun addSubscription(url: String, name: String): Long {
        val sub = Subscription(name = name, url = url)
        return subscriptionDao.insert(sub)
    }

    /**
     * دریافت همه سرورها (Flow برای UI)
     */
    fun getAllServers(): Flow<List<Server>> = serverDao.getAllServers()

    /**
     * دریافت ساب‌های فعال
     */
    suspend fun getEnabledSubscriptions(): List<Subscription> = subscriptionDao.getEnabled()

    suspend fun updateServer(server: Server) {
        serverDao.update(server)
    }

    suspend fun getSubscriptionById(id: Long): Subscription? {
        return subscriptionDao.getById(id)
    }
}