package com.cyc.yearlymemoir.domain.repository

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyc.yearlymemoir.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

private const val BASIC_DB_NAME = "basic_test"

val Context.dataStore by preferencesDataStore(name = BASIC_DB_NAME)

object PreferencesKeys {
    // 变量名（大写字符）为开发时使用的键名
    // 括号内的名字为存储到本地的键名
    const val TODAY_ZFB_BAL = "today_zfb_balance"
    const val ZFB_UPDATE_TIME = "zfb_update_time"
    const val ZFB_ENABLED = "zfb_enabled"
    const val TODAY_YSF_BAL = "today_ysf_balance"
    const val YSF_UPDATE_TIME = "ysf_update_time"
    const val YSF_ENABLED = "ysf_enabled"
    const val TODAY_WX_BAL = "today_wx_balance"
    const val WX_UPDATE_TIME = "wx_update_time"
    const val WX_ENABLED = "wx_enabled"
}

enum class BalanceChannelType(val displayName: String, @DrawableRes val iconRes: Int,) {
    ZFB("支付宝", R.drawable.ic_zfb_gradient),
    WX("微信", R.drawable.ic_wx),
    YSF("云闪付", R.drawable.ic_ysf),
    ALL("全部", R.drawable.chi_avatar); // 注意这里要有分号
}

data class BalanceRecord(val balance: Int, val date: String)

class DatastoreInit(private val context: Context) {
    // 同步 get 方法
    fun getString(key: String): String? {
        val preferencesKey = stringPreferencesKey(key)
        return runBlocking {
            context.dataStore.data.first()[preferencesKey]
        }
    }

    // 同步 set 方法
    fun putString(key: String, value: String?) {
        val preferencesKey = stringPreferencesKey(key)
        runBlocking {
            context.dataStore.edit { pref ->
                if (value != null) {
                    pref[preferencesKey] = value
                } else {
                    pref -= preferencesKey // 删除键值
                }
            }
        }
    }

    // 可选：封装常用类型
    fun getInt(key: String, default: Int = 0): Int {
        val str = getString(key)
        return try {
            str?.toInt() ?: default
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    fun getDouble(key: String, default: Double = 0.0): Double {
        val str = getString(key)
        return try {
            str?.toDouble() ?: default
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun putDouble(key: String, value: Double) {
        putString(key, value.toString())
    }

    fun setTodayBalance(type: BalanceChannelType, balance: Double) {
        when (type) {
            BalanceChannelType.ZFB -> {
                putDouble(PreferencesKeys.TODAY_ZFB_BAL, balance)
                putString(PreferencesKeys.ZFB_UPDATE_TIME, LocalDateTime.now().toString())
            }

            BalanceChannelType.WX -> {
                putDouble(PreferencesKeys.TODAY_WX_BAL, balance)
                putString(PreferencesKeys.WX_UPDATE_TIME, LocalDateTime.now().toString())
            }

            BalanceChannelType.YSF -> {
                putDouble(PreferencesKeys.TODAY_YSF_BAL, balance)
                putString(PreferencesKeys.YSF_UPDATE_TIME, LocalDateTime.now().toString())
            }

            BalanceChannelType.ALL -> {
                return
            }
        }
    }

    fun shouldUpdateBalance(type: BalanceChannelType): Boolean {
        val lastUpdateStr = when (type) {
            BalanceChannelType.ZFB -> getString(PreferencesKeys.ZFB_UPDATE_TIME)
            BalanceChannelType.WX -> getString(PreferencesKeys.WX_UPDATE_TIME)
            BalanceChannelType.YSF -> getString(PreferencesKeys.YSF_UPDATE_TIME)
            BalanceChannelType.ALL -> return false
        }

        val lastUpdate = try {
            LocalDateTime.parse(lastUpdateStr)
        } catch (e: Exception) {
            return true // 解析失败或为空，默认需要更新
        }

        val todayAt0AM = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)

        return lastUpdate < todayAt0AM
    }

    fun resetTodayBalance(type: BalanceChannelType) {
        when (type) {
            BalanceChannelType.ZFB -> {
                putDouble(PreferencesKeys.TODAY_ZFB_BAL, 0.0)
                putString(PreferencesKeys.ZFB_UPDATE_TIME, "2002-05-04T03:22:11.026324500")
            }

            BalanceChannelType.WX -> {
                putDouble(PreferencesKeys.TODAY_WX_BAL, 0.0)
                putString(PreferencesKeys.WX_UPDATE_TIME, "2002-05-04T03:22:11.026324500")
            }

            BalanceChannelType.YSF -> {
                putDouble(PreferencesKeys.TODAY_YSF_BAL, 0.0)
                putString(PreferencesKeys.YSF_UPDATE_TIME, "2002-05-04T03:22:11.026324500")
            }

            BalanceChannelType.ALL -> {
                resetTodayBalance(BalanceChannelType.ZFB)
                resetTodayBalance(BalanceChannelType.WX)
                resetTodayBalance(BalanceChannelType.YSF)
            }
        }

    }

    fun getTodayBalance(type: BalanceChannelType): Double {
        var balance = when (type) {
            BalanceChannelType.ZFB -> getDouble(PreferencesKeys.TODAY_ZFB_BAL) ?: 0.0
            BalanceChannelType.WX -> getDouble(PreferencesKeys.TODAY_WX_BAL) ?: 0.0
            BalanceChannelType.YSF -> getDouble(PreferencesKeys.TODAY_YSF_BAL) ?: 0.0
            BalanceChannelType.ALL -> {
                getTodayBalance(BalanceChannelType.ZFB) +
                        getTodayBalance(BalanceChannelType.WX) +
                        getTodayBalance(BalanceChannelType.YSF)
            }
        }
        balance = "%.2f".format(balance).toDouble()

        return balance
    }
}