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

    // ============================================================
    // 分类图标（按主题分组，共 86 个）
    // ============================================================

    // 饮食 (12)
    val BakeryDining = R.drawable.ic_bakery_dining
    val BakeryDiningFilled = R.drawable.ic_bakery_dining_filled
    val Cake = R.drawable.ic_cake
    val CakeFilled = R.drawable.ic_cake_filled
    val Coffee = R.drawable.ic_coffee
    val CoffeeFilled = R.drawable.ic_coffee_filled
    val DinnerDining = R.drawable.ic_dinner_dining
    val DinnerDiningFilled = R.drawable.ic_dinner_dining_filled
    val Fastfood = R.drawable.ic_fastfood
    val FastfoodFilled = R.drawable.ic_fastfood_filled
    val Icecream = R.drawable.ic_icecream
    val IcecreamFilled = R.drawable.ic_icecream_filled
    val LocalBar = R.drawable.ic_local_bar
    val LocalBarFilled = R.drawable.ic_local_bar_filled
    val LocalCafe = R.drawable.ic_local_cafe
    val LocalCafeFilled = R.drawable.ic_local_cafe_filled
    val LocalDining = R.drawable.ic_local_dining
    val LocalDiningFilled = R.drawable.ic_local_dining_filled
    val LunchDining = R.drawable.ic_lunch_dining
    val LunchDiningFilled = R.drawable.ic_lunch_dining_filled
    val Restaurant = R.drawable.ic_restaurant
    val RestaurantFilled = R.drawable.ic_restaurant_filled
    val WineBar = R.drawable.ic_wine_bar
    val WineBarFilled = R.drawable.ic_wine_bar_filled

    // 交通 (12)
    val DirectionsBike = R.drawable.ic_directions_bike
    val DirectionsBikeFilled = R.drawable.ic_directions_bike_filled
    val DirectionsBoat = R.drawable.ic_directions_boat
    val DirectionsBoatFilled = R.drawable.ic_directions_boat_filled
    val DirectionsBus = R.drawable.ic_directions_bus
    val DirectionsBusFilled = R.drawable.ic_directions_bus_filled
    val DirectionsCar = R.drawable.ic_directions_car
    val DirectionsCarFilled = R.drawable.ic_directions_car_filled
    val DirectionsWalk = R.drawable.ic_directions_walk
    val DirectionsWalkFilled = R.drawable.ic_directions_walk_filled
    val ElectricCar = R.drawable.ic_electric_car
    val ElectricCarFilled = R.drawable.ic_electric_car_filled
    val Flight = R.drawable.ic_flight
    val FlightFilled = R.drawable.ic_flight_filled
    val LocalShipping = R.drawable.ic_local_shipping
    val LocalShippingFilled = R.drawable.ic_local_shipping_filled
    val LocalTaxi = R.drawable.ic_local_taxi
    val LocalTaxiFilled = R.drawable.ic_local_taxi_filled
    val Subway = R.drawable.ic_subway
    val SubwayFilled = R.drawable.ic_subway_filled
    val Train = R.drawable.ic_train
    val TrainFilled = R.drawable.ic_train_filled
    val Tram = R.drawable.ic_tram
    val TramFilled = R.drawable.ic_tram_filled

    // 购物 (6)
    val LocalMall = R.drawable.ic_local_mall
    val LocalMallFilled = R.drawable.ic_local_mall_filled
    val Redeem = R.drawable.ic_redeem
    val RedeemFilled = R.drawable.ic_redeem_filled
    val Sell = R.drawable.ic_sell
    val SellFilled = R.drawable.ic_sell_filled
    val ShoppingBag = R.drawable.ic_shopping_bag
    val ShoppingBagFilled = R.drawable.ic_shopping_bag_filled
    val ShoppingCart = R.drawable.ic_shopping_cart
    val ShoppingCartFilled = R.drawable.ic_shopping_cart_filled
    val Storefront = R.drawable.ic_storefront
    val StorefrontFilled = R.drawable.ic_storefront_filled

    // 家居 (10)
    val Bed = R.drawable.ic_bed
    val BedFilled = R.drawable.ic_bed_filled
    val Chair = R.drawable.ic_chair
    val ChairFilled = R.drawable.ic_chair_filled
    val CleaningServices = R.drawable.ic_cleaning_services
    val CleaningServicesFilled = R.drawable.ic_cleaning_services_filled
    val DoorFront = R.drawable.ic_door_front
    val DoorFrontFilled = R.drawable.ic_door_front_filled
    val Home = R.drawable.ic_home
    val HomeFilled = R.drawable.ic_home_filled
    val Kitchen = R.drawable.ic_kitchen
    val KitchenFilled = R.drawable.ic_kitchen_filled
    val Lightbulb = R.drawable.ic_lightbulb
    val LightbulbFilled = R.drawable.ic_lightbulb_filled
    val LocalLaundryService = R.drawable.ic_local_laundry_service
    val LocalLaundryServiceFilled = R.drawable.ic_local_laundry_service_filled
    val WaterDrop = R.drawable.ic_water_drop
    val WaterDropFilled = R.drawable.ic_water_drop_filled
    val Yard = R.drawable.ic_yard
    val YardFilled = R.drawable.ic_yard_filled

    // 工作 (10)
    val AccountBalance = R.drawable.ic_account_balance
    val AccountBalanceFilled = R.drawable.ic_account_balance_filled
    val Build = R.drawable.ic_build
    val BuildFilled = R.drawable.ic_build_filled
    val BusinessCenter = R.drawable.ic_business_center
    val BusinessCenterFilled = R.drawable.ic_business_center_filled
    val Cloud = R.drawable.ic_cloud
    val CloudFilled = R.drawable.ic_cloud_filled
    val Computer = R.drawable.ic_computer
    val ComputerFilled = R.drawable.ic_computer_filled
    val RequestQuote = R.drawable.ic_request_quote
    val RequestQuoteFilled = R.drawable.ic_request_quote_filled
    val Savings = R.drawable.ic_savings
    val SavingsFilled = R.drawable.ic_savings_filled
    val TrendingUp = R.drawable.ic_trending_up
    val TrendingUpFilled = R.drawable.ic_trending_up_filled
    val Wifi = R.drawable.ic_wifi
    val WifiFilled = R.drawable.ic_wifi_filled
    val Work = R.drawable.ic_work
    val WorkFilled = R.drawable.ic_work_filled

    // 医疗 (8)
    val FitnessCenter = R.drawable.ic_fitness_center
    val FitnessCenterFilled = R.drawable.ic_fitness_center_filled
    val Healing = R.drawable.ic_healing
    val HealingFilled = R.drawable.ic_healing_filled
    val HealthAndSafety = R.drawable.ic_health_and_safety
    val HealthAndSafetyFilled = R.drawable.ic_health_and_safety_filled
    val LocalHospital = R.drawable.ic_local_hospital
    val LocalHospitalFilled = R.drawable.ic_local_hospital_filled
    val Medication = R.drawable.ic_medication
    val MedicationFilled = R.drawable.ic_medication_filled
    val Psychology = R.drawable.ic_psychology
    val PsychologyFilled = R.drawable.ic_psychology_filled
    val Spa = R.drawable.ic_spa
    val SpaFilled = R.drawable.ic_spa_filled
    val Vaccines = R.drawable.ic_vaccines
    val VaccinesFilled = R.drawable.ic_vaccines_filled

    // 教育 (8)
    val AutoStories = R.drawable.ic_auto_stories
    val AutoStoriesFilled = R.drawable.ic_auto_stories_filled
    val Book = R.drawable.ic_book
    val BookFilled = R.drawable.ic_book_filled
    val Calculate = R.drawable.ic_calculate
    val CalculateFilled = R.drawable.ic_calculate_filled
    val EditNote = R.drawable.ic_edit_note
    val EditNoteFilled = R.drawable.ic_edit_note_filled
    val LocalLibrary = R.drawable.ic_local_library
    val LocalLibraryFilled = R.drawable.ic_local_library_filled
    val MenuBook = R.drawable.ic_menu_book
    val MenuBookFilled = R.drawable.ic_menu_book_filled
    val School = R.drawable.ic_school
    val SchoolFilled = R.drawable.ic_school_filled
    val Science = R.drawable.ic_science
    val ScienceFilled = R.drawable.ic_science_filled

    // 娱乐 (11)
    val Camera = R.drawable.ic_camera
    val CameraFilled = R.drawable.ic_camera_filled
    val Casino = R.drawable.ic_casino
    val CasinoFilled = R.drawable.ic_casino_filled
    val Festival = R.drawable.ic_festival
    val FestivalFilled = R.drawable.ic_festival_filled
    val Movie = R.drawable.ic_movie
    val MovieFilled = R.drawable.ic_movie_filled
    val MusicNote = R.drawable.ic_music_note
    val MusicNoteFilled = R.drawable.ic_music_note_filled
    val Pool = R.drawable.ic_pool
    val PoolFilled = R.drawable.ic_pool_filled
    val SportsBasketball = R.drawable.ic_sports_basketball
    val SportsBasketballFilled = R.drawable.ic_sports_basketball_filled
    val SportsEsports = R.drawable.ic_sports_esports
    val SportsEsportsFilled = R.drawable.ic_sports_esports_filled
    val SportsSoccer = R.drawable.ic_sports_soccer
    val SportsSoccerFilled = R.drawable.ic_sports_soccer_filled
    val TheaterComedy = R.drawable.ic_theater_comedy
    val TheaterComedyFilled = R.drawable.ic_theater_comedy_filled
    val TravelExplore = R.drawable.ic_travel_explore
    val TravelExploreFilled = R.drawable.ic_travel_explore_filled

    // 其他 (9)
    val ChildCare = R.drawable.ic_child_care
    val ChildCareFilled = R.drawable.ic_child_care_filled
    val DarkMode = R.drawable.ic_dark_mode
    val DarkModeFilled = R.drawable.ic_dark_mode_filled
    val Favorite = R.drawable.ic_favorite
    val FavoriteFilled = R.drawable.ic_favorite_filled
    val Gavel = R.drawable.ic_gavel
    val GavelFilled = R.drawable.ic_gavel_filled
    val MoreHoriz = R.drawable.ic_more_horiz
    val MoreHorizFilled = R.drawable.ic_more_horiz_filled
    val Payments = R.drawable.ic_payments
    val PaymentsFilled = R.drawable.ic_payments_filled
    val Pets = R.drawable.ic_pets
    val PetsFilled = R.drawable.ic_pets_filled
    val Security = R.drawable.ic_security
    val SecurityFilled = R.drawable.ic_security_filled
    val Star = R.drawable.ic_star
    val StarFilled = R.drawable.ic_star_filled

    // 其他 UI 图标
    val Palette = R.drawable.ic_palette
    val PaletteFilled = R.drawable.ic_palette_filled
    val Tune = R.drawable.ic_tune
    val TuneFilled = R.drawable.ic_tune_filled
    val Info = R.drawable.ic_info
    val InfoFilled = R.drawable.ic_info_filled
    val Help = R.drawable.ic_help
    val HelpFilled = R.drawable.ic_help_filled

    // 预算
    val Budget = R.drawable.ic_budget
    val BudgetFilled = R.drawable.ic_budget_filled

    // 归档与展开
    val Archive = R.drawable.ic_archive
    val Unarchive = R.drawable.ic_unarchive
    val ExpandMore = R.drawable.ic_expand_more
    val ExpandLess = R.drawable.ic_expand_less
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
