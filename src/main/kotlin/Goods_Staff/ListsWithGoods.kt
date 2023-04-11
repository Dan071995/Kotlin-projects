package Goods_Staff

object ListsWithGoods {

    //Создаем 2 списка с товарами. 1 только с продуктами, 2-ой со всем остальным
    private val Food = listOf<Good.FoodGood>(Good.FoodGood("bread"), Good.FoodGood("potato"), Good.FoodGood("milk"))

    private val noFood = listOf(
        Good.BigSizeGood("washing Machine"), Good.BigSizeGood("Fridge"), Good.BigSizeGood("sofa")
        , Good.MediumSizeGood("TV"), Good.MediumSizeGood("table"), Good.MediumSizeGood("microwave"),
        Good.SmallSizeGood("desk Lamp"), Good.SmallSizeGood("chair"), Good.SmallSizeGood("Linen Set")
    )

    val allGoodsList = listOf(Food, noFood)
}