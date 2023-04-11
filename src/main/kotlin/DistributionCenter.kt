import Goods_Staff.Good
import Transport_Staff.Truck
import kotlinx.coroutines.*
import java.io.File

/** Создадим распределительный центр. В нем имеется общий список (имитация склада) с товарами и создаются 2 объекта
 - UnloadArea и LoadArea. UnloadArea разгружает полные грузовики (добавляет товары на склад). LoadArea загружает складским
 товаром пустые грузовики. Так же данный объект имеет функцию генерации грузовика*/

object DistributionCenter {

    var unsortedStorageOfGoods =  mutableListOf<Good>() //создаем хранилище куда будут РАЗГРУЖАТЬСЯ товары, Далее они будут сортироваться по типам и категориям
    //создаем хранилище, с отсортированными товарами по типам товаров
    val sortedStorageOfGoods =  listOf<MutableList<Good>>(mutableListOf<Good.FoodGood>().toMutableList(),mutableListOf<Good.SmallSizeGood>().toMutableList(),
        mutableListOf<Good.MediumSizeGood>().toMutableList(),mutableListOf<Good.BigSizeGood>().toMutableList())

    //Выводим вводную информацию
    private val gameRules = printRules()
    //Просим пользователя ввести необходимые параметры
    val unloadPortsNum = insertNumbersOfUnloadPorts()
    val loadPortsNum = insertNumbersOfLoadPorts()
    val trucksNum = insertNumbersOfTrucks()


    init {
        runBlocking {

           launch() {
                UnloadArea.start() //Запускаем зону РАЗГРУЗКИ товаров.
           }

            launch() {
                LoadArea.start() //Запускаем зону ПОГРУЗКМ товаров.
            }

            //Запускаем корутину, сортирующую товар на складе (распред. центре). Сортировка товара по имени происходит только тогда,
            //когда меняется размер списка товаров.
            launch {
                var flag = true //флаг, чтобы вывести информацию 1 раз
                while (currentCoroutineContext().isActive) {
                    delay(3) //ЭТА ЗАДЕРЖКА ОБЯЗАТЕЛЬНА, БЕЗ НЕЕ НЕ РАБОТАЕТ!!!
                    //Если размер списка unsortedStorageOfGoods не равен 0, значит в зону РАЗГРУЗИЛИ поступил хотя бы 1 товар.
                    //Анализируем тип этого товара и добавляем в соответствующую область склада sortedStorageOfGoods
                    if (unsortedStorageOfGoods.size != 0) {
                        //Производим сортировку товаров по категориям
                        unsortedStorageOfGoods.forEach {
                            when (true){
                              (it is Good.FoodGood) -> sortedStorageOfGoods[0].add(it)
                              (it is Good.SmallSizeGood) -> sortedStorageOfGoods[1].add(it)
                              (it is Good.MediumSizeGood) -> sortedStorageOfGoods[2].add(it)
                              else -> sortedStorageOfGoods[3].add(it)
                            }
                        }
                        unsortedStorageOfGoods.clear() //Удаляем товар из зоны разгрузки.
                        sortedStorageOfGoods.forEach { it.sortBy { good -> good.godName  } } //Производим сортировку товаров внутри категории
                    }
                    //После завершения разгрузки выводим информацию о товарах на СКЛАДЕ РЦ (а не в зоне выгрузки)
                    if (UnloadArea.unloadPortJob.isCancelled && flag) {
                        flag = false
                        printSortedStorageInfo("СПИСОК ТОВАРОВ НА СКЛАДЕ РЦ ПОСЛЕ ЗАВЕРШЕНИЯ РАЗГРУЗКИ")
                    }
                    //Когда порты РАЗГРУЗКИ И ПОГРУЗКИ завершили свою работу, отменяем сортировку товара на складе РЦ
                    //Когда мы доходим до этого места, запускается UnloadArea. Так как в ней есть runBlocking последующее выполнение кода приостанавливается
                    //пока не выполнится код в UnloadArea. А, так как после завершения UnloadArea, проверка условия UnloadArea.unloadPortJob.isCancelled вернет true
                    //то и корутина по сортировки товара на складе тоже отменится и sortedStorageOfGoods будет пустой. Можно попробовать изменить скоуп UnloadArea на не блокирующий
                    if (UnloadArea.unloadPortJob.isCancelled && LoadArea.loadPortJob.isCancelled) {currentCoroutineContext().cancel()}
                }
                printSortedStorageInfo("СПИСОК ТОВАРОВ НА СКЛАДЕ РЦ ПОСЛЕ ЗАВЕРШЕНИЯ ПОГРУЗКИ")
            }

        }

    }

    //Данная функция создает рандомный грузовик при обращении
    @Synchronized
    fun createRandomTruck(): Truck {
        val capacity = (400..5000).random()
        val truckName = when (capacity) {
            in 400..1000 -> "small Truck"
            in 1000..2000 -> "Medium Truck"
            else -> "hard Truck"
        }
        return Truck(capacity,truckName)
    }

    //Синхронная функция добавления элементов на склад. Аннотация @Synchronized позволяет избежать одновременного добавления
    //элементов на склад несколькими портами.
    @Synchronized
    fun putElementsInUnsortedStorage(goodsList: Good){
        unsortedStorageOfGoods.add(goodsList) //Добавляем товары на склад
    }

    // Данная функция вызывается тогда, когда в порт разгрузки/погрузки заехал последний грузовик (остановка генератора грузовиков).
    // Она печатает точки пока ВСЕ порты разгрузки/погрузки не будут свободны
    suspend fun printProgress(text:String){
        print(text.toUpperCase())
        while (currentCoroutineContext().job.isActive){
            delay(800)
            print(".")
        }
    }

    //Данная функция выводит информацию о кол-ве товаров на складе РЦ
    @Synchronized
    private fun printSortedStorageInfo(text:String){
        var sortedStorageOfGoodsSize = 0
        sortedStorageOfGoods.forEach { sortedStorageOfGoodsSize += it.size }
        val emptyString = " ".padEnd(80, ' ')
        println("\n $emptyString $text (${sortedStorageOfGoodsSize} шт): \n${sortedStorageOfGoods}\n")
    }

    private fun printRules() {
        val gameRules = File("data/game rules.txt").readText().split("\n").joinToString("\n")
        println(gameRules)
    }

    private fun insertNumbersOfUnloadPorts():Int {
        print("Insert the number of UNLOAD ports ( from 1 to 10 ) = ")
        var n: Int?
        do {
            n = readLine()?.toIntOrNull()
            if (n == null || n < 1 || n > 10) println("Incorrect Value, distribution center CAN HAVE FROM 1 TO 10 UNLOAD PORTS!")
        } while (n == null || n < 1 || n > 10)
        return n
    }

    private fun insertNumbersOfLoadPorts():Int {
        print("Insert the number of LOAD ports ( from 1 to 10 ) = ")
        var n: Int?
        do {
            n = readLine()?.toIntOrNull()
            if (n == null || n < 1 || n > 10) println("Incorrect Value, distribution center CAN HAVE FROM 1 TO 10 LOAD PORTS!")
        } while (n == null || n < 1 || n > 10)
        return n
    }

    private fun insertNumbersOfTrucks():Int {
        print("Enter the number of TRUCKS to be unloaded at the UNLOADING ports ( from 1 to 100 ) = ")
        var n: Int?
        do {
            n = readLine()?.toIntOrNull()
            if (n == null || n < 1 || n > 100) println("Incorrect Value!")
        } while (n == null || n < 1 || n > 100)
        return n
    }

}