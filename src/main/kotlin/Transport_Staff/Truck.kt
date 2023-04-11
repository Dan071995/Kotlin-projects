package Transport_Staff

import Goods_Staff.Good

class Truck(_capacity:Int, _name:String): Transport() {
    //Проверка грузоподъемности. Если создается грузовик с маленькой или очень большой грузоподъемностью, значения меняются
    //на минимально или максимально допустимые.

    override var capacity = _capacity
    var unloadTime:Double = 0.0 //Время, необходимое для разгрузки всего кузова с товарами

    init {
        if(_capacity <= 400){
            println("Min truck capacity is 400 kg")
            capacity = 400
        }
        if(_capacity >=20000){
            println("Max truck capacity is 20 000 kg")
            capacity = 20000
        }
    }

    val name = _name
        get() = field.capitalize()

    override val timeFactorOfLoadAndUnload = when(capacity){
        in 400..1000 -> 1.0
        in 1000..2000 -> 1.1
        in 2000..3000 -> 1.2
        in 4000..5000 -> 1.3
        in 5000..10000 -> 1.4
        else -> 1.5
    }
    //Создаем переменную, хранящую товары в кузове грузовика и функцию, которая принимает список товара и заполняет им кузов
    val goodsInTruckBack = mutableListOf<Good>()
    var remainingCapacity = capacity //данное поле хранит оставшуюся вместимость грузовика после загрузки товара.
    //В зависимости от прешедшего списка (пищевая категория или не пищевая) идет заполнение кузова. Если пришел список с продуктами,
    //то в таком грузовике могут находиться ТОЛЬКО продукты. Если пришел список с техникой и прочим, то грузовик заполняется рандомными
    //товарами из этого списка. Кол-во товаров в каждом грузовике рандомно, и общий вес НЕ может превышать грузоподъемность грузовика
    //так же, с определенной вероятностью создаются пустые грузовики (без товара), как и требовалось в задании
    var isFull:Boolean = false //Флаг, сигнализирующий, что грузовик ранее заполнялся товаром. Если значение true, то грузовик загружен.
    //Нужно для того, чтобы для одного и того-же экземпляра класса нельзя было вызвать функцию загрузки кузова дважды.
    private val goodsValue = (0..100).random() //Макс. Кол-во товара в кузове грузовика

    fun fullTrackWithGoods(goodsList:List<Good>){
        if (isFull) {println("This truck has been already loaded!"); return}
        var i = 0
        while (i != goodsValue || remainingCapacity < 0 || isFull){
            i++
            val randomItem = (0..goodsList.lastIndex).random() //получаем рандомный индекс товара
            remainingCapacity -= goodsList[randomItem].godWeight //уменьшаем вместимость грузовика на величину веса этого товара
            goodsInTruckBack.add(goodsList[randomItem]) //добавляем товар в кузов.

            if (remainingCapacity < 0){
                remainingCapacity += goodsList[randomItem].godWeight //убираем из кузова товар, который вызвал перевес (грузоподъемность стала отрицательной)
                goodsInTruckBack.remove(goodsList[randomItem])
                //далее во всем списке товаров, попробуем найти товар, который весит меньше пред идущего и, теоретически, поместится в грузовик
                for (k in 0..goodsList.lastIndex){
                    if (goodsList[k].godWeight <= remainingCapacity) { remainingCapacity -= goodsList[k].godWeight; goodsInTruckBack.add(goodsList[k]); break }
                }
                 }
        }
        goodsInTruckBack.sortBy {it.godName} //сортируем товары в кузове по имени товара (в задании просили отсортировать товары)
        //Сразу вычислим время, требующееся для разгрузки товаров
        goodsInTruckBack.forEach {
            unloadTime += (it.timeForLoadAndUnload * timeFactorOfLoadAndUnload) }
        isFull = true //сигнализирует о том, что грузовик загружен
    }

    fun printTrackInfo(){
        println("Truck Type: " + this.name)
        println("Max capacity: " + this.capacity)
        println("Remaining capacity: " + this.remainingCapacity + " kg")
        println("Total weight of goods: " + (this.capacity - this.remainingCapacity))
        println("Number of items in the trunk: " + this.goodsInTruckBack.size + " pie")
        println("Goods List: $goodsInTruckBack")
        println("Load/Unload Time coefficient: " + this.timeFactorOfLoadAndUnload)
        println("UNLOAD TIME: " + "%.1f".format( (unloadTime / 1000) ) + " sec")
    }
}