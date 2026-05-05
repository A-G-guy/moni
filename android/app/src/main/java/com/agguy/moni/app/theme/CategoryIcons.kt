package com.agguy.moni.app.theme

import com.agguy.moni.app.icons.MoniIcons

/**
 * 根据图标名称查找对应的未选中态图标资源 ID。
 *
 * 在 [GroupedCategoryIcons] 中扁平化查找，未命中时回退到 [MoniIcons.Category]。
 *
 * @param iconName 图标名称（与数据库中存储的一致）
 * @return 对应的图标资源 ID
 */
fun iconNameToRes(iconName: String): Int {
    return GroupedCategoryIcons
        .asSequence()
        .flatMap { it.icons.asSequence() }
        .firstOrNull { it.name == iconName }
        ?.iconRes
        ?: MoniIcons.Category
}

/**
 * 分类名称到图标资源的映射。
 *
 * 支持中英文名称匹配，未命中时回退到通用分类图标。
 */
fun iconForCategory(name: String): Int = when (name.lowercase()) {
    "restaurant", "餐饮", "食物", "food", "吃饭", "美食" -> MoniIcons.Restaurant
    "transport", "交通", "出行", "打车", "公交", "地铁" -> MoniIcons.DirectionsCar
    "shopping", "购物", "消费", "买买买", "超市" -> MoniIcons.ShoppingBag
    "salary", "工资", "收入", "薪资", "奖金" -> MoniIcons.Payments
    "housing", "住房", "房租", "房贷", "物业" -> MoniIcons.Home
    "entertainment", "娱乐", "游戏", "电影", "休闲" -> MoniIcons.SportsEsports
    "medical", "医疗", "医院", "药品", "健康" -> MoniIcons.LocalHospital
    "education", "教育", "学习", "培训", "学费" -> MoniIcons.School
    "help" -> MoniIcons.Help
    else -> MoniIcons.Category
}

// ============================================================
// 图标分组数据结构
// ============================================================

/**
 * 单个图标的元数据。
 *
 * @property name 图标名称（存入数据库的标识）
 * @property iconRes 未选中态图标资源 ID
 * @property iconResFilled 选中态图标资源 ID
 */
data class CategoryIcon(val name: String, val iconRes: Int, val iconResFilled: Int)

/**
 * 图标分组。
 *
 * @property label 分组显示名称
 * @property icons 该分组下的图标列表
 */
data class IconGroup(val label: String, val icons: List<CategoryIcon>)

/**
 * 所有可供用户选择的分类图标，按主题分组。
 */
val GroupedCategoryIcons: List<IconGroup> = listOf(
    IconGroup(
        label = "饮食",
        icons = listOf(
            CategoryIcon("restaurant", MoniIcons.Restaurant, MoniIcons.RestaurantFilled),
            CategoryIcon("local_cafe", MoniIcons.LocalCafe, MoniIcons.LocalCafeFilled),
            CategoryIcon("bakery_dining", MoniIcons.BakeryDining, MoniIcons.BakeryDiningFilled),
            CategoryIcon("lunch_dining", MoniIcons.LunchDining, MoniIcons.LunchDiningFilled),
            CategoryIcon("dinner_dining", MoniIcons.DinnerDining, MoniIcons.DinnerDiningFilled),
            CategoryIcon("fastfood", MoniIcons.Fastfood, MoniIcons.FastfoodFilled),
            CategoryIcon("local_bar", MoniIcons.LocalBar, MoniIcons.LocalBarFilled),
            CategoryIcon("local_dining", MoniIcons.LocalDining, MoniIcons.LocalDiningFilled),
            CategoryIcon("coffee", MoniIcons.Coffee, MoniIcons.CoffeeFilled),
            CategoryIcon("wine_bar", MoniIcons.WineBar, MoniIcons.WineBarFilled),
            CategoryIcon("icecream", MoniIcons.Icecream, MoniIcons.IcecreamFilled),
            CategoryIcon("cake", MoniIcons.Cake, MoniIcons.CakeFilled),
        )
    ),
    IconGroup(
        label = "交通",
        icons = listOf(
            CategoryIcon("directions_car", MoniIcons.DirectionsCar, MoniIcons.DirectionsCarFilled),
            CategoryIcon("directions_bus", MoniIcons.DirectionsBus, MoniIcons.DirectionsBusFilled),
            CategoryIcon("train", MoniIcons.Train, MoniIcons.TrainFilled),
            CategoryIcon("flight", MoniIcons.Flight, MoniIcons.FlightFilled),
            CategoryIcon("directions_bike", MoniIcons.DirectionsBike, MoniIcons.DirectionsBikeFilled),
            CategoryIcon("directions_boat", MoniIcons.DirectionsBoat, MoniIcons.DirectionsBoatFilled),
            CategoryIcon("local_taxi", MoniIcons.LocalTaxi, MoniIcons.LocalTaxiFilled),
            CategoryIcon("tram", MoniIcons.Tram, MoniIcons.TramFilled),
            CategoryIcon("subway", MoniIcons.Subway, MoniIcons.SubwayFilled),
            CategoryIcon("directions_walk", MoniIcons.DirectionsWalk, MoniIcons.DirectionsWalkFilled),
            CategoryIcon("local_shipping", MoniIcons.LocalShipping, MoniIcons.LocalShippingFilled),
            CategoryIcon("electric_car", MoniIcons.ElectricCar, MoniIcons.ElectricCarFilled),
        )
    ),
    IconGroup(
        label = "购物",
        icons = listOf(
            CategoryIcon("shopping_bag", MoniIcons.ShoppingBag, MoniIcons.ShoppingBagFilled),
            CategoryIcon("shopping_cart", MoniIcons.ShoppingCart, MoniIcons.ShoppingCartFilled),
            CategoryIcon("storefront", MoniIcons.Storefront, MoniIcons.StorefrontFilled),
            CategoryIcon("local_mall", MoniIcons.LocalMall, MoniIcons.LocalMallFilled),
            CategoryIcon("redeem", MoniIcons.Redeem, MoniIcons.RedeemFilled),
            CategoryIcon("sell", MoniIcons.Sell, MoniIcons.SellFilled),
        )
    ),
    IconGroup(
        label = "家居",
        icons = listOf(
            CategoryIcon("home", MoniIcons.Home, MoniIcons.HomeFilled),
            CategoryIcon("chair", MoniIcons.Chair, MoniIcons.ChairFilled),
            CategoryIcon("bed", MoniIcons.Bed, MoniIcons.BedFilled),
            CategoryIcon("cleaning_services", MoniIcons.CleaningServices, MoniIcons.CleaningServicesFilled),
            CategoryIcon("kitchen", MoniIcons.Kitchen, MoniIcons.KitchenFilled),
            CategoryIcon("local_laundry_service", MoniIcons.LocalLaundryService, MoniIcons.LocalLaundryServiceFilled),
            CategoryIcon("water_drop", MoniIcons.WaterDrop, MoniIcons.WaterDropFilled),
            CategoryIcon("lightbulb", MoniIcons.Lightbulb, MoniIcons.LightbulbFilled),
            CategoryIcon("door_front", MoniIcons.DoorFront, MoniIcons.DoorFrontFilled),
            CategoryIcon("yard", MoniIcons.Yard, MoniIcons.YardFilled),
        )
    ),
    IconGroup(
        label = "工作",
        icons = listOf(
            CategoryIcon("work", MoniIcons.Work, MoniIcons.WorkFilled),
            CategoryIcon("computer", MoniIcons.Computer, MoniIcons.ComputerFilled),
            CategoryIcon("business_center", MoniIcons.BusinessCenter, MoniIcons.BusinessCenterFilled),
            CategoryIcon("account_balance", MoniIcons.AccountBalance, MoniIcons.AccountBalanceFilled),
            CategoryIcon("savings", MoniIcons.Savings, MoniIcons.SavingsFilled),
            CategoryIcon("trending_up", MoniIcons.TrendingUp, MoniIcons.TrendingUpFilled),
            CategoryIcon("request_quote", MoniIcons.RequestQuote, MoniIcons.RequestQuoteFilled),
            CategoryIcon("build", MoniIcons.Build, MoniIcons.BuildFilled),
            CategoryIcon("cloud", MoniIcons.Cloud, MoniIcons.CloudFilled),
            CategoryIcon("wifi", MoniIcons.Wifi, MoniIcons.WifiFilled),
        )
    ),
    IconGroup(
        label = "医疗",
        icons = listOf(
            CategoryIcon("local_hospital", MoniIcons.LocalHospital, MoniIcons.LocalHospitalFilled),
            CategoryIcon("medication", MoniIcons.Medication, MoniIcons.MedicationFilled),
            CategoryIcon("fitness_center", MoniIcons.FitnessCenter, MoniIcons.FitnessCenterFilled),
            CategoryIcon("health_and_safety", MoniIcons.HealthAndSafety, MoniIcons.HealthAndSafetyFilled),
            CategoryIcon("vaccines", MoniIcons.Vaccines, MoniIcons.VaccinesFilled),
            CategoryIcon("healing", MoniIcons.Healing, MoniIcons.HealingFilled),
            CategoryIcon("psychology", MoniIcons.Psychology, MoniIcons.PsychologyFilled),
            CategoryIcon("spa", MoniIcons.Spa, MoniIcons.SpaFilled),
        )
    ),
    IconGroup(
        label = "教育",
        icons = listOf(
            CategoryIcon("school", MoniIcons.School, MoniIcons.SchoolFilled),
            CategoryIcon("book", MoniIcons.Book, MoniIcons.BookFilled),
            CategoryIcon("local_library", MoniIcons.LocalLibrary, MoniIcons.LocalLibraryFilled),
            CategoryIcon("menu_book", MoniIcons.MenuBook, MoniIcons.MenuBookFilled),
            CategoryIcon("science", MoniIcons.Science, MoniIcons.ScienceFilled),
            CategoryIcon("calculate", MoniIcons.Calculate, MoniIcons.CalculateFilled),
            CategoryIcon("edit_note", MoniIcons.EditNote, MoniIcons.EditNoteFilled),
            CategoryIcon("auto_stories", MoniIcons.AutoStories, MoniIcons.AutoStoriesFilled),
        )
    ),
    IconGroup(
        label = "娱乐",
        icons = listOf(
            CategoryIcon("sports_esports", MoniIcons.SportsEsports, MoniIcons.SportsEsportsFilled),
            CategoryIcon("movie", MoniIcons.Movie, MoniIcons.MovieFilled),
            CategoryIcon("music_note", MoniIcons.MusicNote, MoniIcons.MusicNoteFilled),
            CategoryIcon("travel_explore", MoniIcons.TravelExplore, MoniIcons.TravelExploreFilled),
            CategoryIcon("sports_basketball", MoniIcons.SportsBasketball, MoniIcons.SportsBasketballFilled),
            CategoryIcon("sports_soccer", MoniIcons.SportsSoccer, MoniIcons.SportsSoccerFilled),
            CategoryIcon("pool", MoniIcons.Pool, MoniIcons.PoolFilled),
            CategoryIcon("casino", MoniIcons.Casino, MoniIcons.CasinoFilled),
            CategoryIcon("theater_comedy", MoniIcons.TheaterComedy, MoniIcons.TheaterComedyFilled),
            CategoryIcon("festival", MoniIcons.Festival, MoniIcons.FestivalFilled),
            CategoryIcon("camera", MoniIcons.Camera, MoniIcons.CameraFilled),
        )
    ),
    IconGroup(
        label = "其他",
        icons = listOf(
            CategoryIcon("more_horiz", MoniIcons.MoreHoriz, MoniIcons.MoreHorizFilled),
            CategoryIcon("favorite", MoniIcons.Favorite, MoniIcons.FavoriteFilled),
            CategoryIcon("star", MoniIcons.Star, MoniIcons.StarFilled),
            CategoryIcon("pets", MoniIcons.Pets, MoniIcons.PetsFilled),
            CategoryIcon("child_care", MoniIcons.ChildCare, MoniIcons.ChildCareFilled),
            CategoryIcon("gavel", MoniIcons.Gavel, MoniIcons.GavelFilled),
            CategoryIcon("security", MoniIcons.Security, MoniIcons.SecurityFilled),
            CategoryIcon("dark_mode", MoniIcons.DarkMode, MoniIcons.DarkModeFilled),
            CategoryIcon("category", MoniIcons.Category, MoniIcons.CategoryFilled),
        )
    ),
)

/**
 * 扁平化的所有可供用户选择的分类图标列表。
 *
 * Pair 的 first 为 iconName（存入数据库），second 为图标资源 ID。
 * 保持向后兼容，新代码优先使用 [GroupedCategoryIcons]。
 */
val AvailableCategoryIcons: List<Pair<String, Int>> =
    GroupedCategoryIcons.flatMap { group ->
        group.icons.map { it.name to it.iconRes }
    }
