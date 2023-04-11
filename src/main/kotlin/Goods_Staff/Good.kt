package Goods_Staff

sealed class Good{
    abstract val godName:String
    abstract val godWeight:Int
    abstract val timeForLoadAndUnload:Int //время загрузки/разгрузки товара
    fun printGoodInfo(){
        println("Goods_Staff.Good name: " + this.godName)
        println("Weight: " + this.godWeight)
        println("Load/Unload time: " + this.timeForLoadAndUnload)
    }
    //Переопределим функцию toString
    override fun toString(): String {
        return godName
    }

    //Создаем тип товаров КРУПНОГАБАРИТНЫЕ ТОВАРЫ
    class BigSizeGood(_name: String) : Good() {

        override val godName = _name
            get() = field.capitalize()
        override val godWeight = (30..50).random()
        override val timeForLoadAndUnload = (180..300).random()

    }

    //Создаем тип товаров СРЕДНЕГАБАРИТНЫЕ ТОВАРЫ
    class MediumSizeGood(_name: String) : Good() {

        override val godName = _name
            get() = field.capitalize()
        override val godWeight = (20..40).random()
        override val timeForLoadAndUnload = (120..240).random()

    }

    //Создаем тип товаров МАЛОГАБАРИТНЫЕ ТОВАРЫ
    class SmallSizeGood(_name: String) : Good() {

        override val godName = _name
            get() = field.capitalize()
        override val godWeight = (10..20).random()
        override val timeForLoadAndUnload = (60..180).random()

    }

    //Создаем тип товаров ПИЩЕВЫЕ ТОВАРЫ
    class FoodGood(_name: String) : Good() {

        override val godName = _name
            get() = field.capitalize()
        override val godWeight = (5..15).random()
        override val timeForLoadAndUnload = 60

    }
}
