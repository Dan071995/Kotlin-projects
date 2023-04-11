import Goods_Staff.Good
import Goods_Staff.ListsWithGoods
import Transport_Staff.Truck
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

/** Создадим зону ЗАГРУЗКИ, по сути она представляет несколько корутин (каждая отдельная корутина это порт загрузки),
которые принимают пустые грузовики из 1-ого канала (генератор пустых грузовиков) и добавляют товары в них грузовиков из
общего списка товаров распределительного центра (данный список находится в объекте DistributionCenter). Как только будет
 РАЗГРУЖЕННО N грузовиков И зона РАЗГРУЗКИ завершит свою работу, в зону ЗАГРУЗКИ поступит команда о завершении работы.
 (новые пусты грузовики перестанут поступать в зону ЗАГРУЗКИ). Программа дождется завершения работы ВСЕХ портов ЗАГРУЗКИ
 и, затем, остановится. */

object LoadArea{

    val loadPortJob = Job() // Джобы корутин, отвечающих за остановку программы. Этот Джоб отменяется после того, как
    //Порты разгрузки завершили свою работу. Изменение состояния данного джоба будет служить флагом для портов ЗАГРУЗКИ.
    //Как только порты РАЗГРУЗКИ остановленны, следует остановить порты ЗАГРУЗКИ

    init { }
    suspend fun start() {
        coroutineScope {
            //delay(20000) //Запускаем работу портов ЗАГРУЗКИ через пару секунд после старта работы портов ПОГРУЗКИ - ДЛЯ ПЕРВЫХ ЗАПУСКОВ, ЧТОБЫ НЕ БЫЛО КАШИ В КОНСОЛЕ!!!!!
            val producer2 = generateEmptyTrucks() //запускаем генератор (канал 2) ПУСТЫХ грузовиков
            // (на разгрузку отправляются грузовики только 2-ух типов: Small Truck и Medium Truck)
            //Остановим программу, как только порты ПОГРУЗКИ остановят свою работу.

            launch(context = loadPortJob) {
                while (currentCoroutineContext().job.isActive) {
                    delay(7)
                    if (UnloadArea.unloadPortJob.isCompleted) { //когда порты РАЗГРУЗКИ остановлены, останавливаем порты ПОГРУЗКИ
                        producer2.cancel()
                        DistributionCenter.printProgress("ОЖИДАНИЕ ЗАВЕРЩЕНИЯ ЗАГРУЗКИ ПОСЛЕДНЕГО ГРУЗОВИКА В ПОРТУ")
                    }
                }
            }

            //Запускаем 5 порта ПОГРУЗКИ (5 корутин), которые будут перемещать товар из хранилища центра в грузовик
            repeat(DistributionCenter.loadPortsNum) { createUnloadPort(it, producer2) }
        }
        loadPortJob.complete()
        loadPortJob.cancel()
        val emptyString = " ".padEnd(67, ' ')
        println("\n $emptyString СПИСОК ОСТАВШИХСЯ ТОВАРОВ В ЗОНЕ РАЗГРУЗКИ / ПОГРУЗКИ, ПОСЛЕ ЗАВЕРШЕНИЯ ПОГРУЗКИ: \n${DistributionCenter.unsortedStorageOfGoods}\n")
    }

    /** Функция, создающая поток (канал 2), который будет генерировать ПУСТЫЕ грузовики для ПОГРУЗКИ в порту погрузки */
    private fun CoroutineScope.generateEmptyTrucks() = produce(capacity = 50) {
        while (true) {
            //Генерируем рандомный тип грузовика (грузовик пустой). По заданию ПУСТЫЕ грузовики могут быть только 2-ух типов. Small и Medium.
            var randomEmptyTruc = DistributionCenter.createRandomTruck()
            while (randomEmptyTruc.name == "Hard Truck") randomEmptyTruc = DistributionCenter.createRandomTruck()
            send(randomEmptyTruc) // отправляем пустой грузовик в буфер, оттуда он попадет в свободный порт ЗАГРУЗКИ
            delay(15)
        }
    }

    //Создадим функцию, которая создает порт ЗАГРУЗКИ, принимает и ЗАГРУЖАЕТ грузовик из канала 2:
    private fun CoroutineScope.createUnloadPort(id: Int, channel: ReceiveChannel<Truck>) = launch {

        for (emptyTruck in channel) {
            val goodsCategory = ListsWithGoods.allGoodsList.random() //Определяем тип товаров, которыми будет заполнен грузовик, и получаем список этих товаров
            val category:String = if (goodsCategory[0] is Good.FoodGood) { "FOOD" } else "NO FOOD"
            println("\nPORT #${id+1} ~STARTED~ LOAD THE TRACK WITH: !! $category CATEGORY !!")

            while (!emptyTruck.isFull) { //Занимаем порт ПОГРУЗКИ, пока грузовик не будет заполнен товарами из Распред. Центра.
            delay(10)
            val goodFromStorage = findGoodsOnSortedStorage(goodsCategory) //Отправляем список с товарами в функцию поиска по складу РЦ. Как только товар будет найден, функция вернет его и удалит с хранилища ЦУ
            if (goodFromStorage is Good) { //Если товар найден (не null) то:
                emptyTruck.remainingCapacity -= goodFromStorage.godWeight //уменьшаем вместимость грузовика на величину веса этого товара
                emptyTruck.goodsInTruckBack.add(goodFromStorage) //добавляем товар в кузов.
                delay((goodFromStorage.timeForLoadAndUnload*emptyTruck.timeFactorOfLoadAndUnload).toLong()) //ждем добавления товара в кузов

                //Если вместимость грузовика, после добавления товара, стала отрицательной, то:
                if (emptyTruck.remainingCapacity < 0){
                    emptyTruck.remainingCapacity += goodFromStorage.godWeight //убираем из кузова товар, который вызвал перевес (грузоподъемность стала отрицательной)
                    emptyTruck.goodsInTruckBack.remove(goodFromStorage)
                    DistributionCenter.putElementsInUnsortedStorage(goodFromStorage) //Возвращаем данный товар обратно на склад РЦ
                    //Сортировка товара на складе РЦ производится до тех пор, пока не отменены loadPortJob и unloadPortJob
                    delay(3) //не большая задержка, чтобы товар успели переместить на склад РЦ из зоны выгрузки
                    emptyTruck.isFull = true //сигнализирует о том, что грузовик загружен
                }
              } else { if (UnloadArea.unloadPortJob.isCancelled) emptyTruck.isFull = true }
            //Если порты РАЗГРУЗКИ остановлены, значит склад РЦ больше не будет пополняться товарами. Нужно проверить иметься ли
            //нужные товары на складе РЦ, если имеются -> ничего не делать (продолжить крутить цикл погрузки), если отсутствуют -> отправляем грузовик
            //не польностью загруженным или даже пустым (emptyTruck.isFull = true). Без этого условия грузовик в порту может до бесконечности проверять
            //пустой склад РЦ и ждать пока там появится нужный товар. Логика работы проста: функция findGoodsOnSortedStorage возвращает либо товар со склада
            //либо mull (на складе нет данных товарОВ). В этом случае нужно проверить, а могут ли они там появиться (если разгрузка не завершена). Если
            //разгрузка завершена UnloadArea.unloadPortJob.isCancelled, то товару на складе неоткуда взяться -> можно считать грузовик заполненным
            //if (UnloadArea.unloadPortJob.isCancelled) {emptyTruck.isFull = true}
            }
            println("\nPORT #${id+1} ~FINISHED~ LOAD THE TRACK WITH: !! $category CATEGORY !!")
            emptyTruck.printTrackInfo()
        }
    }

    //Функция принимает список товаров, которыми может быть загружен грузовик, ищет нужный товар на складе РЦ и возвращект его.
    //Если нужных товаров на складе РЦ нет, возвращает null.
    @Synchronized
    private fun findGoodsOnSortedStorage(goodsList: List<Good>): Good? {
        goodsList.forEach {
            when (true){
                (it is Good.FoodGood) -> {
                    DistributionCenter.sortedStorageOfGoods[0].forEach { good ->
                        if (good.godName == it.godName) {DistributionCenter.sortedStorageOfGoods[0].remove(good); return good}
                    }
                }
                (it is Good.SmallSizeGood) -> {
                    DistributionCenter.sortedStorageOfGoods[1].forEach { good ->
                        if (good.godName == it.godName) {DistributionCenter.sortedStorageOfGoods[1].remove(good); return good}
                    }
                }
                (it is Good.MediumSizeGood) -> {
                    DistributionCenter.sortedStorageOfGoods[2].forEach { good ->
                        if (good.godName == it.godName) {DistributionCenter.sortedStorageOfGoods[2].remove(good); return good}
                    }
                }
                else -> {
                    DistributionCenter.sortedStorageOfGoods[3].forEach { good ->
                        if (good.godName == it.godName) {DistributionCenter.sortedStorageOfGoods[3].remove(good); return good}
                    }
                }
            }
        }
        return null
    }

}