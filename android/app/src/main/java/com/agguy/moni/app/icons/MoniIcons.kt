package com.agguy.moni.app.icons

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.agguy.moni.R

/**
 * Material Symbols Rounded 图标资源索引。
 *
 * 所有图标均从 Google Fonts Material Symbols 下载（Rounded / Weight 400），
 * 以 Android Vector Drawable 形式托管在 res/drawable/ 中。
 * Filled 变体用于选中态，标准变体用于未选中态。
 */
object MoniIcons {
    // 导航
    val Receipt = R.drawable.ic_receipt
    val ReceiptFilled = R.drawable.ic_receipt_filled
    val BarChart = R.drawable.ic_bar_chart
    val BarChartFilled = R.drawable.ic_bar_chart_filled
    val Settings = R.drawable.ic_settings
    val SettingsFilled = R.drawable.ic_settings_filled

    // 操作
    val Add = R.drawable.ic_add
    val AddFilled = R.drawable.ic_add_filled
    val Check = R.drawable.ic_check
    val CheckFilled = R.drawable.ic_check_filled
    val Delete = R.drawable.ic_delete
    val DeleteFilled = R.drawable.ic_delete_filled
    val ArrowBack = R.drawable.ic_arrow_back
    val ArrowBackFilled = R.drawable.ic_arrow_back_filled
    val FilterList = R.drawable.ic_filter_list
    val FilterListFilled = R.drawable.ic_filter_list_filled

    // 字段/输入
    val AttachMoney = R.drawable.ic_attach_money
    val AttachMoneyFilled = R.drawable.ic_attach_money_filled
    val Event = R.drawable.ic_event
    val EventFilled = R.drawable.ic_event_filled
    val Category = R.drawable.ic_category
    val CategoryFilled = R.drawable.ic_category_filled
    val Edit = R.drawable.ic_edit
    val EditFilled = R.drawable.ic_edit_filled

    // 分类
    val Restaurant = R.drawable.ic_restaurant
    val RestaurantFilled = R.drawable.ic_restaurant_filled
    val DirectionsCar = R.drawable.ic_directions_car
    val DirectionsCarFilled = R.drawable.ic_directions_car_filled
    val ShoppingBag = R.drawable.ic_shopping_bag
    val ShoppingBagFilled = R.drawable.ic_shopping_bag_filled
    val Payments = R.drawable.ic_payments
    val PaymentsFilled = R.drawable.ic_payments_filled
    val Home = R.drawable.ic_home
    val HomeFilled = R.drawable.ic_home_filled
    val SportsEsports = R.drawable.ic_sports_esports
    val SportsEsportsFilled = R.drawable.ic_sports_esports_filled
    val LocalHospital = R.drawable.ic_local_hospital
    val LocalHospitalFilled = R.drawable.ic_local_hospital_filled
    val School = R.drawable.ic_school
    val SchoolFilled = R.drawable.ic_school_filled

    // 其他
    val DarkMode = R.drawable.ic_dark_mode
    val DarkModeFilled = R.drawable.ic_dark_mode_filled
    val Palette = R.drawable.ic_palette
    val PaletteFilled = R.drawable.ic_palette_filled
    val Tune = R.drawable.ic_tune
    val TuneFilled = R.drawable.ic_tune_filled
    val Info = R.drawable.ic_info
    val InfoFilled = R.drawable.ic_info_filled
    val Help = R.drawable.ic_help
    val HelpFilled = R.drawable.ic_help_filled
}

/**
 * 便捷 Composable，使用 Material Symbols Rounded 图标。
 *
 * @param icon MoniIcons 中定义的图标资源 ID
 * @param contentDescription 无障碍描述
 * @param modifier 修饰符
 * @param tint 图标着色，默认跟随 [LocalContentColor]
 */
@Composable
fun MoniIcon(
    icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.Icon(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
