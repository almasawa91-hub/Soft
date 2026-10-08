package com.mohaseb.soft

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohaseb.soft.data.Repo
import com.mohaseb.soft.ui.screens.*

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MaterialTheme {
                    AppNav()
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    fun arg(e: androidx.navigation.NavBackStackEntry, k: String) = e.arguments?.getString(k) ?: ""
    NavHost(nav, "home") {
        composable("home") { HomeScreen(nav) }
        composable("docs/{kind}") { e -> DocListScreen(nav, arg(e, "kind")) }
        composable("new/{kind}") { e -> SaleScreen(nav, arg(e, "kind")) }
        composable("view/{id}/{kind}") { e -> InvoiceViewScreen(nav, arg(e, "id").toLongOrNull() ?: 0L, arg(e, "kind")) }
        composable("vouchers") { VoucherListScreen(nav) }
        composable("voucher") { VoucherScreen(nav) }
        composable("voucherview/{no}") { e -> VoucherViewScreen(nav, arg(e, "no").toIntOrNull() ?: 0) }
        composable("settings") { SettingsScreen(nav) }
        composable("options/{key}") { e -> OptionsScreen(nav, arg(e, "key")) }
        composable("profile") { ProfileScreen(nav) }
        composable("openings") { OpeningListScreen(nav) }
        composable("opening") { OpeningBalanceScreen(nav) }
        composable("daily") { DailyListScreen(nav) }
        composable("accounts") { AccountsScreen(nav) }
        composable("accounts_new") { AccountsScreen(nav, true) }
        composable("statement/{id}") { e -> StatementScreen(nav, arg(e, "id").toLongOrNull() ?: 0L) }
        composable("items") { ItemsScreen(nav) }
        composable("prices") { PricesScreen(nav) }
        composable("itemledger/{id}") { e -> ItemLedgerScreen(nav, arg(e, "id").toLongOrNull() ?: 0L) }
        composable("stocktake") { StocktakeScreen(nav) }
        composable("warehouses") { WarehousesScreen(nav) }
        composable("units") { UnitsScreen(nav) }
        composable("itemtypes") { ItemTypesScreen(nav) }
        composable("groups") { GroupsScreen(nav) }
        composable("taxes") { TaxesScreen(nav) }
        composable("currencies") { CurrenciesScreen(nav) }
        composable("rates") { RatesScreen(nav) }
        composable("ceilings") { CeilingsScreen(nav) }
        composable("users") { UsersScreen(nav) }
        composable("closing") { ClosingScreen(nav) }
        composable("report/{type}") { e -> ReportScreen(nav, arg(e, "type")) }
    }
}
